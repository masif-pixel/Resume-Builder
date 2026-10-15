package com.resume.builder.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resumes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ResumeEntity>>

    @Query("SELECT * FROM resumes WHERE id = :id")
    fun observe(id: String): Flow<ResumeEntity?>

    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun get(id: String): ResumeEntity?

    @Query("SELECT * FROM resumes")
    suspend fun getAll(): List<ResumeEntity>

    @Upsert
    suspend fun upsert(entity: ResumeEntity)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CoverLetterDao {
    @Query("SELECT * FROM cover_letters ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CoverLetterEntity>>

    @Query("SELECT * FROM cover_letters WHERE id = :id")
    suspend fun get(id: String): CoverLetterEntity?

    @Query("SELECT * FROM cover_letters")
    suspend fun getAll(): List<CoverLetterEntity>

    @Upsert
    suspend fun upsert(entity: CoverLetterEntity)

    @Query("DELETE FROM cover_letters WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SavedFileDao {
    @Query("SELECT * FROM saved_files ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedFileEntity>>

    @Upsert
    suspend fun upsert(entity: SavedFileEntity)

    @Query("DELETE FROM saved_files WHERE id = :id")
    suspend fun delete(id: String)
}
