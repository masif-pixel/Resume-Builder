package com.resume.builder.data.local.db

import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.FileCategory
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.model.ResumeStyle
import com.resume.builder.domain.model.SavedFile
import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
}

fun ResumeEntity.toDomain(): Resume = Resume(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    templateId = templateId,
    content = runCatching { AppJson.decodeFromString<ResumeContent>(contentJson) }.getOrDefault(ResumeContent()),
    style = runCatching { AppJson.decodeFromString<ResumeStyle>(styleJson) }.getOrDefault(ResumeStyle()),
)

fun Resume.toEntity(): ResumeEntity = ResumeEntity(
    id = id,
    title = title,
    templateId = templateId,
    contentJson = AppJson.encodeToString(ResumeContent.serializer(), content),
    styleJson = AppJson.encodeToString(ResumeStyle.serializer(), style),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CoverLetterEntity.toDomain() = CoverLetter(
    id = id, title = title, body = body, resumeId = resumeId, companyName = companyName, jobTitle = jobTitle,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun CoverLetter.toEntity() = CoverLetterEntity(
    id = id, title = title, body = body, resumeId = resumeId, companyName = companyName, jobTitle = jobTitle,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun SavedFileEntity.toDomain() = SavedFile(
    id = id, name = name, path = path, sizeBytes = sizeBytes, mimeType = mimeType,
    category = runCatching { FileCategory.valueOf(category) }.getOrDefault(FileCategory.PDF_TOOLS),
    createdAt = createdAt,
)

fun SavedFile.toEntity() = SavedFileEntity(
    id = id, name = name, path = path, sizeBytes = sizeBytes, mimeType = mimeType, category = category.name, createdAt = createdAt,
)
