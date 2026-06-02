package com.complexly.academy.platform

/**
 * Opens the given YouTube video in the native YouTube app.
 * Falls back to a browser if YouTube is not installed.
 */
expect fun openYouTubeVideo(youtubeId: String, startSeconds: Int = 0)

/**
 * Opens the given URL in the platform's default browser.
 */
expect fun openExternalLink(url: String)
