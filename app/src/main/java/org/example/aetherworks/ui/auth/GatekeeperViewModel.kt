package org.example.aetherworks.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.aetherworks.security.GatekeeperRepository

class GatekeeperViewModel(private val repository: GatekeeperRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<GatekeeperUiState>(GatekeeperUiState.Loading)
    val uiState: StateFlow<GatekeeperUiState> = _uiState.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        if (!repository.hasCompletedOnboarding()) {
            _uiState.value = GatekeeperUiState.Onboarding
        } else {
            updateLockoutState()
        }
    }

    private fun updateLockoutState() {
        if (repository.isLockedOut()) {
            val remaining = repository.getRemainingLockoutSeconds()
            _uiState.value = GatekeeperUiState.LockedOut(remaining)
            startLockoutTimer()
        } else {
            _uiState.value = GatekeeperUiState.PromptPassword
        }
    }

    private fun startLockoutTimer() {
        viewModelScope.launch {
            while (repository.isLockedOut()) {
                val remaining = repository.getRemainingLockoutSeconds()
                _uiState.value = GatekeeperUiState.LockedOut(remaining)
                delay(1000)
            }
            _uiState.value = GatekeeperUiState.PromptPassword
        }
    }

    fun submitPassword(password: String) {
        val dbKey = repository.authenticate(password)
        if (dbKey != null) {
            _uiState.value = GatekeeperUiState.Authenticated(dbKey)
        } else {
            updateLockoutState()
            if (_uiState.value == GatekeeperUiState.PromptPassword) {
                // If not locked out, just show error
                _uiState.value = GatekeeperUiState.PasswordError
            }
        }
    }

    fun completeOnboarding(password: String) {
        val dbKey = repository.completeOnboarding(password)
        _uiState.value = GatekeeperUiState.Authenticated(dbKey)
    }
}

sealed class GatekeeperUiState {
    object Loading : GatekeeperUiState()
    object Onboarding : GatekeeperUiState()
    object PromptPassword : GatekeeperUiState()
    object PasswordError : GatekeeperUiState()
    data class LockedOut(val remainingSeconds: Long) : GatekeeperUiState()
    data class Authenticated(val dbKey: ByteArray) : GatekeeperUiState()
}
