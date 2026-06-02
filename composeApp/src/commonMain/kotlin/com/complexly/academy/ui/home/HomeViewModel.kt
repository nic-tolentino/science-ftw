package com.complexly.academy.ui.home

import androidx.lifecycle.viewModelScope
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.data.VideoSort
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Question
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import com.complexly.academy.ui.BaseViewModel
import com.complexly.academy.domain.RecentViewItem
import com.complexly.academy.domain.GetRecentlyViewedUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class HomeUiState(
    val channels: List<Channel> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val popularPlaylists: List<Playlist> = emptyList(),
    val highestRatedPlaylists: List<Playlist> = emptyList(),
    /** IDs of playlists where every video has at least one quiz question. */
    val coursePlaylistIds: Set<String> = emptySet(),
    val videos: List<Video> = emptyList(),
    val recommendedVideos: List<Video> = emptyList(),
    val dailyQuestion: Question? = null,
    val streakCount: Int = 6,
    val completedVideoIds: Set<String> = emptySet(),
    val recentlyViewedItems: List<RecentViewItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface HomeEvent {
    data class ShowToast(val message: String) : HomeEvent
}

class HomeViewModel(
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository,
    private val getRecentlyViewedUseCase: GetRecentlyViewedUseCase
) : BaseViewModel<HomeUiState, HomeEvent>(HomeUiState(isLoading = true)) {

    // Bundles 5 content flows into one so we stay within combine()'s 5-argument arity limit
    // and can then combine the result with user state and recommendations separately.
    private data class CoreContent(
        val channelsRes: Resource<List<Channel>>,
        val playlistsRes: Resource<List<Playlist>>,
        val popularRes: Resource<List<Playlist>>,
        val highestRatedRes: Resource<List<Playlist>>,
        val questionRes: Resource<Question>
    )

    init {
        val coreContent = combine(
            contentRepository.getChannels(),
            contentRepository.getPlaylists(),
            contentRepository.getPopularPlaylists(),
            contentRepository.getHighestRatedPlaylists(),
            contentRepository.getDailyQuestion()
        ) { ch, pl, pop, rated, q -> CoreContent(ch, pl, pop, rated, q) }

        viewModelScope.launch {
            // flatMapLatest: re-subscribes to getRecommendedVideos whenever the user's disabled channel IDs
            // or completed IDs change, so recommendations are always current.
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            combine(
                userRepository.learningProgress,
                userRepository.userPreferences,
                getRecentlyViewedUseCase()
            ) { lp, up, rv -> Triple(lp, up, rv) }
                .flatMapLatest { (learningProgress, userPrefs, recentlyViewed) ->
                    combine(
                        coreContent,
                        contentRepository.searchVideos("", sortBy = VideoSort.NEWEST),
                        contentRepository.getRecommendedVideos(userPrefs.disabledChannelIds, learningProgress.completedVideoIds)
                    ) { core, videosRes, recommendedRes ->
                        val rawPlaylists = (core.playlistsRes as? Resource.Success)?.data ?: emptyList()
                        val playlists = rawPlaylists.filter { it.channelId !in userPrefs.disabledChannelIds }
                        val playlistChannelMap = rawPlaylists.associate { it.id to it.channelId }
                        
                        val coursePlaylistIds = playlists.filter { it.isCourse }.map { it.id }.toSet()
                        
                        val allVideos = (videosRes as? Resource.Success)?.data ?: emptyList()
                        val filteredVideos = allVideos.filter { video ->
                            video.playlistIds.none { playlistChannelMap[it] in userPrefs.disabledChannelIds }
                        }

                        val rawDailyQuestion = (core.questionRes as? Resource.Success)?.data
                        val filteredDailyQuestion = rawDailyQuestion?.let { q ->
                            val questionVideo = allVideos.find { it.questions.contains(q) }
                            if (questionVideo != null && questionVideo.playlistIds.none { playlistChannelMap[it] in userPrefs.disabledChannelIds }) {
                                q
                            } else {
                                null
                            }
                        }

                        val playlistCompletedMap = playlists.associate { playlist ->
                            val playlistVideos = allVideos.filter { playlist.id in it.playlistIds }
                            val isCompleted = playlistVideos.isNotEmpty() && playlistVideos.all { it.id in learningProgress.completedVideoIds }
                            playlist.id to isCompleted
                        }

                        val filteredRecentlyViewed = recentlyViewed.filter { item ->
                            when (item) {
                                is RecentViewItem.VideoItem -> {
                                    val isChannelEnabled = item.video.playlistIds.none { playlistChannelMap[it] in userPrefs.disabledChannelIds }
                                    val isCompleted = learningProgress.completedVideoIds.contains(item.video.id)
                                    isChannelEnabled && !isCompleted
                                }
                                is RecentViewItem.PlaylistItem -> {
                                    val isChannelEnabled = item.playlist.channelId !in userPrefs.disabledChannelIds
                                    val isCompleted = playlistCompletedMap[item.playlist.id] == true
                                    isChannelEnabled && !isCompleted
                                }
                            }
                        }

                        HomeUiState(
                            channels = (core.channelsRes as? Resource.Success)?.data?.filter { it.id !in userPrefs.disabledChannelIds } ?: emptyList(),
                            playlists = playlists,
                            popularPlaylists = (core.popularRes as? Resource.Success)?.data?.filter { it.channelId !in userPrefs.disabledChannelIds } ?: emptyList(),
                            highestRatedPlaylists = (core.highestRatedRes as? Resource.Success)?.data?.filter { it.channelId !in userPrefs.disabledChannelIds } ?: emptyList(),
                            coursePlaylistIds = coursePlaylistIds,
                            videos = filteredVideos,
                            recommendedVideos = (recommendedRes as? Resource.Success)?.data ?: emptyList(),
                            dailyQuestion = filteredDailyQuestion,
                            streakCount = learningProgress.streakCount,
                            completedVideoIds = learningProgress.completedVideoIds,
                            recentlyViewedItems = filteredRecentlyViewed,
                            isLoading = core.channelsRes is Resource.Loading ||
                                        core.playlistsRes is Resource.Loading ||
                                        videosRes is Resource.Loading,
                            errorMessage = (core.channelsRes as? Resource.Error)?.message
                                ?: (core.playlistsRes as? Resource.Error)?.message
                        )
                    }
                }
                .collect { newState -> updateState { newState } }
        }
    }

    fun onStreakClicked() {
        viewModelScope.launch {
            userRepository.updateStreak(increment = true)
            sendEvent(HomeEvent.ShowToast("Streak updated! Keep it up!"))
        }
    }
}
