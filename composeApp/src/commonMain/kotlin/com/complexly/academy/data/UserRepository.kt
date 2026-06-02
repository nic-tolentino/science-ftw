package com.complexly.academy.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.complexly.academy.model.LearningProgress
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.UserPreferences
import com.complexly.academy.model.Video
import com.complexly.academy.spacedrepetition.QuestionReviewState
import com.complexly.academy.spacedrepetition.ReviewSessionResult
import com.complexly.academy.spacedrepetition.ReviewStats
import com.complexly.academy.spacedrepetition.SpacedRepetitionAlgorithm
import com.complexly.academy.util.TimeOverride
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

/**
 * Mutable user state layer, backed by DataStore.
 * Exposes [learningProgress] and [userPreferences] as independent StateFlows so that
 * subscribers to one are not re-triggered by writes to the other.
 */
interface UserRepository {
    val learningProgress: StateFlow<LearningProgress>
    val userPreferences: StateFlow<UserPreferences>

    suspend fun markVideoCompleted(videoId: String)
    suspend fun enrollInCourse(courseId: String)
    suspend fun unenrollFromCourse(courseId: String)
    suspend fun markVideoWatched(videoId: String)
    suspend fun markPlaylistViewed(playlistId: String)
    suspend fun incrementCorrectAnswers()
    suspend fun updateStreak(increment: Boolean)
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setPushNotifications(enabled: Boolean)
    suspend fun setDisabledChannels(disabledChannelIds: Set<String>)

    // Spaced repetition
    suspend fun recordAnswer(questionId: String, videoId: String, quality: Double)
    fun getDueReviews(maxCount: Int = 15): List<QuestionReviewState>
    fun getReviewStats(): ReviewStats

    // Developer time override
    val dayOffset: StateFlow<Int>
    fun advanceDay()
    fun resetDay()
}

/**
 * Takes [JsonDataSource] directly (rather than [ContentRepository]) to check course completion
 * in [markVideoCompleted] — avoids a circular dependency since ContentRepository does not
 * depend on UserRepository.
 */
