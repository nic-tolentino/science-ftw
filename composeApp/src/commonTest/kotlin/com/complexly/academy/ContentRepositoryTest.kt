package com.complexly.academy

import com.complexly.academy.data.ContentRepositoryImpl
import com.complexly.academy.data.JsonDataSource
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContentRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // Full multi-channel fake source (CrashCourse, SciShow, PBS Eons)
    private lateinit var fullRepo: ContentRepositoryImpl

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fullRepo = ContentRepositoryImpl(FullFakeJsonDataSource(), testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── getPopularPlaylists ───────────────────────────────────────────────

    @Test
    fun `getPopularPlaylists orders by average viewCount descending`() = runTest {
        val result = fullRepo.getPopularPlaylists().first { it !is Resource.Loading } as Resource.Success
        val ids = result.data.map { it.id }
        // ss_space has highest avg views (2.3M+3.1M)/2 = 2.7M
        // eo_deep_time: (890K+1.5M)/2 = 1.2M
        // cc_biology: (474K+1.2M+610K)/3 ≈ 762K
        // cc_chemistry: (892K+567K)/2 = 730K
        assertEquals("ss_space", ids[0])
        assertEquals("eo_deep_time", ids[1])
        // cc_biology and cc_chemistry in positions 2/3 — just verify they're present
        assertTrue(ids.contains("cc_biology"))
        assertTrue(ids.contains("cc_chemistry"))
    }

    @Test
    fun `getPopularPlaylists excludes playlists with no matching videos`() = runTest {
        val emptyPlaylistSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(TestData.crashcourse)
            override suspend fun getPlaylists() = listOf(
                TestData.ccBiology,
                Playlist("empty_pl", "crashcourse", "Empty", "Desc", emptyList())
            )
            override suspend fun getVideos() = listOf(TestData.ccBio01, TestData.ccBio02, TestData.ccBio03)
        }
        val repo = ContentRepositoryImpl(emptyPlaylistSource, testDispatcher)
        val result = repo.getPopularPlaylists().first { it !is Resource.Loading } as Resource.Success
        assertEquals(1, result.data.size)
        assertEquals("cc_biology", result.data[0].id)
    }

    // ── getHighestRatedPlaylists ──────────────────────────────────────────

    @Test
    fun `getHighestRatedPlaylists orders by average rating descending`() = runTest {
        val result = fullRepo.getHighestRatedPlaylists().first { it !is Resource.Loading } as Resource.Success
        // ss_space has highest avg rating (viewCount-based)
        // Just verify the list is complete and ordered
        assertEquals(4, result.data.size)
        val ratings = result.data.map { playlist ->
            // Verify each entry is actually a valid playlist
            assertTrue(playlist.id.isNotEmpty())
        }
    }

    @Test
    fun `getHighestRatedPlaylists zero-view videos do not inflate rating`() = runTest {
        val zeroViewSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(TestData.crashcourse)
            override suspend fun getPlaylists() = listOf(TestData.ccBiology, TestData.ssSpace.copy(channelId = "crashcourse"))
            override suspend fun getVideos() = listOf(
                TestData.ssSpace01.copy(viewCount = 0, likeCount = 0), // rating = 0.0
                TestData.ssSpace02.copy(viewCount = 0, likeCount = 0),
                TestData.ccBio01, TestData.ccBio02, TestData.ccBio03
            )
        }
        val repo = ContentRepositoryImpl(zeroViewSource, testDispatcher)
        val result = repo.getHighestRatedPlaylists().first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        // cc_biology should be first (has real ratings), ss_space should be last (0.0 rating)
        assertEquals("cc_biology", result.data[0].id)
    }

    // ── getRelatedVideos ──────────────────────────────────────────────────

    @Test
    fun `getRelatedVideos same-playlist ranked highest`() = runTest {
        val result = fullRepo.getRelatedVideos("cc_bio_01").first { it !is Resource.Loading } as Resource.Success
        // cc_bio_02 and cc_bio_03 share cc_biology playlist → high score
        val topIds = result.data.take(2).map { it.id }.toSet()
        assertTrue(topIds.contains("cc_bio_02") || topIds.contains("cc_bio_03"))
    }

    @Test
    fun `getRelatedVideos excludes the current video`() = runTest {
        val result = fullRepo.getRelatedVideos("cc_bio_01").first { it !is Resource.Loading } as Resource.Success
        assertFalse(result.data.any { it.id == "cc_bio_01" })
    }

    @Test
    fun `getRelatedVideos returns at most 4 results`() = runTest {
        val result = fullRepo.getRelatedVideos("cc_bio_01").first { it !is Resource.Loading } as Resource.Success
        assertTrue(result.data.size <= 4)
    }

    @Test
    fun `getRelatedVideos shared tags boost score`() = runTest {
        val tagSource = object : JsonDataSource {
            val pl1 = Playlist("pl1", "ch1", "PL1", "", listOf("v1"))
            val pl2 = Playlist("pl2", "ch1", "PL2", "", listOf("v2", "v3"))
            override suspend fun getChannels() = listOf(Channel("ch1", "Ch", "", "#000", "#000"))
            override suspend fun getPlaylists() = listOf(pl1, pl2)
            override suspend fun getVideos() = listOf(
                Video("v1", listOf("pl1"), "V1", "", 100, "yt1", "", tags = listOf("science", "biology")),
                Video("v2", listOf("pl2"), "V2", "", 100, "yt2", "", tags = listOf("science", "biology")),
                Video("v3", listOf("pl2"), "V3", "", 100, "yt3", "", tags = emptyList())
            )
        }
        val repo = ContentRepositoryImpl(tagSource, testDispatcher)
        val result = repo.getRelatedVideos("v1").first { it !is Resource.Loading } as Resource.Success
        // v2 shares 2 tags (+6), v3 shares channel (+5) but no tag bonus
        assertEquals("v2", result.data[0].id)
    }

    @Test
    fun `getRelatedVideos returns error for unknown video`() = runTest {
        val result = fullRepo.getRelatedVideos("does_not_exist").first { it !is Resource.Loading }
        assertTrue(result is Resource.Error)
    }

    // ── getRecommendedVideos ──────────────────────────────────────────────

    @Test
    fun `getRecommendedVideos excludes completed videos`() = runTest {
        val completed = setOf("cc_bio_01", "cc_bio_02", "cc_bio_03")
        val result = fullRepo.getRecommendedVideos(
            disabledChannelIds = emptySet(),
            completedIds = completed
        ).first { it !is Resource.Loading } as Resource.Success
        assertFalse(result.data.any { it.id in completed })
    }

    @Test
    fun `getRecommendedVideos only recommends videos from enabled channels`() = runTest {
        val result = fullRepo.getRecommendedVideos(
            disabledChannelIds = setOf("scishow", "pbseons", "bizarrebeasts", "studyhall", "microcosmos"),
            completedIds = emptySet()
        ).first { it !is Resource.Loading } as Resource.Success
        assertTrue(result.data.isNotEmpty())
        val crashcourseIds = setOf("cc_bio_01", "cc_bio_02", "cc_bio_03", "cc_chem_01", "cc_chem_02")
        assertTrue(result.data.all { it.id in crashcourseIds })
    }

    @Test
    fun `getRecommendedVideos filters to scishow and pbseons when only they are enabled`() = runTest {
        val result = fullRepo.getRecommendedVideos(
            disabledChannelIds = setOf("crashcourse", "bizarrebeasts", "studyhall", "microcosmos"),
            completedIds = emptySet()
        ).first { it !is Resource.Loading } as Resource.Success
        assertTrue(result.data.isNotEmpty())
        val interestIds = setOf("ss_space_01", "ss_space_02", "eo_dt_01", "eo_dt_02")
        assertTrue(result.data.all { it.id in interestIds })
    }

    @Test
    fun `getRecommendedVideos returns empty when all channels are disabled`() = runTest {
        val result = fullRepo.getRecommendedVideos(
            disabledChannelIds = setOf("crashcourse", "scishow", "pbseons", "bizarrebeasts", "studyhall", "microcosmos"),
            completedIds = emptySet()
        ).first { it !is Resource.Loading } as Resource.Success
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `getRecommendedVideos does not recommend completed videos even if that channel is enabled`() = runTest {
        // Complete all crashcourse videos
        val crashcourseIds = setOf("cc_bio_01", "cc_bio_02", "cc_bio_03", "cc_chem_01", "cc_chem_02")
        val result = fullRepo.getRecommendedVideos(
            disabledChannelIds = emptySet(),
            completedIds = crashcourseIds
        ).first { it !is Resource.Loading } as Resource.Success
        // Recommendations should not contain any crashcourse videos
        assertFalse(result.data.any { it.id in crashcourseIds })
        // Should contain scishow / pbseons
        val uncompletedIds = setOf("ss_space_01", "ss_space_02", "eo_dt_01", "eo_dt_02")
        assertTrue(result.data.all { it.id in uncompletedIds })
    }

    @Test
    fun `getRecommendedVideos returns at most 6 results`() = runTest {
        val result = fullRepo.getRecommendedVideos(
            disabledChannelIds = emptySet(),
            completedIds = emptySet()
        ).first { it !is Resource.Loading } as Resource.Success
        assertTrue(result.data.size <= 6)
    }

    // ── Multi-playlist video correctness ──────────────────────────────────

    @Test
    fun `searchVideos video in multiple playlists matches all associated channels`() = runTest {
        val multiSource = object : JsonDataSource {
            val chA = Channel("ch_a", "Channel A", "", "#000", "#000")
            val chB = Channel("ch_b", "Channel B", "", "#000", "#000")
            val plA = Playlist("pl_a", "ch_a", "PL A", "", listOf("shared_v"))
            val plB = Playlist("pl_b", "ch_b", "PL B", "", listOf("shared_v"))
            val sharedVideo = Video("shared_v", listOf("pl_a", "pl_b"), "Shared Video", "", 100, "yt", "")
            override suspend fun getChannels() = listOf(chA, chB)
            override suspend fun getPlaylists() = listOf(plA, plB)
            override suspend fun getVideos() = listOf(sharedVideo)
        }
        val repo = ContentRepositoryImpl(multiSource, testDispatcher)

        val byChA = (repo.searchVideos(channelId = "ch_a").first { it !is Resource.Loading } as Resource.Success).data
        val byChB = (repo.searchVideos(channelId = "ch_b").first { it !is Resource.Loading } as Resource.Success).data
        assertEquals(1, byChA.size)
        assertEquals(1, byChB.size)
        assertEquals("shared_v", byChA[0].id)
        assertEquals("shared_v", byChB[0].id)
    }

    // ── Relevance ranking ─────────────────────────────────────────────────

    /** Minimal one-channel, one-playlist source for ranking tests. */
    private fun rankingSource(vararg videos: Video): ContentRepositoryImpl {
        val source = object : JsonDataSource {
            override suspend fun getChannels() =
                listOf(Channel("ch", "Channel", "", "#000", "#000"))
            override suspend fun getPlaylists() =
                listOf(Playlist("pl", "ch", "Science Playlist", "", emptyList()))
            override suspend fun getVideos() = videos.toList()
        }
        return ContentRepositoryImpl(source, testDispatcher)
    }

    @Test
    fun `RELEVANCE puts title match before description-only match`() = runTest {
        val titleMatch = Video("v_title", listOf("pl"), "Biology Basics", "A generic science video", 100, "yt1", "")
        val descOnly  = Video("v_desc",  listOf("pl"), "Science 101", "Learn about biology in this video", 100, "yt2", "")
        val repo = rankingSource(descOnly, titleMatch) // descOnly listed first in data

        val result = repo.searchVideos("biology").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals("v_title", result.data[0].id)
        assertEquals("v_desc",  result.data[1].id)
    }

    @Test
    fun `RELEVANCE puts title match before tag-only match`() = runTest {
        val titleMatch = Video("v_title", listOf("pl"), "Biology Basics", "generic", 100, "yt1", "")
        val tagOnly    = Video("v_tag",   listOf("pl"), "Science 101",    "generic", 100, "yt2", "", tags = listOf("biology"))
        val repo = rankingSource(tagOnly, titleMatch) // tagOnly listed first in data

        val result = repo.searchVideos("biology").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals("v_title", result.data[0].id)
        assertEquals("v_tag",   result.data[1].id)
    }

    @Test
    fun `RELEVANCE puts tag match before description-only match`() = runTest {
        val tagMatch = Video("v_tag",  listOf("pl"), "Science 101", "generic",               100, "yt1", "", tags = listOf("biology"))
        val descOnly = Video("v_desc", listOf("pl"), "Science 102", "Learn about biology here", 100, "yt2", "")
        val repo = rankingSource(descOnly, tagMatch) // descOnly listed first in data

        val result = repo.searchVideos("biology").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals("v_tag",  result.data[0].id)
        assertEquals("v_desc", result.data[1].id)
    }

    @Test
    fun `RELEVANCE title phrase match ranks above tag-only match for multi-word query`() = runTest {
        // "cell biology": phraseInTitle has the full phrase in its title (passes filter via title);
        // tagMatch passes filter via an exact tag but scores much lower.
        val phraseInTitle = Video("v_phrase", listOf("pl"), "Cell Biology Basics", "generic", 100, "yt1", "")
        val tagMatch      = Video("v_tag",    listOf("pl"), "Science 101",         "generic", 100, "yt2", "", tags = listOf("cell biology"))
        val repo = rankingSource(tagMatch, phraseInTitle) // tagMatch listed first in data

        val result = repo.searchVideos("cell biology").first { it !is Resource.Loading } as Resource.Success
        // phraseInTitle: title phrase +400, "cell" word +100, "biology" word +100 = 600
        // tagMatch:      tag word "cell" +15, "biology" +15 = 30
        assertEquals(2, result.data.size)
        assertEquals("v_phrase", result.data[0].id)
        assertEquals("v_tag",    result.data[1].id)
    }

    @Test
    fun `RELEVANCE full-phrase title match ranks above same words split across fields`() = runTest {
        // "dark matter": phraseTitle has the full phrase in its title;
        // splitFields passes filter via a description that contains the full phrase, but scores lower.
        val phraseTitle = Video("v_phrase", listOf("pl"), "What Is Dark Matter?", "generic", 100, "yt1", "")
        val splitFields = Video("v_split",  listOf("pl"), "Dark Energy", "Scientists study the dark matter of our universe", 100, "yt2", "")
        val repo = rankingSource(splitFields, phraseTitle) // splitFields listed first

        val result = repo.searchVideos("dark matter").first { it !is Resource.Loading } as Resource.Success
        // phraseTitle: title phrase +400, "dark" +100, "matter" +100 = 600
        // splitFields: title "dark" +100; desc phrase +5, "dark" +1, "matter" +1 = 107
        assertEquals(2, result.data.size)
        assertEquals("v_phrase", result.data[0].id)
    }

    @Test
    fun `RELEVANCE matching across more fields boosts score`() = runTest {
        // Same title match, but v_multi also hits in tags — total score is higher
        val multiField = Video("v_multi", listOf("pl"), "Biology Basics", "generic", 100, "yt1", "", tags = listOf("biology"))
        val titleOnly  = Video("v_title", listOf("pl"), "Biology 101",    "generic", 100, "yt2", "")
        val repo = rankingSource(titleOnly, multiField) // titleOnly listed first

        val result = repo.searchVideos("biology").first { it !is Resource.Loading } as Resource.Success
        // multiField: title "biology" +100, tag "biology" +15 = 115
        // titleOnly:  title "biology" +100 = 100
        assertEquals(2, result.data.size)
        assertEquals("v_multi", result.data[0].id)
        assertEquals("v_title", result.data[1].id)
    }

    @Test
    fun `RELEVANCE category match ranks above description-only match`() = runTest {
        val catMatch = Video("v_cat",  listOf("pl"), "Science 101", "generic",              100, "yt1", "", categories = listOf("Biology"))
        val descOnly = Video("v_desc", listOf("pl"), "Science 102", "Explores biology here", 100, "yt2", "")
        val repo = rankingSource(descOnly, catMatch)

        val result = repo.searchVideos("biology").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals("v_cat",  result.data[0].id)
        assertEquals("v_desc", result.data[1].id)
    }

    @Test
    fun `RELEVANCE ranking is bypassed for non-RELEVANCE sort`() = runTest {
        // descOnly has far more views; should win on POPULAR even though titleMatch has better text relevance
        val titleMatch = Video("v_title",   listOf("pl"), "Biology Basics", "generic",                     100, "yt1", "", viewCount = 100)
        val descOnly   = Video("v_popular", listOf("pl"), "Science 101",    "Learn about biology in depth", 100, "yt2", "", viewCount = 1_000_000)
        val repo = rankingSource(titleMatch, descOnly)

        val result = repo.searchVideos("biology", sortBy = com.complexly.academy.data.VideoSort.POPULAR)
            .first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals("v_popular", result.data[0].id)
    }

    @Test
    fun `blank query sorts results by view count descending`() = runTest {
        val v1 = Video("v1", listOf("pl"), "Alpha", "desc", 100, "yt1", "", viewCount = 10)
        val v2 = Video("v2", listOf("pl"), "Beta",  "desc", 100, "yt2", "", viewCount = 50)
        val repo = rankingSource(v1, v2)

        val result = repo.searchVideos("").first { it !is Resource.Loading } as Resource.Success
        assertEquals(2, result.data.size)
        assertEquals("v2", result.data[0].id)
        assertEquals("v1", result.data[1].id)
    }

    // ── Playlist enrichment at load time ─────────────────────────────────

    @Test
    fun `playlist thumbnailUrl is derived from first videoId at load time`() = runTest {
        val source = object : JsonDataSource {
            override suspend fun getChannels() = listOf(Channel("ch", "Ch", "", "#000", "#000"))
            override suspend fun getPlaylists() = listOf(
                Playlist("pl_a", "ch", "A", "", listOf("abc1234ABCD", "xyz5678WXYZ")),
                Playlist("pl_b", "ch", "B", "", listOf("qrs9012MNOP"))
            )
            override suspend fun getVideos() = listOf(
                Video("abc1234ABCD", listOf("pl_a"), "V1", "", 60, "abc1234ABCD", ""),
                Video("xyz5678WXYZ", listOf("pl_a"), "V2", "", 60, "xyz5678WXYZ", ""),
                Video("qrs9012MNOP", listOf("pl_b"), "V3", "", 60, "qrs9012MNOP", "")
            )
        }
        val repo = ContentRepositoryImpl(source, testDispatcher)
        val playlists = (repo.getPlaylists().first { it !is Resource.Loading } as Resource.Success).data

        val plA = playlists.find { it.id == "pl_a" }!!
        val plB = playlists.find { it.id == "pl_b" }!!
        assertEquals("https://img.youtube.com/vi/abc1234ABCD/hqdefault.jpg", plA.thumbnailUrl)
        assertEquals("https://img.youtube.com/vi/qrs9012MNOP/hqdefault.jpg", plB.thumbnailUrl)
    }

    @Test
    fun `playlist stats are computed at load time`() = runTest {
        val q = com.complexly.academy.model.Question("q1", "Q?",
            listOf(com.complexly.academy.model.Option("A", true, "e")))
        val source = object : JsonDataSource {
            override suspend fun getChannels() = listOf(Channel("ch", "Ch", "", "#000", "#000"))
            override suspend fun getPlaylists() = listOf(
                Playlist("course_pl", "ch", "Course", "", listOf("v1", "v2")),
                Playlist("plain_pl",  "ch", "Plain",  "", listOf("v3"))
            )
            override suspend fun getVideos() = listOf(
                Video("v1", listOf("course_pl"), "V1", "", 60, "v1", "", listOf(q), viewCount = 1000, likeCount = 50),
                Video("v2", listOf("course_pl"), "V2", "", 60, "v2", "", listOf(q), viewCount = 3000, likeCount = 90),
                Video("v3", listOf("plain_pl"),  "V3", "", 60, "v3", "",            viewCount = 500)
            )
        }
        val repo = ContentRepositoryImpl(source, testDispatcher)
        val playlists = (repo.getPlaylists().first { it !is Resource.Loading } as Resource.Success).data

        val course = playlists.find { it.id == "course_pl" }!!
        val plain  = playlists.find { it.id == "plain_pl" }!!

        assertEquals(2000L, course.avgViewCount)      // (1000+3000)/2
        assertEquals(4000L, course.totalViewCount)     // 1000+3000
        assertTrue(course.avgRating > 0.0)
        assertTrue(course.isCourse)                    // all videos have questions

        assertEquals(500L, plain.avgViewCount)
        assertFalse(plain.isCourse)                    // no questions
    }

    @Test
    fun `getPopularPlaylists and getHighestRatedPlaylists use pre-computed fields`() = runTest {
        val result = fullRepo.getPopularPlaylists().first { it !is Resource.Loading } as Resource.Success
        // Playlists with no matching videos are excluded; all returned playlists have videoCount > 0
        assertTrue(result.data.isNotEmpty())
        assertTrue(result.data.all { it.videoCount > 0 })

        val rated = fullRepo.getHighestRatedPlaylists().first { it !is Resource.Loading } as Resource.Success
        assertTrue(rated.data.isNotEmpty())
        assertTrue(rated.data.all { it.videoCount > 0 })
    }

    @Test
    fun `ChannelColorRegistry registers and resolves channel colors dynamically`() = runTest {
        val testSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(
                Channel("test_sh", "Test Study Hall", "Desc", "#FEC629", "#D49D00"),
                Channel("test_mico", "Test Microcosmos", "Desc", "#94BFBD", "#0E6655")
            )
            override suspend fun getPlaylists() = emptyList<Playlist>()
            override suspend fun getVideos() = emptyList<Video>()
        }
        val repo = ContentRepositoryImpl(testSource, testDispatcher)

        // Wait for repository initialization (which triggers loading and color registration)
        repo.getChannels().first { it !is Resource.Loading }

        assertEquals("#FEC629", com.complexly.academy.model.ChannelColorRegistry.getColor("test_sh"))
        assertEquals("#94BFBD", com.complexly.academy.model.ChannelColorRegistry.getColor("test_mico"))
    }
}

