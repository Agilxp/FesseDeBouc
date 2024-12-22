package com.agilxp.fessedebouc.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.agilxp.fessedebouc.model.EventDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EventViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    fun selectEvent(event: EventDTO?) {
        _uiState.update { currentState ->
            currentState.copy(selectedEvent = event)
        }
    }

    fun attendEvent(event: EventDTO) {

    }

    fun maybeEvent(event: EventDTO) {

    }

    fun cannotEvent(event: EventDTO) {

    }
}

data class EventUiState(
    val selectedEvent: EventDTO? = null
)