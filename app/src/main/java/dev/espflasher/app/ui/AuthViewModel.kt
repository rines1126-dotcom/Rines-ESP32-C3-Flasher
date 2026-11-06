
package dev.espflasher.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.espflasher.app.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val email: String = "",
    val error: String? = null,
    val message: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthUiState(
            isSignedIn = repository.currentUser != null,
            email = repository.currentUser?.email.orEmpty()
        )
    )

    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun register(email: String, password: String) {
        authenticate {
            repository.register(email, password)
        }
    }

    fun signIn(email: String, password: String) {
        authenticate {
            repository.signIn(email, password)
        }
    }

    fun signInAnonymously() {
        authenticate {
            repository.signInAnonymously()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                message = null
            )

            try {
                repository.signOut()

                _state.value = AuthUiState(
                    isSignedIn = false,
                    message = "Successfully signed out."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign out failed."
                )
            }
        }
    }

    private fun authenticate(
        action: suspend () -> com.google.firebase.auth.FirebaseUser
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                message = null
            )

            try {
                val user = action()

                _state.value = AuthUiState(
                    isSignedIn = true,
                    email = user.email.orEmpty(),
                    message = "Authentication successful."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Authentication failed."
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            error = null,
            message = null
        )
    }
}
