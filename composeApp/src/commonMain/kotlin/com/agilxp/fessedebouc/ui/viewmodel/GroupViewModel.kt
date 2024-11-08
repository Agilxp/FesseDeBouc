package com.agilxp.fessedebouc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agilxp.fessedebouc.httpclient.GroupHttpClient
import com.agilxp.fessedebouc.model.GroupDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    init {
        viewModelScope.launch(Dispatchers.Default) {
            val myGroups = GroupHttpClient.getMyGroups()
            _uiState.value = GroupUiState(myGroups.toMutableList())
        }
    }

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    fun addGroup(groupName: String) {
        if (groupName.isBlank()) {
            _uiState.update { currentState ->
                currentState.copy(errorMessage = "Group name cannot be empty")
            }
        } else {
            val group = GroupDTO(groupName)
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val newGroup = GroupHttpClient.createGroup(group)
                    _uiState.update { currentState ->
                        val updatedGroups = currentState.myGroups.toMutableList()
                        updatedGroups.add(newGroup)
                        currentState.copy(myGroups = updatedGroups, errorMessage = null)
                    }
                } catch (e: Exception) {
                    _uiState.update { currentState ->
                        currentState.copy(errorMessage = e.message ?: "Something when creating to group")
                    }
                }
            }
        }
    }

    fun selectGroup(group: GroupDTO) {
        println("Selected group ${group.name}")
        _uiState.update { currentState ->
            currentState.copy(selectedGroup = group)
        }
    }
}

data class GroupUiState(
    val myGroups: MutableList<GroupDTO> = mutableListOf(),
    val errorMessage: String? = null,
    val selectedGroup: GroupDTO? = null
)
