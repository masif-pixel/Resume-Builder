package com.resume.builder

import com.resume.builder.domain.model.ResumeLanguage
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ResumeLanguageTest {

    private val headed = SectionType.entries.filter { it != SectionType.CUSTOM }

    @Test
    fun everyLanguageIsFullyTranslated() {
        assertTrue(ResumeLanguage.entries.size >= 45)
        ResumeLanguage.entries.forEach { lang ->
            val titles = headed.map { lang.heading(it) }
            assertEquals("$lang has duplicate headings", titles.size, titles.toSet().size)
            if (lang != ResumeLanguage.ENGLISH) {
                headed.forEach { type -> assertTrue("$lang $type", lang.heading(type).isNotBlank()) }
            }
            assertEquals(5, lang.levelLabels(SectionType.SKILLS.fields)!!.size)
            assertEquals(5, lang.levelLabels(SectionType.LANGUAGES.fields)!!.size)
        }
    }

    @Test
    fun defaultTitlesFollowLanguage() {
        val section = ResumeSection(type = SectionType.EXPERIENCE)
        assertEquals("الخبرة العملية", ResumeLanguage.ARABIC.headingFor(section))
        // A title saved in another language's default still switches with the language.
        val arabicTitled = section.copy(title = "الخبرة العملية")
        assertEquals("Berufserfahrung", ResumeLanguage.GERMAN.headingFor(arabicTitled))
        // Custom titles are kept as typed.
        assertEquals("My Jobs", ResumeLanguage.ARABIC.headingFor(section.copy(title = "My Jobs")))
        assertEquals("Work Experience", ResumeLanguage.normalizeTitle(SectionType.EXPERIENCE, "الخبرة العملية"))
    }

    @Test
    fun deviceLocaleMapping() {
        assertEquals(ResumeLanguage.URDU, ResumeLanguage.fromDevice(Locale("ur", "PK")))
        assertEquals(ResumeLanguage.INDONESIAN, ResumeLanguage.fromDevice(Locale("in", "ID")))
        assertEquals(ResumeLanguage.CHINESE_TRADITIONAL, ResumeLanguage.fromDevice(Locale("zh", "TW")))
        assertEquals(ResumeLanguage.CHINESE, ResumeLanguage.fromDevice(Locale("zh", "CN")))
        assertEquals(ResumeLanguage.ENGLISH, ResumeLanguage.fromDevice(Locale("xx")))
        assertEquals(ResumeLanguage.SPANISH, ResumeLanguage.fromLabel("Spanish (Español)"))
    }
}
