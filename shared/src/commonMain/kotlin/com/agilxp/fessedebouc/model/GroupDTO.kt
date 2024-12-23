package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupDTO(
    val name: String,
    val description: String? = null,
    val id: String? = null,
    val users: List<UserDTO> = emptyList(),
    val admins: List<UserDTO> = emptyList()
)
