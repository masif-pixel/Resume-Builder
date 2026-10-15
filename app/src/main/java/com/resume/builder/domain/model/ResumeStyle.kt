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

@Serializable
enum class ResumeLanguage(val label: String, val rtl: Boolean = false, val present: String, val headings: Map<SectionType, String>) {
    ENGLISH("English", present = "Present", headings = emptyMap()),
    SPANISH(
        "Español", present = "Actualidad",
        headings = mapOf(
            SectionType.CONTACT to "Contacto", SectionType.SUMMARY to "Perfil", SectionType.SKILLS to "Habilidades",
            SectionType.EXPERIENCE to "Experiencia laboral", SectionType.PROJECTS to "Proyectos", SectionType.EDUCATION to "Educación",
            SectionType.CERTIFICATIONS to "Certificaciones y premios", SectionType.PUBLICATIONS to "Publicaciones",
            SectionType.LANGUAGES to "Idiomas", SectionType.INTERESTS to "Intereses", SectionType.ACTIVITIES to "Actividades",
            SectionType.REFERENCES to "Referencias", SectionType.SIGNATURE to "Firma",
        ),
    ),
    FRENCH(
        "Français", present = "Aujourd'hui",
        headings = mapOf(
            SectionType.CONTACT to "Contact", SectionType.SUMMARY to "Profil", SectionType.SKILLS to "Compétences",
            SectionType.EXPERIENCE to "Expérience professionnelle", SectionType.PROJECTS to "Projets", SectionType.EDUCATION to "Formation",
            SectionType.CERTIFICATIONS to "Certifications et prix", SectionType.PUBLICATIONS to "Publications",
            SectionType.LANGUAGES to "Langues", SectionType.INTERESTS to "Centres d'intérêt", SectionType.ACTIVITIES to "Activités",
            SectionType.REFERENCES to "Références", SectionType.SIGNATURE to "Signature",
        ),
    ),
    GERMAN(
        "Deutsch", present = "Heute",
        headings = mapOf(
            SectionType.CONTACT to "Kontakt", SectionType.SUMMARY to "Profil", SectionType.SKILLS to "Kenntnisse",
            SectionType.EXPERIENCE to "Berufserfahrung", SectionType.PROJECTS to "Projekte", SectionType.EDUCATION to "Ausbildung",
            SectionType.CERTIFICATIONS to "Zertifikate & Auszeichnungen", SectionType.PUBLICATIONS to "Publikationen",
            SectionType.LANGUAGES to "Sprachen", SectionType.INTERESTS to "Interessen", SectionType.ACTIVITIES to "Aktivitäten",
            SectionType.REFERENCES to "Referenzen", SectionType.SIGNATURE to "Unterschrift",
        ),
    ),
    PORTUGUESE(
        "Português", present = "Atual",
        headings = mapOf(
            SectionType.CONTACT to "Contato", SectionType.SUMMARY to "Resumo", SectionType.SKILLS to "Competências",
            SectionType.EXPERIENCE to "Experiência profissional", SectionType.PROJECTS to "Projetos", SectionType.EDUCATION to "Formação",
            SectionType.CERTIFICATIONS to "Certificações e prémios", SectionType.PUBLICATIONS to "Publicações",
            SectionType.LANGUAGES to "Idiomas", SectionType.INTERESTS to "Interesses", SectionType.ACTIVITIES to "Atividades",
            SectionType.REFERENCES to "Referências", SectionType.SIGNATURE to "Assinatura",
        ),
    ),
    TURKISH(
        "Türkçe", present = "Halen",
        headings = mapOf(
            SectionType.CONTACT to "İletişim", SectionType.SUMMARY to "Özet", SectionType.SKILLS to "Yetenekler",
            SectionType.EXPERIENCE to "İş Deneyimi", SectionType.PROJECTS to "Projeler", SectionType.EDUCATION to "Eğitim",
            SectionType.CERTIFICATIONS to "Sertifikalar ve Ödüller", SectionType.PUBLICATIONS to "Yayınlar",
            SectionType.LANGUAGES to "Diller", SectionType.INTERESTS to "İlgi Alanları", SectionType.ACTIVITIES to "Etkinlikler",
            SectionType.REFERENCES to "Referanslar", SectionType.SIGNATURE to "İmza",
        ),
    ),
    HINDI(
        "हिन्दी", present = "वर्तमान",
        headings = mapOf(
            SectionType.CONTACT to "संपर्क", SectionType.SUMMARY to "सारांश", SectionType.SKILLS to "कौशल",
            SectionType.EXPERIENCE to "कार्य अनुभव", SectionType.PROJECTS to "परियोजनाएँ", SectionType.EDUCATION to "शिक्षा",
            SectionType.CERTIFICATIONS to "प्रमाणपत्र और पुरस्कार", SectionType.PUBLICATIONS to "प्रकाशन",
            SectionType.LANGUAGES to "भाषाएँ", SectionType.INTERESTS to "रुचियाँ", SectionType.ACTIVITIES to "गतिविधियाँ",
            SectionType.REFERENCES to "संदर्भ", SectionType.SIGNATURE to "हस्ताक्षर",
        ),
    ),
    ARABIC(
        "العربية", rtl = true, present = "حتى الآن",
        headings = mapOf(
            SectionType.CONTACT to "معلومات الاتصال", SectionType.SUMMARY to "الملخص", SectionType.SKILLS to "المهارات",
            SectionType.EXPERIENCE to "الخبرة العملية", SectionType.PROJECTS to "المشاريع", SectionType.EDUCATION to "التعليم",
            SectionType.CERTIFICATIONS to "الشهادات والجوائز", SectionType.PUBLICATIONS to "المنشورات",
            SectionType.LANGUAGES to "اللغات", SectionType.INTERESTS to "الاهتمامات", SectionType.ACTIVITIES to "الأنشطة",
            SectionType.REFERENCES to "المراجع", SectionType.SIGNATURE to "التوقيع",
        ),
    ),
    URDU(
        "اردو", rtl = true, present = "تاحال",
        headings = mapOf(
            SectionType.CONTACT to "رابطہ", SectionType.SUMMARY to "خلاصہ", SectionType.SKILLS to "مہارتیں",
            SectionType.EXPERIENCE to "کام کا تجربہ", SectionType.PROJECTS to "منصوبے", SectionType.EDUCATION to "تعلیم",
            SectionType.CERTIFICATIONS to "سرٹیفیکیٹس اور اعزازات", SectionType.PUBLICATIONS to "اشاعتیں",
            SectionType.LANGUAGES to "زبانیں", SectionType.INTERESTS to "دلچسپیاں", SectionType.ACTIVITIES to "سرگرمیاں",
            SectionType.REFERENCES to "حوالہ جات", SectionType.SIGNATURE to "دستخط",
        ),
    );

    /** Uses the translated heading only when the user kept the default English title. */
    fun headingFor(section: ResumeSection): String =
        if (section.title == section.type.defaultTitle) headings[section.type] ?: section.title else section.title
}
