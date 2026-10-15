package com.resume.builder.di

import com.resume.builder.presentation.ai.AiResumeViewModel
import com.resume.builder.presentation.coverletter.CoverLetterEditorViewModel
import com.resume.builder.presentation.coverletter.CoverLetterGenerateViewModel
import com.resume.builder.presentation.coverletter.CoverLetterPreviewViewModel
import com.resume.builder.presentation.coverletter.CoverLettersViewModel
import com.resume.builder.presentation.editor.ResumeEditorViewModel
import com.resume.builder.presentation.editor.SectionEditorViewModel
import com.resume.builder.presentation.home.HomeViewModel
import com.resume.builder.presentation.main.AppViewModel
import com.resume.builder.presentation.onboarding.OnboardingViewModel
import com.resume.builder.presentation.pdftools.PdfTool
import com.resume.builder.presentation.pdftools.PdfToolViewModel
import com.resume.builder.presentation.preview.ResumePreviewViewModel
import com.resume.builder.presentation.saved.SavedFilesViewModel
import com.resume.builder.presentation.settings.SettingsViewModel
import com.resume.builder.presentation.templates.TemplatesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::CoverLettersViewModel)
    viewModelOf(::TemplatesViewModel)
    viewModelOf(::SavedFilesViewModel)
    viewModelOf(::AiResumeViewModel)
    viewModelOf(::SettingsViewModel)

    viewModel { (resumeId: String) -> ResumeEditorViewModel(resumeId, get()) }
    viewModel { (resumeId: String, sectionId: String) -> SectionEditorViewModel(resumeId, sectionId, get(), get(), get(), get()) }
    viewModel { (resumeId: String) -> ResumePreviewViewModel(resumeId, get(), get(), get()) }
    viewModel { (resumeId: String) -> CoverLetterGenerateViewModel(resumeId, get(), get()) }
    viewModel { params ->
        CoverLetterEditorViewModel(params.values.getOrNull(0) as String?, params.values.getOrNull(1) as String?, get(), get(), get())
    }
    viewModel { (letterId: String) -> CoverLetterPreviewViewModel(letterId, get(), get(), get()) }
    viewModel { (tool: PdfTool) -> PdfToolViewModel(tool, get(), get()) }
}
