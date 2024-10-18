package com.agilxp.fessedebouc.model

import kotlinx.serialization.*

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    @SerialName("verified_email") val verifiedEmail: Boolean,
    val name: String,
    @SerialName("given_name") val givenName: String,
    @SerialName("family_name") val familyName: String,
    val picture: String,
)
