package com.agilxp.fessedebouc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.httpclient.GroupHttpClient
import com.agilxp.fessedebouc.httpclient.MessageHttpClient
import com.agilxp.fessedebouc.model.GroupDTO
import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.model.PostMessageDTO
import com.agilxp.fessedebouc.model.UserDTO
import io.ktor.websocket.*
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

    fun addGroupAdmin(user: UserDTO) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val selectedGroup = _uiState.value.selectedGroup!!
                GroupHttpClient.addAdminToGroup(selectedGroup.id!!, user)
                val admins = selectedGroup.admins.toMutableList()
                admins.add(user)
                val updatedGroup = selectedGroup.copy(admins = admins)
                val myGroups = GroupHttpClient.getMyGroups()
                _uiState.update { currentState ->
                    currentState.copy(
                        selectedGroup = updatedGroup,
                        myGroups = myGroups.toMutableList(),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = e.message ?: "Something when adding admin to group")
                }
            }
        }
    }

    fun removeGroupAdmin(user: UserDTO) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val selectedGroup = _uiState.value.selectedGroup!!
                GroupHttpClient.removeAdminFromGroup(selectedGroup.id!!, user)
                val admins = selectedGroup.admins.toMutableList()
                admins.remove(user)
                val updatedGroup = selectedGroup.copy(admins = admins)
                val myGroups = GroupHttpClient.getMyGroups()
                _uiState.update { currentState ->
                    currentState.copy(
                        selectedGroup = updatedGroup,
                        myGroups = myGroups.toMutableList(),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = e.message ?: "Something when adding admin to group")
                }
            }
        }
    }

    fun kickUser(user: UserDTO) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val selectedGroup = _uiState.value.selectedGroup!!
                GroupHttpClient.kickUserFromGroup(selectedGroup.id!!, user)
                val myGroups = GroupHttpClient.getMyGroups()
                _uiState.update { currentState ->
                    val users = selectedGroup.users.toMutableList()
                    users.remove(user)
                    val updatedGroup = selectedGroup.copy(users = users)
                    currentState.copy(
                        selectedGroup = updatedGroup,
                        errorMessage = null,
                        myGroups = myGroups.toMutableList()
                    )
                }
            } catch (_: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = "Error removing user from group")
                }
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val selectedGroup = _uiState.value.selectedGroup!!
                val user = getPlatform().getUser()
                GroupHttpClient.kickUserFromGroup(selectedGroup.id!!, user)
                val myGroups = GroupHttpClient.getMyGroups()
                _uiState.update { currentState ->
                    currentState.copy(
                        selectedGroup = null,
                        errorMessage = null,
                        myGroups = myGroups.toMutableList()
                    )
                }
            } catch (_: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = "Error removing user from group")
                }
            }
        }
    }

    fun sendInvitation(email: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
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
            val session = if (!_uiState.value.sessionMap.containsKey(group)) {
                MessageHttpClient.createMessageSession(group) {
                    _uiState.update { currentState ->
                        val messages = currentState.groupMessages.toMutableList()
                        messages.add(it)
                        currentState.copy(
                            groupMessages = messages,
                            errorMessage = currentState.errorMessage + ""
                        )
                    }
                }
            } else {
                _uiState.value.sessionMap[group]!!
            }
            _uiState.update { currentState ->
                currentState.copy(
                    selectedGroup = group,
                    errorMessage = null,
                    groupMessages = groupMessages.toMutableList(),
                    sessionMap = currentState.sessionMap + (group to session)
                )
            }
        }
    }

    fun sendMessage(message: PostMessageDTO) {
        if (_uiState.value.selectedGroup != null && !message.isEmpty()) {
            val group = _uiState.value.selectedGroup!!
            viewModelScope.launch(Dispatchers.Default) {
                val session = _uiState.value.sessionMap[group]
                if (session == null) {
                    println("Error sending message: Couldn't find session for group $group")
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = "Error sending message: Couldn't find session for group $group",
                        )
                    }
                    return@launch
                }
                MessageHttpClient.postMessageToGroup(message, session)
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
    val sessionMap: Map<GroupDTO, DefaultWebSocketSession> = emptyMap()
)
