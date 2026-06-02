package com.complexly.academy.ui.quiz

import androidx.lifecycle.viewModelScope
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.model.Option
import com.complexly.academy.model.Question
import com.complexly.academy.model.Resource
import com.complexly.academy.model.Video
import com.complexly.academy.ui.BaseViewModel
import kotlinx.coroutines.launch

data class QuizUiState(
    val video: Video? = null,
    val channelId: String = "crashcourse",
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedOption: Option? = null,
    val isSubmitted: Boolean = false,
    val correctAnswersCount: Int = 0,
    val isQuizFinished: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface QuizEvent {
    object NavigateBack : QuizEvent
}

class QuizViewModel(
    private val videoId: String,
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository
) : BaseViewModel<QuizUiState, QuizEvent>(QuizUiState(isLoading = true)) {

    init {
        viewModelScope.launch {
            if (videoId == "daily") {
                contentRepository.getDailyQuestion().collect { res ->
                    when (res) {
                        is Resource.Loading -> updateState { it.copy(isLoading = true) }
                        is Resource.Success -> updateState { it.copy(isLoading = false, questions = listOf(res.data), channelId = "crashcourse") }
                        is Resource.Error -> updateState { it.copy(isLoading = false, errorMessage = res.message) }
                    }
                }
            } else {
                contentRepository.getVideo(videoId).collect { res ->
                    when (res) {
                        is Resource.Loading -> updateState { it.copy(isLoading = true) }
                        is Resource.Success -> updateState {
                            it.copy(
                                isLoading = false,
                                video = res.data,
                                questions = res.data.questions,
                                channelId = res.data.playlistIds.firstOrNull()
                                    ?.let { plId -> contentRepository.getChannelIdForPlaylist(plId) } ?: ""
                            )
                        }
                        is Resource.Error -> updateState { it.copy(isLoading = false, errorMessage = res.message) }
                    }
                }
            }
        }
    }

    fun selectOption(option: Option) {
        if (uiState.value.isSubmitted) return
        updateState { it.copy(selectedOption = option) }
    }

    fun submitAnswer() {
        val current = uiState.value
        val selected = current.selectedOption ?: return
        if (current.isSubmitted) return
        if (selected.isCorrect) {
            viewModelScope.launch {
                userRepository.incrementCorrectAnswers()
            }
        }
        updateState {
            it.copy(
                isSubmitted = true,
                correctAnswersCount = if (selected.isCorrect) current.correctAnswersCount + 1 else current.correctAnswersCount
            )
        }
    }

    fun nextQuestion() {
        val current = uiState.value
        val nextIndex = current.currentQuestionIndex + 1
        if (nextIndex < current.questions.size) {
            updateState { it.copy(currentQuestionIndex = nextIndex, selectedOption = null, isSubmitted = false) }
        } else {
            updateState { it.copy(isQuizFinished = true) }
            viewModelScope.launch {
                if (videoId != "daily") userRepository.markVideoCompleted(videoId)
                userRepository.updateStreak(increment = true)
            }
        }
    }
}
