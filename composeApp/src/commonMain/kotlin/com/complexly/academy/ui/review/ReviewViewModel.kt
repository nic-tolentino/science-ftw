package com.complexly.academy.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.model.Question
import com.complexly.academy.model.Video
import com.complexly.academy.spacedrepetition.QuestionReviewState
import com.complexly.academy.spacedrepetition.ReviewSessionResult
import com.complexly.academy.spacedrepetition.ReviewStats
import com.complexly.academy.spacedrepetition.SpacedRepetitionAlgorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val isSessionActive: Boolean = false,
    val dueReviews: List<QuestionReviewState> = emptyList(),
    val currentIndex: Int = 0,
    val currentQuestion: Question? = null,
    val currentVideo: Video? = null,
    val selectedAnswerIndex: Int? = null,
    val isSubmitted: Boolean = false,
    val sessionResults: ReviewSessionResult? = null,
    val reviewStats: ReviewStats = ReviewStats(0, 0, 0, 0, 0.0, 0, 0),
    val isLoading: Boolean = false
)

sealed interface ReviewEvent {
    data class ShowToast(val message: String) : ReviewEvent
}

class ReviewViewModel(
    private val userRepository: UserRepository,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ReviewUiState()
        )

    fun onSessionStarted() {
        val due = userRepository.getDueReviews()
        if (due.isEmpty()) return

        _state.value = _state.value.copy(
            isSessionActive = true,
            dueReviews = due,
            currentIndex = 0,
            sessionResults = null
        )
        loadCurrentQuestion()
    }

    fun onSessionDismissed() {
        _state.value = _state.value.copy(isSessionActive = false)
    }

    fun onSessionComplete() {
        val results = _state.value.sessionResults ?: return
        _state.value = _state.value.copy(isSessionActive = false, sessionResults = null)
    }

    fun onSelectAnswer(index: Int) {
        if (_state.value.isSubmitted) return
        _state.value = _state.value.copy(selectedAnswerIndex = index)
    }

    fun onSubmitAnswer() {
        val state = _state.value
        if (state.isSubmitted || state.selectedAnswerIndex == null || state.currentQuestion == null) return

        val question = state.currentQuestion!!
        val selectedIndex = state.selectedAnswerIndex!!
        val isCorrect = question.options.getOrNull(selectedIndex)?.isCorrect == true
        val quality = if (isCorrect) 1.0 else 0.0

        // Record the answer in the spaced repetition engine and get the new review state
        val videoId = state.currentVideo?.id ?: ""
        var newRescheduledTomorrow = 0
        var newRescheduledThisWeek = 0
        var newRescheduledLater = 0

        // Synchronously compute what the new review state will be so we can track rescheduling
        val currentState = state.dueReviews.getOrNull(state.currentIndex)
        if (currentState != null) {
            val updatedState = SpacedRepetitionAlgorithm.updateState(currentState, quality)
            val daysUntilNext = updatedState.intervalDays
            if (daysUntilNext <= 1) newRescheduledTomorrow = 1
            else if (daysUntilNext <= 7) newRescheduledThisWeek = 1
            else newRescheduledLater = 1
        }

        viewModelScope.launch {
            userRepository.recordAnswer(question.id, videoId, quality)
        }

        // Update session results — accumulate from current results
        val currentResults = state.sessionResults ?: ReviewSessionResult(0, 0, 0, 0, 0)
        val newResults = currentResults.copy(
            totalAnswered = currentResults.totalAnswered + 1,
            correctCount = if (isCorrect) currentResults.correctCount + 1 else currentResults.correctCount,
            rescheduledTomorrow = currentResults.rescheduledTomorrow + newRescheduledTomorrow,
            rescheduledThisWeek = currentResults.rescheduledThisWeek + newRescheduledThisWeek,
            rescheduledLater = currentResults.rescheduledLater + newRescheduledLater
        )

        _state.value = state.copy(isSubmitted = true, sessionResults = newResults)
    }

    fun onNextQuestion() {
        val state = _state.value
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.dueReviews.size) {
            // Session complete
            _state.value = state.copy(
                isSessionActive = false,
                sessionResults = state.sessionResults,
                dueReviews = emptyList()
            )
            loadReviewStats()
            return
        }

        _state.value = state.copy(
            currentIndex = nextIndex,
            selectedAnswerIndex = null,
            isSubmitted = false
        )
        loadCurrentQuestion()
    }

    private fun loadCurrentQuestion() {
        val state = _state.value
        if (state.currentIndex >= state.dueReviews.size) return

        val reviewState = state.dueReviews[state.currentIndex]
        viewModelScope.launch {
            contentRepository.getVideo(reviewState.videoId).collect { videoRes ->
                if (videoRes is com.complexly.academy.model.Resource.Success) {
                    val video = videoRes.data
                    val question = video.questions.find { it.id == reviewState.questionId }
                    if (question != null) {
                        _state.value = _state.value.copy(
                            currentQuestion = question,
                            currentVideo = video
                        )
                    }
                }
            }
        }
    }

    private fun loadReviewStats() {
        _state.value = _state.value.copy(reviewStats = userRepository.getReviewStats())
    }
}
