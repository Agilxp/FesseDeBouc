package com.agilxp.fessedebouc.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agilxp.fessedebouc.httpclient.GroupHttpClient
import com.agilxp.fessedebouc.model.GroupDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    init {
        viewModelScope.launch(Dispatchers.Default) {
            myGroups = GroupHttpClient.getMyGroups()
        }
    }

    private var myGroups: List<GroupDTO> by mutableStateOf(listOf())

    fun getMyGroups(): List<GroupDTO> = myGroups
}