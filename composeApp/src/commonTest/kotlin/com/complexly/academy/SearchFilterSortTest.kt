package com.complexly.academy

import com.complexly.academy.data.ContentRepositoryImpl
import com.complexly.academy.data.VideoSort
import com.complexly.academy.data.createTestDataStore
import com.complexly.academy.model.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for repository search, filtering, and sorting
 * using the full multi-channel dataset (CrashCourse, SciShow, PBS Eons).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchFilterSortTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeDataSource = FullFakeJsonDataSource()
    private lateinit var contentRepository: ContentRepositoryImpl

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        contentRepository = ContentRepositoryImpl(fakeDataSource, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Basic data loading ────────────────────────────────────────────────

    @Test
    fun `loads all channels`() = runTest {
        val result = contentRepository.getChannels().first { it !is Resource.Loading } as Resource.Success
        assertEquals(3, result.data.size)
        assertEquals(setOf("crashcourse", "scishow", "pbseons"), result.data.map { it.id }.toSet())
    }

    @Test
    fun `loads all playlists`() = runTest {
        val result = contentRepository.getPlaylists().first { it !is Resource.Loading } as Resource.Success
        assertEquals(4, result.data.size)
    }

    @Test
    fun `loads all videos`() = runTest {
        val result = contentRepository.searchVideos("").first { it !is Resource.Loading } as Resource.Success
        assertEquals(9, result.data.size)
    }

    // ── Search by query ───────────────────────────────────────────────────

    @Test
    fun `search by video title returns match`() = runTest {
        val result = contentRepository.searchVideos("Water").first { it !is Resource.Loading } as Resource.Success
        assertEquals(1, result.data.size)
        assertEquals("cc_bio_02", result.data[0].id)
    }

    @Test
    fun `search by description returns match`() = runTest {
        val result = contentRepository.searchVideos("dark matter").first { it !is Resource.Loading } as Resource.Success
        assertEquals(1, result.data.size)
        assertEquals("ss_space_02", result.data[0].id)
    }

    @Test
    fun `search is case insensitive`() = runTest {
        val result = contentRepository.searchVideos("BIOLOGY").first { it !is Resource.Loading } as Resource.Success
        assert(result.data.isNotEmpty())
    }

    @Test
    fun `blank query returns all videos`() = runTest {
        val result = contentRepository.searchVideos("").first { it !is Resource.Loading } as Resource.Success
        assertEquals(9, result.data.size)
    }

    @Test
    fun `search by playlist title returns that playlist's videos`() = runTest {
        val result = contentRepository.searchVideos("Space").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals(setOf("ss_space_01", "ss_space_02"), result.data.map { it.id }.toSet())
    }

    // ── Channel filter ────────────────────────────────────────────────────

    @Test
    fun `filter by CrashCourse returns all CrashCourse videos`() = runTest {
        val result = contentRepository.searchVideos(channelId = "crashcourse").first { it !is Resource.Loading } as Resource.Success
        assertEquals(5, result.data.size)
    }

    @Test
    fun `filter by SciShow returns SciShow videos`() = runTest {
        val result = contentRepository.searchVideos(channelId = "scishow").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals(setOf("ss_space_01", "ss_space_02"), result.data.map { it.id }.toSet())
    }

    @Test
    fun `filter by PBS Eons returns Eons videos`() = runTest {
        val result = contentRepository.searchVideos(channelId = "pbseons").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals(setOf("eo_dt_01", "eo_dt_02"), result.data.map { it.id }.toSet())
    }

    @Test
    fun `channel filter + query both apply`() = runTest {
        val result = contentRepository.searchVideos(query = "Water", channelId = "crashcourse").first { it !is Resource.Loading } as Resource.Success
        assertEquals(1, result.data.size)
        assertEquals("cc_bio_02", result.data[0].id)
    }

    @Test
    fun `channel filter excludes non-matching`() = runTest {
        val result = contentRepository.searchVideos(query = "Water", channelId = "scishow").first { it !is Resource.Loading } as Resource.Success
        assertEquals(0, result.data.size)
    }

    // ── Playlist filter ───────────────────────────────────────────────────

    @Test
    fun `filter by playlist returns that playlist's videos`() = runTest {
        val result = contentRepository.searchVideos(playlistId = "cc_biology").first { it !is Resource.Loading } as Resource.Success
        assertEquals(3, result.data.size)
        assertEquals(setOf("cc_bio_01", "cc_bio_02", "cc_bio_03"), result.data.map { it.id }.toSet())
    }

    @Test
    fun `playlist filter + channel filter both apply`() = runTest {
        val result = contentRepository.searchVideos(channelId = "scishow", playlistId = "ss_space").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
    }

    @Test
    fun `playlist filter + query both apply`() = runTest {
        val result = contentRepository.searchVideos(query = "Cambrian", playlistId = "eo_deep_time").first { it !is Resource.Loading } as Resource.Success
        assertEquals(1, result.data.size)
        assertEquals("eo_dt_02", result.data[0].id)
    }

    // ── Sort by popularity ────────────────────────────────────────────────

    @Test
    fun `sort by popularity desc`() = runTest {
        val result = contentRepository.searchVideos(sortBy = VideoSort.POPULAR).first { it !is Resource.Loading } as Resource.Success
        assertEquals(9, result.data.size)
        assertEquals("ss_space_02", result.data[0].id)
        assertEquals("ss_space_01", result.data[1].id)
        assertEquals("eo_dt_02", result.data[2].id)
    }

    @Test
    fun `sort by popularity within channel filter`() = runTest {
        val result = contentRepository.searchVideos(channelId = "crashcourse", sortBy = VideoSort.POPULAR).first { it !is Resource.Loading } as Resource.Success
        assertEquals(5, result.data.size)
        assertEquals("cc_bio_02", result.data[0].id)
    }

    // ── Sort by date ──────────────────────────────────────────────────────

    @Test
    fun `sort by newest desc`() = runTest {
        val result = contentRepository.searchVideos(sortBy = VideoSort.NEWEST).first { it !is Resource.Loading } as Resource.Success
        assertEquals(9, result.data.size)
        assertEquals("2024-04-15", result.data[0].releaseDate)
        assertEquals("2024-04-01", result.data[1].releaseDate)
    }

    @Test
    fun `sort by oldest asc`() = runTest {
        val result = contentRepository.searchVideos(sortBy = VideoSort.OLDEST).first { it !is Resource.Loading } as Resource.Success
        assertEquals(9, result.data.size)
        assertEquals("2024-01-15", result.data[0].releaseDate)
        assertEquals("2024-01-22", result.data[1].releaseDate)
    }

    // ── Sort by duration ──────────────────────────────────────────────────

    @Test
    fun `sort by duration shortest first`() = runTest {
        val result = contentRepository.searchVideos(sortBy = VideoSort.DURATION_SHORT_FIRST).first { it !is Resource.Loading } as Resource.Success
        assertEquals(9, result.data.size)
        assertEquals(480, result.data[0].durationSeconds)
        assertEquals(520, result.data[1].durationSeconds)
    }

    @Test
    fun `sort by duration longest first`() = runTest {
        val result = contentRepository.searchVideos(sortBy = VideoSort.DURATION_LONG_FIRST).first { it !is Resource.Loading } as Resource.Success
        assertEquals(9, result.data.size)
        assertEquals(720, result.data[0].durationSeconds)
    }

    // ── Sort by title ─────────────────────────────────────────────────────

    @Test
    fun `sort by title A-Z`() = runTest {
        val result = contentRepository.searchVideos(sortBy = VideoSort.TITLE_AZ).first { it !is Resource.Loading } as Resource.Success
        val titles = result.data.map { it.title.lowercase() }
        assertEquals(titles, titles.sorted())
    }

    @Test
    fun `sort by title Z-A`() = runTest {
        val result = contentRepository.searchVideos(sortBy = VideoSort.TITLE_ZA).first { it !is Resource.Loading } as Resource.Success
        val titles = result.data.map { it.title.lowercase() }
        assertEquals(titles, titles.sortedDescending())
    }

    // ── getChannelIdForPlaylist ───────────────────────────────────────────

    @Test
    fun `getChannelIdForPlaylist resolves CrashCourse playlist`() = runTest {
        assertEquals("crashcourse", contentRepository.getChannelIdForPlaylist("cc_biology"))
        assertEquals("crashcourse", contentRepository.getChannelIdForPlaylist("cc_chemistry"))
    }

    @Test
    fun `getChannelIdForPlaylist resolves SciShow playlist`() = runTest {
        assertEquals("scishow", contentRepository.getChannelIdForPlaylist("ss_space"))
    }

    @Test
    fun `getChannelIdForPlaylist resolves PBS Eons playlist`() = runTest {
        assertEquals("pbseons", contentRepository.getChannelIdForPlaylist("eo_deep_time"))
    }

    @Test
    fun `getChannelIdForPlaylist returns default for unknown`() = runTest {
        assertEquals("complexly", contentRepository.getChannelIdForPlaylist("nonexistent"))
    }

    @Test
    fun `getRecommendedVideos returns results sorted by release date descending`() = runTest {
        val result = contentRepository.getRecommendedVideos(
            disabledChannelIds = setOf("scishow", "pbseons", "bizarrebeasts", "studyhall", "microcosmos"),
            completedIds = emptySet()
        ).first { it !is Resource.Loading } as Resource.Success
        
        val recommended = result.data
        assertTrue(recommended.isNotEmpty())
        
        // CrashCourse Chemistry and Biology videos both match interest and should be sorted descending by releaseDate:
        // cc_chem_02 (2024-02-08) -> cc_chem_01 (2024-02-01) -> cc_bio_03 (2024-01-29)
        assertEquals("cc_chem_02", recommended[0].id)
        assertEquals("cc_chem_01", recommended[1].id)
        assertEquals("cc_bio_03", recommended[2].id)
    }

    @Test
    fun `getRecommendedVideos prioritizes partially completed course and excludes completed videos`() = runTest {
        val result = contentRepository.getRecommendedVideos(
            disabledChannelIds = emptySet(),
            completedIds = setOf("cc_bio_01")
        ).first { it !is Resource.Loading } as Resource.Success
        
        val recommended = result.data
        assertTrue(recommended.isNotEmpty())
        
        // 1. Completed video "cc_bio_01" must be excluded
        assertTrue(recommended.none { it.id == "cc_bio_01" })
        
        // 2. Uncompleted videos from the same playlist ("cc_biology") should be ranked first (+20 playlist match, +10 channel match):
        // cc_bio_03 (2024-01-29) -> cc_bio_02 (2024-01-22)
        assertEquals("cc_bio_03", recommended[0].id)
        assertEquals("cc_bio_02", recommended[1].id)
        
        // 3. Other videos from the same channel ("crashcourse", e.g. chemistry) get channel match (+10 pts):
        // cc_chem_02 (2024-02-08) -> cc_chem_01 (2024-02-01)
        assertEquals("cc_chem_02", recommended[2].id)
        assertEquals("cc_chem_01", recommended[3].id)
    }

    // ── Word-level search filter ───────────────────────────────────────────

    @Test
    fun `multi-word query matches video containing any single word`() = runTest {
        // "cambrian ediacaran" — eo_dt_02 has "Cambrian", eo_dt_01 has "Ediacaran".
        // With phrase-only matching this would return 0 results; word-level OR returns both.
        val result = contentRepository.searchVideos("cambrian ediacaran").first { it !is Resource.Loading } as Resource.Success
        val ids = result.data.map { it.id }.toSet()
        assertTrue("eo_dt_02" in ids) // title "The Cambrian Explosion" matches word "cambrian"
        assertTrue("eo_dt_01" in ids) // title "The Ediacaran" matches word "ediacaran"
    }

    @Test
    fun `multi-word query ranks video matching more words higher`() = runTest {
        // "biology water" — cc_bio_02 has BOTH words in its title ("Water...Biology #2")
        // cc_bio_01 / cc_bio_03 have only "biology" → lower score
        val result = contentRepository.searchVideos("biology water").first { it !is Resource.Loading } as Resource.Success
        assertTrue(result.data.isNotEmpty())
        assertEquals("cc_bio_02", result.data[0].id)
    }
}

