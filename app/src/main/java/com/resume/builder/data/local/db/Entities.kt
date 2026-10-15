package com.resume.builder.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val templateId: String,
    val contentJson: String,
    val styleJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "cover_letters")
data class CoverLetterEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val resumeId: String?,
    val companyName: String,
    val jobTitle: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "saved_files", indices = [Index("category")])
data class SavedFileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val mimeType: String,
    val category: String,
    val createdAt: Long,
)
