package com.resume.builder.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copies user images (profile photos, signatures) into private app storage. */
class ImageStore(private val context: Context) {

    private fun dir(name: String) = File(context.filesDir, name).apply { mkdirs() }

    suspend fun savePhoto(resumeId: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 900 && bounds.outHeight / (sample * 2) >= 900) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Unsupported image")
        val file = File(dir("photos"), "$resumeId-${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        file.absolutePath
    }

    suspend fun saveSignature(ownerId: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val file = File(dir("signatures"), "$ownerId-${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    }

    suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        path?.let { File(it).delete() }
    }
}
