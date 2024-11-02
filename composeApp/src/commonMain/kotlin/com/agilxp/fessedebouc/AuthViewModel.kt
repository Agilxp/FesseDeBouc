package com.agilxp.fessedebouc

import androidx.lifecycle.ViewModel
import com.agilxp.fessedebouc.model.AuthResponse
import com.agilxp.fessedebouc.model.RefreshTokenResponse

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setAuthenticated(authResponse: AuthResponse) {
        _uiState.value = AuthUiState(authResponse.accessToken, authResponse.refreshToken)
    }

    fun updateAccessToken(refreshTokenResponse: RefreshTokenResponse) {
        _uiState.update { currentState ->
            currentState.copy(accessToken = refreshTokenResponse.accessToken)
        }
    }

    fun isAuthenticated(): Boolean {
        if (_uiState.value.accessToken.isNullOrBlank()) {
            return false
        }
        // TODO parse token to get expiry
        return true
    }
}

data class AuthUiState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)