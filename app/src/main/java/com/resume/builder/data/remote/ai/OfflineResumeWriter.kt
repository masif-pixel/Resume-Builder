package com.resume.builder.data.remote.ai

import com.resume.builder.domain.model.AiResumeRequest
import com.resume.builder.domain.model.ContactInfo
import com.resume.builder.domain.model.CoverLetterLength
import com.resume.builder.domain.model.CoverLetterRequest
import com.resume.builder.domain.model.CoverLetterTone
import com.resume.builder.domain.model.ImproveKind
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionItem
import com.resume.builder.domain.model.SectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Year

/**
 * Rule-based writing assistant that runs entirely on-device.
 *
 * It pulls the name, jobs, education, skills and languages out of free text with patterns and fills
 * phrase templates, so the AI screens still produce a solid, editable draft without any model or network.
 * Output is English regardless of the requested language.
 */
object OfflineResumeWriter {

    private const val MIN_WORK_MS = 900L
    private const val MAX_SKILLS = 15

    suspend fun generateResume(request: AiResumeRequest): Pair<String, ResumeContent> = work { buildResume(request) }

    suspend fun generateCoverLetter(request: CoverLetterRequest): String = work { buildCoverLetter(request) }

    suspend fun improveText(text: String, kind: ImproveKind, context: String): String = work { improve(text, kind, context) }

    // A short minimum duration keeps the progress dialog from flashing on and off.
    private suspend fun <T> work(block: () -> T): T = withContext(Dispatchers.Default) {
        delay(MIN_WORK_MS)
        block()
    }

    // region Resume

    internal fun buildResume(request: AiResumeRequest): Pair<String, ResumeContent> {
        val background = request.background.trim()
        val target = request.targetJob.trim()
        val structured = ResumeTextParser.parse(background).second
        val base = if (structured.itemsOf(SectionType.EXPERIENCE).isNotEmpty() || structured.itemsOf(SectionType.EDUCATION).isNotEmpty()) {
            structured
        } else {
            parseFreeText(background)
        }
        val experience = base.itemsOf(SectionType.EXPERIENCE)
        val skills = base.itemsOf(SectionType.SKILLS).sortedByDescending { target.contains(it.title, ignoreCase = true) }
        val targetRole = targetRole(target)
        val role = targetRole.ifBlank { base.contact.jobTitle }.ifBlank { experience.firstOrNull()?.title.orEmpty() }
        val summary = base.summary.ifBlank {
            summary(role, experience, base.itemsOf(SectionType.EDUCATION), skills, targetRole, seed = background.hashCode() and Int.MAX_VALUE)
        }
        val content = base.copy(
            contact = base.contact.copy(jobTitle = role),
            summary = summary,
            sections = base.sections.map { if (it.type == SectionType.SKILLS) it.copy(items = skills) else it },
        )
        val title = role.ifBlank { base.contact.fullName.takeIf { it.isNotBlank() }?.let { "$it Resume" } ?: "My Resume" }
        return title to content
    }

    private fun parseFreeText(text: String): ResumeContent {
        val sentences = text.split(sentenceBreak).map { it.trim() }.filter { it.isNotEmpty() }
        val experience = mutableListOf<SectionItem>()
        val education = mutableListOf<SectionItem>()
        sentences
            .filterNot { skillsHeading.containsMatchIn(it) || languageCue.containsMatchIn(it) }
            .flatMap { sentence -> sentence.split(clauseBreak) }
            .map { it.trim(' ', ',', '.', ';') }
            .filter { it.isNotEmpty() }
            .forEach { clause ->
                when {
                    degree.containsMatchIn(clause) -> educationFrom(clause)?.let(education::add)
                    companyAt.containsMatchIn(clause) && workCue.containsMatchIn(clause) -> experienceFrom(clause)?.let(experience::add)
                    studyCue.containsMatchIn(clause) -> educationFrom(clause)?.let(education::add)
                    experience.isNotEmpty() -> {
                        // Achievements in a later sentence belong to the job mentioned just before them.
                        val bullets = fragments(clause)
                        if (bullets.isNotEmpty()) {
                            val last = experience.last()
                            val lines = last.description.lines().filter { it.isNotBlank() } + bullets.map { "• $it" }
                            experience[experience.lastIndex] = last.copy(description = lines.joinToString("\n"))
                        }
                    }
                }
            }

        val nameMatch = nameLead.find(text)
        val name = nameMatch?.groupValues?.get(1).orEmpty()
        val locationScope = sentences.firstOrNull { name.isNotEmpty() && it.contains(name) } ?: sentences.firstOrNull().orEmpty()
        val contact = ContactInfo(
            fullName = name,
            email = ResumeTextParser.email.find(text)?.value.orEmpty(),
            phone = ResumeTextParser.phone.findAll(text).map { it.value.trim() }.firstOrNull { it.count(Char::isDigit) >= 10 }.orEmpty(),
            location = locationLead.find(locationScope)?.groupValues?.get(1).orEmpty(),
            linkedin = ResumeTextParser.linkedin.find(text)?.value.orEmpty(),
            github = ResumeTextParser.github.find(text)?.value.orEmpty(),
        )
        val latestFirst = experience.sortedWith(
            compareByDescending<SectionItem> { it.isCurrent }.thenByDescending { yearOf(it.startDate) ?: 0 },
        )
        val sections = ResumeSection.defaults().map { section ->
            when (section.type) {
                SectionType.EXPERIENCE -> section.copy(items = latestFirst)
                SectionType.EDUCATION -> section.copy(items = education)
                SectionType.SKILLS -> section.copy(items = skillsIn(text))
                SectionType.LANGUAGES -> section.copy(items = languagesIn(sentences))
                else -> section
            }
        }
        return ResumeContent(contact = contact, sections = sections)
    }

