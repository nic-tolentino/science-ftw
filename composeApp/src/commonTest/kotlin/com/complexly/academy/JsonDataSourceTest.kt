package com.complexly.academy

import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Video
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonDataSourceTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun readResourceText(path: String): String {
        var localPath = "composeApp/src/commonMain/composeResources/$path".toPath()
        if (!FileSystem.SYSTEM.exists(localPath)) {
            localPath = "src/commonMain/composeResources/$path".toPath()
        }
        if (FileSystem.SYSTEM.exists(localPath)) {
            return FileSystem.SYSTEM.read(localPath) {
                readUtf8()
            }
        }
        throw IllegalArgumentException("Resource path not found in test environment: $path")
    }

    @Test
    fun testRealChannelsJsonParsing() = runTest {
        try {
            val content = readResourceText("files/channels.json")
            val channels = json.decodeFromString<List<Channel>>(content)
            assertFalse(channels.isEmpty(), "Channels list should not be empty")
            
            channels.forEach { channel ->
                assertTrue(channel.id.isNotEmpty(), "Channel ID should not be empty")
                assertTrue(channel.title.isNotEmpty(), "Channel Title should not be empty")
            }
        } catch (e: Exception) {
            println("FAILED PARSING CHANNELS JSON: $e")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testRealPlaylistsJsonParsing() = runTest {
        try {
            val content = readResourceText("files/playlists_crashcourse.json")
            val playlists = json.decodeFromString<List<Playlist>>(content)
            assertFalse(playlists.isEmpty(), "Playlists list should not be empty")
            
            playlists.forEach { playlist ->
                assertTrue(playlist.id.isNotEmpty(), "Playlist ID should not be empty")
                assertTrue(playlist.channelId.isNotEmpty(), "Playlist Channel ID should not be empty")
                assertTrue(playlist.title.isNotEmpty(), "Playlist Title should not be empty")
            }
        } catch (e: Exception) {
            println("FAILED PARSING PLAYLISTS JSON: $e")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testRealVideosJsonParsing() = runTest {
        try {
            val content = readResourceText("files/videos_crashcourse.json")
            val videos = json.decodeFromString<List<Video>>(content)
            assertFalse(videos.isEmpty(), "Videos list should not be empty")
            
            videos.forEach { video ->
                assertTrue(video.id.isNotEmpty(), "Video ID should not be empty")
                assertTrue(video.playlistIds.isNotEmpty(), "Video Playlist IDs should not be empty")
                assertTrue(video.title.isNotEmpty(), "Video Title should not be empty")
                assertTrue(video.youtubeId.isNotEmpty(), "Video YouTube ID should not be empty")
            }
        } catch (e: Exception) {
            println("FAILED PARSING VIDEOS JSON: $e")
            e.printStackTrace()
            throw e
        }
    }
}
