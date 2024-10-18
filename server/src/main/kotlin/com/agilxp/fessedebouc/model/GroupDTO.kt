package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupDTO(val id: Int? = null, val name: String, val description: String? = null)