    private fun experienceFrom(clause: String): SectionItem? {
        val company = companyAt.find(clause) ?: return null
        val role = roleAs.find(clause) ?: roleBeforeAt.find(clause)
        val dates = datesIn(clause)
        var rest = clause.replace(company.value, " ")
        role?.let { rest = rest.replace(it.value, " ") }
        dates.matched?.let { rest = rest.replace(it, " ") }
        return SectionItem(
            title = role?.groupValues?.get(1)?.let(::titleCase).orEmpty(),
            subtitle = company.groupValues[1].trim(),
            startDate = dates.start,
            endDate = dates.end,
            isCurrent = dates.current,
            description = fragments(rest).joinToString("\n") { "• $it" },
        )
    }

    private fun educationFrom(clause: String): SectionItem? {
        val degreeName = degree.find(clause)?.value?.trim(' ', ',', '.')
        val school = schoolAt.find(clause)?.groupValues?.get(1)
            ?: institution.find(clause)?.value
            ?: schoolFrom.find(clause)?.groupValues?.get(1)
        if (degreeName == null && school == null) return null
        val dates = datesIn(clause)
        return SectionItem(
            title = degreeName.orEmpty(),
            subtitle = school?.trim().orEmpty(),
            startDate = dates.start,
            endDate = dates.end,
            isCurrent = dates.current,
        )
    }

    /** Splits leftover clause text into achievement bullets, keeping only verb-led fragments. */
    private fun fragments(text: String): List<String> =
        text.replace(Regex("\\s+"), " ").split(fragmentBreak).mapNotNull { bulletFrom(it) }

    private fun bulletFrom(fragment: String): String? {
        var words = fragment.trim(' ', '.', ',', ';', ':', '•', '-', '*', '(', ')').split(' ').filter { it.isNotBlank() }
        while (words.isNotEmpty() && words.first().lowercase() in fillers) words = words.drop(1)
        if (words.size >= 2 && words[0].lowercase() in setOf("worked", "working", "work") && pastOf(words[1]) != null) words = words.drop(1)
        if (words.size < 2) return null
        val lead = pastOf(words.first()) ?: if (words.first().equals("on", ignoreCase = true) && words.size >= 3) "worked on" else return null
        return (listOf(lead) + words.drop(1)).joinToString(" ").replaceFirstChar { it.uppercase() }
    }

    private fun skillsIn(text: String): List<SectionItem> {
        val listed = skillsLead.findAll(text)
            .flatMap { it.groupValues[1].split(listSplit) }
            .map { it.trim().removePrefix("some ").removePrefix("strong ").trim(' ', '.', ':') }
            .filter { it.length in 2..30 && it.split(' ').size <= 4 }
            .toList()
        val known = knownSkillPatterns.filter { (_, pattern) -> pattern.containsMatchIn(text) }.keys
        return (listed + known)
            .map { it.replaceFirstChar(Char::uppercase) }
            .filterNot { skill -> languageNames.any { it.equals(skill, ignoreCase = true) } }
            .distinctBy { it.lowercase() }
            .take(MAX_SKILLS)
            .map { SectionItem(title = it) }
    }

