package com.agilxp.fessedebouc

import kotlinx.serialization.Serializable

class BadRequestException(override val message: String): Exception()

class UnknownServerException(override val message: String): Exception()

@Serializable
data class SimpleMessageDTO(val message: String)