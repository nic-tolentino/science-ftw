package com.complexly.academy

import com.complexly.academy.data.ContentRepositoryImpl
import com.complexly.academy.data.JsonDataSource
import com.complexly.academy.data.UserRepositoryImpl
import com.complexly.academy.data.createTestDataStore
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Option
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Question
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object LegacyData {
    val channel = Channel("crashcourse", "Crash Course", "Educational video courses", "#00A88F", "#FAF8F5")
    val playlist = Playlist("biology", "crashcourse", "Biology", "General biology course", listOf("bio_1", "bio_2"))
    val question = Question("q1", "What is the powerhouse of the cell?", listOf(
        Option("Nucleus", false, "Controls the cell"),
        Option("Mitochondria", true, "Generates energy"),
        Option("Ribosome", false, "Makes proteins")
    ))
    val video1 = Video("bio_1", listOf("biology"), "Intro to Biology", "Introduction video", 600, "yt_1", "https://img.youtube.com/vi/yt_1/hqdefault.jpg", listOf(question))
    val video2 = Video("bio_2", listOf("biology"), "Intro to Cells", "Cell biology basics", 700, "yt_2", "https://img.youtube.com/vi/yt_2/hqdefault.jpg")
}

internal object LegacyJsonDataSource : JsonDataSource {
    override suspend fun getChannels(): List<Channel> = listOf(LegacyData.channel)
    override suspend fun getPlaylists(): List<Playlist> = listOf(LegacyData.playlist)
    override suspend fun getVideos(): List<Video> = listOf(LegacyData.video1, LegacyData.video2)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScienceRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var contentRepository: ContentRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val dataStore = createTestDataStore("repository", testScope)
        contentRepository = ContentRepositoryImpl(LegacyJsonDataSource, testDispatcher)
        userRepository = UserRepositoryImpl(LegacyJsonDataSource, dataStore, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Content operations ────────────────────────────────────────────────

    @Test
    fun testGetChannelsReturnsSuccess() = runTest {
        val result = contentRepository.getChannels().first { it !is Resource.Loading }
        assertTrue(result is Resource.Success)
        assertEquals(1, result.data.size)
        assertEquals("crashcourse", result.data[0].id)
    }

    @Test
    fun testGetPlaylistsFiltersCorrectly() = runTest {
        val all = (contentRepository.getPlaylists().first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(1, all.size)

        val cc = (contentRepository.getPlaylists("crashcourse").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(1, cc.size)

        val missing = (contentRepository.getPlaylists("non_existent").first { it !is Resource.Loading } as Resource.Success).data
        assertTrue(missing.isEmpty())
    }

    @Test
    fun testGetPlaylistDetails() = runTest {
        val result = contentRepository.getPlaylist("biology").first { it !is Resource.Loading }
        assertTrue(result is Resource.Success)
        assertEquals("Biology", result.data.title)

        assertTrue(contentRepository.getPlaylist("non_existent").first { it !is Resource.Loading } is Resource.Error)
    }

    @Test
    fun testGetVideoDetails() = runTest {
        val result = contentRepository.getVideo("bio_1").first { it !is Resource.Loading }
        assertTrue(result is Resource.Success)
        assertEquals("Intro to Biology", result.data.title)

        assertTrue(contentRepository.getVideo("non_existent").first { it !is Resource.Loading } is Resource.Error)
    }

    @Test
    fun testGetVideosForPlaylist() = runTest {
        val videos = (contentRepository.getVideosForPlaylist("biology").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(2, videos.size)
        assertEquals("bio_1", videos[0].id)
        assertEquals("bio_2", videos[1].id)
    }

    @Test
    fun testSearchVideos() = runTest {
        // "Intro to Biology": bio_2 "Intro to Cells" also matches ("intro", "to" present in its title),
        // but bio_1 scores far higher (phrase match + all 3 words in title).
        val byTitle = (contentRepository.searchVideos("Intro to Biology").first { it !is Resource.Loading } as Resource.Success).data
        assertTrue(byTitle.isNotEmpty())
        assertEquals("bio_1", byTitle[0].id)

        val byCells = (contentRepository.searchVideos("cells").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(1, byCells.size)
        assertEquals("bio_2", byCells[0].id)

        val all = (contentRepository.searchVideos("").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(2, all.size)
    }

    @Test
    fun testGetDailyQuestion() = runTest {
        val result = contentRepository.getDailyQuestion().first { it !is Resource.Loading }
        assertTrue(result is Resource.Success)
        assertEquals("What is the powerhouse of the cell?", result.data.text)
    }

    @Test
    fun testGetVideosForPlaylistReturnsInVideoIdsOrder() = runTest {
        // After the playlist ID migration, videoIds contains YouTube IDs and defines the ordering.
        // Inserting realVideo2 first in the video list to confirm the result order follows
        // videoIds, not the data source order.
        val video1 = Video("yt_abc1234", listOf("ordered_pl"), "First", "Desc", 600, "yt_abc1234", "https://img.youtube.com/vi/yt_abc1234/hqdefault.jpg")
        val video2 = Video("yt_xyz5678", listOf("ordered_pl"), "Second", "Desc", 700, "yt_xyz5678", "https://img.youtube.com/vi/yt_xyz5678/hqdefault.jpg")
        val playlist = Playlist("ordered_pl", "crashcourse", "Ordered", "Desc", listOf("yt_abc1234", "yt_xyz5678"))

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(LegacyData.channel)
            override suspend fun getPlaylists() = listOf(playlist)
            override suspend fun getVideos() = listOf(video2, video1) // reversed order in source
        }
        val repo = ContentRepositoryImpl(customDataSource, testDispatcher)

        val videos = (repo.getVideosForPlaylist("ordered_pl").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(2, videos.size)
        assertEquals("yt_abc1234", videos[0].id) // playlist order, not source order
        assertEquals("yt_xyz5678", videos[1].id)
    }

    @Test
    fun testGetVideosForPlaylistWithPlaceholderIds() = runTest {
        // Playlist still has unresolved placeholder IDs (not yet migrated or stale data):
        // direct lookup finds nothing, so falls back to playlistIds-based filtering.
        val customPlaylist = Playlist("placeholder_playlist", "crashcourse", "Placeholder Playlist", "Desc", listOf("cc_biology_01", "cc_biology_02"))
        val realVideo1 = Video("real_id_1", listOf("placeholder_playlist"), "Real Video 1", "Desc", 600, "yt_1", "https://img.youtube.com/vi/yt_1/hqdefault.jpg")
        val realVideo2 = Video("real_id_2", listOf("placeholder_playlist"), "Real Video 2", "Desc", 700, "yt_2", "https://img.youtube.com/vi/yt_2/hqdefault.jpg")

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels(): List<Channel> = listOf(LegacyData.channel)
            override suspend fun getPlaylists(): List<Playlist> = listOf(LegacyData.playlist, customPlaylist)
            override suspend fun getVideos(): List<Video> = listOf(LegacyData.video1, LegacyData.video2, realVideo1, realVideo2)
        }
        val repo = ContentRepositoryImpl(customDataSource, testDispatcher)

        val videos = (repo.getVideosForPlaylist("placeholder_playlist").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(2, videos.size)
        assertEquals("real_id_1", videos[0].id)
        assertEquals("real_id_2", videos[1].id)
    }

    @Test
    fun testGetVideosForPlaylistFallback() = runTest {
        val customPlaylist = Playlist("fallback_playlist", "crashcourse", "Fallback Playlist", "Desc", listOf("no_suffix_no_match"))
        val realVideo1 = Video("real_id_1", listOf("fallback_playlist"), "Real Video 1", "Desc", 600, "yt_1", "https://img.youtube.com/vi/yt_1/hqdefault.jpg")
        val realVideo2 = Video("real_id_2", listOf("fallback_playlist"), "Real Video 2", "Desc", 700, "yt_2", "https://img.youtube.com/vi/yt_2/hqdefault.jpg")

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels(): List<Channel> = listOf(LegacyData.channel)
            override suspend fun getPlaylists(): List<Playlist> = listOf(LegacyData.playlist, customPlaylist)
            override suspend fun getVideos(): List<Video> = listOf(LegacyData.video1, LegacyData.video2, realVideo1, realVideo2)
        }
        val repo = ContentRepositoryImpl(customDataSource, testDispatcher)

        val videos = (repo.getVideosForPlaylist("fallback_playlist").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(2, videos.size)
        assertEquals("real_id_1", videos[0].id)
        assertEquals("real_id_2", videos[1].id)
    }

    // ── User operations ───────────────────────────────────────────────────

    @Test
    fun testMarkVideoCompletedAndStreak() = runTest {
        userRepository.learningProgress.first { it.streakCount == 6 }
        assertTrue(userRepository.learningProgress.value.completedVideoIds.isEmpty())

        userRepository.markVideoCompleted("bio_1")
        userRepository.learningProgress.first { it.completedVideoIds.contains("bio_1") }
        assertTrue(userRepository.learningProgress.value.completedCourseIds.isEmpty())

        userRepository.markVideoCompleted("bio_2")
        userRepository.learningProgress.first { it.completedVideoIds.contains("bio_2") }
        assertTrue(userRepository.learningProgress.value.completedCourseIds.contains("biology"))

        userRepository.updateStreak(increment = true)
        userRepository.learningProgress.first { it.streakCount == 7 }
        assertEquals(7, userRepository.learningProgress.value.streakCount)
    }
}