    private fun languagesIn(sentences: List<String>): List<SectionItem> = sentences
        .filter { languageCue.containsMatchIn(it) }
        .flatMap { sentence ->
            languageNames.mapNotNull { language ->
                val match = Regex("\\b$language\\b", RegexOption.IGNORE_CASE).find(sentence) ?: return@mapNotNull null
                val before = sentence.substring(maxOf(0, match.range.first - 24), match.range.first).lowercase().split(Regex(",| and ")).last()
                val after = Regex("^\\s*\\((\\w+)").find(sentence.substring(match.range.last + 1))?.groupValues?.get(1).orEmpty().lowercase()
                val cue = "$before $after"
                val level = when {
                    "native" in cue || "mother tongue" in cue -> 5
                    "fluent" in cue || "bilingual" in cue -> 4
                    "professional" in cue || "business" in cue || "conversational" in cue || "intermediate" in cue -> 3
                    "some" in cue || "basic" in cue || "little" in cue || "bit of" in cue || "beginner" in cue -> 2
                    else -> 0
                }
                match.range.first to SectionItem(title = language, level = level)
            }.sortedBy { it.first }.map { it.second }
        }
        .distinctBy { it.title }

    private fun summary(
        role: String,
        experience: List<SectionItem>,
        education: List<SectionItem>,
        skills: List<SectionItem>,
        targetRole: String,
        seed: Int,
    ): String {
        fun pick(salt: Int, options: List<String>) = options[(seed + salt) % options.size]
        val label = role.ifBlank { "professional" }
        val years = yearsOfExperience(experience)
        val companies = experience.map { it.subtitle }.filter { it.isNotBlank() }.distinct().take(2)
        val atCompanies = if (companies.isEmpty()) "" else " at ${natural(companies)}"
        val sentences = mutableListOf<String>()

        sentences += when {
            years > 0 -> pick(
                0,
                listOf(
                    "$label with $years+ years of experience$atCompanies.",
                    "Results-driven $label bringing $years+ years of hands-on experience$atCompanies.",
                    "Dedicated $label with a proven track record over $years+ years$atCompanies.",
                ),
            )
            experience.isNotEmpty() -> "$label with hands-on experience$atCompanies."
            education.isNotEmpty() -> {
                val edu = education.first()
                val from = edu.subtitle.takeIf { it.isNotBlank() }?.let { " from $it" }.orEmpty()
                "Motivated ${edu.title.ifBlank { "university" }} graduate$from, ready to grow as ${article(label)}."
            }
            else -> "Motivated $label eager to learn, contribute and grow."
        }.replaceFirstChar { it.uppercase() }

        val skillNames = skills.take(4).map { it.title }
        if (skillNames.isNotEmpty()) {
            val list = natural(skillNames)
            sentences += pick(1, listOf("Skilled in $list.", "Strong command of $list.", "Hands-on expertise with $list."))
        }

        experience.firstOrNull()?.let { job ->
            val highlight = job.description.lines()
                .map { it.trim().removePrefix("•").trim().trimEnd('.') }
                .filter { it.isNotBlank() && pastOf(it.substringBefore(' ')) != null }
                .sortedBy { it.startsWith("Worked", ignoreCase = true) }
                .firstOrNull()
            if (highlight != null) {
                val at = job.subtitle.takeIf { it.isNotBlank() }?.let { " at $it" }.orEmpty()
                sentences += "Most recently ${lowerFirst(highlight)}$at."
            }
        }

        sentences += if (targetRole.isNotBlank()) {
            pick(2, listOf("Eager to bring this experience to ${article(targetRole)} role.", "Now looking to make an impact as ${article(targetRole)}."))
        } else {
            pick(2, listOf("Known for clear communication, ownership and delivering quality work on time.", "Brings a collaborative mindset, attention to detail and a focus on results."))
        }
        return sentences.joinToString(" ")
    }

    private fun targetRole(target: String): String {
        if (target.isBlank()) return ""
        val firstLine = target.lines().first { it.isNotBlank() }.trim().trim(':', '-', '.', ' ')
        val isHeader = firstLine.lowercase() in setOf("about the job", "job description", "about us", "overview", "responsibilities", "requirements", "we're hiring", "we are hiring")
        if (!isHeader && firstLine.length <= 50 && firstLine.split(' ').size <= 6 && !firstLine.contains(Regex("[.!?,]"))) return titleCase(firstLine)
        return hiring.find(target)?.groupValues?.get(1)?.let(::titleCase).orEmpty()
    }

    // endregion

    // region Cover letter

