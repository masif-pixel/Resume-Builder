package com.resume.builder.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class Resume(
    val id: String = newId(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val templateId: String = TemplateCatalog.DEFAULT_ID,
    val content: ResumeContent = ResumeContent(),
    val style: ResumeStyle = ResumeStyle(),
)

@Serializable
data class ResumeContent(
    val contact: ContactInfo = ContactInfo(),
    val summary: String = "",
    val sections: List<ResumeSection> = ResumeSection.defaults(),
    val signature: SignatureInfo = SignatureInfo(),
) {
    fun section(type: SectionType): ResumeSection? = sections.firstOrNull { it.type == type }
    fun sectionById(id: String): ResumeSection? = sections.firstOrNull { it.id == id }

    fun isComplete(section: ResumeSection): Boolean = when (section.type) {
        SectionType.CONTACT -> contact.fullName.isNotBlank() && (contact.email.isNotBlank() || contact.phone.isNotBlank())
        SectionType.SUMMARY -> summary.isNotBlank()
        SectionType.SIGNATURE -> signature.imagePath != null || signature.fullName.isNotBlank()
        else -> section.items.isNotEmpty()
    }

    /** 0..100 score that nudges users towards a well-rounded resume. */
    fun strength(): Int {
        var score = 0
        if (contact.fullName.isNotBlank()) score += 10
        if (contact.jobTitle.isNotBlank()) score += 5
        if (contact.email.isNotBlank()) score += 5
        if (contact.phone.isNotBlank()) score += 5
        if (contact.linkedin.isNotBlank() || contact.website.isNotBlank() || contact.github.isNotBlank()) score += 5
        if (summary.length >= 120) score += 15 else if (summary.isNotBlank()) score += 7
        val exp = section(SectionType.EXPERIENCE)?.items.orEmpty()
        if (exp.isNotEmpty()) score += 15
        if (exp.any { it.description.length > 80 }) score += 5
        if (section(SectionType.EDUCATION)?.items.orEmpty().isNotEmpty()) score += 12
        val skills = section(SectionType.SKILLS)?.items.orEmpty().size
        score += when {
            skills >= 6 -> 13
            skills > 0 -> 7
            else -> 0
        }
        if (section(SectionType.PROJECTS)?.items.orEmpty().isNotEmpty() ||
            section(SectionType.CERTIFICATIONS)?.items.orEmpty().isNotEmpty()
        ) score += 5
        if (section(SectionType.LANGUAGES)?.items.orEmpty().isNotEmpty()) score += 5
        return score.coerceIn(0, 100)
    }
}

@Serializable
data class ContactInfo(
    val fullName: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedin: String = "",
    val github: String = "",
    val dateOfBirth: String = "",
    val photoPath: String? = null,
) {
    fun field(key: ContactField): String = when (key) {
        ContactField.EMAIL -> email
        ContactField.PHONE -> phone
        ContactField.LOCATION -> location
        ContactField.WEBSITE -> website
        ContactField.LINKEDIN -> linkedin
        ContactField.GITHUB -> github
        ContactField.BIRTHDAY -> dateOfBirth
    }
}

@Serializable
enum class ContactField(val label: String) {
    EMAIL("Email"), PHONE("Phone"), LOCATION("Location"), WEBSITE("Website"),
    LINKEDIN("LinkedIn"), GITHUB("GitHub"), BIRTHDAY("Date of birth"),
}

@Serializable
data class SignatureInfo(
    val imagePath: String? = null,
    val fullName: String = "",
    val place: String = "",
    val date: String = "",
)

@Serializable
data class ResumeSection(
    val id: String = newId(),
    val type: SectionType,
    val title: String = type.defaultTitle,
    val visible: Boolean = true,
    val items: List<SectionItem> = emptyList(),
    val customLayout: CustomLayout = CustomLayout.TITLE_DESCRIPTION,
) {
    companion object {
        fun defaults(): List<ResumeSection> = SectionType.entries
            .filter { it != SectionType.CUSTOM }
            .map { ResumeSection(id = it.name.lowercase(), type = it) }
    }
}

@Serializable
enum class CustomLayout(val label: String) {
    TITLE("Title only"),
    TITLE_DESCRIPTION("Title & description"),
    TITLE_DESCRIPTION_DATE("Title, description & date"),
}

@Serializable
data class SectionItem(
    val id: String = newId(),
    val title: String = "",
    val subtitle: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isCurrent: Boolean = false,
    val description: String = "",
    val link: String = "",
    /** 0 = not rated, 1..5 proficiency. */
    val level: Int = 0,
) {
    fun dateRange(presentLabel: String = "Present"): String {
        val end = if (isCurrent) presentLabel else endDate
        return listOf(startDate, end).filter { it.isNotBlank() }.joinToString(" – ")
    }
}
