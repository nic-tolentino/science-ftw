package com.complexly.academy.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Opens the given YouTube video in the native YouTube app.
 * Falls back to Safari if YouTube is not installed.
 */
actual fun openYouTubeVideo(youtubeId: String, startSeconds: Int) {
    val youtubeUrl = if (startSeconds > 0) {
        "https://www.youtube.com/watch?v=$youtubeId&t=${startSeconds}s"
    } else {
        "https://www.youtube.com/watch?v=$youtubeId"
    }
    val youtubeAppUrl = "youtube://$youtubeId"

    val youtubeUrlObj = NSURL.URLWithString(youtubeAppUrl)
    val webUrlObj = NSURL.URLWithString(youtubeUrl)

    if (startSeconds > 0) {
        if (webUrlObj != null) {
            UIApplication.sharedApplication.openURL(webUrlObj)
        }
    } else {
        if (youtubeUrlObj != null && UIApplication.sharedApplication.canOpenURL(youtubeUrlObj)) {
            UIApplication.sharedApplication.openURL(youtubeUrlObj)
        } else if (webUrlObj != null) {
            UIApplication.sharedApplication.openURL(webUrlObj)
        }
    }
}

/**
 * Opens the given URL in Safari.
 */
actual fun openExternalLink(url: String) {
    val urlObj = NSURL.URLWithString(url)
    if (urlObj != null) {
        UIApplication.sharedApplication.openURL(urlObj)
    }
}