    internal fun buildCoverLetter(request: CoverLetterRequest): String {
        val content = request.resume.content
        val experience = content.itemsOf(SectionType.EXPERIENCE)
        val education = content.itemsOf(SectionType.EDUCATION)
        val latest = experience.firstOrNull()
        val role = request.jobTitle.trim().ifBlank { content.contact.jobTitle }.ifBlank { "open" }
        val company = request.companyName.trim()
        val companyRef = company.ifBlank { "your company" }
        val atCompany = if (company.isBlank()) "" else " at $company"
        val skills = content.itemsOf(SectionType.SKILLS).map { it.title }
        val matched = skills.filter { request.jobDescription.contains(it, ignoreCase = true) }
        val featured = matched.ifEmpty { skills }.take(4)
        val years = yearsOfExperience(experience)
        val highlights = (latest?.description.orEmpty().lines() + experience.drop(1).flatMap { it.description.lines() })
            .map { strengthen(it) }
            .filter { it.split(' ').size >= 3 }
            .sortedBy { it.startsWith("Contributed", ignoreCase = true) || it.startsWith("Worked", ignoreCase = true) }
            .take(2)

        val paragraphs = mutableListOf<String>()
        paragraphs += buildList {
            add(opening(request.tone, role, atCompany))
            when {
                latest != null -> {
                    val asRole = latest.title.takeIf { it.isNotBlank() }?.let { " as ${article(it)}" }.orEmpty()
                    val at = latest.subtitle.takeIf { it.isNotBlank() }?.let { " at $it" }.orEmpty()
                    add(if (years > 0) "I bring $years+ years of experience, most recently$asRole$at." else "Most recently, I worked$asRole$at.")
                }
                education.isNotEmpty() -> {
                    val edu = education.first()
                    val at = edu.subtitle.takeIf { it.isNotBlank() }?.let { " at $it" }.orEmpty()
                    add("I recently completed my ${edu.title.ifBlank { "studies" }}$at and I'm ready to put what I've learned into practice.")
                }
                else -> add("I'm eager to bring my skills, energy and work ethic to your team.")
            }
            if (featured.isNotEmpty()) add("My core strengths include ${natural(featured)}.")
        }.joinToString(" ")

        when {
            highlights.isNotEmpty() -> {
                val where = latest?.subtitle?.takeIf { it.isNotBlank() }?.let { "At $it, " } ?: "In my recent work, "
                val first = highlights.first()
                val body = if (pastOf(first.substringBefore(' ')) != null) {
                    "I " + highlights.joinToString(" and ") { lowerFirst(it) }
                } else {
                    "my work included " + highlights.joinToString(" and ") { lowerFirst(it) }
                }
                paragraphs += "$where$body. These results reflect how I work: taking ownership, staying focused on outcomes and collaborating closely with the people around me."
            }
            content.summary.isNotBlank() -> paragraphs += content.summary.trim()
        }

        if (request.length != CoverLetterLength.SHORT) {
            paragraphs += if (matched.isNotEmpty()) {
                "Your posting's focus on ${natural(matched.take(3))} lines up closely with the work I do every day, and I'm excited about the chance to apply it at $companyRef."
            } else {
                "I'm drawn to $companyRef because of the opportunity to take on meaningful challenges, learn from a strong team and deliver work that makes a real difference."
            }
        }

        if (request.length == CoverLetterLength.LONG) {
            val edu = education.firstOrNull()
            val languages = content.itemsOf(SectionType.LANGUAGES).map { it.title }
            paragraphs += buildList {
                if (edu != null) {
                    val from = edu.subtitle.takeIf { it.isNotBlank() }?.let { " from $it" }.orEmpty()
                    add("My ${edu.title.ifBlank { "education" }}$from gave me a strong foundation that I continue to build on.")
                }
                if (languages.size > 1) add("I also communicate comfortably in ${natural(languages)}.")
                add("I take ownership of my work, communicate clearly and enjoy collaborating across teams to get things done.")
            }.joinToString(" ")
        }

        paragraphs += closing(request.tone, companyRef)
        val greeting = "Dear ${request.hiringManager.trim().ifBlank { "Hiring Manager" }},"
        val name = content.contact.fullName.ifBlank { "[Your Name]" }
        return (listOf(greeting) + paragraphs + "${signOff(request.tone)}\n$name").joinToString("\n\n")
    }

    private fun opening(tone: CoverLetterTone, role: String, atCompany: String) = when (tone) {
        CoverLetterTone.PROFESSIONAL -> "I am writing to apply for the $role position$atCompany."
        CoverLetterTone.CONFIDENT -> "I'm excited to apply for the $role role$atCompany, and I'm confident I can make an immediate impact."
        CoverLetterTone.FRIENDLY -> "I was really happy to come across the $role opening$atCompany, and I'd love to be considered."
        CoverLetterTone.ENTHUSIASTIC -> "I am thrilled to apply for the $role position$atCompany!"
        CoverLetterTone.FORMAL -> "Please accept this letter as my formal application for the $role position$atCompany."
    }

    private fun closing(tone: CoverLetterTone, companyRef: String) = when (tone) {
        CoverLetterTone.PROFESSIONAL -> "Thank you for considering my application. I would welcome the opportunity to discuss how I can contribute to $companyRef."
        CoverLetterTone.CONFIDENT -> "I'd welcome a conversation about how I can help $companyRef reach its goals. Thank you for your time."
        CoverLetterTone.FRIENDLY -> "Thanks so much for your time. I'd love to chat about how I could help the team."
        CoverLetterTone.ENTHUSIASTIC -> "I would love the chance to discuss how I can contribute to $companyRef. Thank you for your consideration!"
        CoverLetterTone.FORMAL -> "Thank you for your time and consideration. I look forward to the opportunity to discuss my application further."
    }

