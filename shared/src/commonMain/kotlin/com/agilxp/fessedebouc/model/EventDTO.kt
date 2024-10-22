package com.agilxp.fessedebouc.model

import kotlinx.serialization.Serializable

@Serializable
data class EventDTO(
    val name: String,
    val description: String,
    val start: String,
    val end: String,
    val location: String
)
