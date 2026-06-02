package com.complexly.academy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Event>(initialState: State) : ViewModel() {
    
    private val _uiState = MutableStateFlow(initialState)
    open val uiState: StateFlow<State> = _uiState.asStateFlow()

    protected fun updateState(action: (State) -> State) {
        _uiState.update(action)
    }

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    protected fun sendEvent(event: Event) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}
