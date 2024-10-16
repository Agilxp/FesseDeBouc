package com.agilxp.fessedebouc.model

import com.agilxp.fessedebouc.util.SerializableOffsetDateTime
import kotlinx.serialization.Serializable

@Serializable
data class EventDTO(
    val name: String,
    val description: String,
    val start: SerializableOffsetDateTime,
    val end: SerializableOffsetDateTime,
    val location: String
)
