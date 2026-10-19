package com.resume.builder.presentation.components

import com.resume.builder.R
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileActions {

    private fun uriFor(context: Context, path: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))

    fun open(context: Context, path: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, path), mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_no_app_to_open), Toast.LENGTH_SHORT).show()
        }
    }

    fun share(context: Context, path: String, mimeType: String, subject: String = File(path).nameWithoutExtension) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uriFor(context, path))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.common_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_no_browser), Toast.LENGTH_SHORT).show()
        }
    }

    fun rateApp(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            openUrl(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
        }
    }

    fun sendFeedback(context: Context, version: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject, context.getString(R.string.app_name), version))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_no_email_app), Toast.LENGTH_SHORT).show()
        }
    }

    fun shareApp(context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_text, "https://play.google.com/store/apps/details?id=${context.packageName}"))
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.drawer_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
