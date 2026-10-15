package com.resume.builder.data.remote.ai

import com.resume.builder.domain.model.ContactInfo
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionItem
import com.resume.builder.domain.model.SectionType

/**
 * Heuristic, on-device parser that maps text extracted from a resume PDF into sections. Input is expected in reading
 * order (see `LayoutReader`), one visual line per text line; chunks separated by wide gaps arrive joined with " | ".
 */
object ResumeTextParser {

    internal val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    internal val phone = Regex("\\+?\\d[\\d\\s()-]{7,}\\d")
    internal val linkedin = Regex("(https?://)?(www\\.)?linkedin\\.com/[^\\s|,]+", RegexOption.IGNORE_CASE)
    internal val github = Regex("(https?://)?(www\\.)?github\\.com/[^\\s|,]+", RegexOption.IGNORE_CASE)
    /** Standalone domains only, so "name.surname.comsat@gmail.com" yields neither "surname.com" nor "gmail.com". */
    private val website = Regex(
        "(?<![A-Za-z0-9._%+@-])(https?://)?(www\\.)?[a-z0-9-]+\\.(dev|io|me|com|net|org)(?![A-Za-z0-9@.-])(/[^\\s|,]*)?",
        RegexOption.IGNORE_CASE,
    )
    private const val MONTH = "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?"
    private val dateRange = Regex(
        "($MONTH\\s*)?(19|20)\\d{2}\\s*[-–—]+\\s*(($MONTH\\s*)?(19|20)\\d{2}|Present|Current|Now|Till Date|To Date)",
        RegexOption.IGNORE_CASE,
    )
    private val singleDate = Regex("^($MONTH\\s*)?(19|20)\\d{2}$", RegexOption.IGNORE_CASE)
    /** A line cut mid-range, like "May 2023 -" followed by "Present" on the next line. */
    private val openRangeEnd = Regex("((19|20)\\d{2}\\s*[-–—]|[-–—]\\s*$MONTH)\\s*$", RegexOption.IGNORE_CASE)
    private val rangeContinuation = Regex("^(($MONTH\\s*)?(19|20)\\d{2}|Present|Current|Now)\\b", RegexOption.IGNORE_CASE)
    private val bulletChars = charArrayOf('•', '●', '▪', '◦', '‣', '∙', '·', '-', '*', '–', '')
    private val profileSlug = Regex("(?<![\\w.])/[A-Za-z0-9_-]{3,}")
    private val place = Regex("^[A-Z][A-Za-z .'-]+,\\s*[A-Z][A-Za-z .'-]+$")

    private val headingKeywords = linkedMapOf(
        SectionType.SUMMARY to listOf("summary", "profile", "about me", "about", "objective", "career objective"),
        SectionType.EXPERIENCE to listOf("experience", "employment", "work history", "employment history", "career history"),
        SectionType.EDUCATION to listOf("education", "academic", "academics", "qualifications", "academic qualifications"),
        SectionType.SKILLS to listOf("skills", "technical skills", "competencies", "expertise", "core competencies", "technologies"),
        SectionType.PROJECTS to listOf("projects", "personal projects", "key projects"),
        SectionType.CERTIFICATIONS to listOf(
            "certifications", "certificates", "certification", "awards", "achievements", "accomplishments",
            "honors", "honours", "courses", "training", "professional development", "licenses",
        ),
        SectionType.LANGUAGES to listOf("languages"),
        SectionType.INTERESTS to listOf("interests", "hobbies"),
        SectionType.PUBLICATIONS to listOf("publications", "research"),
        SectionType.ACTIVITIES to listOf("activities", "volunteer", "volunteering", "extracurricular activities"),
        SectionType.REFERENCES to listOf("references"),
    )

