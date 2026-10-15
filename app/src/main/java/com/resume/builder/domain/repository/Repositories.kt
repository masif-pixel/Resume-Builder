package com.resume.builder.domain.repository

import android.net.Uri
import com.resume.builder.domain.model.AiResumeRequest
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.CoverLetterRequest
import com.resume.builder.domain.model.FileCategory
import com.resume.builder.domain.model.ImproveKind
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface ResumeRepository {
    fun observeResumes(): Flow<List<Resume>>
    fun observeResume(id: String): Flow<Resume?>
    suspend fun getResume(id: String): Resume?
    suspend fun upsert(resume: Resume, touch: Boolean = true)
    suspend fun delete(id: String)
    suspend fun getAll(): List<Resume>
}

interface CoverLetterRepository {
    fun observeLetters(): Flow<List<CoverLetter>>
    suspend fun getLetter(id: String): CoverLetter?
    suspend fun upsert(letter: CoverLetter, touch: Boolean = true)
    suspend fun delete(id: String)
    suspend fun getAll(): List<CoverLetter>
}

interface SavedFileRepository {
    fun observeFiles(): Flow<List<SavedFile>>
    /** Moves [source] into the app's export folder, mirrors it to public Documents and records it. */
    suspend fun register(source: File, displayName: String, mimeType: String, category: FileCategory): SavedFile
    suspend fun rename(file: SavedFile, newName: String)
    suspend fun delete(file: SavedFile)
    fun exportDir(): File
    fun publicFolderLabel(): String
}

interface PreferencesRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
    fun isOnboardingDone(): Boolean
    fun setOnboardingDone(referralSource: String?)

    /** Epoch ms until which the rewarded "ads-free" window runs; 0 or past = ads on. */
    val adFreeUntilEpochMs: StateFlow<Long>

    /**
     * Grants ads-free for [durationMs] from now. If a window is already running, keeps that
     * expiry and does not add more time.
     * @return absolute expiry epoch ms.
     */
    fun grantAdFree(durationMs: Long): Long
}

/** On-device writing assistant for resumes and cover letters. */
interface AiRepository {
    suspend fun generateResume(request: AiResumeRequest): Result<Pair<String, ResumeContent>>
    suspend fun parseResumePdf(pdf: Uri): Result<Pair<String, ResumeContent>>
    suspend fun generateCoverLetter(request: CoverLetterRequest): Result<String>
    suspend fun improveText(text: String, kind: ImproveKind, context: String): Result<String>
}
