package com.complexly.academy.ui.video

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

data class VideoPlayerUiState(
    val video: Video? = null,
    val channelId: String = "",
    val channelName: String = "",
    val playlists: List<Playlist> = emptyList(),
    /** IDs of the video's playlists that qualify as courses (all videos have quiz questions). */
    val coursePlaylistIds: Set<String> = emptySet(),
    val relatedVideos: List<Video> = emptyList(),
    val isCompleted: Boolean = false,
    val isWatched: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val playlistId: String? = null
)

class VideoPlayerViewModel(
    private val videoId: String,
    private val playlistId: String? = null,
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository
) : BaseViewModel<VideoPlayerUiState, Any>(VideoPlayerUiState()) {

    override val uiState: StateFlow<VideoPlayerUiState> = combine(
        contentRepository.getVideo(videoId),
        contentRepository.getChannels(),
        contentRepository.getPlaylists(),
        contentRepository.getRelatedVideos(videoId),
        userRepository.learningProgress
    ) { videoRes, channelsRes, playlistsRes, relatedRes, learningProgress ->
        val video = (videoRes as? Resource.Success)?.data
        val channels = (channelsRes as? Resource.Success)?.data ?: emptyList()
        val playlists = (playlistsRes as? Resource.Success)?.data ?: emptyList()
        val relatedVideos = (relatedRes as? Resource.Success)?.data ?: emptyList()

        val channelId = video?.let { v ->
            val firstPlId = v.playlistIds.firstOrNull()
            playlists.find { it.id == firstPlId }?.channelId
                ?: firstPlId?.let { contentRepository.getChannelIdForPlaylist(it) } ?: ""
        } ?: ""
        val channelName = channels.find { it.id == channelId }?.title ?: ""
        val videoPlaylists = video?.let { v -> playlists.filter { it.id in v.playlistIds } } ?: emptyList()
        val coursePlaylistIds = videoPlaylists.filter { it.isCourse }.map { it.id }.toSet()

        VideoPlayerUiState(
            video = video,
            channelId = channelId,
            channelName = channelName,
            playlists = videoPlaylists,
            coursePlaylistIds = coursePlaylistIds,
            relatedVideos = relatedVideos,
            isCompleted = learningProgress.completedVideoIds.contains(videoId),
            isWatched = learningProgress.recentlyWatchedVideoIds.contains(videoId),
            isLoading = videoRes is Resource.Loading || channelsRes is Resource.Loading || playlistsRes is Resource.Loading,
            errorMessage = (videoRes as? Resource.Error)?.message
                ?: (channelsRes as? Resource.Error)?.message
                ?: (playlistsRes as? Resource.Error)?.message,
            playlistId = playlistId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VideoPlayerUiState(isLoading = true)
    )

    fun playVideo() {
        viewModelScope.launch {
            userRepository.markVideoWatched(videoId)
            playlistId?.let { userRepository.markPlaylistViewed(it) }
        }
    }

    fun onTakeQuiz() {
        playlistId?.let { plId ->
            viewModelScope.launch {
                userRepository.markPlaylistViewed(plId)
            }
        }
    }
}
