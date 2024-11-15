package com.agilxp.fessedebouc

import android.os.Build
import io.ktor.client.*

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val client: HttpClient
        get() = TODO("Not yet implemented")

    override fun getUserEmail(): String {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()