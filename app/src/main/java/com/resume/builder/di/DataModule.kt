package com.resume.builder.di

import androidx.room.Room
import com.resume.builder.data.local.ImageStore
import com.resume.builder.data.local.db.AppDatabase
import com.resume.builder.data.local.prefs.AppPreferences
import com.resume.builder.data.pdf.DocumentExporterImpl
import com.resume.builder.data.pdf.PdfTextExtractor
import com.resume.builder.data.pdf.PdfToolsEngine
import com.resume.builder.data.pdf.ResumeRenderer
import com.resume.builder.data.remote.ai.OfflineAiRepository
import com.resume.builder.domain.repository.AiRepository
import com.resume.builder.data.repository.CoverLetterRepositoryImpl
import com.resume.builder.data.repository.ResumeRepositoryImpl
import com.resume.builder.data.repository.SavedFileRepositoryImpl
import com.resume.builder.domain.repository.CoverLetterRepository
import com.resume.builder.domain.repository.PreferencesRepository
import com.resume.builder.domain.repository.ResumeRepository
import com.resume.builder.domain.repository.SavedFileRepository
import com.resume.builder.domain.usecase.CreateResumeUseCase
import com.resume.builder.domain.usecase.DeleteCoverLetterUseCase
import com.resume.builder.domain.usecase.DeleteResumeUseCase
import com.resume.builder.domain.usecase.DocumentExporter
import com.resume.builder.domain.usecase.DuplicateResumeUseCase
import com.resume.builder.domain.usecase.GenerateAiResumeUseCase
import com.resume.builder.domain.usecase.GenerateCoverLetterUseCase
import com.resume.builder.domain.usecase.ImportResumeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val dataModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().resumeDao() }
    single { get<AppDatabase>().coverLetterDao() }
    single { get<AppDatabase>().savedFileDao() }

    single<PreferencesRepository> { AppPreferences(androidContext()) }
    single<ResumeRepository> { ResumeRepositoryImpl(get()) }
    single<CoverLetterRepository> { CoverLetterRepositoryImpl(get()) }
    single<SavedFileRepository> { SavedFileRepositoryImpl(androidContext(), get()) }

    single { ImageStore(androidContext()) }
    single { PdfTextExtractor(androidContext()) }
    single<AiRepository> { OfflineAiRepository(get()) }
    single { ResumeRenderer() }
    single { PdfToolsEngine(androidContext()) }
    single<DocumentExporter> { DocumentExporterImpl(androidContext(), get(), get()) }
}

val domainModule = module {
    factoryOf(::CreateResumeUseCase)
    factoryOf(::DuplicateResumeUseCase)
    factoryOf(::DeleteResumeUseCase)
    factoryOf(::DeleteCoverLetterUseCase)
    factoryOf(::GenerateAiResumeUseCase)
    factoryOf(::ImportResumeUseCase)
    factoryOf(::GenerateCoverLetterUseCase)
}

val appModules = listOf(dataModule, domainModule, presentationModule)
