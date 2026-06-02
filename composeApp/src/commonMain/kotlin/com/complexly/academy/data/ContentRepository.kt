package com.complexly.academy.data

import com.complexly.academy.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class VideoSort {
    RELEVANCE,
    TITLE_AZ,
    TITLE_ZA,
    POPULAR,
    MOST_LIKED,
    NEWEST,
    OLDEST,
    DURATION_SHORT_FIRST,
    DURATION_LONG_FIRST,
}

/**
 * Read-only content layer: channels, playlists, videos, and derived recommendations.
 * Contains no user state — see [UserRepository] for progress and preferences.
 */
interface ContentRepository {
    fun getChannels(): Flow<Resource<List<Channel>>>
    fun getPlaylists(channelId: String? = null): Flow<Resource<List<Playlist>>>
    fun getPlaylist(playlistId: String): Flow<Resource<Playlist>>
    fun getVideo(videoId: String): Flow<Resource<Video>>
    fun getVideosForPlaylist(playlistId: String): Flow<Resource<List<Video>>>
    fun searchVideos(
        query: String = "",
        channelId: String? = null,
        playlistId: String? = null,
        sortBy: VideoSort = VideoSort.RELEVANCE
    ): Flow<Resource<List<Video>>>
    fun getDailyQuestion(): Flow<Resource<Question>>
    /** Synchronous lookup from cached state — returns "complexly" if called before data loads. */
    fun getChannelIdForPlaylist(playlistId: String): String
    /**
     * Returns true if every video in [playlistId] has at least one quiz question.
     * Reads from cached state — returns false if called before video data has loaded.
     */
    fun isCourse(playlistId: String): Boolean
    /** Playlists sorted by average view count; excludes playlists with no matching videos. */
    fun getPopularPlaylists(): Flow<Resource<List<Playlist>>>
    /** Playlists sorted by average [Video.rating]; excludes playlists with no matching videos. */
    fun getHighestRatedPlaylists(): Flow<Resource<List<Playlist>>>
    /**
     * Up to 4 related videos, scored by: same playlist (+10), same channel (+5),
     * each shared tag (+3), each shared category (+2). Ties broken by view count.
     */
    fun getRelatedVideos(videoId: String): Flow<Resource<List<Video>>>
    /**
     * Up to 6 recommended videos in priority order: uncompleted videos belonging to enabled channels.
     * Parameters are passed in (not read from UserRepository) to keep the two repositories decoupled.
     */
    fun getRecommendedVideos(
        disabledChannelIds: Set<String>,
        completedIds: Set<String>
    ): Flow<Resource<List<Video>>>
}

