package com.resume.builder.data.remote.ai

import com.resume.builder.domain.model.ContactInfo
import com.resume.builder.domain.model.CustomLayout
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionItem
import com.resume.builder.domain.model.SectionType
import kotlinx.serialization.Serializable

@Serializable
data class AiResumeDto(
    val title: String = "",
    val contact: AiContactDto = AiContactDto(),
    val summary: String = "",
    val sections: List<AiSectionDto> = emptyList(),
)

@Serializable
data class AiContactDto(
    val fullName: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedin: String = "",
    val github: String = "",
)

@Serializable
data class AiSectionDto(
    val type: String = "CUSTOM",
    val title: String = "",
    val items: List<AiItemDto> = emptyList(),
)

@Serializable
data class AiItemDto(
    val title: String = "",
    val subtitle: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isCurrent: Boolean = false,
    val description: String = "",
    val link: String = "",
    val level: Int = 0,
)

val AI_SECTION_TYPES = listOf(
    SectionType.SKILLS, SectionType.EXPERIENCE, SectionType.PROJECTS, SectionType.EDUCATION, SectionType.CERTIFICATIONS,
    SectionType.PUBLICATIONS, SectionType.LANGUAGES, SectionType.INTERESTS, SectionType.ACTIVITIES, SectionType.REFERENCES,
    SectionType.CUSTOM,
)

fun AiResumeDto.toContent(): ResumeContent {
    val defaults = ResumeSection.defaults()
    val byType = sections.groupBy { dto -> runCatching { SectionType.valueOf(dto.type) }.getOrDefault(SectionType.CUSTOM) }
    val merged = defaults.map { section ->
        val items = byType[section.type].orEmpty().flatMap { it.items }.map { it.toItem() }
        section.copy(items = items)
    }
    val custom = byType[SectionType.CUSTOM].orEmpty().filter { it.items.isNotEmpty() }.map { dto ->
        ResumeSection(
            type = SectionType.CUSTOM,
            title = dto.title.ifBlank { "Additional" },
            items = dto.items.map { it.toItem() },
            customLayout = if (dto.items.any { it.startDate.isNotBlank() }) CustomLayout.TITLE_DESCRIPTION_DATE else CustomLayout.TITLE_DESCRIPTION,
        )
    }
    val withCustom = merged.toMutableList().apply {
        val signatureIndex = indexOfFirst { it.type == SectionType.SIGNATURE }.takeIf { it >= 0 } ?: size
        addAll(signatureIndex, custom)
    }
    return ResumeContent(
        contact = ContactInfo(
            fullName = contact.fullName.trim(), jobTitle = contact.jobTitle.trim(), email = contact.email.trim(),
            phone = contact.phone.trim(), location = contact.location.trim(), website = contact.website.trim(),
            linkedin = contact.linkedin.trim(), github = contact.github.trim(),
        ),
        summary = summary.trim(),
        sections = withCustom,
    )
}

private fun AiItemDto.toItem() = SectionItem(
    title = title.trim(), subtitle = subtitle.trim(), location = location.trim(), startDate = startDate.trim(),
    endDate = endDate.trim(), isCurrent = isCurrent, description = description.trim(), link = link.trim(),
    level = level.coerceIn(0, 5),
)

/** JSON schema (structured outputs) mirroring [AiResumeDto]. */
fun resumeJsonSchema(): Map<String, Any> {
    fun str() = mapOf("type" to "string")
    val item = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("title", "subtitle", "location", "startDate", "endDate", "isCurrent", "description", "link", "level"),
        "properties" to mapOf(
            "title" to str(), "subtitle" to str(), "location" to str(), "startDate" to str(), "endDate" to str(),
            "isCurrent" to mapOf("type" to "boolean"), "description" to str(), "link" to str(),
            "level" to mapOf("type" to "integer"),
        ),
    )
    val section = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("type", "title", "items"),
        "properties" to mapOf(
            "type" to mapOf("type" to "string", "enum" to AI_SECTION_TYPES.map { it.name }),
            "title" to str(),
            "items" to mapOf("type" to "array", "items" to item),
        ),
    )
    val contactKeys = listOf("fullName", "jobTitle", "email", "phone", "location", "website", "linkedin", "github")
    val contact = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to contactKeys,
        "properties" to contactKeys.associateWith { str() },
    )
    return mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("title", "contact", "summary", "sections"),
        "properties" to mapOf(
            "title" to str(),
            "contact" to contact,
            "summary" to str(),
            "sections" to mapOf("type" to "array", "items" to section),
        ),
    )
}
