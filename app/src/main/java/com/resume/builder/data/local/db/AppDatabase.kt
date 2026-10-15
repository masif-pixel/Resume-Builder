package com.resume.builder.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ResumeEntity::class, CoverLetterEntity::class, SavedFileEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao
    abstract fun coverLetterDao(): CoverLetterDao
    abstract fun savedFileDao(): SavedFileDao

    companion object {
        const val NAME = "resume_builder.db"
    }
}
