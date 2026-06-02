package com.complexly.academy

import com.complexly.academy.data.ContentRepositoryImpl
import com.complexly.academy.data.JsonDataSource
import com.complexly.academy.data.UserRepositoryImpl
import com.complexly.academy.data.createTestDataStore
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import com.complexly.academy.ui.home.formatRating
import com.complexly.academy.ui.course.CourseDetailsViewModel
import com.complexly.academy.ui.discover.DiscoverViewModel
import com.complexly.academy.ui.home.HomeViewModel
import com.complexly.academy.domain.GetRecentlyViewedUseCase
import com.complexly.academy.ui.quiz.QuizViewModel
import com.complexly.academy.ui.video.VideoPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var contentRepository: ContentRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val dataStore = createTestDataStore("viewmodel", testScope)
        contentRepository = ContentRepositoryImpl(LegacyJsonDataSource, testDispatcher)
        userRepository = UserRepositoryImpl(LegacyJsonDataSource, dataStore, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testHomeViewModelState() = runTest {
        val viewModel = HomeViewModel(contentRepository, userRepository, GetRecentlyViewedUseCase(contentRepository, userRepository))
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.channels.size)
        assertEquals("crashcourse", state.channels[0].id)
        assertEquals(1, state.playlists.size)
        assertEquals("What is the powerhouse of the cell?", state.dailyQuestion?.text)
        assertEquals(2, state.recommendedVideos.size)

        viewModel.onStreakClicked()
        userRepository.learningProgress.first { it.streakCount == 7 }
        assertEquals(7, userRepository.learningProgress.value.streakCount)
    }

    @Test
    fun testDiscoverViewModelState() = runTest {
        val viewModel = DiscoverViewModel(contentRepository, userRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertEquals(2, state.searchResults.size)

        viewModel.onQueryChanged("Cells")
        advanceTimeBy(400)
        runCurrent()

        state = viewModel.uiState.value
        assertEquals(1, state.searchResults.size)
        assertEquals("bio_2", state.searchResults[0].video.id)
    }

    @Test
    fun testCourseDetailsViewModelState() = runTest {
        val viewModel = CourseDetailsViewModel("biology", contentRepository, userRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.playlist)
        assertEquals("Biology", state.playlist.title)
        assertEquals(2, state.videos.size)
        assertFalse(state.isEnrolled)

        viewModel.enroll()
        userRepository.learningProgress.first { it.enrolledCourseIds.contains("biology") }
        runCurrent()
        state = viewModel.uiState.value
        assertTrue(state.isEnrolled)

        viewModel.unenroll()
        userRepository.learningProgress.first { !it.enrolledCourseIds.contains("biology") }
        runCurrent()
        state = viewModel.uiState.value
        assertFalse(state.isEnrolled)
    }

    @Test
    fun testVideoPlayerViewModelState() = runTest {
        val viewModel = VideoPlayerViewModel("bio_1", null, contentRepository, userRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("bio_1", state.video?.id)
        assertFalse(state.isCompleted)

        userRepository.markVideoCompleted("bio_1")
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertTrue(state.isCompleted)
    }

    @Test
    fun testQuizViewModelStateAndFlow() = runTest {
        val viewModel = QuizViewModel("bio_1", contentRepository, userRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.questions.size)
        assertEquals(0, state.currentQuestionIndex)
        assertNull(state.selectedOption)
        assertFalse(state.isSubmitted)

        val incorrectOption = state.questions[0].options.first { !it.isCorrect }
        viewModel.selectOption(incorrectOption)
        assertEquals(incorrectOption, viewModel.uiState.value.selectedOption)

        viewModel.submitAnswer()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSubmitted)
        assertEquals(0, viewModel.uiState.value.correctAnswersCount)

        viewModel.nextQuestion()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isQuizFinished)
    }

    @Test
    fun testProgressViewModel() = runTest {
        val viewModel = com.complexly.academy.ui.progress.ProgressViewModel(userRepository, contentRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertEquals(6, state.progress.streakCount)
        assertTrue(state.recentVideos.isEmpty())

        userRepository.updateStreak(true)
        userRepository.learningProgress.first { it.streakCount == 7 }
        runCurrent()
        state = viewModel.uiState.value
        assertEquals(7, state.progress.streakCount)
    }

    @Test
    fun testYouViewModel() = runTest {
        val viewModel = com.complexly.academy.ui.you.YouViewModel(contentRepository, userRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertFalse(state.darkModeEnabled)
        assertTrue(state.pushNotificationsEnabled)
        val crashCoursePair = state.channels.find { it.first.id == "crashcourse" }
        assertNotNull(crashCoursePair)
        assertTrue(crashCoursePair.second)

        viewModel.toggleDarkMode()
        userRepository.userPreferences.first { it.darkModeEnabled }
        runCurrent()
        state = viewModel.uiState.value
        assertTrue(state.darkModeEnabled)

        viewModel.togglePushNotifications()
        userRepository.userPreferences.first { !it.pushNotificationsEnabled }
        runCurrent()
        state = viewModel.uiState.value
        assertFalse(state.pushNotificationsEnabled)

        viewModel.toggleChannel("crashcourse")
        userRepository.userPreferences.first { it.disabledChannelIds.contains("crashcourse") }
        runCurrent()
        state = viewModel.uiState.value
        val crashCoursePairUpdated = state.channels.find { it.first.id == "crashcourse" }
        assertNotNull(crashCoursePairUpdated)
        assertFalse(crashCoursePairUpdated.second)
    }

    @Test
    fun testHomeViewModelEngagementSorting() = runTest {
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val testDataStore = createTestDataStore("engagement_test", testScope)

        val testVideos = listOf(
            Video("v1", listOf("p1"), "Video 1", "Desc", 100, "yt1", "thumb1", viewCount = 1000, likeCount = 45),
            Video("v2", listOf("p1"), "Video 2", "Desc", 100, "yt2", "thumb2", viewCount = 2000, likeCount = 94),
            Video("v3", listOf("p2"), "Video 3", "Desc", 100, "yt3", "thumb3", viewCount = 5000, likeCount = 175),
            Video("v4", listOf("p2"), "Video 4", "Desc", 100, "yt4", "thumb4", viewCount = 7000, likeCount = 273),
            Video("v5", listOf("p3"), "Video 5", "Desc", 100, "yt5", "thumb5", viewCount = 500, likeCount = 24),
        )
        val testPlaylists = listOf(
            Playlist("p1", "crashcourse", "Playlist 1", "Desc", listOf("v1", "v2")),
            Playlist("p2", "crashcourse", "Playlist 2", "Desc", listOf("v3", "v4")),
            Playlist("p3", "crashcourse", "Playlist 3", "Desc", listOf("v5"))
        )
        val testChannels = listOf(Channel("crashcourse", "CrashCourse", "Desc", "#123456", "#654321"))

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels(): List<Channel> = testChannels
            override suspend fun getPlaylists(): List<Playlist> = testPlaylists
            override suspend fun getVideos(): List<Video> = testVideos
        }
        val customContent = ContentRepositoryImpl(customDataSource, testDispatcher)
        val customUser = UserRepositoryImpl(customDataSource, testDataStore, testDispatcher)
        val viewModel = HomeViewModel(customContent, customUser, GetRecentlyViewedUseCase(customContent, customUser))

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)

        assertEquals(3, state.popularPlaylists.size)
        assertEquals("p2", state.popularPlaylists[0].id)
        assertEquals("p1", state.popularPlaylists[1].id)
        assertEquals("p3", state.popularPlaylists[2].id)

        assertEquals(3, state.highestRatedPlaylists.size)
        assertEquals("p3", state.highestRatedPlaylists[0].id)
        assertEquals("p1", state.highestRatedPlaylists[1].id)
        assertEquals("p2", state.highestRatedPlaylists[2].id)
    }

    @Test
    fun testFormatRating() {
        assertEquals("4.50", formatRating(4.5))
        assertEquals("4.12", formatRating(4.123))
        assertEquals("5.00", formatRating(5.0))
        assertEquals("3.99", formatRating(3.99))
        assertEquals("0.00", formatRating(0.0))
        assertEquals("4.89", formatRating(4.899))
    }

    @Test
    fun testFormatDate() {
        assertEquals("9 May 2019", com.complexly.academy.ui.video.formatDate("2019-05-09"))
        assertEquals("18 Sep 2012", com.complexly.academy.ui.video.formatDate("2012-09-18"))
        assertEquals("", com.complexly.academy.ui.video.formatDate(""))
        assertEquals("invalid-date", com.complexly.academy.ui.video.formatDate("invalid-date"))
    }

    @Test
    fun testVideoPlayerViewModelRelatedContentAndChannel() = runTest {
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val testDataStore = createTestDataStore("videoplayer_vm_test", testScope)

        val channelA = Channel("channel_a", "Channel A Title", "Desc", "#123456", "#654321")
        val channelB = Channel("channel_b", "Channel B Title", "Desc", "#123456", "#654321")
        val playlist1 = Playlist("pl_1", "channel_a", "Playlist 1", "Desc", listOf("v_current", "v_same_pl", "v_same_ch"))
        val playlist2 = Playlist("pl_2", "channel_a", "Playlist 2", "Desc", listOf("v_diff_pl_same_ch"))
        val playlist3 = Playlist("pl_3", "channel_b", "Playlist 3", "Desc", listOf("v_diff_ch_same_tags"))

        val currentVideo = Video("v_current", listOf("pl_1"), "Current Video", "Desc", 100, "yt_current", "thumb_current", tags = listOf("tag1", "tag2"), categories = listOf("cat1"))
        val samePlVideo = Video("v_same_pl", listOf("pl_1"), "Same Playlist Video", "Desc", 100, "yt_same_pl", "thumb_same_pl")
        val sameChVideo = Video("v_same_ch", listOf("pl_1"), "Same Channel Video", "Desc", 100, "yt_same_ch", "thumb_same_ch")
        val diffPlSameChVideo = Video("v_diff_pl_same_ch", listOf("pl_2"), "Diff Playlist Same Channel Video", "Desc", 100, "yt_diff_pl_same_ch", "thumb_diff_pl_same_ch")
        val diffChSameTagsVideo = Video("v_diff_ch_same_tags", listOf("pl_3"), "Diff Channel Same Tags Video", "Desc", 100, "yt_diff_ch_same_tags", "thumb_diff_ch_same_tags", tags = listOf("tag1", "tag2"))
        val otherVideo = Video("v_other", listOf("pl_3"), "Other Video", "Desc", 100, "yt_other", "thumb_other")

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels(): List<Channel> = listOf(channelA, channelB)
            override suspend fun getPlaylists(): List<Playlist> = listOf(playlist1, playlist2, playlist3)
            override suspend fun getVideos(): List<Video> = listOf(currentVideo, samePlVideo, sameChVideo, diffPlSameChVideo, diffChSameTagsVideo, otherVideo)
        }
        val customContent = ContentRepositoryImpl(customDataSource, testDispatcher)
        val customUser = UserRepositoryImpl(customDataSource, testDataStore, testDispatcher)
        val viewModel = VideoPlayerViewModel("v_current", null, customContent, customUser)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("v_current", state.video?.id)
        assertEquals("channel_a", state.channelId)
        assertEquals("Channel A Title", state.channelName)
        assertEquals(1, state.playlists.size)
        assertEquals("pl_1", state.playlists[0].id)

        assertEquals(4, state.relatedVideos.size)
        val relatedIds = state.relatedVideos.map { it.id }
        assertTrue(relatedIds[0] == "v_same_pl" || relatedIds[0] == "v_same_ch")
        assertTrue(relatedIds[1] == "v_same_pl" || relatedIds[1] == "v_same_ch")
        assertEquals("v_diff_ch_same_tags", relatedIds[2])
        assertEquals("v_diff_pl_same_ch", relatedIds[3])
    }

    @Test
    fun testVideoPlayerViewModelPlaylistThumbnails() = runTest {
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val testDataStore = createTestDataStore("playlist_thumbnails_test", testScope)

        // playlist1 has "yt_FIRST_VID" as its first videoId — this is the YouTube ID used to
        // construct the CDN thumbnail URL. playlist2's first videoId is "yt_OTHER_VID".
        val playlist1 = Playlist("pl_1", "ch_a", "Playlist 1", "Desc", listOf("yt_FIRST_VID", "yt_SECOND_VID"))
        val playlist2 = Playlist("pl_2", "ch_a", "Playlist 2", "Desc", listOf("yt_OTHER_VID"))

        val currentVideo = Video("v_current", listOf("pl_1", "pl_2"), "Current", "Desc", 100,
            "yt_current", "https://img.youtube.com/vi/yt_current/hqdefault.jpg")
        val firstVideo = Video("yt_FIRST_VID", listOf("pl_1"), "First", "Desc", 100,
            "yt_FIRST_VID", "https://img.youtube.com/vi/yt_FIRST_VID/hqdefault.jpg")

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(Channel("ch_a", "Ch A", "", "#000", "#000"))
            override suspend fun getPlaylists() = listOf(playlist1, playlist2)
            override suspend fun getVideos() = listOf(currentVideo, firstVideo)
        }
        val customContent = ContentRepositoryImpl(customDataSource, testDispatcher)
        val customUser = UserRepositoryImpl(customDataSource, testDataStore, testDispatcher)
        val viewModel = VideoPlayerViewModel("v_current", null, customContent, customUser)

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.playlists.size)

        // thumbnailUrl is enriched at load time from the playlist's first videoId.
        // playlist1's first videoId is "yt_FIRST_VID", playlist2's is "yt_OTHER_VID".
        val pl1 = state.playlists.find { it.id == "pl_1" }
        val pl2 = state.playlists.find { it.id == "pl_2" }
        assertEquals("https://img.youtube.com/vi/yt_FIRST_VID/hqdefault.jpg", pl1?.thumbnailUrl)
        assertEquals("https://img.youtube.com/vi/yt_OTHER_VID/hqdefault.jpg", pl2?.thumbnailUrl)
    }

    @Test
    fun testHomeVideosAndRecommendationsSorting() = runTest {
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val testDataStore = createTestDataStore("sorting_test", testScope)
        
        val v1 = Video("v1", listOf("p1"), "Video 1", "Desc", 100, "yt1", "thumb1", releaseDate = "2020-01-01")
        val v2 = Video("v2", listOf("p1"), "Video 2", "Desc", 100, "yt2", "thumb2", releaseDate = "2025-01-01")
        val v3 = Video("v3", listOf("p1"), "Video 3", "Desc", 100, "yt3", "thumb3", releaseDate = "2015-01-01")
        
        val playlist1 = Playlist("p1", "crashcourse", "Playlist 1", "Desc", listOf("v1", "v2", "v3"))
        val channelA = Channel("crashcourse", "CrashCourse", "Desc", "#123456", "#654321")
        
        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels(): List<Channel> = listOf(channelA)
            override suspend fun getPlaylists(): List<Playlist> = listOf(playlist1)
            override suspend fun getVideos(): List<Video> = listOf(v1, v2, v3)
        }
        
        val customContentRepository = ContentRepositoryImpl(customDataSource, testDispatcher)
        val customUserRepository = UserRepositoryImpl(customDataSource, testDataStore, testDispatcher)
        
        val viewModel = HomeViewModel(customContentRepository, customUserRepository, GetRecentlyViewedUseCase(customContentRepository, customUserRepository))
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        
        // 1. Verify "New this week" (state.videos) are sorted newest first: v2 (2025) -> v1 (2020) -> v3 (2015)
        assertEquals(3, state.videos.size)
        assertEquals("v2", state.videos[0].id)
        assertEquals("v1", state.videos[1].id)
        assertEquals("v3", state.videos[2].id)
        
        // 2. Verify Recommended videos are also sorted newest first
        assertEquals(3, state.recommendedVideos.size)
        assertEquals("v2", state.recommendedVideos[0].id)
        assertEquals("v1", state.recommendedVideos[1].id)
        assertEquals("v3", state.recommendedVideos[2].id)
    }

    @Test
    fun testChannelFilteringAcrossViewModels() = runTest {
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val testDataStore = createTestDataStore("filtering_integration_test", testScope)
        
        val channelA = Channel("crashcourse", "CrashCourse", "Desc", "#123", "#456")
        val channelB = Channel("scishow", "SciShow", "Desc", "#123", "#456")
        
        val playlistA = Playlist("pl_a", "crashcourse", "CC Playlist", "Desc", listOf("v_a"))
        val playlistB = Playlist("pl_b", "scishow", "SS Playlist", "Desc", listOf("v_b"))
        
        val videoA = Video("v_a", listOf("pl_a"), "CC Video", "Desc", 100, "yt_a", "thumb", releaseDate = "2024-01-01")
        val videoB = Video("v_b", listOf("pl_b"), "SS Video", "Desc", 100, "yt_b", "thumb", releaseDate = "2024-01-02")
        
        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(channelA, channelB)
            override suspend fun getPlaylists() = listOf(playlistA, playlistB)
            override suspend fun getVideos() = listOf(videoA, videoB)
        }
        
        val customContent = ContentRepositoryImpl(customDataSource, testDispatcher)
        val customUser = UserRepositoryImpl(customDataSource, testDataStore, testDispatcher)
        
        // 1. Initially, both channels should be enabled by default
        val homeViewModel = HomeViewModel(customContent, customUser, GetRecentlyViewedUseCase(customContent, customUser))
        val discoverViewModel = DiscoverViewModel(customContent, customUser)
        
        backgroundScope.launch { homeViewModel.uiState.collect {} }
        backgroundScope.launch { discoverViewModel.uiState.collect {} }
        advanceUntilIdle()
        
        // Verify default state (all enabled)
        assertEquals(2, homeViewModel.uiState.value.channels.size)
        assertEquals(2, homeViewModel.uiState.value.playlists.size)
        assertEquals(2, homeViewModel.uiState.value.videos.size)
        
        assertEquals(2, discoverViewModel.uiState.value.channels.size)
        assertEquals(2, discoverViewModel.uiState.value.playlists.size)
        assertEquals(2, discoverViewModel.uiState.value.searchResults.size)
        
        // 2. Disable SciShow, keep only CrashCourse
        customUser.setDisabledChannels(setOf("scishow"))
        advanceUntilIdle()
        
        // Home screen should only show CrashCourse content
        assertEquals(1, homeViewModel.uiState.value.channels.size)
        assertEquals("crashcourse", homeViewModel.uiState.value.channels[0].id)
        assertEquals(1, homeViewModel.uiState.value.playlists.size)
        assertEquals("pl_a", homeViewModel.uiState.value.playlists[0].id)
        assertEquals(1, homeViewModel.uiState.value.videos.size)
        assertEquals("v_a", homeViewModel.uiState.value.videos[0].id)
        
        // Discover/Search screen should only show CrashCourse content
        assertEquals(1, discoverViewModel.uiState.value.channels.size)
        assertEquals("crashcourse", discoverViewModel.uiState.value.channels[0].id)
        assertEquals(1, discoverViewModel.uiState.value.playlists.size)
        assertEquals("pl_a", discoverViewModel.uiState.value.playlists[0].id)
        assertEquals(1, discoverViewModel.uiState.value.searchResults.size)
        assertEquals("v_a", discoverViewModel.uiState.value.searchResults[0].video.id)
        
        // 3. Enable both back
        customUser.setDisabledChannels(emptySet())
        advanceUntilIdle()
        
        assertEquals(2, homeViewModel.uiState.value.channels.size)
        assertEquals(2, discoverViewModel.uiState.value.channels.size)
    }

    @Test
    fun testGetRecentlyViewedUseCase() = runTest {
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val testDataStore = createTestDataStore("recently_viewed_usecase_test", testScope)

        val channel = Channel("crashcourse", "CrashCourse", "Desc", "#123", "#456")
        val playlist = Playlist("pl_1", "crashcourse", "CC Playlist", "Desc", listOf("v_1"))
        val video = Video("v_1", listOf("pl_1"), "CC Video", "Desc", 100, "yt_1", "thumb", releaseDate = "2024-01-01")

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(channel)
            override suspend fun getPlaylists() = listOf(playlist)
            override suspend fun getVideos() = listOf(video)
        }

        val customContent = ContentRepositoryImpl(customDataSource, testDispatcher)
        val customUser = UserRepositoryImpl(customDataSource, testDataStore, testDispatcher)
        val useCase = GetRecentlyViewedUseCase(customContent, customUser)

        customUser.markVideoWatched("v_1")
        customUser.markPlaylistViewed("pl_1")
        advanceUntilIdle()

                val recentlyViewed = useCase().first()
        assertEquals(2, recentlyViewed.size)
        
        // Assert both the playlist and the video are returned since duplication prevention is removed
        val firstItem = recentlyViewed[0]
        assertTrue(firstItem is com.complexly.academy.domain.RecentViewItem.PlaylistItem)
        assertEquals("pl_1", (firstItem as com.complexly.academy.domain.RecentViewItem.PlaylistItem).playlist.id)

        val secondItem = recentlyViewed[1]
        assertTrue(secondItem is com.complexly.academy.domain.RecentViewItem.VideoItem)
        assertEquals("v_1", (secondItem as com.complexly.academy.domain.RecentViewItem.VideoItem).video.id)
    }

    @Test
    fun testContextBasedPlaylistViewing() = runTest {
        val testScope = CoroutineScope(testDispatcher + SupervisorJob())
        val testDataStore = createTestDataStore("context_viewing_test", testScope)

        val channel = Channel("crashcourse", "CrashCourse", "Desc", "#123", "#456")
        val playlist = Playlist("pl_1", "crashcourse", "CC Playlist", "Desc", listOf("v_1"))
        val video = Video("v_1", listOf("pl_1"), "CC Video", "Desc", 100, "yt_1", "thumb", releaseDate = "2024-01-01", questions = listOf(com.complexly.academy.model.Question("q1", "Q?", listOf(com.complexly.academy.model.Option("o1", true, "explanation")))))

        val customDataSource = object : JsonDataSource {
            override suspend fun getChannels() = listOf(channel)
            override suspend fun getPlaylists() = listOf(playlist)
            override suspend fun getVideos() = listOf(video)
        }

        val customContent = ContentRepositoryImpl(customDataSource, testDispatcher)
        val customUser = UserRepositoryImpl(customDataSource, testDataStore, testDispatcher)

        // 1. Opening CourseDetailsViewModel should NOT mark playlist viewed
        val courseViewModel = CourseDetailsViewModel("pl_1", customContent, customUser)
        backgroundScope.launch { courseViewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(customUser.learningProgress.value.recentlyViewedItemIds.none { it.contains("pl_1") })

        // 2. Opening VideoPlayerViewModel with playlistId should NOT mark playlist viewed initially
        val videoViewModel = VideoPlayerViewModel("v_1", "pl_1", customContent, customUser)
        backgroundScope.launch { videoViewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(customUser.learningProgress.value.recentlyViewedItemIds.none { it.contains("pl_1") })

        // 3. Calling playVideo() SHOULD mark playlist and video viewed
        videoViewModel.playVideo()
        advanceUntilIdle()
        assertTrue(customUser.learningProgress.value.recentlyViewedItemIds.contains("playlist:pl_1"))
        assertTrue(customUser.learningProgress.value.recentlyViewedItemIds.contains("video:v_1"))

        // Reset viewed
        val cleanDataStore = createTestDataStore("context_viewing_test_2", testScope)
        val cleanUser = UserRepositoryImpl(customDataSource, cleanDataStore, testDispatcher)

        // 3. Calling onTakeQuiz() SHOULD mark playlist viewed
        val videoViewModel2 = VideoPlayerViewModel("v_1", "pl_1", customContent, cleanUser)
        backgroundScope.launch { videoViewModel2.uiState.collect {} }
        advanceUntilIdle()
        videoViewModel2.onTakeQuiz()
        advanceUntilIdle()
        assertTrue(cleanUser.learningProgress.value.recentlyViewedItemIds.contains("playlist:pl_1"))
    }
}