class UserRepositoryImpl(
    private val jsonDataSource: JsonDataSource,
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : UserRepository {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    // Loaded eagerly; also fetched defensively inside markVideoCompleted in case init hasn't
    // completed yet. JsonDataSource caches after the first call, so re-fetching is free.
    private var cachedPlaylists: List<Playlist> = emptyList()
    private var cachedVideos: List<Video> = emptyList()

    init {
        scope.launch {
            cachedPlaylists = jsonDataSource.getPlaylists()
            cachedVideos = jsonDataSource.getVideos()
        }
    }

    companion object {
        private val KEY_COMPLETED_VIDEO_IDS = stringSetPreferencesKey("completed_video_ids")
        private val KEY_COMPLETED_COURSE_IDS = stringSetPreferencesKey("completed_course_ids")
        private val KEY_ENROLLED_COURSE_IDS = stringSetPreferencesKey("enrolled_course_ids")
        private val KEY_RECENTLY_WATCHED_VIDEO_IDS = stringPreferencesKey("recently_watched_video_ids")
        private val KEY_RECENTLY_COMPLETED_VIDEO_IDS = stringPreferencesKey("recently_completed_video_ids")
        private val KEY_CORRECT_ANSWERS_COUNT = intPreferencesKey("correct_answers_count")
        private val KEY_RECENTLY_VIEWED_ITEM_IDS = stringPreferencesKey("recently_viewed_item_ids")
        private val KEY_STREAK_COUNT = intPreferencesKey("streak_count")
        private val KEY_LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications_enabled")
        private val KEY_DISABLED_CHANNELS = stringSetPreferencesKey("disabled_channel_ids")
        private val KEY_QUESTION_REVIEW_STATES = stringPreferencesKey("question_review_states")
        private val KEY_REVIEW_STREAK = intPreferencesKey("review_streak")
        private val KEY_LONGEST_REVIEW_STREAK = intPreferencesKey("longest_review_streak")
        private val KEY_LAST_REVIEW_DATE = stringPreferencesKey("last_review_date")
    }

    // In-memory review states — lazy-loaded from DataStore
    private val _reviewStates = MutableStateFlow<Map<String, QuestionReviewState>>(emptyMap())

    init {
        scope.launch {
            cachedPlaylists = jsonDataSource.getPlaylists()
            cachedVideos = jsonDataSource.getVideos()
            
            dataStore.data.map { prefs ->
                prefs[KEY_QUESTION_REVIEW_STATES] ?: "{}"
            }.collect { jsonStr ->
                try {
                    val states = if (jsonStr.isEmpty() || jsonStr == "{}") {
                        emptyMap()
                    } else {
                        Json.decodeFromString<Map<String, QuestionReviewState>>(jsonStr)
                    }
                    _reviewStates.value = states
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
        }
    }

    override val learningProgress: StateFlow<LearningProgress> = dataStore.data
        .map { prefs ->
            val recentlyWatchedStr = prefs[KEY_RECENTLY_WATCHED_VIDEO_IDS] ?: ""
            val recentlyCompletedStr = prefs[KEY_RECENTLY_COMPLETED_VIDEO_IDS] ?: ""
            val recentlyViewedStr = prefs[KEY_RECENTLY_VIEWED_ITEM_IDS] ?: ""
            LearningProgress(
                completedVideoIds = prefs[KEY_COMPLETED_VIDEO_IDS] ?: emptySet(),
                completedCourseIds = prefs[KEY_COMPLETED_COURSE_IDS] ?: emptySet(),
                enrolledCourseIds = prefs[KEY_ENROLLED_COURSE_IDS] ?: emptySet(),
                recentlyWatchedVideoIds = if (recentlyWatchedStr.isEmpty()) emptyList() else recentlyWatchedStr.split(","),
                recentlyCompletedVideoIds = if (recentlyCompletedStr.isEmpty()) emptyList() else recentlyCompletedStr.split(","),
                recentlyViewedItemIds = if (recentlyViewedStr.isEmpty()) emptyList() else recentlyViewedStr.split(","),
                correctAnswersCount = prefs[KEY_CORRECT_ANSWERS_COUNT] ?: 0,
                streakCount = prefs[KEY_STREAK_COUNT] ?: 6, // 6 is a demo default, not a product invariant
                lastActiveDate = prefs[KEY_LAST_ACTIVE_DATE]
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = LearningProgress(streakCount = 6)
        )

    override val userPreferences: StateFlow<UserPreferences> = dataStore.data
        .map { prefs ->
            UserPreferences(
                darkModeEnabled = prefs[KEY_DARK_MODE] ?: false,
                pushNotificationsEnabled = prefs[KEY_PUSH_NOTIFICATIONS] ?: true,
                disabledChannelIds = prefs[KEY_DISABLED_CHANNELS] ?: emptySet()
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences()
        )

    override suspend fun markVideoCompleted(videoId: String) {
        // Ensure data is loaded (no-op after first call due to JsonDataSource caching)
        if (cachedPlaylists.isEmpty()) cachedPlaylists = jsonDataSource.getPlaylists()
        if (cachedVideos.isEmpty()) cachedVideos = jsonDataSource.getVideos()

        dataStore.edit { prefs ->
            val currentCompleted = prefs[KEY_COMPLETED_VIDEO_IDS] ?: emptySet()
            if (!currentCompleted.contains(videoId)) {
                val newCompleted = currentCompleted + videoId
                prefs[KEY_COMPLETED_VIDEO_IDS] = newCompleted

                // Bootstrap review states: create initial states for all questions in this video
                val video = cachedVideos.find { it.id == videoId }
                if (video != null && video.questions.isNotEmpty()) {
                    val currentStates = _reviewStates.value.toMutableMap()
                    video.questions.forEach { question ->
                        if (question.id !in currentStates) {
                            currentStates[question.id] = SpacedRepetitionAlgorithm.initialState(question.id, videoId)
                        }
                    }
                    _reviewStates.value = currentStates
                    prefs[KEY_QUESTION_REVIEW_STATES] = Json.encodeToString(currentStates)
                }

                // Append to recentlyCompletedVideoIds
                val completedStr = prefs[KEY_RECENTLY_COMPLETED_VIDEO_IDS] ?: ""
                val completedList = if (completedStr.isEmpty()) emptyList() else completedStr.split(",")
                val newCompletedList = (listOf(videoId) + (completedList - videoId)).take(10)
                prefs[KEY_RECENTLY_COMPLETED_VIDEO_IDS] = newCompletedList.joinToString(",")

                val newCompletedCourses = (prefs[KEY_COMPLETED_COURSE_IDS] ?: emptySet()).toMutableSet()
                // Use video.playlistIds rather than playlist.videoIds: the latter contains
                // placeholder IDs (e.g. "economics_01") that never match actual video IDs.
                cachedPlaylists.forEach { playlist ->
                    val videosInPlaylist = cachedVideos.filter { playlist.id in it.playlistIds }
                    if (videosInPlaylist.isNotEmpty() && videosInPlaylist.all { it.id in newCompleted }) {
                        newCompletedCourses.add(playlist.id)
                    }
                }
                prefs[KEY_COMPLETED_COURSE_IDS] = newCompletedCourses
            }
        }
    }

    override suspend fun enrollInCourse(courseId: String) {
        dataStore.edit { prefs ->
            val enrolled = prefs[KEY_ENROLLED_COURSE_IDS] ?: emptySet()
            if (!enrolled.contains(courseId)) {
                prefs[KEY_ENROLLED_COURSE_IDS] = enrolled + courseId
            }
        }
    }

    override suspend fun unenrollFromCourse(courseId: String) {
        dataStore.edit { prefs ->
            val enrolled = prefs[KEY_ENROLLED_COURSE_IDS] ?: emptySet()
            if (enrolled.contains(courseId)) {
                prefs[KEY_ENROLLED_COURSE_IDS] = enrolled - courseId
            }
        }
    }

    private suspend fun addToRecentlyViewed(item: String) {
        dataStore.edit { prefs ->
            val viewedStr = prefs[KEY_RECENTLY_VIEWED_ITEM_IDS] ?: ""
            val viewedList = if (viewedStr.isEmpty()) emptyList() else viewedStr.split(",")
            val newViewedList = (listOf(item) + (viewedList - item)).take(20)
            prefs[KEY_RECENTLY_VIEWED_ITEM_IDS] = newViewedList.joinToString(",")
        }
    }

    override suspend fun markPlaylistViewed(playlistId: String) {
        addToRecentlyViewed("playlist:$playlistId")
    }

    override suspend fun markVideoWatched(videoId: String) {
        dataStore.edit { prefs ->
            val watchedStr = prefs[KEY_RECENTLY_WATCHED_VIDEO_IDS] ?: ""
            val watchedList = if (watchedStr.isEmpty()) emptyList() else watchedStr.split(",")
            val newWatchedList = (listOf(videoId) + (watchedList - videoId)).take(10)
            prefs[KEY_RECENTLY_WATCHED_VIDEO_IDS] = newWatchedList.joinToString(",")
        }
        addToRecentlyViewed("video:$videoId")
    }

    override suspend fun incrementCorrectAnswers() {
        dataStore.edit { prefs ->
            val current = prefs[KEY_CORRECT_ANSWERS_COUNT] ?: 0
            prefs[KEY_CORRECT_ANSWERS_COUNT] = current + 1
        }
    }

    override suspend fun updateStreak(increment: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_STREAK_COUNT] ?: 6
            prefs[KEY_STREAK_COUNT] = if (increment) current + 1 else current
        }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DARK_MODE] = enabled }
    }

    override suspend fun setPushNotifications(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PUSH_NOTIFICATIONS] = enabled }
    }

    override suspend fun setDisabledChannels(disabledChannelIds: Set<String>) {
        dataStore.edit { prefs -> prefs[KEY_DISABLED_CHANNELS] = disabledChannelIds }
    }

    override suspend fun recordAnswer(questionId: String, videoId: String, quality: Double) {
        val currentStates = _reviewStates.value
        val currentState = currentStates[questionId] ?: SpacedRepetitionAlgorithm.initialState(questionId, videoId)
        val newState = SpacedRepetitionAlgorithm.updateState(currentState, quality)
        val newStates = currentStates + (questionId to newState)
        
        dataStore.edit { prefs ->
            prefs[KEY_QUESTION_REVIEW_STATES] = Json.encodeToString(newStates)
        }
        _reviewStates.value = newStates
    }

    override fun getDueReviews(maxCount: Int): List<QuestionReviewState> {
        val todayDate = TimeOverride.effectiveToday()
        return _reviewStates.value.values
            .filter { it.isDue(todayDate) }
            .sortedBy { it.nextReviewDate }
            .take(maxCount)
    }

    override fun getReviewStats(): ReviewStats {
        val completedIds = learningProgress.value.completedVideoIds
        val completedVideos = cachedVideos.filter { it.id in completedIds }
        val completedQuestions = completedVideos.flatMap { it.questions }
        val totalQuestions = completedQuestions.size
        
        val reviewedQuestions = completedQuestions.filter { it.id in _reviewStates.value }.size
        
        val todayDate = TimeOverride.effectiveToday()
        val dueToday = _reviewStates.value.values.count { it.isDue(todayDate) }
        
        val nextWeek = todayDate.plus(7L, kotlinx.datetime.DateTimeUnit.DAY)
        val dueThisWeek = _reviewStates.value.values.count { state ->
            if (state.nextReviewDate.isEmpty()) true
            else {
                try {
                    val date = LocalDate.parse(state.nextReviewDate)
                    date <= nextWeek
                } catch (e: Exception) {
                    true
                }
            }
        }
        
        val scores = _reviewStates.value.values.map { it.lastScore }
        val overallAccuracy = if (scores.isNotEmpty()) scores.average() else 0.0
        
        return ReviewStats(
            totalQuestions = totalQuestions,
            reviewedQuestions = reviewedQuestions,
            dueToday = dueToday,
            dueThisWeek = dueThisWeek,
            overallAccuracy = overallAccuracy,
            currentStreak = 0,
            longestStreak = 0
        )
    }

    // ── Developer Time Override ─────────────────────────────────────────────

    private val _dayOffset = MutableStateFlow(0)
    override val dayOffset: StateFlow<Int> = _dayOffset

    override fun advanceDay() {
        TimeOverride.setOffset(TimeOverride.getOffset() + 1)
        _dayOffset.value = TimeOverride.getOffset()
    }

    override fun resetDay() {
        TimeOverride.reset()
        _dayOffset.value = 0
    }
}