    fun parse(raw: String): Pair<String, ResumeContent> {
        val lines = joinBrokenRanges(raw.lines().map { it.trim() }.filter { it.isNotEmpty() })
        val firstHeading = lines.indexOfFirst { headingOf(it) != null }.let { if (it < 0) lines.size else it }
        val top = lines.take(firstHeading)

        val nameIndex = top.indexOfFirst(::looksLikeName).takeIf { it >= 0 }
            ?: lines.indexOfFirst(::looksLikeName)
        val name = lines.getOrNull(nameIndex)?.let(::tidyName).orEmpty()
        val titleIndex = top.indices.firstOrNull { i -> i > nameIndex && looksLikeJobTitle(top[i]) } ?: -1
        val jobTitle = top.getOrNull(titleIndex)?.substringBefore(" | ")?.trim().orEmpty()

        val buckets = linkedMapOf<SectionType, MutableList<String>>()
        var current: SectionType? = null
        lines.drop(firstHeading).forEach { line ->
            val heading = headingOf(line)
            if (heading != null) {
                current = heading
                buckets.getOrPut(heading) { mutableListOf() }
            } else {
                current?.let { buckets.getValue(it).add(line) }
            }
        }

        val contactLines = top.filterIndexed { i, line -> i != nameIndex && i != titleIndex && isContactLine(line) }
        val contactText = contactLines.joinToString("\n")
        val contact = ContactInfo(
            fullName = name,
            jobTitle = jobTitle,
            email = email.find(raw)?.value.orEmpty(),
            phone = phone.find(raw)?.value?.trim().orEmpty(),
            location = contactText.split('|', '\n').map { it.trim() }.firstOrNull { place.matches(it) && !email.containsMatchIn(it) }.orEmpty(),
            linkedin = linkedin.find(raw)?.value
                ?: profileSlug.find(contactText)?.value?.let { "linkedin.com/in${it}" }.orEmpty(),
            github = github.find(raw)?.value.orEmpty(),
            website = website.findAll(raw).map { it.value }
                .firstOrNull { !it.contains("linkedin", true) && !it.contains("github", true) }
                .orEmpty(),
        )

        // Resumes without a "Summary" heading usually open with the summary paragraph under the name.
        val introSummary = top.filterIndexed { i, line ->
            i != nameIndex && i != titleIndex && !isContactLine(line) && line.length > 25
        }.joinToString(" ")
        val summary = buckets[SectionType.SUMMARY].orEmpty().joinToString(" ").ifBlank { introSummary }

        val sections = ResumeSection.defaults().map { section ->
            val body = buckets[section.type].orEmpty()
            val items = when {
                body.isEmpty() -> emptyList()
                section.type.isCompact -> joinWrapped(body).flatMap { it.split(',', '•', '|', ';') }
                    .map { stripBullet(it) }
                    .filter { it.length in 2..40 }
                    .distinct()
                    .map { SectionItem(title = it) }
                section.type.fields != null -> items(body)
                else -> emptyList()
            }
            section.copy(items = items)
        }
        return jobTitle.ifBlank { name.ifBlank { "Imported Resume" } } to ResumeContent(contact = contact, summary = summary, sections = sections)
    }

    private fun headingOf(line: String): SectionType? {
        if (line.length > 40 || isBullet(line) || line.first().isLowerCase() || line.trimEnd().endsWith('.')) return null
        val normalized = line.lowercase().replace('&', ' ').trim(':', ' ').replace(Regex("\\s+"), " ")
        if (normalized.split(' ').size > 4) return null
        return headingKeywords.entries.firstOrNull { (_, keys) ->
            keys.any { normalized == it || normalized.startsWith("$it ") || normalized.endsWith(" $it") }
        }?.key
    }

    private fun looksLikeName(line: String): Boolean {
        val words = line.split(' ').filter { it.isNotBlank() }
        return line.length in 3..40 && words.size in 1..5 && !line.any { it.isDigit() } &&
            !email.containsMatchIn(line) && !line.contains('|') && !line.endsWith('.') && !isBullet(line) &&
            headingOf(line) == null && words.all { it.first().isUpperCase() }
    }

    private fun looksLikeJobTitle(line: String): Boolean =
        line.length in 3..60 && !line.endsWith('.') && headingOf(line) == null && !isContactLine(line) && !isBullet(line) &&
            line.split(' ').size <= 8

    private fun isContactLine(line: String) =
        email.containsMatchIn(line) || phone.containsMatchIn(line) || linkedin.containsMatchIn(line) || github.containsMatchIn(line)

    /** "MUHAMMAD ASIF Ali" → "Muhammad Asif Ali"; mixed-case names are kept as written. */
    private fun tidyName(line: String): String = line.split(' ').filter { it.isNotBlank() }.joinToString(" ") { word ->
        if (word.length > 1 && word.all { !it.isLetter() || it.isUpperCase() }) word.lowercase().replaceFirstChar(Char::titlecase) else word
    }

    private fun isBullet(line: String) = line.isNotEmpty() && line[0] in bulletChars && (line.length < 2 || line[1] == ' ' || line[0] == '•' || line[0] == '')

    private fun stripBullet(line: String) = line.trim().trimStart(*bulletChars).trim()

    private fun joinBrokenRanges(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        lines.forEach { line ->
            val last = out.lastOrNull()
            if (last != null && openRangeEnd.containsMatchIn(last) && rangeContinuation.containsMatchIn(line)) {
                out[out.lastIndex] = "$last $line"
            } else {
                out += line
            }
        }
        return out
    }

