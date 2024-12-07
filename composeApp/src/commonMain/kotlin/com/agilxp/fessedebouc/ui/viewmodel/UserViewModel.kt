package com.agilxp.fessedebouc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agilxp.fessedebouc.httpclient.UserHttpClient
import com.agilxp.fessedebouc.model.UserStatusDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserStatusDTO(emptyList(), emptyList(), emptyList()))
    val uiState: StateFlow<UserStatusDTO> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                UserHttpClient.getUserStatus().let {
                    _uiState.value = it
                }
                delay(10000)
            }
        }
    }
}