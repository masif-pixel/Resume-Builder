package com.resume.builder.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.resume.builder.R
import com.resume.builder.domain.model.ContactField
import com.resume.builder.domain.model.CoverLetterLength
import com.resume.builder.domain.model.CoverLetterTone
import com.resume.builder.domain.model.CustomLayout
import com.resume.builder.domain.model.FileCategory
import com.resume.builder.domain.model.PageSize
import com.resume.builder.domain.model.ResumeFont
import com.resume.builder.domain.model.TemplateCategory
import com.resume.builder.domain.model.TemplateSpec
import com.resume.builder.domain.model.ThemeMode

// Localized display names for domain values. The domain keeps its English labels because they
// also feed the offline writer, PDF parsing and editor logic.

@Composable
fun CustomLayout.displayName(): String = stringResource(
    when (this) {
        CustomLayout.TITLE -> R.string.layout_title_only
        CustomLayout.TITLE_DESCRIPTION -> R.string.layout_title_description
        CustomLayout.TITLE_DESCRIPTION_DATE -> R.string.layout_title_description_date
    },
)

@Composable
fun ContactField.displayName(): String = stringResource(
    when (this) {
        ContactField.EMAIL -> R.string.field_email
        ContactField.PHONE -> R.string.field_phone
        ContactField.LOCATION -> R.string.field_location
        ContactField.WEBSITE -> R.string.field_website
        ContactField.LINKEDIN -> R.string.field_linkedin
        ContactField.GITHUB -> R.string.field_github
        ContactField.BIRTHDAY -> R.string.field_date_of_birth
    },
)

@Composable
fun ResumeFont.displayName(): String = stringResource(
    when (this) {
        ResumeFont.SANS -> R.string.font_modern_sans
        ResumeFont.SERIF -> R.string.font_classic_serif
        ResumeFont.CONDENSED -> R.string.font_condensed
        ResumeFont.LIGHT -> R.string.font_elegant_light
        ResumeFont.MEDIUM -> R.string.font_bold_sans
        ResumeFont.MONO -> R.string.font_monospace
        ResumeFont.SERIF_MONO -> R.string.font_typewriter
        ResumeFont.CASUAL -> R.string.font_casual
    },
)

@Composable
fun PageSize.displayName(): String = when (this) {
    PageSize.A4 -> label
    PageSize.LETTER -> stringResource(R.string.page_size_letter)
}

@Composable
fun FileCategory.displayName(): String = stringResource(
    when (this) {
        FileCategory.RESUME -> R.string.category_resume
        FileCategory.COVER_LETTER -> R.string.category_cover_letter
        FileCategory.PDF_TOOLS -> R.string.category_pdf_tools
        FileCategory.IMAGES -> R.string.category_images
    },
)

@Composable
fun ThemeMode.displayName(): String = stringResource(
    when (this) {
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.SYSTEM -> R.string.theme_system
    },
)

@Composable
fun CoverLetterTone.displayName(): String = stringResource(
    when (this) {
        CoverLetterTone.PROFESSIONAL -> R.string.tone_professional
        CoverLetterTone.CONFIDENT -> R.string.tone_confident
        CoverLetterTone.FRIENDLY -> R.string.tone_friendly
        CoverLetterTone.ENTHUSIASTIC -> R.string.tone_enthusiastic
        CoverLetterTone.FORMAL -> R.string.tone_formal
    },
)

@Composable
fun CoverLetterLength.displayName(): String = stringResource(
    when (this) {
        CoverLetterLength.SHORT -> R.string.length_short
        CoverLetterLength.MEDIUM -> R.string.length_medium
        CoverLetterLength.LONG -> R.string.length_long
    },
)

@Composable
fun TemplateCategory.displayName(): String = stringResource(
    when (this) {
        TemplateCategory.MODERN -> R.string.template_category_modern
        TemplateCategory.PROFESSIONAL -> R.string.template_category_professional
        TemplateCategory.CREATIVE -> R.string.template_category_creative
        TemplateCategory.ATS -> R.string.template_category_ats
    },
)

