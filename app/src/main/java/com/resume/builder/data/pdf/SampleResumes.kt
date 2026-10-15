package com.resume.builder.data.pdf

import com.resume.builder.domain.model.ContactInfo
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionItem
import com.resume.builder.domain.model.SectionType

/** Realistic example content used for template thumbnails and the "start from example" option. */
object SampleResumes {

    fun content(): ResumeContent {
        val sections = ResumeSection.defaults().map { section ->
            when (section.type) {
                SectionType.SKILLS -> section.copy(
                    items = listOf(
                        "Kotlin" to 5, "Jetpack Compose" to 5, "System Design" to 4, "TypeScript" to 4,
                        "Cloud Architecture" to 4, "CI/CD" to 3, "Team Leadership" to 4,
                    ).map { (name, level) -> SectionItem(title = name, level = level) },
                )
                SectionType.EXPERIENCE -> section.copy(
                    items = listOf(
                        SectionItem(
                            title = "Senior Product Engineer", subtitle = "Northwind Labs", location = "Remote",
                            startDate = "2021", isCurrent = true,
                            description = "• Led a squad of 6 engineers shipping a fintech app used by 2M+ customers\n" +
                                "• Cut cold-start time by 42% by modularising the codebase and baseline profiles\n" +
                                "• Introduced design-system tokens that halved UI review cycles",
                        ),
                        SectionItem(
                            title = "Mobile Engineer", subtitle = "Brightpath Health", location = "Austin, TX",
                            startDate = "2018", endDate = "2021",
                            description = "• Built offline-first appointment booking with end-to-end encryption\n" +
                                "• Raised crash-free sessions from 97.1% to 99.6% through observability work",
                        ),
                    ),
                )
                SectionType.PROJECTS -> section.copy(
                    items = listOf(
                        SectionItem(
                            title = "Open Transit", subtitle = "Kotlin Multiplatform", startDate = "2023", endDate = "2024",
                            description = "• Real-time transit tracker with 40k monthly users and a 4.8★ rating",
                        ),
                    ),
                )
                SectionType.EDUCATION -> section.copy(
                    items = listOf(
                        SectionItem(
                            title = "B.Sc. Computer Science", subtitle = "University of Texas", startDate = "2014", endDate = "2018",
                            description = "GPA 3.8 · Graduated with honours",
                        ),
                    ),
                )
                SectionType.CERTIFICATIONS -> section.copy(
                    items = listOf(SectionItem(title = "Google Associate Android Developer", subtitle = "Google", startDate = "2022")),
                )
                SectionType.LANGUAGES -> section.copy(
                    items = listOf(SectionItem(title = "English", level = 5), SectionItem(title = "Spanish", level = 4), SectionItem(title = "German", level = 2)),
                )
                SectionType.INTERESTS -> section.copy(
                    items = listOf("Trail running", "Photography", "Mentoring").map { SectionItem(title = it) },
                )
                else -> section
            }
        }
        return ResumeContent(
            contact = ContactInfo(
                fullName = "Jordan Reyes",
                jobTitle = "Senior Product Engineer",
                email = "jordan.reyes@mail.com",
                phone = "+1 555 204 7781",
                location = "Austin, TX",
                website = "jordanreyes.dev",
                linkedin = "linkedin.com/in/jordanreyes",
            ),
            summary = "Product-minded engineer with 7+ years building delightful, high-scale mobile experiences. " +
                "Known for turning ambiguous ideas into shipped features, mentoring teams and obsessing over performance.",
            sections = sections,
        )
    }

    fun preview(templateId: String): Resume = Resume(
        id = "sample-$templateId",
        title = "Sample",
        templateId = templateId,
        content = content(),
    )
}