    /** Lines that continue a wrapped bullet are appended to it; other lines are kept as they are. */
    private fun joinWrapped(body: List<String>): List<String> {
        if (body.none(::isBullet)) return body
        val out = mutableListOf<String>()
        body.forEach { line ->
            val last = out.lastOrNull()
            val continues = last != null && isBullet(last) && !isBullet(line) && !dateRange.containsMatchIn(line) && !line.contains(" | ")
            if (continues) out[out.lastIndex] = "$last $line" else out += line
        }
        return out
    }

    private fun items(body: List<String>): List<SectionItem> {
        val lines = joinWrapped(body)
        val dated = lines.any { !isBullet(it) && (dateRange.containsMatchIn(it) || singleDate.matches(it)) }
        // Projects, awards and courses are often plain bullet lists: one bullet per item.
        if (!dated && lines.count(::isBullet) >= lines.size / 2) return lines.map { bulletItem(stripBullet(it)) }
        return entries(lines).map(::entryItem)
    }

    private fun bulletItem(text: String): SectionItem {
        val colon = text.indexOf(':').takeIf { it in 1..80 }
        if (colon != null) return SectionItem(title = text.take(colon).trim(), description = text.drop(colon + 1).trim())
        val dash = Regex("\\s[–—-]\\s").find(text)?.takeIf { text.length <= 90 }
        if (dash != null) return SectionItem(title = text.take(dash.range.first).trim(), subtitle = text.drop(dash.range.last + 1).trim())
        return if (text.length <= 80) SectionItem(title = text) else SectionItem(title = text.take(60).substringBeforeLast(' ') + "…", description = text)
    }

    /** Groups lines into entries: a header line (often with a date) followed by details and bullets. */
    private fun entries(lines: List<String>): List<List<String>> {
        val entries = mutableListOf<MutableList<String>>()
        lines.forEach { line ->
            val entry = entries.lastOrNull()
            val entryHasDate = entry?.any { !isBullet(it) && (dateRange.containsMatchIn(it) || singleDate.matches(it)) } == true
            val startsNew = entry == null || !isBullet(line) && (
                (dateRange.containsMatchIn(line) && entry.size > 1) ||
                    (entryHasDate && entry.size >= 2 && !singleDate.matches(line)) ||
                    (entry.any(::isBullet) && line.contains(" | "))
                )
            if (startsNew) entries += mutableListOf(line) else entry!!.add(line)
        }
        return entries
    }

    private fun entryItem(entry: List<String>): SectionItem {
        val bullets = entry.dropWhile { !isBullet(it) }
        // Narrow columns wrap "COMSATS University, Islamabad," / "Pakistan"; rejoin lines that end mid-list.
        val headerLines = entry.takeWhile { !isBullet(it) }.fold(mutableListOf<String>()) { acc, line ->
            if (acc.lastOrNull()?.endsWith(',') == true && !singleDate.matches(line)) acc[acc.lastIndex] = "${acc.last()} $line" else acc += line
            acc
        }
        val dateLine = headerLines.firstOrNull { dateRange.containsMatchIn(it) || singleDate.matches(it) }
        val range = dateLine?.let { dateRange.find(it)?.value ?: singleDate.find(it)?.value }.orEmpty()
        val parts = range.split(Regex("\\s*[-–—]+\\s*")).map { it.trim() }.filter { it.isNotBlank() }
        val end = parts.getOrNull(1)
        val isCurrent = end != null && Regex("present|current|now|till date|to date", RegexOption.IGNORE_CASE).matches(end)

        // "Senior Android Developer | HyperSoft, Rawalpindi, Pakistan | May 2023 - Present"
        val fields = headerLines.flatMap { it.replace(range, "").split('|') }.map { it.trim(' ', ',', '–', '-') }.filter { it.isNotBlank() }
        val title = fields.getOrNull(0).orEmpty()
        val organisation = fields.getOrNull(1).orEmpty()
        val (subtitle, location) = splitLocation(organisation)
        val extra = fields.drop(2)

        // A lone year (e.g. graduation) reads as the end date.
        val single = parts.size == 1
        return SectionItem(
            title = title,
            subtitle = subtitle,
            location = location,
            startDate = if (single) "" else parts.getOrNull(0).orEmpty(),
            endDate = if (single) parts[0] else if (isCurrent) "" else end.orEmpty(),
            isCurrent = isCurrent,
            description = (extra + bullets.map { "• ${stripBullet(it)}" }).joinToString("\n"),
        )
    }

    /** "HyperSoft, Bahria Phase 8, Rawalpindi, Pakistan" → ("HyperSoft", "Bahria Phase 8, Rawalpindi, Pakistan"). */
    private fun splitLocation(text: String): Pair<String, String> {
        val pieces = text.split(',').map { it.trim() }
        return if (pieces.size >= 3) pieces.first() to pieces.drop(1).joinToString(", ") else text to ""
    }
}
