package com.yugentech.jigaku.authentication

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.yugentech.jigaku.authentication.authUtils.AuthResult
import com.yugentech.jigaku.authentication.authUtils.AuthState
import com.yugentech.jigaku.authentication.authUtils.UserData
import com.yugentech.jigaku.authentication.authRepository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AuthViewModel handles the authentication logic and manages UI state using StateFlow.
 * It interacts with the AuthRepository to perform sign-in, sign-up, and Google sign-in operations.
 */
class AuthViewModel(
    private val repository: AuthRepository // Dependency-injected repository
) : ViewModel() {

    // Mutable state to hold authentication status, loading state, and any error messages
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState // Public read-only state exposed to UI

    init {
        // Check if user is already signed in when ViewModel is created
        when (val result = repository.getCurrentUser()) {
            is AuthResult.Success -> updateAuthState(result.data)
            else -> _authState.value = AuthState(isUserLoggedIn = false)
        }
    }

    /**
     * Signs up a user using email and password, updates the state accordingly.
     */
    fun signUp(name: String, email: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true)

        viewModelScope.launch {
            when (val result = repository.signUp(name, email, password)) {
                is AuthResult.Success -> updateAuthState(result.data)
                is AuthResult.Error -> _authState.value =
                    _authState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /**
     * Signs in an existing user using email and password.
     */
    fun signIn(email: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true)

        viewModelScope.launch {
            when (val result = repository.signIn(email, password)) {
                is AuthResult.Success -> updateAuthState(result.data)
                is AuthResult.Error -> _authState.value =
                    _authState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /**
     * Signs out the current user and resets the auth state.
     */
    fun signOut() {
        repository.signOut()
        _authState.value = AuthState(isUserLoggedIn = false)
    }

    /**
     * Requests a Google Sign-In PendingIntent and updates the state for launching One Tap UI.
     */
    fun getGoogleSignInIntent(webClientId: String) {
        _authState.value = _authState.value.copy(isLoading = true)

        viewModelScope.launch {
            when (val result = repository.getGoogleSignInIntent(webClientId)) {
                is AuthResult.Success -> _authState.value =
                    _authState.value.copy(isLoading = false, pendingIntent = result.data)

                is AuthResult.Error -> _authState.value =
                    _authState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /**
     * Handles the result from Google One Tap and authenticates the user via Firebase.
     */
    fun handleGoogleSignInResult(data: Intent?) {
        _authState.value = _authState.value.copy(isLoading = true)

        viewModelScope.launch {
            when (val result = repository.handleGoogleSignInResult(data)) {
                is AuthResult.Success -> updateAuthState(result.data)
                is AuthResult.Error -> _authState.value =
                    _authState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /**
     * Updates the authentication state based on the FirebaseUser returned.
     * Converts FirebaseUser to domain-level UserData for UI use.
     */
    private fun updateAuthState(user: FirebaseUser?) {
        if (user != null) {
            val userData = UserData(
                userId = user.uid,
                username = user.displayName,
                profilePictureUrl = user.photoUrl?.toString(),
                email = user.email
            )

            _authState.value = AuthState(
                isUserLoggedIn = true,
                userId = user.uid,
                userData = userData
            )
        } else {
            _authState.value = AuthState(isUserLoggedIn = false)
        }
    }
}