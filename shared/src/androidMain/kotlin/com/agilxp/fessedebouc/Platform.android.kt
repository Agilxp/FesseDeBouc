package com.agilxp.fessedebouc

import android.os.Build
import io.ktor.client.*
import com.agilxp.fessedebouc.model.UserDTO

class AndroidPlatform : PlatformClass() {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val client: HttpClient
        get() = TODO("Not yet implemented")

    override fun getUser(): UserDTO {
        TODO("Not yet implemented")
    }

    override fun getToken(): String {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()