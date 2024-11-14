package com.agilxp.fessedebouc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agilxp.fessedebouc.httpclient.GroupHttpClient
import com.agilxp.fessedebouc.httpclient.MessageHttpClient
import com.agilxp.fessedebouc.model.GroupDTO
import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.model.PostMessageDTO
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

    fun sendInvitation(email: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                println("Sending invitation (model)")
                GroupHttpClient.sendInvitation(email, _uiState.value.selectedGroup?.id)
                _uiState.update { currentState ->
                    currentState.copy(invitationMessages = "Invitation send successfully to $email")
                }
            } catch (_: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(invitationMessages = "Error sending invitation to $email")
                }
            }
        }
    }

    fun selectGroup(group: GroupDTO) {
        viewModelScope.launch(Dispatchers.Default) {
            val groupMessages = MessageHttpClient.getGroupMessages(group)
            _uiState.update { currentState ->
                currentState.copy(
                    selectedGroup = group,
                    errorMessage = null,
                    groupMessages = groupMessages.toMutableList()
                )
            }
        }
    }

    fun sendMessage(message: PostMessageDTO) {
        println("group: ${_uiState.value.selectedGroup} and message: ${message.isEmpty()}")
        if (_uiState.value.selectedGroup != null && !message.isEmpty()) {
            viewModelScope.launch(Dispatchers.Default) {
                MessageHttpClient.postMessageToGroup(_uiState.value.selectedGroup!!, message)
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = null,
                        groupMessages = MessageHttpClient.getGroupMessages(currentState.selectedGroup!!).toMutableList()
                    )
                }
            }
        }
    }

}

data class GroupUiState(
    val myGroups: MutableList<GroupDTO> = mutableListOf(),
    val groupMessages: MutableList<MessageDTO> = mutableListOf(),
    val errorMessage: String? = null,
    val selectedGroup: GroupDTO? = null,
    val invitationMessages: String? = null,
)
