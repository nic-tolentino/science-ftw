package com.complexly.academy.ui.course

import androidx.lifecycle.viewModelScope
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import com.complexly.academy.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CourseDetailsUiState(
    val playlist: Playlist? = null,
    val videos: List<Video> = emptyList(),
    val completedVideoIds: Set<String> = emptySet(),
    val watchedVideoIds: Set<String> = emptySet(),
    val channelTitle: String = "",
    /** True when every video in this playlist has at least one quiz question. */
    val isCourse: Boolean = false,
    val isEnrolled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CourseDetailsViewModel(
    private val playlistId: String,
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository
) : BaseViewModel<CourseDetailsUiState, Any>(CourseDetailsUiState()) {

    override val uiState: StateFlow<CourseDetailsUiState> = combine(
        contentRepository.getPlaylist(playlistId),
        contentRepository.getVideosForPlaylist(playlistId),
        userRepository.learningProgress,
        contentRepository.getChannels()
    ) { playlistRes, videosRes, learningProgress, channelsRes ->
        val playlist = (playlistRes as? Resource.Success)?.data
        val channelTitle = (channelsRes as? Resource.Success)?.data
            ?.find { it.id == playlist?.channelId }?.title ?: ""

        val videos = (videosRes as? Resource.Success)?.data ?: emptyList()
        val isCourse = videos.isNotEmpty() && videos.all { it.questions.isNotEmpty() }
        CourseDetailsUiState(
            playlist = playlist,
            videos = videos,
            completedVideoIds = learningProgress.completedVideoIds,
            watchedVideoIds = learningProgress.recentlyWatchedVideoIds.toSet(),
            channelTitle = channelTitle,
            isCourse = isCourse,
            isEnrolled = learningProgress.enrolledCourseIds.contains(playlistId),
            isLoading = playlistRes is Resource.Loading || videosRes is Resource.Loading,
            errorMessage = (playlistRes as? Resource.Error)?.message ?: (videosRes as? Resource.Error)?.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CourseDetailsUiState(isLoading = true)
    )

    fun enroll() {
        viewModelScope.launch {
            userRepository.enrollInCourse(playlistId)
        }
    }

    fun unenroll() {
        viewModelScope.launch {
            userRepository.unenrollFromCourse(playlistId)
        }
    }
}
