package com.complexly.academy.data

import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import complexlyacademy.composeapp.generated.resources.Res

interface JsonDataSource {
    suspend fun getChannels(): List<Channel>
    suspend fun getPlaylists(): List<Playlist>
    suspend fun getVideos(): List<Video>
}

/**
 * Reads per-channel JSON files from composeResources.
 * 
 * File layout:
 *   channels.json
 *   videos_{channelId}.json   (e.g. videos_crashcourse.json)
 *   playlists_{channelId}.json (e.g. playlists_crashcourse.json)
 *
 * This allows each channel's data to be updated independently,
 * and makes it easy to host files remotely for incremental downloads.
 */
class JsonDataSourceImpl(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) : JsonDataSource {
    private var cachedChannels: List<Channel>? = null
    private var cachedPlaylists: List<Playlist>? = null
    private var cachedVideos: List<Video>? = null

    private suspend fun readResourceText(path: String): String {
        @OptIn(ExperimentalResourceApi::class)
        return Res.readBytes(path).decodeToString()
    }

    override suspend fun getChannels(): List<Channel> = withContext(Dispatchers.IO) {
        cachedChannels ?: try {
            val content = readResourceText("files/channels.json")
            json.decodeFromString<List<Channel>>(content).also { channels ->
                cachedChannels = channels
                channels.forEach { channel ->
                    com.complexly.academy.model.ChannelColorRegistry.registerColor(channel.id, channel.primaryColor)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        cachedPlaylists ?: run {
            val allPlaylists = mutableListOf<Playlist>()
            val channels = getChannels()
            for (channel in channels) {
                try {
                    val content = readResourceText("files/playlists_${channel.id}.json")
                    val channelPlaylists = json.decodeFromString<List<Playlist>>(content)
                    allPlaylists.addAll(channelPlaylists)
                } catch (e: Exception) {
                    // Channel may not have a playlists file yet (e.g. bizarrebeasts)
                }
            }
            allPlaylists.also { cachedPlaylists = it }
        }
    }

    override suspend fun getVideos(): List<Video> = withContext(Dispatchers.IO) {
        cachedVideos ?: run {
            val allVideos = mutableListOf<Video>()
            val channels = getChannels()
            for (channel in channels) {
                try {
                    val content = readResourceText("files/videos_${channel.id}.json")
                    val channelVideos = json.decodeFromString<List<Video>>(content)
                    allVideos.addAll(channelVideos)
                } catch (e: Exception) {
                    // Channel may not have a videos file yet
                }
            }
            allVideos.also { cachedVideos = it }
        }
    }
}
