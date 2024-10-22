package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupDTO(val name: String, val description: String? = null)