    private fun signOff(tone: CoverLetterTone) = when (tone) {
        CoverLetterTone.PROFESSIONAL, CoverLetterTone.FORMAL -> "Sincerely,"
        CoverLetterTone.CONFIDENT -> "Best regards,"
        CoverLetterTone.FRIENDLY -> "Warm regards,"
        CoverLetterTone.ENTHUSIASTIC -> "Kind regards,"
    }

    // endregion

    // region Improve

    internal fun improve(text: String, kind: ImproveKind, context: String): String {
        val role = Regex("Target role: ([^.]+)").find(context)?.groupValues?.get(1)?.trim().orEmpty()
        return when (kind) {
            ImproveKind.SUMMARY -> if (text.isBlank()) draftSummary(role) else ensurePeriod(fixSkillCasing(polishProse(stripFirstPerson(polishProse(text)))))
            ImproveKind.BULLETS -> if (text.isBlank()) {
                PLACEHOLDER_BULLETS
            } else {
                text.split(bulletBreak).map { strengthen(it) }.filter { it.isNotBlank() }.joinToString("\n") { "• $it" }
            }
            ImproveKind.COVER_LETTER -> text.trim().split(Regex("\\n\\s*\\n")).joinToString("\n\n") { paragraph ->
                paragraph.lines().joinToString("\n") { polishProse(it) }
            }
        }
    }

    private fun draftSummary(role: String): String {
        val label = role.ifBlank { "professional" }
        return "Motivated $label with a strong focus on quality, collaboration and continuous learning. " +
            "Skilled at turning goals into measurable results and adapting quickly to new challenges. " +
            "Ready to bring energy and ownership to a growing team."
    }

    /** Turns a responsibility line into an action-verb achievement line (without the bullet). */
    private fun strengthen(line: String): String {
        var s = line.trim().trimStart('•', '-', '*', '·', ' ').trim().trimEnd('.', ';', ',')
        if (s.isEmpty()) return ""
        var needsVerb = false
        weakLeads.forEach { (pattern, replacement) ->
            if (pattern.containsMatchIn(s)) {
                s = pattern.replace(s, replacement)
                if (replacement.isEmpty()) needsVerb = true
            }
        }
        val words = s.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return ""
        s = when {
            pastOf(words.first()) != null -> (listOf(pastOf(words.first())!!) + words.drop(1)).joinToString(" ")
            needsVerb -> "managed ${words.joinToString(" ")}"
            else -> words.joinToString(" ")
        }
        return polishProse(s).replaceFirstChar { it.uppercase() }
    }

    private fun polishProse(text: String): String {
        var s = text.replace(Regex("[ \\t]+"), " ").trim()
        proseFixes.forEach { (pattern, replacement) -> s = pattern.replace(s, replacement) }
        s = s.replace(Regex(" +([,.!?;:])"), "\$1")
        return sentenceStart.replace(s) { it.groupValues[1] + it.groupValues[2].uppercase() }
    }

    /** "kotlin" → "Kotlin", "firebase" → "Firebase" for skills whose spelling is unambiguous. */
    private fun fixSkillCasing(text: String) = knownSkillPatterns
        .filterKeys { it !in caseSensitiveSkills }
        .entries.fold(text) { acc, (skill, pattern) -> pattern.replace(acc, Regex.escapeReplacement(skill)) }

    private fun stripFirstPerson(text: String) = firstPersonLead.replace(text.trim()) { it.groupValues[1] }

    private fun ensurePeriod(text: String) = if (text.isEmpty() || text.last() in ".!?") text else "$text."

    private const val PLACEHOLDER_BULLETS =
        "• Delivered [key project or task] that improved [result] by [X%]\n" +
            "• Collaborated with [team or stakeholders] to [goal]\n" +
            "• Streamlined [process], saving [time or cost]"

    // endregion

    // region Helpers

    private fun ResumeContent.itemsOf(type: SectionType): List<SectionItem> = section(type)?.items.orEmpty()

    private data class Dates(val start: String = "", val end: String = "", val current: Boolean = false, val matched: String? = null)

    private fun datesIn(text: String): Dates {
        yearRange.find(text)?.let { match ->
            val end = match.groupValues[2]
            val current = !end.first().isDigit()
            return Dates(match.groupValues[1], if (current) "" else end, current, match.value)
        }
        sinceYear.find(text)?.let { return Dates(start = it.groupValues[1], current = true, matched = it.value) }
        singleYear.find(text)?.let { return Dates(start = it.groupValues[1], matched = it.value) }
        return Dates()
    }

