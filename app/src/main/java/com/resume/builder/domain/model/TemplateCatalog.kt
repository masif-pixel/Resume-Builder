package com.resume.builder.domain.model

enum class LayoutKind { SIDEBAR, SINGLE_COLUMN, SPLIT_BELOW_HEADER }

enum class HeaderStyle {
    /** Photo sits in the sidebar, name + job title top of the main column. */
    SIDEBAR_PHOTO,
    /** Full-width tinted band with photo left of the name. */
    BAND,
    /** Centered photo, name and title. */
    CENTERED,
    /** Plain, left aligned name with small photo. */
    LEFT,
    /** Full-bleed accent block with light name, title and contact. */
    SOLID,
    /** Name (and photo) on the left, contact details stacked on the right. */
    SPLIT,
}

enum class HeadingStyle {
    UNDERLINE, FILLED_BAR, BOXED, UPPERCASE_ACCENT, LEFT_BAR, CENTERED_RULE,
    /** Uppercase label inside a soft rounded pill. */
    PILL,
    /** Uppercase label between a heavy top rule and a hairline. */
    DOUBLE_RULE,
    /** Square accent marker, label, then a hairline to the column edge. */
    DOT_RULE,
    /** Label in an accent tab sitting on a full-width rule. */
    TAB,
}

/** Visual recipe for a template. Colors are ARGB longs so the domain stays Android-free. */
data class TemplateSpec(
    val id: String,
    val name: String,
    val tagline: String,
    val layout: LayoutKind,
    val header: HeaderStyle,
    val heading: HeadingStyle,
    val sidebarRatio: Float = 0.33f,
    val sidebarOnRight: Boolean = false,
    val accent: Long,
    val sidebar: Long = 0xFFFFFFFF,
    val sidebarText: Long = 0xFFFFFFFF,
    val background: Long = 0xFFFFFFFF,
    val text: Long = 0xFF2B2F3A,
    val nameColor: Long = 0xFF1B1E27,
    val jobTitle: Long = 0xFF5B6170,
    val headingColor: Long = accent,
    val font: ResumeFont = ResumeFont.SANS,
    val photoRound: Boolean = true,
    val timelineDates: Boolean = false,
    val skillBars: Boolean = false,
    val angledHeader: Boolean = false,
    val uppercaseName: Boolean = false,
    val atsFriendly: Boolean = false,
    /** Thin accent stripe across the top edge of every page. */
    val topStripe: Boolean = false,
    /** Hairline frame inset from the page edges. */
    val pageFrame: Boolean = false,
    val category: TemplateCategory = TemplateCategory.MODERN,
)

enum class TemplateCategory(val label: String) { MODERN("Modern"), PROFESSIONAL("Professional"), CREATIVE("Creative"), ATS("ATS-friendly") }

object TemplateCatalog {
    const val DEFAULT_ID = "midnight"

