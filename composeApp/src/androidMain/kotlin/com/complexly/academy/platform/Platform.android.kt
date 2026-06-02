package com.complexly.academy.platform

import android.content.Intent
import android.net.Uri
import com.complexly.academy.ScienceApp

/**
 * Opens the given YouTube video in the native YouTube app.
 * Falls back to a browser if YouTube is not installed.
 */
actual fun openYouTubeVideo(youtubeId: String, startSeconds: Int) {
    val context = ScienceApp.instance
    if (startSeconds > 0) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$youtubeId&t=${startSeconds}s")
        )
        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(webIntent)
    } else {
        val uri = "vnd.youtube:$youtubeId"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.youtube")

        if (intent.resolveActivity(context.packageManager) != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=$youtubeId")
            )
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }
}

/**
 * Opens the given URL in the device's default browser.
 */
actual fun openExternalLink(url: String) {
    val context = ScienceApp.instance
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
