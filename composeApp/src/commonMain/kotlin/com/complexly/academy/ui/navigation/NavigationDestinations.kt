package com.complexly.academy.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object DiscoverRoute

@Serializable
object ProgressRoute

@Serializable
object YouRoute

@Serializable
object StoreRoute

@Serializable
object MainTabsRoute

@Serializable
data class CourseDetailsRoute(val courseId: String)

@Serializable
data class VideoPlayerRoute(val videoId: String, val playlistId: String? = null)

@Serializable
data class QuizRoute(val videoId: String)

@Serializable
object ReviewRoute
