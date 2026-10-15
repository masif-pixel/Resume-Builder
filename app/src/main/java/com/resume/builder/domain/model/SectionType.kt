package com.resume.builder.domain.model

import kotlinx.serialization.Serializable

/**
 * Describes which fields an item of a section uses. Labels are used both by the editor
 * and by AI prompts, so they live next to the type definition.
 */
data class ItemFields(
    val titleLabel: String,
    val subtitleLabel: String? = null,
    val locationLabel: String? = null,
    val hasDates: Boolean = false,
    val hasCurrent: Boolean = false,
    val descriptionLabel: String? = null,
    val linkLabel: String? = null,
    val levelLabels: List<String>? = null,
    val quickAdd: Boolean = false,
)

private val SKILL_LEVELS = listOf("Beginner", "Elementary", "Intermediate", "Advanced", "Expert")
private val LANGUAGE_LEVELS = listOf("Basic", "Conversational", "Professional", "Fluent", "Native")

@Serializable
enum class SectionType(val defaultTitle: String, val fields: ItemFields?) {
    CONTACT("Contact", null),
    SUMMARY("Summary", null),
    SKILLS("Skills", ItemFields(titleLabel = "Skill", levelLabels = SKILL_LEVELS, quickAdd = true)),
    EXPERIENCE(
        "Work Experience",
        ItemFields(
            titleLabel = "Job title", subtitleLabel = "Company", locationLabel = "Location",
            hasDates = true, hasCurrent = true, descriptionLabel = "Responsibilities & achievements",
        ),
    ),
    PROJECTS(
        "Projects",
        ItemFields(
            titleLabel = "Project name", subtitleLabel = "Tech stack / role", hasDates = true,
            hasCurrent = true, descriptionLabel = "What you built & impact", linkLabel = "Link",
        ),
    ),
    EDUCATION(
        "Education",
        ItemFields(
            titleLabel = "Degree", subtitleLabel = "School / University", locationLabel = "Location",
            hasDates = true, hasCurrent = true, descriptionLabel = "Grade, coursework, honours",
        ),
    ),
    CERTIFICATIONS(
        "Certifications & Awards",
        ItemFields(titleLabel = "Title", subtitleLabel = "Issuer", hasDates = true, descriptionLabel = "Details", linkLabel = "Credential link"),
    ),
    PUBLICATIONS(
        "Publications",
        ItemFields(titleLabel = "Title", subtitleLabel = "Publisher", hasDates = true, descriptionLabel = "Summary", linkLabel = "Link"),
    ),
    LANGUAGES("Languages", ItemFields(titleLabel = "Language", levelLabels = LANGUAGE_LEVELS, quickAdd = true)),
    INTERESTS("Interests", ItemFields(titleLabel = "Interest", quickAdd = true)),
    ACTIVITIES(
        "Activities",
        ItemFields(titleLabel = "Activity", subtitleLabel = "Organization", hasDates = true, hasCurrent = true, descriptionLabel = "Description"),
    ),
    REFERENCES(
        "References",
        ItemFields(
            titleLabel = "Full name", subtitleLabel = "Position & company", locationLabel = "Email",
            linkLabel = "Phone",
        ),
    ),
    SIGNATURE("Signature", null),
    CUSTOM("Custom", null);

    val isListSection: Boolean get() = fields != null || this == CUSTOM

    /** Short, list-like sections that fit a narrow sidebar column. */
    val isCompact: Boolean get() = this == SKILLS || this == LANGUAGES || this == INTERESTS

    fun fieldsFor(layout: CustomLayout): ItemFields = fields ?: when (layout) {
        CustomLayout.TITLE -> ItemFields(titleLabel = "Title", quickAdd = true)
        CustomLayout.TITLE_DESCRIPTION -> ItemFields(titleLabel = "Title", subtitleLabel = "Subtitle", descriptionLabel = "Description")
        CustomLayout.TITLE_DESCRIPTION_DATE -> ItemFields(
            titleLabel = "Title", subtitleLabel = "Subtitle", hasDates = true, hasCurrent = true, descriptionLabel = "Description",
        )
    }
}
