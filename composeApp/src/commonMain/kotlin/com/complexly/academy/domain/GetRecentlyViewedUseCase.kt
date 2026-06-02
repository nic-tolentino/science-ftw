package com.complexly.academy.domain

import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

sealed interface RecentViewItem {
    data class VideoItem(val video: Video) : RecentViewItem
    data class PlaylistItem(val playlist: Playlist, val isCourse: Boolean) : RecentViewItem
}

class GetRecentlyViewedUseCase(
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<List<RecentViewItem>> {
        return combine(
            contentRepository.getPlaylists(),
            contentRepository.searchVideos(""),
            userRepository.learningProgress
        ) { playlistsRes, videosRes, learningProgress ->
            if (playlistsRes !is Resource.Success || videosRes !is Resource.Success) {
                return@combine emptyList()
            }

            val playlists = playlistsRes.data
            val videos = videosRes.data
            val playlistMap = playlists.associateBy { it.id }
            val videoMap = videos.associateBy { it.id }

            val resolved = learningProgress.recentlyViewedItemIds.mapNotNull { itemStr ->
                val parts = itemStr.split(":")
                if (parts.size != 2) return@mapNotNull null
                val type = parts[0]
                val id = parts[1]

                when (type) {
                    "video" -> {
                        val video = videoMap[id] ?: return@mapNotNull null
                        RecentViewItem.VideoItem(video)
                    }
                    "playlist" -> {
                        val playlist = playlistMap[id] ?: return@mapNotNull null
                        val isCourse = contentRepository.isCourse(playlist.id)
                        RecentViewItem.PlaylistItem(playlist, isCourse)
                    }
                    else -> null
                }
            }

            resolved
        }
    }
}
