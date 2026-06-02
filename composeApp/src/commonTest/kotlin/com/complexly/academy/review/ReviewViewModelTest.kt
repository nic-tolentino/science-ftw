package com.complexly.academy.review

import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.model.Option
import com.complexly.academy.model.Question
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import com.complexly.academy.spacedrepetition.QuestionReviewState
import com.complexly.academy.spacedrepetition.ReviewStats
import com.complexly.academy.ui.review.ReviewUiState
import com.complexly.academy.ui.review.ReviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fake UserRepository that records answers and returns configurable due reviews.
 */
class FakeUserRepository : UserRepository {
    var dueReviews: List<QuestionReviewState> = emptyList()
    var statsToReturn: ReviewStats = ReviewStats(0, 0, 0, 0, 0.0, 0, 0)
    val recordedAnswers = mutableListOf<Triple<String, String, Double>>()
    private var _dayOffset: Int = 0
    private val _dayOffsetFlow = MutableStateFlow(0)

    override val dayOffset = _dayOffsetFlow

    override suspend fun recordAnswer(questionId: String, videoId: String, quality: Double) {
        recordedAnswers.add(Triple(questionId, videoId, quality))
    }

    override fun getDueReviews(maxCount: Int): List<QuestionReviewState> {
        return dueReviews.take(maxCount)
    }

    override fun getReviewStats(): ReviewStats = statsToReturn

    override fun advanceDay() { _dayOffset++; _dayOffsetFlow.value = _dayOffset }
    override fun resetDay() { _dayOffset = 0; _dayOffsetFlow.value = 0 }

    // Stub implementations for other UserRepository methods (not needed for these tests)
    override val learningProgress = MutableStateFlow(com.complexly.academy.model.LearningProgress())
    override val userPreferences = MutableStateFlow(com.complexly.academy.model.UserPreferences())
    override suspend fun markVideoCompleted(videoId: String) {}
    override suspend fun enrollInCourse(courseId: String) {}
    override suspend fun unenrollFromCourse(courseId: String) {}
    override suspend fun markVideoWatched(videoId: String) {}
    override suspend fun markPlaylistViewed(playlistId: String) {}
    override suspend fun incrementCorrectAnswers() {}
    override suspend fun updateStreak(increment: Boolean) {}
    override suspend fun setDarkMode(enabled: Boolean) {}
    override suspend fun setPushNotifications(enabled: Boolean) {}
    override suspend fun setDisabledChannels(disabledChannelIds: Set<String>) {}
}

/**
 * Fake ContentRepository that returns a single video when asked.
 */
class FakeContentRepository : ContentRepository {
    var videoToReturn: Video? = null
    var videoToReturnError: String? = null

    override fun getVideo(videoId: String): kotlinx.coroutines.flow.Flow<Resource<Video>> {
        return if (videoToReturnError != null) {
            flowOf(Resource.Error(videoToReturnError!!))
        } else if (videoToReturn != null) {
            flowOf(Resource.Success(videoToReturn!!))
        } else {
            flowOf(Resource.Error("Video not found"))
        }
    }

    // Stub implementations for other ContentRepository methods
    override fun getChannels(): kotlinx.coroutines.flow.Flow<Resource<List<com.complexly.academy.model.Channel>>> =
        flowOf(Resource.Success(emptyList()))
    override fun getPlaylists(channelId: String?): kotlinx.coroutines.flow.Flow<Resource<List<com.complexly.academy.model.Playlist>>> =
        flowOf(Resource.Success(emptyList()))
    override fun getPlaylist(playlistId: String): kotlinx.coroutines.flow.Flow<Resource<com.complexly.academy.model.Playlist>> =
        flowOf(Resource.Error("Not found"))
    override fun getVideosForPlaylist(playlistId: String): kotlinx.coroutines.flow.Flow<Resource<List<Video>>> =
        flowOf(Resource.Success(emptyList()))
    override fun searchVideos(query: String, channelId: String?, playlistId: String?, sortBy: com.complexly.academy.data.VideoSort): kotlinx.coroutines.flow.Flow<Resource<List<Video>>> =
        flowOf(Resource.Success(emptyList()))
    override fun getDailyQuestion(): kotlinx.coroutines.flow.Flow<Resource<Question>> =
        flowOf(Resource.Error("No questions"))
    override fun getChannelIdForPlaylist(playlistId: String): String = "complexly"
    override fun isCourse(playlistId: String): Boolean = false
    override fun getPopularPlaylists(): kotlinx.coroutines.flow.Flow<Resource<List<com.complexly.academy.model.Playlist>>> =
        flowOf(Resource.Success(emptyList()))
    override fun getHighestRatedPlaylists(): kotlinx.coroutines.flow.Flow<Resource<List<com.complexly.academy.model.Playlist>>> =
        flowOf(Resource.Success(emptyList()))
    override fun getRelatedVideos(videoId: String): kotlinx.coroutines.flow.Flow<Resource<List<Video>>> =
        flowOf(Resource.Success(emptyList()))
    override fun getRecommendedVideos(disabledChannelIds: Set<String>, completedIds: Set<String>): kotlinx.coroutines.flow.Flow<Resource<List<Video>>> =
        flowOf(Resource.Success(emptyList()))
}