    val all: List<TemplateSpec> = listOf(
        TemplateSpec(
            id = "midnight", name = "Midnight", tagline = "Dark sidebar, confident and clean",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.UNDERLINE,
            accent = 0xFF3D4A63, sidebar = 0xFF2F3A4F, headingColor = 0xFF2F3A4F, category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "aurum", name = "Aurum", tagline = "Charcoal & gold with skill bars",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.UPPERCASE_ACCENT,
            accent = 0xFFE7A93B, sidebar = 0xFF2A2A2E, headingColor = 0xFF2A2A2E, skillBars = true, category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "evergreen", name = "Evergreen", tagline = "Fresh green with boxed headings",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.BOXED,
            accent = 0xFF3F9A6E, sidebar = 0xFF3F9A6E, timelineDates = true, uppercaseName = false,
            category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "horizon", name = "Horizon", tagline = "Soft header band, single column",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.BAND, heading = HeadingStyle.LEFT_BAR,
            accent = 0xFF3B82F6, headingColor = 0xFF1E3A8A, category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "prism", name = "Prism", tagline = "Angled color header, airy sidebar",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.UNDERLINE,
            accent = 0xFF6D7FD6, sidebar = 0xFFF1F4FB, sidebarText = 0xFF2B2F3A, angledHeader = true,
            nameColor = 0xFF4E61C0, category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "atelier", name = "Atelier", tagline = "Centered portrait, timeless balance",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.CENTERED, heading = HeadingStyle.CENTERED_RULE,
            accent = 0xFF374151, headingColor = 0xFF111827, category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "editorial", name = "Editorial", tagline = "Serif typography in two columns",
            layout = LayoutKind.SPLIT_BELOW_HEADER, header = HeaderStyle.LEFT, heading = HeadingStyle.UNDERLINE,
            accent = 0xFF6B7280, headingColor = 0xFF374151, font = ResumeFont.SERIF, sidebarRatio = 0.36f,
            category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "violet", name = "Violet Ribbon", tagline = "Bold ribbons on a light sidebar",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.FILLED_BAR,
            accent = 0xFF9B51E0, sidebar = 0xFFF6F1FD, sidebarText = 0xFF2B2F3A, category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "clarity", name = "Clarity", tagline = "Recruiter-proof, parses perfectly",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.CENTERED, heading = HeadingStyle.UNDERLINE,
            accent = 0xFF1D4ED8, nameColor = 0xFF1D4ED8, headingColor = 0xFF1D4ED8, atsFriendly = true, category = TemplateCategory.ATS,
        ),
        TemplateSpec(
            id = "compact", name = "Compact Pro", tagline = "Dense layout, fits more on one page",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.LEFT, heading = HeadingStyle.UNDERLINE,
            accent = 0xFF111827, headingColor = 0xFF111827, photoRound = false, atsFriendly = true, category = TemplateCategory.ATS,
        ),
        TemplateSpec(
            id = "ember", name = "Ember", tagline = "Warm orange accents, ATS safe",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.LEFT, heading = HeadingStyle.UPPERCASE_ACCENT,
            accent = 0xFFEA580C, nameColor = 0xFFEA580C, headingColor = 0xFFC2410C, atsFriendly = true, category = TemplateCategory.ATS,
        ),
        TemplateSpec(
            id = "oceanic", name = "Oceanic", tagline = "Right sidebar with teal timeline",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.LEFT_BAR,
            accent = 0xFF0F8B8D, sidebar = 0xFF0B4F5C, sidebarOnRight = true, timelineDates = true, skillBars = true,
            headingColor = 0xFF0B4F5C, category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "executive", name = "Executive", tagline = "Serif authority with a split header",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.SPLIT, heading = HeadingStyle.DOUBLE_RULE,
            accent = 0xFF1F3A5F, nameColor = 0xFF1F3A5F, headingColor = 0xFF1F3A5F, font = ResumeFont.SERIF,
            uppercaseName = true, category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "sapphire", name = "Sapphire", tagline = "Bold blue header, pill headings",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.SOLID, heading = HeadingStyle.PILL,
            accent = 0xFF1E40AF, headingColor = 0xFF1E3A8A, category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "noir", name = "Noir", tagline = "Dark luxe page with gold accents",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.CENTERED, heading = HeadingStyle.UPPERCASE_ACCENT,
            accent = 0xFFD4AF37, background = 0xFF14161C, font = ResumeFont.LIGHT, uppercaseName = true,
            category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "graphite", name = "Graphite", tagline = "Graphite sidebar, emerald markers",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.DOT_RULE,
            accent = 0xFF10B981, sidebar = 0xFF374151, headingColor = 0xFF1F2937, skillBars = true,
            category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "blush", name = "Blush", tagline = "Soft rose sidebar on the right",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.PILL,
            accent = 0xFFDB2777, sidebar = 0xFFFBEFF3, sidebarText = 0xFF2B2F3A, sidebarOnRight = true,
            nameColor = 0xFF9D174D, headingColor = 0xFF9D174D, font = ResumeFont.LIGHT, category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "boardroom", name = "Boardroom", tagline = "Two columns with tabbed headings",
            layout = LayoutKind.SPLIT_BELOW_HEADER, header = HeaderStyle.SPLIT, heading = HeadingStyle.TAB,
            accent = 0xFF0F172A, headingColor = 0xFF0F172A, font = ResumeFont.MEDIUM, sidebarRatio = 0.34f,
            category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "terracotta", name = "Terracotta", tagline = "Earthy serif with a warm top stripe",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.LEFT, heading = HeadingStyle.DOT_RULE,
            accent = 0xFFC2410C, nameColor = 0xFF7C2D12, headingColor = 0xFF7C2D12, font = ResumeFont.SERIF,
            topStripe = true, category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "nordic", name = "Nordic", tagline = "Minimal Scandinavian calm",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.LEFT, heading = HeadingStyle.UNDERLINE,
            accent = 0xFF475569, headingColor = 0xFF334155, font = ResumeFont.LIGHT, photoRound = false,
            atsFriendly = true, category = TemplateCategory.ATS,
        ),
        TemplateSpec(
            id = "royal", name = "Royal", tagline = "Deep purple with classic rules",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.DOUBLE_RULE,
            accent = 0xFFA855F7, sidebar = 0xFF3B0764, headingColor = 0xFF3B0764, nameColor = 0xFF3B0764,
            font = ResumeFont.SERIF, category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "coral", name = "Coral Reef", tagline = "Coral band with a dated timeline",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.BAND, heading = HeadingStyle.TAB,
            accent = 0xFFF43F5E, headingColor = 0xFFBE123C, timelineDates = true, category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "slate", name = "Slate Pro", tagline = "Condensed two columns, packs more in",
            layout = LayoutKind.SPLIT_BELOW_HEADER, header = HeaderStyle.LEFT, heading = HeadingStyle.LEFT_BAR,
            accent = 0xFF334155, headingColor = 0xFF1E293B, font = ResumeFont.CONDENSED, topStripe = true,
            sidebarRatio = 0.34f, category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "mint", name = "Mint", tagline = "Fresh mint sidebar with tab headings",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.TAB,
            accent = 0xFF059669, sidebar = 0xFFECFDF5, sidebarText = 0xFF2B2F3A, nameColor = 0xFF065F46,
            timelineDates = true, category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "heritage", name = "Heritage", tagline = "Framed classic for senior roles",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.CENTERED, heading = HeadingStyle.CENTERED_RULE,
            accent = 0xFF7C2D12, nameColor = 0xFF7C2D12, headingColor = 0xFF451A03, font = ResumeFont.SERIF,
            uppercaseName = true, pageFrame = true, category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "cobalt", name = "Cobalt", tagline = "Cobalt sidebar with filled bars",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.FILLED_BAR,
            accent = 0xFF2563EB, sidebar = 0xFF1E3A8A, skillBars = true, nameColor = 0xFF1E3A8A,
            category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "linen", name = "Linen", tagline = "Warm paper tone, refined serif",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.SPLIT, heading = HeadingStyle.UNDERLINE,
            accent = 0xFF8B6F47, background = 0xFFFAF7F2, nameColor = 0xFF3F3122, headingColor = 0xFF5C4630,
            font = ResumeFont.SERIF, category = TemplateCategory.PROFESSIONAL,
        ),
        TemplateSpec(
            id = "quantum", name = "Quantum", tagline = "Teal header, tech-ready two columns",
            layout = LayoutKind.SPLIT_BELOW_HEADER, header = HeaderStyle.SOLID, heading = HeadingStyle.DOT_RULE,
            accent = 0xFF0F766E, headingColor = 0xFF134E4A, sidebarRatio = 0.34f, category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "standard", name = "Standard", tagline = "Classic black & white, maximum ATS",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.LEFT, heading = HeadingStyle.DOUBLE_RULE,
            accent = 0xFF111111, nameColor = 0xFF111111, headingColor = 0xFF111111, font = ResumeFont.SERIF,
            photoRound = false, atsFriendly = true, category = TemplateCategory.ATS,
        ),
        TemplateSpec(
            id = "sunrise", name = "Sunrise", tagline = "Golden angled header, sunny sidebar",
            layout = LayoutKind.SIDEBAR, header = HeaderStyle.SIDEBAR_PHOTO, heading = HeadingStyle.UPPERCASE_ACCENT,
            accent = 0xFFF59E0B, sidebar = 0xFFFFF7ED, sidebarText = 0xFF2B2F3A, angledHeader = true,
            nameColor = 0xFFB45309, headingColor = 0xFF78350F, category = TemplateCategory.CREATIVE,
        ),
        TemplateSpec(
            id = "metro", name = "Metro", tagline = "Black header, bold modern timeline",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.SOLID, heading = HeadingStyle.UPPERCASE_ACCENT,
            accent = 0xFF111827, font = ResumeFont.MEDIUM, uppercaseName = true, timelineDates = true,
            category = TemplateCategory.MODERN,
        ),
        TemplateSpec(
            id = "crisp", name = "Crisp", tagline = "Clean cyan pills, ATS safe",
            layout = LayoutKind.SINGLE_COLUMN, header = HeaderStyle.CENTERED, heading = HeadingStyle.PILL,
            accent = 0xFF0891B2, nameColor = 0xFF0E7490, headingColor = 0xFF155E75, topStripe = true,
            atsFriendly = true, category = TemplateCategory.ATS,
        ),
    )

    fun byId(id: String): TemplateSpec = all.firstOrNull { it.id == id } ?: all.first()
}
