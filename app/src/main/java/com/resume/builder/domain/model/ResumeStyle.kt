package com.resume.builder.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PageSize(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792),
}

@Serializable
enum class ResumeFont(val label: String, val family: String) {
    SANS("Modern Sans", "sans-serif"),
    SERIF("Classic Serif", "serif"),
    CONDENSED("Condensed", "sans-serif-condensed"),
    LIGHT("Elegant Light", "sans-serif-light"),
    MEDIUM("Bold Sans", "sans-serif-medium"),
    MONO("Monospace", "monospace"),
    SERIF_MONO("Typewriter", "serif-monospace"),
    CASUAL("Casual", "casual"),
}

/** Every value is an override on top of the chosen template; null / 0 means "template default". */
@Serializable
data class ResumeStyle(
    val accentColor: Long? = null,
    val sidebarColor: Long? = null,
    val backgroundColor: Long? = null,
    val nameColor: Long? = null,
    val jobTitleColor: Long? = null,
    val headingColor: Long? = null,
    val textColor: Long? = null,
    val photoBorderColor: Long? = null,
    val font: ResumeFont? = null,
    val nameSize: Float = 0f,
    val jobTitleSize: Float = 0f,
    val headingSize: Float = 0f,
    val bodySize: Float = 0f,
    val marginHorizontal: Float = 0f,
    val marginVertical: Float = 0f,
    val sectionSpacing: Float = 0f,
    val headingSpacing: Float = 0f,
    val columnWidth: Float = 0f,
    val swapColumns: Boolean = false,
    val pageSize: PageSize = PageSize.A4,
    val showPhoto: Boolean = true,
    val photoSize: Float = 0f,
    val photoCornerRadius: Float? = null,
    val photoBorderWidth: Float? = null,
    val lineSpacing: Float = 0f,
    val letterSpacing: Float = 0f,
    val showContactIcons: Boolean = true,
    val contactOrder: List<ContactField> = ContactField.entries,
    val hiddenContactFields: Set<ContactField> = emptySet(),
    val language: ResumeLanguage = ResumeLanguage.ENGLISH,
)
