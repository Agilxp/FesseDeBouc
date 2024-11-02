package com.agilxp.fessedebouc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agilxp.fessedebouc.httpclient.GroupHttpClient
import com.agilxp.fessedebouc.model.GroupDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    init {
        viewModelScope.launch(Dispatchers.Default) {
            val myGroups = GroupHttpClient.getMyGroups()
            _uiState.value = GroupUiState(myGroups)
        }
    }

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()
}

data class GroupUiState(
    val myGroups: List<GroupDTO> = listOf()
)
