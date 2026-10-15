package com.resume.builder.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.resume.builder.data.local.db.CoverLetterDao
import com.resume.builder.data.local.db.ResumeDao
import com.resume.builder.data.local.db.SavedFileDao
import com.resume.builder.data.local.db.toDomain
import com.resume.builder.data.local.db.toEntity
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.FileCategory
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.repository.CoverLetterRepository
import com.resume.builder.domain.repository.ResumeRepository
import com.resume.builder.domain.repository.SavedFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class ResumeRepositoryImpl(private val dao: ResumeDao) : ResumeRepository {
    override fun observeResumes(): Flow<List<Resume>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeResume(id: String): Flow<Resume?> = dao.observe(id).map { it?.toDomain() }
    override suspend fun getResume(id: String): Resume? = dao.get(id)?.toDomain()
    override suspend fun getAll(): List<Resume> = dao.getAll().map { it.toDomain() }

    override suspend fun upsert(resume: Resume, touch: Boolean) {
        val value = if (touch) resume.copy(updatedAt = System.currentTimeMillis()) else resume
        dao.upsert(value.toEntity())
    }

    override suspend fun delete(id: String) = dao.delete(id)
}

class CoverLetterRepositoryImpl(private val dao: CoverLetterDao) : CoverLetterRepository {
    override fun observeLetters(): Flow<List<CoverLetter>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getLetter(id: String): CoverLetter? = dao.get(id)?.toDomain()
    override suspend fun getAll(): List<CoverLetter> = dao.getAll().map { it.toDomain() }

    override suspend fun upsert(letter: CoverLetter, touch: Boolean) {
        val value = if (touch) letter.copy(updatedAt = System.currentTimeMillis()) else letter
        dao.upsert(value.toEntity())
    }

    override suspend fun delete(id: String) = dao.delete(id)
}

class SavedFileRepositoryImpl(
    private val context: Context,
    private val dao: SavedFileDao,
) : SavedFileRepository {

    override fun observeFiles(): Flow<List<SavedFile>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }.filter { File(it.path).exists() }
    }

    override fun exportDir(): File = File(context.filesDir, "exports").apply { mkdirs() }

    override fun publicFolderLabel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "Documents › $PUBLIC_FOLDER" else "App storage"

    override suspend fun register(source: File, displayName: String, mimeType: String, category: FileCategory): SavedFile =
        withContext(Dispatchers.IO) {
            val target = uniqueFile(exportDir(), sanitize(displayName))
            if (source.absolutePath != target.absolutePath) {
                source.copyTo(target, overwrite = true)
                if (source.parentFile == context.cacheDir || source.path.startsWith(context.cacheDir.path)) source.delete()
            }
            runCatching { mirrorToPublicStorage(target, mimeType, category) }
            val saved = SavedFile(
                name = target.name, path = target.absolutePath, sizeBytes = target.length(),
                mimeType = mimeType, category = category,
            )
            dao.upsert(saved.toEntity())
            saved
        }

    override suspend fun rename(file: SavedFile, newName: String) = withContext(Dispatchers.IO) {
        val current = File(file.path)
        val extension = current.extension
        val base = sanitize(newName).removeSuffix(".$extension")
        val target = uniqueFile(current.parentFile ?: exportDir(), "$base.$extension")
        if (current.renameTo(target)) {
            dao.upsert(file.copy(name = target.name, path = target.absolutePath).toEntity())
        }
    }

    override suspend fun delete(file: SavedFile) = withContext(Dispatchers.IO) {
        File(file.path).delete()
        dao.delete(file.id)
    }

    /** Keeps a user-visible copy in Documents (or Pictures for images) on Android 10+. */
    private fun mirrorToPublicStorage(file: File, mimeType: String, category: FileCategory) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val isImage = category == FileCategory.IMAGES
        val collection = if (isImage) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val root = if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOCUMENTS
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$root/$PUBLIC_FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "document" }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext")
            index++
        }
        return candidate
    }

    companion object {
        const val PUBLIC_FOLDER = "Resume Builder"
    }
}
