package com.complexly.academy.ui.you

import androidx.lifecycle.viewModelScope
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Resource
import com.complexly.academy.ui.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class YouUiState(
    val email: String = "academy.learner@example.com",
    val username: String = "Academy Learner",
    val channels: List<Pair<Channel, Boolean>> = emptyList(),
    val pushNotificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val dayOffset: Int = 0
)

class YouViewModel(
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository
) : BaseViewModel<YouUiState, Any>(YouUiState()) {

    init {
        viewModelScope.launch {
            combine(
                contentRepository.getChannels(),
                userRepository.userPreferences,
                userRepository.dayOffset
            ) { channelsRes, prefs, offset ->
                if (channelsRes is Resource.Success) {
                    val channelsList = channelsRes.data
                    val disabledIds = prefs.disabledChannelIds
                    val list = channelsList.map { channel ->
                        channel to !disabledIds.contains(channel.id)
                    }
                    updateState { state ->
                        state.copy(
                            darkModeEnabled = prefs.darkModeEnabled,
                            pushNotificationsEnabled = prefs.pushNotificationsEnabled,
                            channels = list,
                            dayOffset = offset
                        )
                    }
                } else {
                    updateState { state ->
                        state.copy(
                            darkModeEnabled = prefs.darkModeEnabled,
                            pushNotificationsEnabled = prefs.pushNotificationsEnabled,
                            dayOffset = offset
                        )
                    }
                }
            }.collect {}
        }
    }

    fun toggleChannel(channelId: String) {
        val currentDisabled = userRepository.userPreferences.value.disabledChannelIds.toMutableSet()
        if (currentDisabled.contains(channelId)) {
            currentDisabled.remove(channelId)
        } else {
            currentDisabled.add(channelId)
        }
        viewModelScope.launch {
            userRepository.setDisabledChannels(currentDisabled)
        }
    }

    fun togglePushNotifications() {
        val newSetting = !uiState.value.pushNotificationsEnabled
        viewModelScope.launch { userRepository.setPushNotifications(newSetting) }
    }

    fun toggleDarkMode() {
        val newSetting = !uiState.value.darkModeEnabled
        viewModelScope.launch { userRepository.setDarkMode(newSetting) }
    }

    fun advanceDay() {
        userRepository.advanceDay()
    }

    fun resetDay() {
        userRepository.resetDay()
    }
}
