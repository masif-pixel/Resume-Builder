package com.resume.builder.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Opens a PDF in the in-app viewer: (path, title). Provided by the nav host. */
val LocalOpenPdf = staticCompositionLocalOf<((path: String, title: String) -> Unit)?> { null }

/** PDFs open in the in-app viewer; other files (images) fall back to an external app. */
@Composable
fun rememberFileOpener(): (path: String, mimeType: String, name: String) -> Unit {
    val context = LocalContext.current
    val openPdf = LocalOpenPdf.current
    return remember(context, openPdf) {
        { path, mimeType, name ->
            val isPdf = mimeType == "application/pdf" || path.endsWith(".pdf", ignoreCase = true)
            if (isPdf && openPdf != null) openPdf(path, name.substringBeforeLast('.'))
            else FileActions.open(context, path, mimeType)
        }
    }
}
