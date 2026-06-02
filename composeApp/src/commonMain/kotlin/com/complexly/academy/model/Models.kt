package com.complexly.academy.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Channel(
    val id: String,
    val title: String,
    val description: String,
    val primaryColor: String,
    val accentColor: String,
    val image: String = "",
    val youtubeChannelId: String = ""
)

@Serializable
data class Playlist(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val videoIds: List<String>,
    /** Thumbnail URL for display, derived from the first video. Populated at load time. */
    @Transient val thumbnailUrl: String = "",
    /** Average view count across all matched videos. Populated at load time. */
    @Transient val avgViewCount: Long = 0L,
    /** Total view count across all matched videos. Populated at load time. */
    @Transient val totalViewCount: Long = 0L,
    /** Average rating (likes/views) across all matched videos. Populated at load time. */
    @Transient val avgRating: Double = 0.0,
    /** True if every video in this playlist has at least one quiz question. Populated at load time. */
    @Transient val isCourse: Boolean = false,
    /** Number of videos in this playlist that exist in the dataset. Populated at load time. */
    @Transient val videoCount: Int = 0
)

@Serializable
data class Option(
    val text: String,
    val isCorrect: Boolean,
    val explanation: String
)

@Serializable
data class Question(
    val id: String,
    val text: String,
    val options: List<Option>,
    val flags: List<String> = emptyList()
)

/**
 * A timestamped section within a video (YouTube chapters).
 */
@Serializable
data class ChapterSegment(
    val title: String,
    val startSeconds: Int,
    val endSeconds: Int
)

/**
 * Transcript segment from a YouTube video (caption/subtitle data).
 * Not currently bundled in videos.json — kept here for future Phase 2 work
 * (local transcript search, AI-generated video summaries).
 */
@Serializable
data class TranscriptSegment(
    val text: String,
    val startSeconds: Int,
    val endSeconds: Int
)

@Serializable
data class Video(
    val id: String,
    /** Playlists this video belongs to. A video can appear in multiple playlists but always in exactly one channel. */
    val playlistIds: List<String> = emptyList(),
    val title: String,
    val description: String,
    val durationSeconds: Long,
    val youtubeId: String,
    val thumbnailUrl: String,
    val questions: List<Question> = emptyList(),
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val releaseDate: String = "", // YYYY-MM-DD
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val chapters: List<ChapterSegment> = emptyList()
) {
    /** First playlist ID, or empty string. Convenience accessor for single-playlist videos. */
    val firstPlaylistId: String
        get() = playlistIds.firstOrNull() ?: ""

    /** Computed engagement ratio: likes per 100 views, capped at 5.0. */
    val rating: Double
        get() = if (viewCount > 0L) {
            (likeCount.toDouble() / viewCount.toDouble() * 100.0).coerceIn(0.0, 5.0)
        } else {
            0.0
        }
}

/** Tracks a learner's progress through content — completions, streak, and last-active date. */
@Serializable
data class LearningProgress(
    val completedVideoIds: Set<String> = emptySet(),
    val completedCourseIds: Set<String> = emptySet(),
    val enrolledCourseIds: Set<String> = emptySet(),
    val recentlyWatchedVideoIds: List<String> = emptyList(),
    val recentlyCompletedVideoIds: List<String> = emptyList(),
    val recentlyViewedItemIds: List<String> = emptyList(),
    val correctAnswersCount: Int = 0,
    val streakCount: Int = 0,
    val lastActiveDate: String? = null // YYYY-MM-DD
)

/** Stores user-configurable app settings — display, notifications, and channel preferences. */
@Serializable
data class UserPreferences(
    val darkModeEnabled: Boolean = false,
    val pushNotificationsEnabled: Boolean = true,
    val disabledChannelIds: Set<String> = emptySet()
)

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
    object Loading : Resource<Nothing>
}

object ChannelColorRegistry {
    @kotlin.concurrent.Volatile
    private var colorsMap = mapOf<String, String>()

    fun getColor(channelId: String): String? {
        return colorsMap[channelId]
    }

    fun registerColor(channelId: String, hexColor: String) {
        colorsMap = colorsMap + (channelId to hexColor)
    }
}