private fun makeQuestion(id: String, correctIndex: Int): Question {
    return Question(
        id = id,
        text = "Question $id?",
        options = listOf(
            Option("Wrong A", false, "Incorrect"),
            Option("Correct", true, "Correct answer"),
            Option("Wrong B", false, "Incorrect"),
        ).mapIndexed { i, opt ->
            if (i == correctIndex) opt else opt.copy(isCorrect = i == correctIndex)
        },
        flags = listOf("TEST")
    )
}

private fun makeReviewState(questionId: String, videoId: String): QuestionReviewState {
    return QuestionReviewState(
        questionId = questionId,
        videoId = videoId,
        easeFactor = 2.5,
        intervalDays = 1,
        repetitions = 0,
        nextReviewDate = "2026-01-01",
        lastScore = 0.0,
        lastAnsweredDate = ""
    )
}

/**
 * Unit tests for ReviewViewModel.
 *
 * Tests cover:
 * - Session start populates due reviews and loads first question
 * - Session start with no reviews does nothing
 * - Selecting and submitting a correct answer
 * - Selecting and submitting a wrong answer
 * - Reschedule counts are accumulated correctly
 * - Next question advances the session
 * - Last question completes the session
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userRepository: FakeUserRepository
    private lateinit var contentRepository: FakeContentRepository
    private lateinit var viewModel: ReviewViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = FakeUserRepository()
        contentRepository = FakeContentRepository()
        viewModel = ReviewViewModel(userRepository, contentRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Session Start ────────────────────────────────────────────────────────

    @Test
    fun `onSessionStarted starts session with due reviews`() = runTest {
        val question1 = makeQuestion("q1", 1)
        val video = Video(
            id = "vid1", playlistIds = listOf("pl1"), title = "Test", description = "",
            durationSeconds = 600, youtubeId = "yt1", thumbnailUrl = "",
            questions = listOf(question1)
        )
        contentRepository.videoToReturn = video
        userRepository.dueReviews = listOf(makeReviewState("q1", "vid1"))

        viewModel.onSessionStarted()
        val state = viewModel.uiState.value

        assertTrue(state.isSessionActive)
        assertEquals(1, state.dueReviews.size)
        assertEquals(0, state.currentIndex)
        assertNotNull(state.currentQuestion)
        assertEquals("q1", state.currentQuestion?.id)
    }

    @Test
    fun `onSessionStarted does nothing when no reviews are due`() = runTest {
        userRepository.dueReviews = emptyList()

        viewModel.onSessionStarted()
        val state = viewModel.uiState.value

        assertFalse(state.isSessionActive)
        assertEquals(0, state.dueReviews.size)
        assertNull(state.currentQuestion)
    }

    // ── Answer Selection ─────────────────────────────────────────────────────

    @Test
    fun `onSelectAnswer sets selected option index`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(1)
        val state = viewModel.uiState.value

        assertEquals(1, state.selectedAnswerIndex)
        assertFalse(state.isSubmitted)
    }

    @Test
    fun `onSelectAnswer does nothing after submission`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(1)
        viewModel.onSubmitAnswer()

        // Try to select another answer after submission
        viewModel.onSelectAnswer(2)

        val state = viewModel.uiState.value
        assertEquals(1, state.selectedAnswerIndex) // Unchanged
        assertTrue(state.isSubmitted)
    }

    // ── Correct Answer ───────────────────────────────────────────────────────

    @Test
    fun `submitAnswer with correct answer marks isSubmitted and records answer`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(1) // Correct answer is at index 1
        viewModel.onSubmitAnswer()

        val state = viewModel.uiState.value
        assertTrue(state.isSubmitted)
        assertTrue(state.sessionResults != null)
        assertEquals(1, state.sessionResults?.totalAnswered)
        assertEquals(1, state.sessionResults?.correctCount)

        assertEquals(1, userRepository.recordedAnswers.size)
        assertEquals(Triple("q1", "vid1", 1.0), userRepository.recordedAnswers[0])
    }

    // ── Wrong Answer ─────────────────────────────────────────────────────────

    @Test
    fun `submitAnswer with wrong answer records quality 0`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(0) // Wrong answer
        viewModel.onSubmitAnswer()

        val state = viewModel.uiState.value
        assertTrue(state.isSubmitted)
        assertEquals(1, state.sessionResults?.totalAnswered)
        assertEquals(0, state.sessionResults?.correctCount)

        assertEquals(1, userRepository.recordedAnswers.size)
        assertEquals(Triple("q1", "vid1", 0.0), userRepository.recordedAnswers[0])
    }

    // ── Reschedule Counts ────────────────────────────────────────────────────

    @Test
    fun `submitAnswer tracks reschedule for wrong answer (tomorrow)`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(0) // Wrong → interval = 1 day → tomorrow
        viewModel.onSubmitAnswer()

        val results = viewModel.uiState.value.sessionResults
        assertNotNull(results)
        assertEquals(1, results.rescheduledTomorrow)
        assertEquals(0, results.rescheduledThisWeek)
        assertEquals(0, results.rescheduledLater)
    }

    @Test
    fun `submitAnswer tracks reschedule for correct first answer (3 days)`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(1) // Correct, rep=1 → interval = 3 days → this week
        viewModel.onSubmitAnswer()

        val results = viewModel.uiState.value.sessionResults
        assertNotNull(results)
        assertEquals(0, results.rescheduledTomorrow)
        assertEquals(1, results.rescheduledThisWeek)
        assertEquals(0, results.rescheduledLater)
    }

    @Test
    fun `submitAnswer accumulates reschedule counts across answers`() = runTest {
        val question1 = makeQuestion("q1", 1)
        val question2 = makeQuestion("q2", 1)
        val video = Video(
            id = "vid1", playlistIds = listOf("pl1"), title = "Test", description = "",
            durationSeconds = 600, youtubeId = "yt1", thumbnailUrl = "",
            questions = listOf(question1, question2)
        )
        contentRepository.videoToReturn = video
        userRepository.dueReviews = listOf(
            makeReviewState("q1", "vid1"),
            makeReviewState("q2", "vid1")
        )
        viewModel.onSessionStarted()

        // First answer: correct → 3 days (this week)
        viewModel.onSelectAnswer(1)
        viewModel.onSubmitAnswer()
        // Advance to next question
        viewModel.onNextQuestion()

        // Second answer: wrong → 1 day (tomorrow)
        viewModel.onSelectAnswer(0)
        viewModel.onSubmitAnswer()

        val results = viewModel.uiState.value.sessionResults
        assertNotNull(results)
        assertEquals(2, results.totalAnswered)
        assertEquals(1, results.correctCount)
        assertEquals(1, results.rescheduledTomorrow)  // From second answer (wrong)
        assertEquals(1, results.rescheduledThisWeek)  // From first answer (correct)
        assertEquals(0, results.rescheduledLater)
    }

    // ── Session Navigation ───────────────────────────────────────────────────

    @Test
    fun `nextQuestion advances to next question`() = runTest {
        val question1 = makeQuestion("q1", 1)
        val question2 = makeQuestion("q2", 1)
        val video = Video(
            id = "vid1", playlistIds = listOf("pl1"), title = "Test", description = "",
            durationSeconds = 600, youtubeId = "yt1", thumbnailUrl = "",
            questions = listOf(question1, question2)
        )
        contentRepository.videoToReturn = video
        userRepository.dueReviews = listOf(
            makeReviewState("q1", "vid1"),
            makeReviewState("q2", "vid1")
        )
        viewModel.onSessionStarted()

        // Submit first answer
        viewModel.onSelectAnswer(1)
        viewModel.onSubmitAnswer()
        viewModel.onNextQuestion()

        val state = viewModel.uiState.value
        assertTrue(state.isSessionActive)
        assertEquals(1, state.currentIndex)
        assertEquals("q2", state.currentQuestion?.id)
    }

    @Test
    fun `nextQuestion on last question completes session`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(1)
        viewModel.onSubmitAnswer()
        viewModel.onNextQuestion()

        val state = viewModel.uiState.value
        assertFalse(state.isSessionActive)
        assertNotNull(state.sessionResults)
        assertEquals(1, state.sessionResults?.totalAnswered)
    }

    @Test
    fun `sessionComplete clears session state`() = runTest {
        setupSessionWithQuestion()

        viewModel.onSelectAnswer(1)
        viewModel.onSubmitAnswer()
        viewModel.onNextQuestion() // Completes session

        viewModel.onSessionComplete()

        val state = viewModel.uiState.value
        assertFalse(state.isSessionActive)
        assertNull(state.sessionResults)
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun setupSessionWithQuestion() {
        val question1 = makeQuestion("q1", 1)
        val video = Video(
            id = "vid1", playlistIds = listOf("pl1"), title = "Test", description = "",
            durationSeconds = 600, youtubeId = "yt1", thumbnailUrl = "",
            questions = listOf(question1)
        )
        contentRepository.videoToReturn = video
        userRepository.dueReviews = listOf(makeReviewState("q1", "vid1"))
        viewModel.onSessionStarted()
        // With UnconfinedTestDispatcher, the launch inside loadCurrentQuestion executes immediately
    }
}
