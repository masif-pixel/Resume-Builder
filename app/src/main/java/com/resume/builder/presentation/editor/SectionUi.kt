package com.resume.builder.presentation.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.resume.builder.domain.model.SectionType

fun SectionType.icon(): ImageVector = when (this) {
    SectionType.CONTACT -> Icons.Rounded.Person
    SectionType.SUMMARY -> Icons.Rounded.Subject
    SectionType.SKILLS -> Icons.Rounded.Star
    SectionType.EXPERIENCE -> Icons.Rounded.Work
    SectionType.PROJECTS -> Icons.Rounded.Layers
    SectionType.EDUCATION -> Icons.Rounded.School
    SectionType.CERTIFICATIONS -> Icons.Rounded.EmojiEvents
    SectionType.PUBLICATIONS -> Icons.Rounded.MenuBook
    SectionType.LANGUAGES -> Icons.Rounded.Language
    SectionType.INTERESTS -> Icons.Rounded.Favorite
    SectionType.ACTIVITIES -> Icons.Rounded.VolunteerActivism
    SectionType.REFERENCES -> Icons.Rounded.RecordVoiceOver
    SectionType.SIGNATURE -> Icons.Rounded.Draw
    SectionType.CUSTOM -> Icons.AutoMirrored.Rounded.Article
}

fun SectionType.tint(): Color = when (this) {
    SectionType.CONTACT -> Color(0xFF4F46E5)
    SectionType.SUMMARY -> Color(0xFF0EA5E9)
    SectionType.SKILLS -> Color(0xFFF59E0B)
    SectionType.EXPERIENCE -> Color(0xFF7C3AED)
    SectionType.PROJECTS -> Color(0xFF0D9488)
    SectionType.EDUCATION -> Color(0xFF2563EB)
    SectionType.CERTIFICATIONS -> Color(0xFFEA580C)
    SectionType.PUBLICATIONS -> Color(0xFF64748B)
    SectionType.LANGUAGES -> Color(0xFF16A34A)
    SectionType.INTERESTS -> Color(0xFFE11D48)
    SectionType.ACTIVITIES -> Color(0xFFDB2777)
    SectionType.REFERENCES -> Color(0xFF9333EA)
    SectionType.SIGNATURE -> Color(0xFF475569)
    SectionType.CUSTOM -> Color(0xFF0891B2)
}