    private fun yearOf(text: String): Int? = Regex("(19|20)\\d{2}").find(text)?.value?.toInt()

    private fun yearsOfExperience(experience: List<SectionItem>): Int {
        val first = experience.mapNotNull { yearOf(it.startDate) }.minOrNull() ?: return 0
        return (Year.now().value - first).coerceAtLeast(0)
    }

    private fun pastOf(word: String): String? = pastByForm[word.lowercase().trim(',', '.', ';', ':')]

    private val minorWords = setOf("of", "and", "in", "for", "the", "to", "at", "&")

    private fun titleCase(text: String) = text.trim().split(Regex("\\s+")).mapIndexed { index, word ->
        when {
            word.any { it.isUpperCase() } -> word
            index > 0 && word in minorWords -> word
            else -> word.replaceFirstChar { it.uppercase() }
        }
    }.joinToString(" ")

    private fun natural(items: List<String>) = when (items.size) {
        0 -> ""
        1 -> items.first()
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }

    private fun article(word: String) = if (word.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "an $word" else "a $word"

    private fun lowerFirst(text: String) =
        if (text.length > 1 && text[1].isUpperCase()) text else text.replaceFirstChar { it.lowercase() }

    // endregion

    // region Vocabulary & patterns

    /** base past gerund */
    private const val VERBS =
        "achieve achieved achieving, analyze analyzed analyzing, automate automated automating, boost boosted boosting, " +
            "build built building, coach coached coaching, collaborate collaborated collaborating, conduct conducted conducting, " +
            "coordinate coordinated coordinating, create created creating, cut cut cutting, deliver delivered delivering, " +
            "design designed designing, develop developed developing, drive drove driving, enhance enhanced enhancing, " +
            "establish established establishing, expand expanded expanding, grow grew growing, handle handled handling, " +
            "implement implemented implementing, improve improved improving, increase increased increasing, launch launched launching, " +
            "lead led leading, maintain maintained maintaining, manage managed managing, mentor mentored mentoring, " +
            "migrate migrated migrating, optimize optimized optimizing, organize organized organizing, oversee oversaw overseeing, " +
            "prepare prepared preparing, reduce reduced reducing, redesign redesigned redesigning, resolve resolved resolving, " +
            "run ran running, sell sold selling, serve served serving, ship shipped shipping, streamline streamlined streamlining, " +
            "supervise supervised supervising, teach taught teaching, train trained training, write wrote writing, make made making, " +
            "deploy deployed deploying, integrate integrated integrating, refactor refactored refactoring, negotiate negotiated negotiating, " +
            "generate generated generating, translate translated translating, revamp revamped revamping, scale scaled scaling, " +
            "secure secured securing, win won winning, help helped helping, assist assisted assisting, contribute contributed contributing, " +
            "monitor monitored monitoring, review reviewed reviewing, introduce introduced introducing, spearhead spearheaded spearheading, " +
            "architect architected architecting, lower lowered lowering, raise raised raising, cofound cofounded cofounding, " +
            "found founded founding, recruit recruited recruiting, hire hired hiring, save saved saving, set set setting"

    private val pastByForm: Map<String, String> = buildMap {
        VERBS.split(Regex("\\s*,\\s*")).map { it.split(' ') }.forEach { (base, past, gerund) ->
            put(base, past)
            put(past, past)
            put(gerund, past)
        }
        // Past-only: their base forms are too often nouns at the start of a line.
        listOf("supported", "tested", "owned", "researched", "planned", "documented", "presented", "worked").forEach { put(it, it) }
    }

    private val fillers = setOf("i", "also", "then", "and", "where", "who", "which", "while", "have", "has", "had", "successfully")

    private val languageNames = listOf(
        "English", "Urdu", "Punjabi", "Sindhi", "Pashto", "Balochi", "Saraiki", "Hindi", "Arabic", "French", "Spanish",
        "German", "Chinese", "Mandarin", "Turkish", "Persian", "Farsi", "Bengali", "Portuguese", "Italian", "Russian",
        "Japanese", "Korean", "Malay", "Indonesian", "Dutch", "Swahili", "Tamil", "Telugu",
    )

    private val knownSkills = listOf(
        "Kotlin", "Java", "Swift", "Python", "JavaScript", "TypeScript", "React", "React Native", "Angular", "Vue", "Node.js",
        "Flutter", "Dart", "Android", "iOS", "Jetpack Compose", "SQL", "MySQL", "PostgreSQL", "MongoDB", "Firebase", "AWS",
        "Azure", "Google Cloud", "Docker", "Kubernetes", "Git", "GraphQL", "REST APIs", "HTML", "CSS", "PHP", "Laravel",
        "Django", "Spring Boot", ".NET", "C#", "C++", "Rust", "Figma", "Photoshop", "Illustrator", "UI/UX Design",
        "Machine Learning", "Data Analysis", "Excel", "Power BI", "Tableau", "SEO", "Digital Marketing", "Social Media Marketing",
        "Content Writing", "Copywriting", "Project Management", "Agile", "Scrum", "Jira", "Salesforce", "SAP", "QuickBooks",
        "Accounting", "Bookkeeping", "Customer Service", "Sales", "Negotiation", "Leadership", "Team Management",
        "Communication", "Public Speaking", "Teaching", "Recruitment", "Microsoft Office", "PowerPoint", "AutoCAD",
        "Graphic Design", "Video Editing", "Photography", "Linux", "Networking", "Cybersecurity", "Testing", "Unit Testing",
        "CI/CD", "Mentoring", "Problem Solving", "Time Management", "Research", "Nursing", "Patient Care", "Data Entry",
        "Logistics", "Supply Chain", "Inventory Management", "Budgeting", "Financial Analysis", "Marketing", "WordPress",
        "Shopify", "Blender", "Unity", "MATLAB",
    )

    /** Everyday words that only count as a skill when written with a capital letter. */
    private val caseSensitiveSkills = setOf("Swift", "Rust", "Dart", "Excel", "Unity", "Sales", "Research", "Testing", "Communication", "Leadership", "Teaching", "Networking", "Marketing")

    private val knownSkillPatterns: Map<String, Regex> by lazy {
        knownSkills.associateWith { skill ->
            val options = if (skill in caseSensitiveSkills) emptySet() else setOf(RegexOption.IGNORE_CASE)
            Regex("(?<![A-Za-z0-9])" + Regex.escape(skill) + "(?![A-Za-z0-9+#])", options)
        }
    }

    private val verbAlternation = pastByForm.keys.sortedByDescending { it.length }.joinToString("|")

    private val sentenceBreak = Regex("(?<=[.!?])\\s+|\\n+")
    private val clauseBreak = Regex(",?\\s+then\\s+|;\\s*")
    private val fragmentBreak = Regex("(?i)\\s*(?:,|;|\\bwhere\\b|\\bwhich\\b|\\bwhile\\b|\\bthen\\b|\\band\\s+(?=(?:$verbAlternation)\\b))\\s*")
    private val bulletBreak = Regex("\\n+|(?<=[.!?])\\s+")
    private val listSplit = Regex("\\s*(?:,|;|\\||&|\\band\\b)\\s*")

    private val skillsHeading = Regex("(?i)^\\s*(?:my\\s+)?(?:key\\s+|core\\s+|technical\\s+)?(?:skills?|tools|technologies|tech stack)\\b")
    private val skillsLead = Regex(
        "(?i)\\b(?:skills?|tools|technologies|tech stack|proficient (?:in|with)|skilled (?:in|at)|good at|experienced (?:with|in)|familiar with|expertise in)" +
            "\\s*(?:include|includes|are|is)?\\s*[:\\-]?\\s*(.+?)(?=\\.\\s|\\.$|\\n|$)",
    )
    private val languageCue = Regex("(?i)\\b(?:speak|speaks|spoken|languages?|fluent|native|bilingual)\\b")
    private val nameLead = Regex("(?:(?i:my name is|i am|i'm|i’m|this is)|[Nn]ame\\s*:)\\s+([A-Z][a-zA-Z'-]+(?:\\s+[A-Z][a-zA-Z'-]+){0,2})")
    private val locationLead = Regex("(?i:\\bfrom|based in|live in|living in|lives in|located in)\\s+([A-Z][a-zA-Z]+(?:,?\\s+[A-Z][a-zA-Z]+){0,2})")

    private val workCue = Regex("(?i)\\b(?:work(?:ed|ing|s)?|job|employ(?:ed|er)|joined|intern(?:ed|ship)?|position|role|since|as an?|currently)\\b")
    private val studyCue = Regex("(?i)\\b(?:studied|studying|graduated|university|college|school|degree|institute)\\b")
    private val companyAt = Regex("\\b(?:at|joined)\\s+([A-Z][\\w&.'-]*(?:\\s+(?:(?:of|&|and|for)\\s+)?[A-Z][\\w&.'-]*){0,4})")
    private val schoolAt = Regex("\\bat\\s+([A-Z][\\w&.'-]*(?:\\s+(?:(?:of|&|and|for)\\s+(?:the\\s+)?)?[A-Z][\\w&.'-]*){0,5})")
    private val schoolFrom = Regex("\\bfrom\\s+([A-Z][\\w&.'-]*(?:\\s+(?:(?:of|&|and|for)\\s+(?:the\\s+)?)?[A-Z][\\w&.'-]*){0,5})")
    private val institution = Regex("(?:[A-Z][\\w.'-]*\\s+){0,4}(?:University|College|Institute|School|Academy)(?:\\s+of(?:\\s+the)?(?:\\s+[A-Z][\\w.'-]*)+)?")
    private val roleAs = Regex(
        "(?i)\\bas\\s+(?:an?\\s+|the\\s+)?(?!well\\b)([a-z][a-z /&+.-]{1,40}?)" +
            "(?=\\s+(?:at|in|on|for|from|since|building|where|and|with|to|until|till|during|between)\\b|\\s*[,.;(]|\\s+\\d|$)",
    )
    private val roleBeforeAt = Regex(
        "(?i)(?:\\b(?:i am|i'm|i was|currently)\\s+(?:an?\\s+|the\\s+)?|,\\s*(?:an?|the)\\s+)([a-z][a-z /&+-]{2,40}?)(?=\\s+at\\s+(?-i:[A-Z]))",
    )
    private val degree = Regex(
        "\\b(?:BS|BSc|B\\.Sc|BA|BBA|BE|BEng|BCS|BCom|B\\.Com|MS|MSc|M\\.Sc|MA|MBA|MCS|MPhil|PhD|[Bb]achelor(?:'s)?|[Mm]aster(?:'s|s)?|" +
            "[Dd]iploma|[Aa]ssociate(?:'s)? [Dd]egree|Matric(?:ulation)?|FSc|F\\.Sc|ICS|I\\.Com|Intermediate|A[- ]Levels?|O[- ]Levels?|[Hh]igh [Ss]chool)\\b" +
            "[A-Za-z .&']*?(?=\\s+(?:at|from)\\b|\\s+in\\s+\\d|\\s*[,(;]|\\s+\\d|\\.\\s|\\.?$)",
    )
    private val yearRange = Regex("(?i)\\(?\\b((?:19|20)\\d{2})\\s*(?:-|–|—|to|till|until)\\s*((?:19|20)\\d{2}|present|now|current|today)\\b\\)?")
    private val sinceYear = Regex("(?i)\\b(?:since|from)\\s+((?:19|20)\\d{2})\\b")
    private val singleYear = Regex("(?i)\\b(?:in|graduated|completed|finished)\\s+((?:19|20)\\d{2})\\b")
    private val hiring = Regex(
        "(?i)\\b(?:hiring|looking for|seeking|position of|role of|job title:?)\\s+(?:an?\\s+|the\\s+)?(?:experienced\\s+|talented\\s+|motivated\\s+)?" +
            "([a-z][a-z /&+-]{2,40}?)(?=\\s+(?:to|who|with|at|in|for|that|and)\\b|[.,;:\\n]|$)",
    )

    private val weakLeads = listOf(
        Regex("(?i)^(?:i\\s+was\\s+|was\\s+)?responsible\\s+for\\s+") to "",
        Regex("(?i)^(?:i\\s+was\\s+|was\\s+)?tasked\\s+with\\s+") to "",
        Regex("(?i)^(?:duties|responsibilities)\\s+include[ds]?:?\\s+") to "",
        Regex("(?i)^(?:i\\s+was\\s+|was\\s+)?in\\s+charge\\s+of\\s+") to "led ",
        Regex("(?i)^(?:i\\s+)?helped\\s+(?:with|to)\\s+") to "supported ",
        Regex("(?i)^(?:i\\s+)?worked\\s+on\\s+") to "contributed to ",
        Regex("(?i)^(?:i\\s+was\\s+|was\\s+)?involved\\s+in\\s+") to "contributed to ",
        Regex("(?i)^i\\s+") to "",
    )
    private val proseFixes = listOf(
        Regex("(?i)\\bvery\\s+") to "",
        Regex("(?i)\\b(?:basically|really)\\s+") to "",
        Regex("(?i)\\bi think\\s+") to "",
        Regex("(?i)\\ba lot of\\b") to "extensive",
        Regex("(?i)\\bhard[- ]?working\\b") to "dedicated",
        Regex("(?i)\\bgood at\\b") to "skilled in",
        Regex("(?i)\\bhelped with\\b") to "supported",
        Regex("(?i)\\bteam player\\b") to "collaborative teammate",
        Regex("\\bi\\b") to "I",
    )
    private val sentenceStart = Regex("(^|[.!?]\\s+)([a-z])")
    private val firstPersonLead = Regex("(?i)(^|[.!?]\\s+)(?:i am|i'm|i’m)\\s+(?:an?\\s+)?")

    // endregion
}