/** Template names are brand names and stay as they are; taglines are translated. */
@Composable
fun TemplateSpec.localizedTagline(): String {
    val res = when (id) {
        "midnight" -> R.string.tagline_midnight
        "aurum" -> R.string.tagline_aurum
        "evergreen" -> R.string.tagline_evergreen
        "horizon" -> R.string.tagline_horizon
        "prism" -> R.string.tagline_prism
        "atelier" -> R.string.tagline_atelier
        "editorial" -> R.string.tagline_editorial
        "violet" -> R.string.tagline_violet
        "clarity" -> R.string.tagline_clarity
        "compact" -> R.string.tagline_compact
        "ember" -> R.string.tagline_ember
        "oceanic" -> R.string.tagline_oceanic
        "executive" -> R.string.tagline_executive
        "sapphire" -> R.string.tagline_sapphire
        "noir" -> R.string.tagline_noir
        "graphite" -> R.string.tagline_graphite
        "blush" -> R.string.tagline_blush
        "boardroom" -> R.string.tagline_boardroom
        "terracotta" -> R.string.tagline_terracotta
        "nordic" -> R.string.tagline_nordic
        "royal" -> R.string.tagline_royal
        "coral" -> R.string.tagline_coral
        "slate" -> R.string.tagline_slate
        "mint" -> R.string.tagline_mint
        "heritage" -> R.string.tagline_heritage
        "cobalt" -> R.string.tagline_cobalt
        "linen" -> R.string.tagline_linen
        "quantum" -> R.string.tagline_quantum
        "standard" -> R.string.tagline_standard
        "sunrise" -> R.string.tagline_sunrise
        "metro" -> R.string.tagline_metro
        "crisp" -> R.string.tagline_crisp
        else -> null
    }
    return res?.let { stringResource(it) } ?: tagline
}

/** Translates an English editor label from [com.resume.builder.domain.model.ItemFields]; unknown labels pass through. */
@Composable
fun fieldLabel(english: String): String {
    val res = when (english) {
        "Skill" -> R.string.field_skill
        "Job title" -> R.string.field_job_title
        "Company" -> R.string.field_company
        "Location" -> R.string.field_location
        "Responsibilities & achievements" -> R.string.field_responsibilities
        "Project name" -> R.string.field_project_name
        "Tech stack / role" -> R.string.field_tech_stack
        "What you built & impact" -> R.string.field_project_impact
        "Link" -> R.string.field_link
        "Degree" -> R.string.field_degree
        "School / University" -> R.string.field_school
        "Grade, coursework, honours" -> R.string.field_grade
        "Title" -> R.string.field_title
        "Issuer" -> R.string.field_issuer
        "Details" -> R.string.field_details
        "Credential link" -> R.string.field_credential_link
        "Publisher" -> R.string.field_publisher
        "Summary" -> R.string.field_summary
        "Language" -> R.string.field_language
        "Interest" -> R.string.field_interest
        "Activity" -> R.string.field_activity
        "Organization" -> R.string.field_organization
        "Description" -> R.string.field_description
        "Full name" -> R.string.field_full_name
        "Position & company" -> R.string.field_position_company
        "Email" -> R.string.field_email
        "Phone" -> R.string.field_phone
        "Subtitle" -> R.string.field_subtitle
        "Beginner" -> R.string.level_beginner
        "Elementary" -> R.string.level_elementary
        "Intermediate" -> R.string.level_intermediate
        "Advanced" -> R.string.level_advanced
        "Expert" -> R.string.level_expert
        "Basic" -> R.string.level_basic
        "Conversational" -> R.string.level_conversational
        "Professional" -> R.string.level_professional
        "Fluent" -> R.string.level_fluent
        "Native" -> R.string.level_native
        else -> null
    }
    return res?.let { stringResource(it) } ?: english
}
