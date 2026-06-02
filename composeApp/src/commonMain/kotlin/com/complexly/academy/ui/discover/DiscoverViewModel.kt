package com.complexly.academy.ui.discover

import androidx.lifecycle.viewModelScope
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.data.VideoSort
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import com.complexly.academy.ui.BaseViewModel
import kotlinx.coroutines.channels.Channel as ConflatedChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A video with its resolved channel info. */
data class VideoUiModel(
    val video: Video,
    val channelId: String,
    val playlistTitle: String = ""
)

enum class PlaylistTypeFilter { ALL, COURSES, PLAYLISTS_ONLY }

/** A playlist enriched with aggregate video stats. */
data class PlaylistUiModel(
    val playlist: Playlist,
    val channelId: String,
    val channelTitle: String,
    val videoCount: Int,
    val totalViews: Long,
    val totalDurationSeconds: Long,
    val latestDate: String,
    val firstVideoThumbnailUrl: String = "",
    val isCourse: Boolean = false
) {
    val formattedDuration: String
        get() {
            val hours = totalDurationSeconds / 3600
            return if (hours > 0) "${hours}h ${((totalDurationSeconds % 3600) / 60)}m"
            else "${totalDurationSeconds / 60}m"
        }
}

data class DiscoverUiState(
    val query: String = "",
    val searchResults: List<VideoUiModel> = emptyList(),
    val matchingPlaylists: List<PlaylistUiModel> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface DiscoverEvent {
    data class ShowToast(val message: String) : DiscoverEvent
}

private data class SearchParams(
    val query: String,
    val channelId: String?,
    val playlistId: String?,
    val sortBy: VideoSort
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
class DiscoverViewModel(
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository
) : BaseViewModel<DiscoverUiState, DiscoverEvent>(DiscoverUiState()) {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _activeChannel = MutableStateFlow<String?>(null)
    val activeChannel = _activeChannel.asStateFlow()

    private val _activePlaylist = MutableStateFlow<String?>(null)
    val activePlaylist = _activePlaylist.asStateFlow()

    private val _sort = MutableStateFlow(VideoSort.RELEVANCE)
    val sort = _sort.asStateFlow()

    private val _typeFilter = MutableStateFlow(PlaylistTypeFilter.ALL)
    val typeFilter = _typeFilter.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    private val _disabledChannelIds = MutableStateFlow<Set<String>>(emptySet())

    private val _searchTrigger = ConflatedChannel<SearchParams>(ConflatedChannel.CONFLATED)

    private var videosByPlaylist: Map<String, List<Video>> = emptyMap()

    init {
        viewModelScope.launch {
            userRepository.userPreferences.collect { prefs ->
                _disabledChannelIds.value = prefs.disabledChannelIds
                // If active channel is disabled, reset it to null
                if (_activeChannel.value != null && prefs.disabledChannelIds.contains(_activeChannel.value)) {
                    _activeChannel.value = null
                }
                // Refresh categories with correct filtering
                val enabledCh = _channels.value.filter { it.id !in prefs.disabledChannelIds }
                val enabledPl = _playlists.value.filter { it.channelId !in prefs.disabledChannelIds }
                updateState { s ->
                    s.copy(
                        channels = enabledCh,
                        playlists = enabledPl
                    )
                }
                _searchTrigger.send(SearchParams(_query.value, _activeChannel.value, _activePlaylist.value, _sort.value))
            }
        }
        viewModelScope.launch {
            contentRepository.getChannels().collect { res ->
                if (res is Resource.Success) {
                    _channels.value = res.data
                    val enabled = res.data.filter { it.id !in _disabledChannelIds.value }
                    updateState { s -> s.copy(channels = enabled) }
                }
            }
        }
        viewModelScope.launch {
            contentRepository.getPlaylists().collect { res ->
                if (res is Resource.Success) {
                    _playlists.value = res.data
                    val enabled = res.data.filter { it.channelId !in _disabledChannelIds.value }
                    updateState { s -> s.copy(playlists = enabled) }
                }
            }
        }
        viewModelScope.launch {
            _searchTrigger.receiveAsFlow()
                .collectLatest { params ->
                    contentRepository.searchVideos(
                        query = params.query,
                        channelId = params.channelId,
                        playlistId = params.playlistId,
                        sortBy = params.sortBy
                    ).collect { searchRes ->
                        val rawResults = (searchRes as? Resource.Success)?.data ?: emptyList()
                        val playlistMap = _playlists.value.associate { it.id to it.title }
                        val channelMap = _channels.value.associateBy { it.id }

                        // Filter results by excluding disabled channel IDs
                        val enabledResults = rawResults.filter { video ->
                            video.playlistIds.none { plId -> _playlists.value.find { it.id == plId }?.channelId in _disabledChannelIds.value }
                        }

                        val coursePlaylistIds = _playlists.value.filter { it.isCourse }.map { it.id }.toSet()

                        // Apply type filter to video results
                        val results = when (_typeFilter.value) {
                            PlaylistTypeFilter.ALL -> enabledResults
                            PlaylistTypeFilter.COURSES -> enabledResults.filter { v ->
                                v.playlistIds.any { it in coursePlaylistIds }
                            }
                            PlaylistTypeFilter.PLAYLISTS_ONLY -> enabledResults.filter { v ->
                                v.playlistIds.none { it in coursePlaylistIds }
                            }
                        }

                        val uiModels = results.map { video ->
                            val firstPlId = video.playlistIds.firstOrNull()
                            VideoUiModel(
                                video = video,
                                channelId = firstPlId?.let { contentRepository.getChannelIdForPlaylist(it) } ?: "",
                                playlistTitle = firstPlId?.let { playlistMap[it] } ?: ""
                            )
                        }

                        videosByPlaylist = results
                            .flatMap { video -> video.playlistIds.map { plId -> plId to video } }
                            .groupBy({ it.first }, { it.second })

                        val matchingPlaylists = buildPlaylistModels(params, channelMap)

                        updateState { s ->
                            s.copy(
                                searchResults = uiModels,
                                matchingPlaylists = matchingPlaylists,
                                isLoading = searchRes is Resource.Loading,
                                errorMessage = (searchRes as? Resource.Error)?.message
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            _query.debounce(300).collect { q ->
                _searchTrigger.send(SearchParams(q, _activeChannel.value, _activePlaylist.value, _sort.value))
            }
        }
        viewModelScope.launch {
            _activeChannel.collect { ch ->
                _searchTrigger.send(SearchParams("", ch, _activePlaylist.value, _sort.value))
            }
        }
        viewModelScope.launch {
            _activePlaylist.collect { pl ->
                _searchTrigger.send(SearchParams(_query.value, _activeChannel.value, pl, _sort.value))
            }
        }
        viewModelScope.launch {
            _sort.collect { s ->
                _searchTrigger.send(SearchParams(_query.value, _activeChannel.value, _activePlaylist.value, s))
            }
        }
        viewModelScope.launch {
            _typeFilter.collect {
                _searchTrigger.send(SearchParams(_query.value, _activeChannel.value, _activePlaylist.value, _sort.value))
            }
        }
    }

    private fun buildPlaylistModels(
        params: SearchParams,
        channelMap: Map<String, Channel>
    ): List<PlaylistUiModel> {
        val channelNames = _channels.value.associate { it.id to it.title.lowercase() }
        val queryLower = params.query.lowercase()

        val filteredPlaylists = _playlists.value.filter { it.channelId !in _disabledChannelIds.value }

        return filteredPlaylists.mapNotNull { playlist ->
            if (params.playlistId != null && params.playlistId != playlist.id) return@mapNotNull null
            if (params.channelId != null && params.channelId != playlist.channelId) return@mapNotNull null

            val titleMatches = queryLower.isBlank() ||
                playlist.title.lowercase().contains(queryLower) ||
                channelNames[playlist.channelId]?.contains(queryLower) == true
            if (!titleMatches) return@mapNotNull null

            val plVideos = videosByPlaylist[playlist.id]
            if (plVideos.isNullOrEmpty()) return@mapNotNull null

            // Apply type filter to playlists
            when (_typeFilter.value) {
                PlaylistTypeFilter.COURSES -> if (!playlist.isCourse) return@mapNotNull null
                PlaylistTypeFilter.PLAYLISTS_ONLY -> if (playlist.isCourse) return@mapNotNull null
                PlaylistTypeFilter.ALL -> {}
            }

            PlaylistUiModel(
                playlist = playlist,
                channelId = playlist.channelId,
                channelTitle = channelMap[playlist.channelId]?.title ?: "",
                videoCount = plVideos.size,
                totalViews = playlist.totalViewCount,
                totalDurationSeconds = plVideos.sumOf { it.durationSeconds },
                latestDate = plVideos.maxOf { it.releaseDate }.takeIf { it.isNotEmpty() } ?: "",
                firstVideoThumbnailUrl = playlist.thumbnailUrl,
                isCourse = playlist.isCourse
            )
        }.let { list ->
            when (params.sortBy) {
                VideoSort.POPULAR -> list.sortedByDescending { it.totalViews }
                VideoSort.NEWEST -> list.sortedByDescending { it.latestDate }
                VideoSort.OLDEST -> list.sortedBy { it.latestDate }
                VideoSort.DURATION_LONG_FIRST -> list.sortedByDescending { it.totalDurationSeconds }
                VideoSort.DURATION_SHORT_FIRST -> list.sortedBy { it.totalDurationSeconds }
                VideoSort.RELEVANCE -> list.sortedByDescending { it.totalViews }
                else -> list.sortedBy { it.playlist.title.lowercase() }
            }
        }
    }

    val channelPlaylists: StateFlow<List<Playlist>> =
        combine(_activeChannel, _playlists, _disabledChannelIds) { chanId, all, disabledIds ->
            val enabledPlaylists = all.filter { it.channelId !in disabledIds }
            if (chanId == null) enabledPlaylists else enabledPlaylists.filter { it.channelId == chanId }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        if (_activeChannel.value != null) {
            _activeChannel.value = null
            _activePlaylist.value = null
        }
    }

    fun setChannelFilter(channelId: String?) {
        _activeChannel.value = channelId
        _activePlaylist.value = null
        _query.value = ""
    }

    fun setPlaylistFilter(playlistId: String?) { _activePlaylist.value = playlistId }
    fun setSort(newSort: VideoSort) { _sort.value = newSort }
    fun setTypeFilter(filter: PlaylistTypeFilter) { _typeFilter.value = filter }

    fun clearFilters() {
        _query.value = ""
        _activeChannel.value = null
        _activePlaylist.value = null
        _sort.value = VideoSort.RELEVANCE
    }
}
