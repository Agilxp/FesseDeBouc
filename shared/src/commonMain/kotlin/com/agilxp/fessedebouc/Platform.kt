package com.agilxp.fessedebouc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform