package com.complexly.academy.ui.progress

import androidx.lifecycle.viewModelScope
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.data.VideoSort
import com.complexly.academy.model.LearningProgress
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import com.complexly.academy.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val progress: LearningProgress = LearningProgress(),
    val recentVideos: List<Video> = emptyList(),
    val inProgressCourses: List<Pair<Playlist, Float>> = emptyList(),
    val completedCourses: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProgressViewModel(
    private val userRepository: UserRepository,
    private val contentRepository: ContentRepository
) : BaseViewModel<ProgressUiState, Any>(ProgressUiState()) {

    private val coreData = combine(
        contentRepository.getPlaylists(),
        contentRepository.searchVideos("", sortBy = VideoSort.RELEVANCE),
        contentRepository.getChannels()
    ) { playlistsRes, videosRes, channelsRes ->
        Triple(playlistsRes, videosRes, channelsRes)
    }

    override val uiState: StateFlow<ProgressUiState> = combine(
        coreData,
        userRepository.learningProgress
    ) { (playlistsRes, videosRes, channelsRes), learningProgress ->
        if (playlistsRes is Resource.Loading || videosRes is Resource.Loading || channelsRes is Resource.Loading) {
            return@combine ProgressUiState(isLoading = true)
        }
        if (playlistsRes is Resource.Error) return@combine ProgressUiState(errorMessage = (playlistsRes as Resource.Error).message)
        if (videosRes is Resource.Error) return@combine ProgressUiState(errorMessage = (videosRes as Resource.Error).message)

        val playlists = (playlistsRes as Resource.Success).data
        val videos = (videosRes as Resource.Success).data

        // 1. Resolve recent videos (recently watched and recently completed, merged & distincted, up to 10)
        val recentVideoIds = (learningProgress.recentlyWatchedVideoIds + learningProgress.recentlyCompletedVideoIds).distinct().take(10)
        val recentVideos = recentVideoIds.mapNotNull { id ->
            videos.find { it.id == id }
        }

        // 2. Resolve in-progress and completed courses (enrolled course playlists)
        val inProgressCourses = mutableListOf<Pair<Playlist, Float>>()
        val completedCourses = mutableListOf<Playlist>()

        val enrolledCourses = playlists.filter { it.id in learningProgress.enrolledCourseIds }

        enrolledCourses.forEach { playlist ->
            val playlistVideos = videos.filter { playlist.id in it.playlistIds }
            if (playlistVideos.isNotEmpty()) {
                val completedCount = playlistVideos.count { it.id in learningProgress.completedVideoIds }
                val totalCount = playlistVideos.size
                val progressVal = completedCount.toFloat() / totalCount

                if (completedCount == totalCount) {
                    completedCourses.add(playlist)
                } else {
                    inProgressCourses.add(playlist to progressVal)
                }
            }
        }

        ProgressUiState(
            progress = learningProgress,
            recentVideos = recentVideos,
            inProgressCourses = inProgressCourses,
            completedCourses = completedCourses,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState(isLoading = true)
    )
}
