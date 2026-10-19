package com.resume.builder.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * The language a resume is written in. Drives the section headings, the "Present" label, level
 * labels and right-to-left layout of the rendered document. The user's own text is never translated.
 *
 * Serialized by constant name, so entries can be reordered or added but never renamed.
 *
 * [titles] follow [HEADING_ORDER]; the level lists follow the English ones in [SectionType].
 */
@Serializable
enum class ResumeLanguage(
    val codes: List<String>,
    val englishName: String,
    val nativeName: String,
    val present: String,
    private val titles: List<String>,
    private val skillLevels: List<String>,
    private val languageLevels: List<String>,
    val rtl: Boolean = false,
) {
    ENGLISH(
        listOf("en"), "English", "English", "Present",
        HEADING_ORDER.map { it.defaultTitle },
        SectionType.SKILLS.fields!!.levelLabels!!,
        SectionType.LANGUAGES.fields!!.levelLabels!!,
    ),
    SPANISH(
        listOf("es"), "Spanish", "Español", "Actualidad",
        listOf(
            "Contacto", "Perfil", "Habilidades", "Experiencia laboral", "Proyectos", "Educación",
            "Certificaciones y premios", "Publicaciones", "Idiomas", "Intereses", "Actividades", "Referencias", "Firma",
        ),
        listOf("Principiante", "Elemental", "Intermedio", "Avanzado", "Experto"),
        listOf("Básico", "Conversacional", "Profesional", "Fluido", "Nativo"),
    ),
    FRENCH(
        listOf("fr"), "French", "Français", "Aujourd'hui",
        listOf(
            "Contact", "Profil", "Compétences", "Expérience professionnelle", "Projets", "Formation",
            "Certifications et prix", "Publications", "Langues", "Centres d'intérêt", "Activités", "Références", "Signature",
        ),
        listOf("Débutant", "Élémentaire", "Intermédiaire", "Avancé", "Expert"),
        listOf("Notions", "Conversationnel", "Professionnel", "Courant", "Langue maternelle"),
    ),
    PORTUGUESE(
        listOf("pt"), "Portuguese", "Português", "Atual",
        listOf(
            "Contato", "Resumo", "Competências", "Experiência profissional", "Projetos", "Formação",
            "Certificações e prêmios", "Publicações", "Idiomas", "Interesses", "Atividades", "Referências", "Assinatura",
        ),
        listOf("Iniciante", "Básico", "Intermediário", "Avançado", "Especialista"),
        listOf("Básico", "Conversação", "Profissional", "Fluente", "Nativo"),
    ),
    INDONESIAN(
        listOf("id", "in"), "Indonesian", "Bahasa Indonesia", "Sekarang",
        listOf(
            "Kontak", "Ringkasan", "Keahlian", "Pengalaman Kerja", "Proyek", "Pendidikan",
            "Sertifikasi & Penghargaan", "Publikasi", "Bahasa", "Minat", "Kegiatan", "Referensi", "Tanda Tangan",
        ),
        listOf("Pemula", "Dasar", "Menengah", "Mahir", "Ahli"),
        listOf("Dasar", "Percakapan", "Profesional", "Fasih", "Penutur Asli"),
    ),
    ARABIC(
        listOf("ar"), "Arabic", "العربية", "حتى الآن",
        listOf(
            "معلومات الاتصال", "الملخص", "المهارات", "الخبرة العملية", "المشاريع", "التعليم",
            "الشهادات والجوائز", "المنشورات", "اللغات", "الاهتمامات", "الأنشطة", "المراجع", "التوقيع",
        ),
        listOf("مبتدئ", "أساسي", "متوسط", "متقدم", "خبير"),
        listOf("أساسي", "محادثة", "مهني", "بطلاقة", "اللغة الأم"),
        rtl = true,
    ),
    ITALIAN(
        listOf("it"), "Italian", "Italiano", "Presente",
        listOf(
            "Contatti", "Profilo", "Competenze", "Esperienza lavorativa", "Progetti", "Istruzione",
            "Certificazioni e premi", "Pubblicazioni", "Lingue", "Interessi", "Attività", "Referenze", "Firma",
        ),
        listOf("Principiante", "Elementare", "Intermedio", "Avanzato", "Esperto"),
        listOf("Base", "Conversazione", "Professionale", "Fluente", "Madrelingua"),
    ),
    MALAY(
        listOf("ms"), "Malay", "Bahasa Melayu", "Kini",
        listOf(
            "Hubungan", "Ringkasan", "Kemahiran", "Pengalaman Kerja", "Projek", "Pendidikan",
            "Sijil & Anugerah", "Penerbitan", "Bahasa", "Minat", "Aktiviti", "Rujukan", "Tandatangan",
        ),
        listOf("Permulaan", "Asas", "Pertengahan", "Mahir", "Pakar"),
        listOf("Asas", "Perbualan", "Profesional", "Fasih", "Bahasa Ibunda"),
    ),
    RUSSIAN(
        listOf("ru"), "Russian", "Русский", "настоящее время",
        listOf(
            "Контакты", "О себе", "Навыки", "Опыт работы", "Проекты", "Образование",
            "Сертификаты и награды", "Публикации", "Языки", "Интересы", "Деятельность", "Рекомендации", "Подпись",
        ),
        listOf("Начальный", "Базовый", "Средний", "Продвинутый", "Эксперт"),
        listOf("Базовый", "Разговорный", "Профессиональный", "Свободный", "Родной"),
    ),
    GERMAN(
        listOf("de"), "German", "Deutsch", "Heute",
        listOf(
            "Kontakt", "Profil", "Kenntnisse", "Berufserfahrung", "Projekte", "Ausbildung",
            "Zertifikate & Auszeichnungen", "Publikationen", "Sprachen", "Interessen", "Aktivitäten", "Referenzen", "Unterschrift",
        ),
        listOf("Anfänger", "Grundkenntnisse", "Mittelstufe", "Fortgeschritten", "Experte"),
        listOf("Grundkenntnisse", "Konversationssicher", "Verhandlungssicher", "Fließend", "Muttersprache"),
    ),
    VIETNAMESE(
        listOf("vi"), "Vietnamese", "Tiếng Việt", "Hiện tại",
        listOf(
            "Liên hệ", "Tóm tắt", "Kỹ năng", "Kinh nghiệm làm việc", "Dự án", "Học vấn",
            "Chứng chỉ & Giải thưởng", "Ấn phẩm", "Ngoại ngữ", "Sở thích", "Hoạt động", "Người tham chiếu", "Chữ ký",
        ),
        listOf("Mới bắt đầu", "Cơ bản", "Trung bình", "Nâng cao", "Chuyên gia"),
        listOf("Cơ bản", "Giao tiếp", "Chuyên nghiệp", "Thành thạo", "Bản ngữ"),
    ),
    TURKISH(
        listOf("tr"), "Turkish", "Türkçe", "Halen",
        listOf(
            "İletişim", "Özet", "Yetenekler", "İş Deneyimi", "Projeler", "Eğitim",
            "Sertifikalar ve Ödüller", "Yayınlar", "Diller", "İlgi Alanları", "Etkinlikler", "Referanslar", "İmza",
        ),
        listOf("Başlangıç", "Temel", "Orta", "İleri", "Uzman"),
        listOf("Temel", "Konuşma düzeyi", "Profesyonel", "Akıcı", "Anadil"),
    ),
    HINDI(
        listOf("hi"), "Hindi", "हिन्दी", "वर्तमान",
        listOf(
            "संपर्क", "सारांश", "कौशल", "कार्य अनुभव", "परियोजनाएँ", "शिक्षा",
            "प्रमाणपत्र और पुरस्कार", "प्रकाशन", "भाषाएँ", "रुचियाँ", "गतिविधियाँ", "संदर्भ", "हस्ताक्षर",
        ),
        listOf("शुरुआती", "प्रारंभिक", "मध्यम", "उन्नत", "विशेषज्ञ"),
        listOf("बुनियादी", "बातचीत", "व्यावसायिक", "धाराप्रवाह", "मातृभाषा"),
    ),
    URDU(
        listOf("ur"), "Urdu", "اردو", "تاحال",
        listOf(
            "رابطہ", "خلاصہ", "مہارتیں", "کام کا تجربہ", "منصوبے", "تعلیم",
            "سرٹیفیکیٹس اور اعزازات", "اشاعتیں", "زبانیں", "دلچسپیاں", "سرگرمیاں", "حوالہ جات", "دستخط",
        ),
        listOf("ابتدائی", "بنیادی", "درمیانہ", "اعلیٰ", "ماہر"),
        listOf("بنیادی", "بول چال", "پیشہ ورانہ", "روانی", "مادری زبان"),
        rtl = true,
    ),
    BENGALI(
        listOf("bn"), "Bengali", "বাংলা", "বর্তমান",
        listOf(
            "যোগাযোগ", "সারসংক্ষেপ", "দক্ষতা", "কাজের অভিজ্ঞতা", "প্রকল্প", "শিক্ষা",
            "সনদ ও পুরস্কার", "প্রকাশনা", "ভাষা", "আগ্রহ", "কার্যক্রম", "রেফারেন্স", "স্বাক্ষর",
        ),
        listOf("শিক্ষানবিশ", "প্রাথমিক", "মধ্যম", "উন্নত", "বিশেষজ্ঞ"),
        listOf("প্রাথমিক", "কথোপকথন", "পেশাদার", "সাবলীল", "মাতৃভাষা"),
    ),
    PERSIAN(
        listOf("fa"), "Persian", "فارسی", "تاکنون",
        listOf(
            "اطلاعات تماس", "خلاصه", "مهارت‌ها", "سوابق کاری", "پروژه‌ها", "تحصیلات",
            "گواهینامه‌ها و جوایز", "انتشارات", "زبان‌ها", "علایق", "فعالیت‌ها", "معرف‌ها", "امضا",
        ),
        listOf("مبتدی", "پایه", "متوسط", "پیشرفته", "متخصص"),
        listOf("مقدماتی", "مکالمه", "حرفه‌ای", "روان", "زبان مادری"),
        rtl = true,
    ),
    CHINESE(
        listOf("zh"), "Chinese", "简体中文", "至今",
        listOf(
            "联系方式", "个人简介", "技能", "工作经历", "项目经历", "教育背景",
            "证书与奖项", "出版物", "语言能力", "兴趣爱好", "活动经历", "推荐人", "签名",
        ),
        listOf("入门", "初级", "中级", "高级", "专家"),
        listOf("基础", "日常会话", "专业", "流利", "母语"),
    ),
    JAPANESE(
        listOf("ja"), "Japanese", "日本語", "現在",
        listOf(
            "連絡先", "自己PR", "スキル", "職歴", "プロジェクト", "学歴",
            "資格・受賞歴", "出版物", "語学", "趣味・関心", "活動", "推薦者", "署名",
        ),
        listOf("初心者", "初級", "中級", "上級", "エキスパート"),
        listOf("基礎", "日常会話", "ビジネス", "流暢", "ネイティブ"),
    ),
    KOREAN(
        listOf("ko"), "Korean", "한국어", "현재",
        listOf(
            "연락처", "자기소개", "기술", "경력", "프로젝트", "학력",
            "자격증 및 수상", "출판물", "어학", "관심사", "활동", "추천인", "서명",
        ),
        listOf("입문", "초급", "중급", "고급", "전문가"),
        listOf("기초", "일상 회화", "비즈니스", "유창함", "원어민"),
    ),
    THAI(
        listOf("th"), "Thai", "ไทย", "ปัจจุบัน",
        listOf(
            "ข้อมูลติดต่อ", "ประวัติโดยย่อ", "ทักษะ", "ประสบการณ์ทำงาน", "โครงการ", "การศึกษา",
            "ใบรับรองและรางวัล", "ผลงานตีพิมพ์", "ภาษา", "ความสนใจ", "กิจกรรม", "บุคคลอ้างอิง", "ลายมือชื่อ",
        ),
        listOf("เริ่มต้น", "พื้นฐาน", "ปานกลาง", "ขั้นสูง", "เชี่ยวชาญ"),
        listOf("พื้นฐาน", "สนทนาได้", "ระดับมืออาชีพ", "คล่องแคล่ว", "เจ้าของภาษา"),
    ),
    DUTCH(
        listOf("nl"), "Dutch", "Nederlands", "Heden",
        listOf(
            "Contact", "Profiel", "Vaardigheden", "Werkervaring", "Projecten", "Opleiding",
            "Certificaten & prijzen", "Publicaties", "Talen", "Interesses", "Activiteiten", "Referenties", "Handtekening",
        ),
        listOf("Beginner", "Basis", "Gemiddeld", "Gevorderd", "Expert"),
        listOf("Basis", "Conversatie", "Professioneel", "Vloeiend", "Moedertaal"),
    ),
    POLISH(
        listOf("pl"), "Polish", "Polski", "obecnie",
        listOf(
            "Kontakt", "Podsumowanie", "Umiejętności", "Doświadczenie zawodowe", "Projekty", "Wykształcenie",
            "Certyfikaty i nagrody", "Publikacje", "Języki", "Zainteresowania", "Aktywności", "Referencje", "Podpis",
        ),
        listOf("Początkujący", "Podstawowy", "Średniozaawansowany", "Zaawansowany", "Ekspert"),
        listOf("Podstawowy", "Komunikatywny", "Zawodowy", "Biegły", "Ojczysty"),
    ),
    FILIPINO(
        listOf("fil", "tl"), "Filipino", "Filipino", "Kasalukuyan",
        listOf(
            "Kontak", "Buod", "Mga Kasanayan", "Karanasan sa Trabaho", "Mga Proyekto", "Edukasyon",
            "Mga Sertipikasyon at Parangal", "Mga Publikasyon", "Mga Wika", "Mga Interes", "Mga Aktibidad", "Mga Sanggunian", "Lagda",
        ),
        listOf("Baguhan", "Elementarya", "Katamtaman", "Mahusay", "Eksperto"),
        listOf("Batayan", "Pakikipag-usap", "Propesyonal", "Matatas", "Katutubo"),
    ),
    CHINESE_TRADITIONAL(
        listOf("zh-Hant"), "Chinese (Traditional)", "繁體中文", "至今",
        listOf(
            "聯絡方式", "個人簡介", "技能", "工作經歷", "專案經歷", "學歷",
            "證照與獎項", "出版品", "語言能力", "興趣", "活動經歷", "推薦人", "簽名",
        ),
        listOf("入門", "初級", "中級", "高級", "專家"),
        listOf("基礎", "日常會話", "專業", "流利", "母語"),
    ),
    UKRAINIAN(
        listOf("uk"), "Ukrainian", "Українська", "теперішній час",
        listOf(
            "Контакти", "Про себе", "Навички", "Досвід роботи", "Проєкти", "Освіта",
            "Сертифікати та нагороди", "Публікації", "Мови", "Інтереси", "Діяльність", "Рекомендації", "Підпис",
        ),
        listOf("Початковий", "Базовий", "Середній", "Просунутий", "Експерт"),
        listOf("Базовий", "Розмовний", "Професійний", "Вільний", "Рідна"),
    ),
    ROMANIAN(
        listOf("ro"), "Romanian", "Română", "Prezent",
        listOf(
            "Contact", "Profil", "Competențe", "Experiență profesională", "Proiecte", "Educație",
            "Certificări și premii", "Publicații", "Limbi străine", "Interese", "Activități", "Referințe", "Semnătură",
        ),
        listOf("Începător", "Elementar", "Intermediar", "Avansat", "Expert"),
        listOf("De bază", "Conversațional", "Profesional", "Fluent", "Nativ"),
    ),
    GREEK(
        listOf("el"), "Greek", "Ελληνικά", "Σήμερα",
        listOf(
            "Επικοινωνία", "Προφίλ", "Δεξιότητες", "Επαγγελματική εμπειρία", "Έργα", "Εκπαίδευση",
            "Πιστοποιήσεις και βραβεία", "Δημοσιεύσεις", "Γλώσσες", "Ενδιαφέροντα", "Δραστηριότητες", "Συστάσεις", "Υπογραφή",
        ),
        listOf("Αρχάριος", "Στοιχειώδης", "Μέτριος", "Προχωρημένος", "Ειδικός"),
        listOf("Βασικό", "Συνομιλίας", "Επαγγελματικό", "Άριστο", "Μητρική"),
    ),
    CZECH(
        listOf("cs"), "Czech", "Čeština", "současnost",
        listOf(
            "Kontakt", "Profil", "Dovednosti", "Pracovní zkušenosti", "Projekty", "Vzdělání",
            "Certifikáty a ocenění", "Publikace", "Jazyky", "Zájmy", "Aktivity", "Reference", "Podpis",
        ),
        listOf("Začátečník", "Základní", "Středně pokročilý", "Pokročilý", "Expert"),
        listOf("Základní", "Konverzační", "Profesionální", "Plynulá", "Rodilý mluvčí"),
    ),
    SLOVAK(
        listOf("sk"), "Slovak", "Slovenčina", "súčasnosť",
        listOf(
            "Kontakt", "Profil", "Zručnosti", "Pracovné skúsenosti", "Projekty", "Vzdelanie",
            "Certifikáty a ocenenia", "Publikácie", "Jazyky", "Záujmy", "Aktivity", "Referencie", "Podpis",
        ),
        listOf("Začiatočník", "Základný", "Mierne pokročilý", "Pokročilý", "Expert"),
        listOf("Základný", "Konverzačný", "Profesionálny", "Plynulý", "Materinský"),
    ),
    HUNGARIAN(
        listOf("hu"), "Hungarian", "Magyar", "jelenleg",
        listOf(
            "Elérhetőség", "Bemutatkozás", "Készségek", "Szakmai tapasztalat", "Projektek", "Tanulmányok",
            "Tanúsítványok és díjak", "Publikációk", "Nyelvtudás", "Érdeklődési kör", "Tevékenységek", "Referenciák", "Aláírás",
        ),
        listOf("Kezdő", "Alapfokú", "Középfokú", "Haladó", "Szakértő"),
        listOf("Alapfok", "Társalgási szint", "Szakmai szint", "Folyékony", "Anyanyelv"),
    ),
    SWEDISH(
        listOf("sv"), "Swedish", "Svenska", "Nuvarande",
        listOf(
            "Kontakt", "Profil", "Färdigheter", "Arbetslivserfarenhet", "Projekt", "Utbildning",
            "Certifikat och utmärkelser", "Publikationer", "Språk", "Intressen", "Aktiviteter", "Referenser", "Underskrift",
        ),
        listOf("Nybörjare", "Grundläggande", "Medel", "Avancerad", "Expert"),
        listOf("Grundläggande", "Konversation", "Professionell", "Flytande", "Modersmål"),
    ),
    DANISH(
        listOf("da"), "Danish", "Dansk", "Nu",
        listOf(
            "Kontakt", "Profil", "Kompetencer", "Erhvervserfaring", "Projekter", "Uddannelse",
            "Certificeringer og priser", "Publikationer", "Sprog", "Interesser", "Aktiviteter", "Referencer", "Underskrift",
        ),
        listOf("Begynder", "Grundlæggende", "Mellem", "Avanceret", "Ekspert"),
        listOf("Grundlæggende", "Samtale", "Professionel", "Flydende", "Modersmål"),
    ),
    NORWEGIAN(
        listOf("nb", "no", "nn"), "Norwegian", "Norsk", "Nå",
        listOf(
            "Kontakt", "Profil", "Ferdigheter", "Arbeidserfaring", "Prosjekter", "Utdanning",
            "Sertifiseringer og priser", "Publikasjoner", "Språk", "Interesser", "Aktiviteter", "Referanser", "Signatur",
        ),
        listOf("Nybegynner", "Grunnleggende", "Middels", "Avansert", "Ekspert"),
        listOf("Grunnleggende", "Samtale", "Profesjonell", "Flytende", "Morsmål"),
    ),
    FINNISH(
        listOf("fi"), "Finnish", "Suomi", "nykyhetki",
        listOf(
            "Yhteystiedot", "Profiili", "Taidot", "Työkokemus", "Projektit", "Koulutus",
            "Sertifikaatit ja palkinnot", "Julkaisut", "Kielitaito", "Kiinnostuksen kohteet", "Aktiviteetit", "Suosittelijat", "Allekirjoitus",
        ),
        listOf("Aloittelija", "Perustaso", "Keskitaso", "Edistynyt", "Asiantuntija"),
        listOf("Perustaso", "Keskustelutaso", "Ammatillinen", "Sujuva", "Äidinkieli"),
    ),
    HEBREW(
        listOf("he", "iw"), "Hebrew", "עברית", "היום",
        listOf(
            "פרטי קשר", "תקציר", "כישורים", "ניסיון תעסוקתי", "פרויקטים", "השכלה",
            "תעודות ופרסים", "פרסומים", "שפות", "תחומי עניין", "פעילויות", "ממליצים", "חתימה",
        ),
        listOf("מתחיל", "בסיסי", "בינוני", "מתקדם", "מומחה"),
        listOf("בסיסית", "שיחה", "מקצועית", "שוטפת", "שפת אם"),
        rtl = true,
    ),
    SWAHILI(
        listOf("sw"), "Swahili", "Kiswahili", "Sasa",
        listOf(
            "Mawasiliano", "Muhtasari", "Ujuzi", "Uzoefu wa Kazi", "Miradi", "Elimu",
            "Vyeti na Tuzo", "Machapisho", "Lugha", "Mambo Yanayonivutia", "Shughuli", "Wadhamini", "Sahihi",
        ),
        listOf("Mwanzilishi", "Msingi", "Kati", "Juu", "Mtaalamu"),
        listOf("Msingi", "Mazungumzo", "Kitaaluma", "Fasaha", "Lugha ya Mama"),
    ),
    TAMIL(
        listOf("ta"), "Tamil", "தமிழ்", "தற்போது",
        listOf(
            "தொடர்பு", "சுருக்கம்", "திறன்கள்", "பணி அனுபவம்", "திட்டங்கள்", "கல்வி",
            "சான்றிதழ்கள் & விருதுகள்", "வெளியீடுகள்", "மொழிகள்", "ஆர்வங்கள்", "செயல்பாடுகள்", "பரிந்துரையாளர்கள்", "கையொப்பம்",
        ),
        listOf("தொடக்கநிலை", "அடிப்படை", "இடைநிலை", "மேம்பட்ட", "நிபுணர்"),
        listOf("அடிப்படை", "உரையாடல்", "தொழில்முறை", "சரளம்", "தாய்மொழி"),
    ),
    TELUGU(
        listOf("te"), "Telugu", "తెలుగు", "ప్రస్తుతం",
        listOf(
            "సంప్రదింపు", "సారాంశం", "నైపుణ్యాలు", "పని అనుభవం", "ప్రాజెక్టులు", "విద్య",
            "ధృవపత్రాలు & అవార్డులు", "ప్రచురణలు", "భాషలు", "ఆసక్తులు", "కార్యకలాపాలు", "సిఫార్సుదారులు", "సంతకం",
        ),
        listOf("ప్రారంభ", "ప్రాథమిక", "మధ్యస్థ", "ఉన్నత", "నిపుణుడు"),
        listOf("ప్రాథమిక", "సంభాషణ", "వృత్తిపరమైన", "అనర్గళం", "మాతృభాష"),
    ),
    MARATHI(
        listOf("mr"), "Marathi", "मराठी", "सध्या",
        listOf(
            "संपर्क", "सारांश", "कौशल्ये", "कामाचा अनुभव", "प्रकल्प", "शिक्षण",
            "प्रमाणपत्रे व पुरस्कार", "प्रकाशने", "भाषा", "आवडी", "उपक्रम", "संदर्भ", "स्वाक्षरी",
        ),
        listOf("नवशिका", "प्राथमिक", "मध्यम", "प्रगत", "तज्ज्ञ"),
        listOf("प्राथमिक", "संभाषण", "व्यावसायिक", "अस्खलित", "मातृभाषा"),
    ),
    GUJARATI(
        listOf("gu"), "Gujarati", "ગુજરાતી", "હાલમાં",
        listOf(
            "સંપર્ક", "સારાંશ", "કૌશલ્ય", "કાર્ય અનુભવ", "પ્રોજેક્ટ્સ", "શિક્ષણ",
            "પ્રમાણપત્રો અને પુરસ્કારો", "પ્રકાશનો", "ભાષાઓ", "રુચિઓ", "પ્રવૃત્તિઓ", "સંદર્ભો", "સહી",
        ),
        listOf("શરૂઆતી", "પ્રાથમિક", "મધ્યમ", "અદ્યતન", "નિષ્ણાત"),
        listOf("પ્રાથમિક", "વાતચીત", "વ્યાવસાયિક", "અસ્ખલિત", "માતૃભાષા"),
    ),
    PUNJABI(
        listOf("pa"), "Punjabi", "ਪੰਜਾਬੀ", "ਹੁਣ ਤੱਕ",
        listOf(
            "ਸੰਪਰਕ", "ਸਾਰ", "ਹੁਨਰ", "ਕੰਮ ਦਾ ਤਜਰਬਾ", "ਪ੍ਰੋਜੈਕਟ", "ਸਿੱਖਿਆ",
            "ਸਰਟੀਫਿਕੇਟ ਅਤੇ ਇਨਾਮ", "ਪ੍ਰਕਾਸ਼ਨ", "ਭਾਸ਼ਾਵਾਂ", "ਰੁਚੀਆਂ", "ਗਤੀਵਿਧੀਆਂ", "ਹਵਾਲੇ", "ਦਸਤਖਤ",
        ),
        listOf("ਸ਼ੁਰੂਆਤੀ", "ਮੁੱਢਲਾ", "ਦਰਮਿਆਨਾ", "ਉੱਨਤ", "ਮਾਹਰ"),
        listOf("ਮੁੱਢਲੀ", "ਗੱਲਬਾਤ", "ਪੇਸ਼ੇਵਰ", "ਰਵਾਂ", "ਮਾਂ-ਬੋਲੀ"),
    ),
    NEPALI(
        listOf("ne"), "Nepali", "नेपाली", "हालसम्म",
        listOf(
            "सम्पर्क", "सारांश", "सीपहरू", "कार्य अनुभव", "परियोजनाहरू", "शिक्षा",
            "प्रमाणपत्र तथा पुरस्कार", "प्रकाशनहरू", "भाषाहरू", "रुचिहरू", "गतिविधिहरू", "सन्दर्भहरू", "हस्ताक्षर",
        ),
        listOf("सुरुवाती", "आधारभूत", "मध्यम", "उन्नत", "विशेषज्ञ"),
        listOf("आधारभूत", "कुराकानी", "व्यावसायिक", "धाराप्रवाह", "मातृभाषा"),
    ),
    SERBIAN(
        listOf("sr"), "Serbian", "Српски", "данас",
        listOf(
            "Контакт", "Профил", "Вештине", "Радно искуство", "Пројекти", "Образовање",
            "Сертификати и награде", "Публикације", "Језици", "Интересовања", "Активности", "Препоруке", "Потпис",
        ),
        listOf("Почетник", "Основни", "Средњи", "Напредни", "Експерт"),
        listOf("Основни", "Конверзацијски", "Професионални", "Течно", "Матерњи"),
    ),
    CROATIAN(
        listOf("hr", "bs"), "Croatian", "Hrvatski", "danas",
        listOf(
            "Kontakt", "Profil", "Vještine", "Radno iskustvo", "Projekti", "Obrazovanje",
            "Certifikati i nagrade", "Publikacije", "Jezici", "Interesi", "Aktivnosti", "Preporuke", "Potpis",
        ),
        listOf("Početnik", "Osnovno", "Srednje", "Napredno", "Stručnjak"),
        listOf("Osnovno", "Konverzacijsko", "Profesionalno", "Tečno", "Materinski"),
    ),
    BULGARIAN(
        listOf("bg"), "Bulgarian", "Български", "досега",
        listOf(
            "Контакти", "Профил", "Умения", "Професионален опит", "Проекти", "Образование",
            "Сертификати и награди", "Публикации", "Езици", "Интереси", "Дейности", "Препоръки", "Подпис",
        ),
        listOf("Начинаещ", "Основно", "Средно", "Напреднало", "Експерт"),
        listOf("Основно", "Разговорно", "Професионално", "Свободно", "Роден"),
    ),
    CATALAN(
        listOf("ca"), "Catalan", "Català", "Actualitat",
        listOf(
            "Contacte", "Perfil", "Habilitats", "Experiència laboral", "Projectes", "Formació",
            "Certificacions i premis", "Publicacions", "Idiomes", "Interessos", "Activitats", "Referències", "Signatura",
        ),
        listOf("Principiant", "Elemental", "Intermedi", "Avançat", "Expert"),
        listOf("Bàsic", "Conversacional", "Professional", "Fluid", "Natiu"),
    ),
    AZERBAIJANI(
        listOf("az"), "Azerbaijani", "Azərbaycanca", "hazırda",
        listOf(
            "Əlaqə", "Haqqımda", "Bacarıqlar", "İş təcrübəsi", "Layihələr", "Təhsil",
            "Sertifikatlar və mükafatlar", "Nəşrlər", "Dillər", "Maraqlar", "Fəaliyyətlər", "Tövsiyəçilər", "İmza",
        ),
        listOf("Başlanğıc", "Əsas", "Orta", "İrəli", "Ekspert"),
        listOf("Əsas", "Danışıq", "Peşəkar", "Sərbəst", "Ana dili"),
    ),
    KAZAKH(
        listOf("kk"), "Kazakh", "Қазақша", "қазірге дейін",
        listOf(
            "Байланыс", "Өзім туралы", "Дағдылар", "Жұмыс тәжірибесі", "Жобалар", "Білім",
            "Сертификаттар мен марапаттар", "Жарияланымдар", "Тілдер", "Қызығушылықтар", "Іс-шаралар", "Ұсынымдар", "Қолтаңба",
        ),
        listOf("Бастауыш", "Негізгі", "Орта", "Жоғары", "Сарапшы"),
        listOf("Негізгі", "Сөйлесу", "Кәсіби", "Еркін", "Ана тілі"),
    ),
    UZBEK(
        listOf("uz"), "Uzbek", "Oʻzbekcha", "hozirgacha",
        listOf(
            "Aloqa", "Men haqimda", "Koʻnikmalar", "Ish tajribasi", "Loyihalar", "Taʼlim",
            "Sertifikatlar va mukofotlar", "Nashrlar", "Tillar", "Qiziqishlar", "Faoliyat", "Tavsiyanomalar", "Imzo",
        ),
        listOf("Boshlangʻich", "Elementar", "Oʻrta", "Yuqori", "Ekspert"),
        listOf("Boshlangʻich", "Soʻzlashuv", "Professional", "Erkin", "Ona tili"),
    ),
    PASHTO(
        listOf("ps"), "Pashto", "پښتو", "تر اوسه",
        listOf(
            "اړیکه", "لنډیز", "مهارتونه", "کاري تجربه", "پروژې", "زده کړې",
            "سندونه او جایزې", "خپرونې", "ژبې", "لېوالتیاوې", "فعالیتونه", "سپارښتنې", "لاسلیک",
        ),
        listOf("پیل کوونکی", "ابتدايي", "منځنی", "پرمختللی", "متخصص"),
        listOf("ابتدايي", "خبرې اترې", "مسلکي", "روان", "مورنۍ ژبه"),
        rtl = true,
    ),
    AFRIKAANS(
        listOf("af"), "Afrikaans", "Afrikaans", "Hede",
        listOf(
            "Kontak", "Profiel", "Vaardighede", "Werkservaring", "Projekte", "Opleiding",
            "Sertifikate en toekennings", "Publikasies", "Tale", "Belangstellings", "Aktiwiteite", "Verwysings", "Handtekening",
        ),
        listOf("Beginner", "Basies", "Intermediêr", "Gevorderd", "Kenner"),
        listOf("Basies", "Gesprek", "Professioneel", "Vlot", "Moedertaal"),
    );

    /** "Spanish (Español)" style label used by pickers. */
    val label: String get() = if (englishName == nativeName) englishName else "$englishName ($nativeName)"

    fun heading(type: SectionType): String = titles.getOrNull(HEADING_ORDER.indexOf(type)) ?: type.defaultTitle

    /** Uses the translated heading only while the user kept a default title (in any language). */
    fun headingFor(section: ResumeSection): String =
        if (isDefaultTitle(section.type, section.title)) heading(section.type) else section.title

    /** Translates the English level labels of [fields]; anything else is returned unchanged. */
    fun levelLabels(fields: ItemFields?): List<String>? = when (val english = fields?.levelLabels) {
        SectionType.SKILLS.fields?.levelLabels -> skillLevels
        SectionType.LANGUAGES.fields?.levelLabels -> languageLevels
        else -> english
    }

    companion object {
        /** Language matching the device locale, English otherwise. */
        fun fromDevice(locale: Locale = Locale.getDefault()): ResumeLanguage {
            val traditional = locale.language == "zh" && (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO"))
            if (traditional) return CHINESE_TRADITIONAL
            return entries.firstOrNull { locale.language in it.codes } ?: ENGLISH
        }

        /** Matches a picker label or plain English name (e.g. from the AI request) back to a language. */
        fun fromLabel(value: String): ResumeLanguage? =
            entries.firstOrNull { it.label.equals(value, true) || it.englishName.equals(value, true) || it.nativeName.equals(value, true) }

        fun isDefaultTitle(type: SectionType, title: String): Boolean =
            type != SectionType.CUSTOM && (title == type.defaultTitle || entries.any { it.heading(type) == title })

        /** Stores a title the user typed as the language-neutral default when it matches a translated heading. */
        fun normalizeTitle(type: SectionType, title: String): String =
            if (title.isBlank() || isDefaultTitle(type, title)) type.defaultTitle else title
    }
}

private val HEADING_ORDER = listOf(
    SectionType.CONTACT, SectionType.SUMMARY, SectionType.SKILLS, SectionType.EXPERIENCE, SectionType.PROJECTS,
    SectionType.EDUCATION, SectionType.CERTIFICATIONS, SectionType.PUBLICATIONS, SectionType.LANGUAGES,
    SectionType.INTERESTS, SectionType.ACTIVITIES, SectionType.REFERENCES, SectionType.SIGNATURE,
)