class ContentRepositoryImpl(
    private val jsonDataSource: JsonDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ContentRepository {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val channelsState = MutableStateFlow<Resource<List<Channel>>>(Resource.Loading)
    private val playlistsState = MutableStateFlow<Resource<List<Playlist>>>(Resource.Loading)
    private val videosState = MutableStateFlow<Resource<List<Video>>>(Resource.Loading)

    init {
        scope.launch { loadAllData() }
    }

    private suspend fun loadAllData() {
        try {
            val channels = jsonDataSource.getChannels()
            channels.forEach { channel ->
                com.complexly.academy.model.ChannelColorRegistry.registerColor(channel.id, channel.primaryColor)
            }
            channelsState.value = Resource.Success(channels)
        } catch (e: Exception) {
            channelsState.value = Resource.Error("Failed to load channels", e)
        }
        // Videos load before playlists so enrichPlaylists can derive stats in one pass.
        val videos: List<Video>
        try {
            videos = jsonDataSource.getVideos()
            videosState.value = Resource.Success(videos)
        } catch (e: Exception) {
            videosState.value = Resource.Error("Failed to load videos", e)
            return
        }
        try {
            val playlists = jsonDataSource.getPlaylists()
            playlistsState.value = Resource.Success(enrichPlaylists(playlists, videos))
        } catch (e: Exception) {
            playlistsState.value = Resource.Error("Failed to load playlists", e)
        }
    }

    private fun enrichPlaylists(playlists: List<Playlist>, videos: List<Video>): List<Playlist> {
        val byPlaylist = videosByPlaylist(videos)
        return playlists.map { pl ->
            val plVideos = byPlaylist[pl.id] ?: emptyList()
            val thumbnailUrl = pl.videoIds.firstOrNull()
                ?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" } ?: ""
            val avgViewCount = if (plVideos.isNotEmpty()) plVideos.map { it.viewCount }.average().toLong() else 0L
            val totalViewCount = plVideos.sumOf { it.viewCount }
            val avgRating = if (plVideos.isNotEmpty()) plVideos.map { it.rating }.average() else 0.0
            val isCourse = plVideos.isNotEmpty() && plVideos.all { it.questions.isNotEmpty() }
            pl.copy(
                thumbnailUrl = thumbnailUrl,
                avgViewCount = avgViewCount,
                totalViewCount = totalViewCount,
                avgRating = avgRating,
                isCourse = isCourse,
                videoCount = plVideos.size
            )
        }
    }

    override fun getChannels(): Flow<Resource<List<Channel>>> = channelsState

    override fun getPlaylists(channelId: String?): Flow<Resource<List<Playlist>>> =
        playlistsState.map { res ->
            when (res) {
                is Resource.Success -> Resource.Success(
                    if (channelId != null) res.data.filter { it.channelId == channelId } else res.data
                )
                is Resource.Error -> Resource.Error(res.message, res.cause)
                is Resource.Loading -> Resource.Loading
            }
        }

    override fun getPlaylist(playlistId: String): Flow<Resource<Playlist>> =
        playlistsState.map { res ->
            when (res) {
                is Resource.Success -> res.data.find { it.id == playlistId }
                    ?.let { Resource.Success(it) }
                    ?: Resource.Error("Playlist not found: $playlistId")
                is Resource.Error -> Resource.Error(res.message, res.cause)
                is Resource.Loading -> Resource.Loading
            }
        }

    override fun getVideo(videoId: String): Flow<Resource<Video>> =
        videosState.map { res ->
            when (res) {
                is Resource.Success -> res.data.find { it.id == videoId }
                    ?.let { Resource.Success(it) }
                    ?: Resource.Error("Video not found: $videoId")
                is Resource.Error -> Resource.Error(res.message, res.cause)
                is Resource.Loading -> Resource.Loading
            }
        }

    override fun getVideosForPlaylist(playlistId: String): Flow<Resource<List<Video>>> =
        combine2(playlistsState, videosState) { playlistsRes, videosRes ->
            if (playlistsRes is Resource.Success && videosRes is Resource.Success) {
                val playlist = playlistsRes.data.find { it.id == playlistId }
                    ?: return@combine2 Resource.Error("Playlist not found: $playlistId")

                val ordered = playlist.videoIds.mapNotNull { vidId ->
                    videosRes.data.find { it.id == vidId }
                }
                Resource.Success(if (ordered.isNotEmpty()) ordered else videosRes.data.filter { playlistId in it.playlistIds })
            } else if (playlistsRes is Resource.Error) {
                Resource.Error(playlistsRes.message, playlistsRes.cause)
            } else if (videosRes is Resource.Error) {
                Resource.Error(videosRes.message, videosRes.cause)
            } else {
                Resource.Loading
            }
        }

    override fun searchVideos(
        query: String,
        channelId: String?,
        playlistId: String?,
        sortBy: VideoSort
    ): Flow<Resource<List<Video>>> =
        combine3(videosState, channelsState, playlistsState) { videosRes, channelsRes, playlistsRes ->
            if (videosRes is Resource.Loading || channelsRes is Resource.Loading || playlistsRes is Resource.Loading) {
                return@combine3 Resource.Loading
            }
            if (videosRes is Resource.Error) return@combine3 Resource.Error(videosRes.message, videosRes.cause)

            val allVideos = (videosRes as Resource.Success).data
            val allChannels = (channelsRes as Resource.Success).data
            val allPlaylists = (playlistsRes as Resource.Success).data

            val playlistChannelMap = allPlaylists.associate { it.id to it.channelId }
            val playlistTitleMap = allPlaylists.associate { it.id to it.title }
            fun channelFor(plId: String) = playlistChannelMap[plId] ?: "complexly"

            val channelFromQuery = if (query.isNotBlank()) {
                allChannels.firstOrNull { it.title.contains(query, ignoreCase = true) }?.id
            } else null

            var results = allVideos

            if (query.isNotBlank() && channelFromQuery == null) {
                // Split into words so "cell biology" matches any video mentioning "cell" OR "biology".
                // The relevance scorer then sums per-word scores, so a video matching more words ranks higher.
                val queryWords = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                results = results.filter { video ->
                    val titleLower = video.title.lowercase()
                    val descLower  = video.description.lowercase()
                    queryWords.any { word ->
                        titleLower.contains(word) ||
                        video.playlistIds.any { plId -> playlistTitleMap[plId]?.lowercase()?.contains(word) == true } ||
                        video.tags.any { it.lowercase().contains(word) } ||
                        video.categories.any { it.lowercase().contains(word) } ||
                        descLower.contains(word)
                    }
                }
            }
            if (channelFromQuery != null) {
                results = results.filter { v -> v.playlistIds.any { channelFor(it) == channelFromQuery } }
            }
            if (channelId != null) {
                results = results.filter { v -> v.playlistIds.any { channelFor(it) == channelId } }
            }
            if (playlistId != null) {
                results = results.filter { v -> playlistId in v.playlistIds }
            }

            results = when (sortBy) {
                // Rank by weighted field relevance; only applied when query text is the signal.
                // Channel-name queries already filtered to one channel, so no intra-set ranking needed.
                VideoSort.RELEVANCE -> if (query.isNotBlank() && channelFromQuery == null) {
                    results.sortedByDescending { relevanceScore(it, query, playlistTitleMap) }
                } else {
                    results.sortedByDescending { it.viewCount }
                }
                VideoSort.TITLE_AZ -> results.sortedBy { it.title.lowercase() }
                VideoSort.TITLE_ZA -> results.sortedByDescending { it.title.lowercase() }
                VideoSort.POPULAR -> results.sortedByDescending { it.viewCount }
                VideoSort.MOST_LIKED -> results.sortedByDescending { it.likeCount }
                VideoSort.NEWEST -> results.sortedByDescending { it.releaseDate }
                VideoSort.OLDEST -> results.sortedBy { it.releaseDate }
                VideoSort.DURATION_SHORT_FIRST -> results.sortedBy { it.durationSeconds }
                VideoSort.DURATION_LONG_FIRST -> results.sortedByDescending { it.durationSeconds }
            }

            Resource.Success(results)
        }

    override fun getDailyQuestion(): Flow<Resource<Question>> = videosState.map { res ->
        when (res) {
            is Resource.Success -> res.data.flatMap { it.questions }.firstOrNull()
                ?.let { Resource.Success(it) }
                ?: Resource.Error("No questions available")
            is Resource.Error -> Resource.Error(res.message, res.cause)
            is Resource.Loading -> Resource.Loading
        }
    }

    override fun getChannelIdForPlaylist(playlistId: String): String {
        val playlists = (playlistsState.value as? Resource.Success)?.data ?: emptyList()
        return playlists.find { it.id == playlistId }?.channelId ?: "complexly"
    }

    override fun isCourse(playlistId: String): Boolean =
        (playlistsState.value as? Resource.Success)?.data?.find { it.id == playlistId }?.isCourse ?: false

    override fun getPopularPlaylists(): Flow<Resource<List<Playlist>>> =
        playlistsState.map { res ->
            when (res) {
                is Resource.Success -> Resource.Success(
                    res.data.filter { it.videoCount > 0 }.sortedByDescending { it.avgViewCount }
                )
                is Resource.Error -> Resource.Error(res.message, res.cause)
                is Resource.Loading -> Resource.Loading
            }
        }

    override fun getHighestRatedPlaylists(): Flow<Resource<List<Playlist>>> =
        playlistsState.map { res ->
            when (res) {
                is Resource.Success -> Resource.Success(
                    res.data.filter { it.videoCount > 0 }.sortedByDescending { it.avgRating }
                )
                is Resource.Error -> Resource.Error(res.message, res.cause)
                is Resource.Loading -> Resource.Loading
            }
        }

    override fun getRelatedVideos(videoId: String): Flow<Resource<List<Video>>> =
        combine2(videosState, playlistsState) { videosRes, playlistsRes ->
            if (videosRes is Resource.Loading || playlistsRes is Resource.Loading) return@combine2 Resource.Loading
            if (videosRes is Resource.Error) return@combine2 Resource.Error(videosRes.message, videosRes.cause)

            val allVideos = (videosRes as Resource.Success).data
            val allPlaylists = (playlistsRes as? Resource.Success)?.data ?: emptyList()

            val video = allVideos.find { it.id == videoId }
                ?: return@combine2 Resource.Error("Video not found: $videoId")

            val playlistChannelMap = allPlaylists.associate { it.id to it.channelId }
            val videoChannel = video.playlistIds.firstOrNull()?.let { playlistChannelMap[it] }

            val related = allVideos
                .filter { it.id != video.id }
                .map { other ->
                    var score = 0
                    if (other.playlistIds.any { it in video.playlistIds }) score += 10
                    val otherChannel = other.playlistIds.firstOrNull()?.let { playlistChannelMap[it] }
                    if (videoChannel != null && otherChannel == videoChannel) score += 5
                    score += other.tags.intersect(video.tags.toSet()).size * 3
                    score += other.categories.intersect(video.categories.toSet()).size * 2
                    other to score
                }
                .sortedWith(compareByDescending<Pair<Video, Int>> { it.second }.thenByDescending { it.first.viewCount })
                .map { it.first }
                .take(4)

            Resource.Success(related)
        }

    override fun getRecommendedVideos(
        disabledChannelIds: Set<String>,
        completedIds: Set<String>
    ): Flow<Resource<List<Video>>> =
        combine2(videosState, playlistsState) { videosRes, playlistsRes ->
            if (videosRes is Resource.Loading) return@combine2 Resource.Loading
            if (videosRes is Resource.Error) return@combine2 Resource.Error(videosRes.message, videosRes.cause)

            val allVideos = (videosRes as Resource.Success).data
            val allPlaylists = (playlistsRes as? Resource.Success)?.data ?: emptyList()
            val playlistChannelMap = allPlaylists.associate { it.id to it.channelId }

            val completedVideos = allVideos.filter { it.id in completedIds }
            val completedPlaylists = completedVideos.flatMap { it.playlistIds }.toSet()
            val completedChannels = completedVideos.mapNotNull { v ->
                v.playlistIds.firstOrNull()?.let { playlistChannelMap[it] }
            }.toSet()
            val completedTags = completedVideos.flatMap { it.tags }.toSet()
            val completedCategories = completedVideos.flatMap { it.categories }.toSet()

            // Filter uncompleted videos to only include those belonging to enabled channels
            val uncompleted = allVideos.filter { video ->
                video.id !in completedIds &&
                video.playlistIds.none { playlistChannelMap[it] in disabledChannelIds }
            }

            val scoredRecommendations = uncompleted.map { video ->
                var score = 0
                
                // 1. In-progress playlist match (+20 pts)
                val inProgressPl = video.playlistIds.any { it in completedPlaylists }
                if (inProgressPl) {
                    score += 20
                }
                
                // 2. Channel match with watched channel (+10 pts)
                val videoChannel = video.playlistIds.firstOrNull()?.let { playlistChannelMap[it] } ?: ""
                if (videoChannel.isNotEmpty() && videoChannel in completedChannels) {
                    score += 10
                }
                
                // 3. Tag and category similarity to completed videos (+2 pts per tag, +1 pt per category)
                score += video.tags.intersect(completedTags).size * 2
                score += video.categories.intersect(completedCategories).size * 1
                
                video to score
            }

            val sortedRecommendations = scoredRecommendations
                .sortedWith(
                    compareByDescending<Pair<Video, Int>> { it.second }
                        .thenByDescending { it.first.releaseDate }
                        .thenByDescending { it.first.viewCount }
                )
                .map { it.first }

            Resource.Success(
                sortedRecommendations.take(6)
            )
        }

    /**
     * Weighted relevance score for a single video against [query].
     *
     * Tier weights are chosen so that even one match in a higher tier outranks many matches
     * in a lower tier for typical query lengths (1–5 words):
     *   Title phrase +400 | title word +100
     *   Playlist title phrase +15 | playlist title word +8
     *   Tag word +15 | category word +15
     *   Description phrase +5 | description word +1
     */
    private fun relevanceScore(
        video: Video,
        query: String,
        playlistTitleMap: Map<String, String>
    ): Int {
        val q = query.lowercase().trim()
        val words = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return 0

        var score = 0
        val titleLower = video.title.lowercase()
        val descLower = video.description.lowercase()

        // Title (highest priority)
        if (titleLower.contains(q)) score += 400
        score += words.count { titleLower.contains(it) } * 100

        // Playlist title (medium-high — inherited context, not the video's own metadata)
        video.playlistIds.forEach { plId ->
            val plTitle = playlistTitleMap[plId]?.lowercase() ?: ""
            if (plTitle.contains(q)) score += 15
            score += words.count { plTitle.contains(it) } * 8
        }

        // Tags and categories (medium — explicit metadata on the video)
        score += words.count { w -> video.tags.any { it.lowercase().contains(w) } } * 15
        score += words.count { w -> video.categories.any { it.lowercase().contains(w) } } * 15

        // Description (lowest — broad prose, generates many false-positive signals)
        if (descLower.contains(q)) score += 5
        score += words.count { descLower.contains(it) }

        return score
    }

    private fun videosByPlaylist(videos: List<Video>): Map<String, List<Video>> =
        videos.flatMap { v -> v.playlistIds.map { plId -> plId to v } }
            .groupBy({ it.first }, { it.second })
}

// Named aliases to disambiguate from the `combine` import used inside ContentRepositoryImpl.
private fun <T1, T2, R> combine2(
    flow1: kotlinx.coroutines.flow.Flow<T1>,
    flow2: kotlinx.coroutines.flow.Flow<T2>,
    transform: suspend (T1, T2) -> R
): kotlinx.coroutines.flow.Flow<R> = kotlinx.coroutines.flow.combine(flow1, flow2, transform)

private fun <T1, T2, T3, R> combine3(
    flow1: kotlinx.coroutines.flow.Flow<T1>,
    flow2: kotlinx.coroutines.flow.Flow<T2>,
    flow3: kotlinx.coroutines.flow.Flow<T3>,
    transform: suspend (T1, T2, T3) -> R
): kotlinx.coroutines.flow.Flow<R> = kotlinx.coroutines.flow.combine(flow1, flow2, flow3) { a, b, c ->
    transform(a, b, c)
}
