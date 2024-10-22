package com.agilxp.fessedebouc.util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object KOffsetDateTimeSerializer {

    fun serialize(value: OffsetDateTime): String {
        val format = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return format.format(value)
    }

    fun deserialize(value: String): OffsetDateTime {
        return OffsetDateTime.parse(value)
    }
}
