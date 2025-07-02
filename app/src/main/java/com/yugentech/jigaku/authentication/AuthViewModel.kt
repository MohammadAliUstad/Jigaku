package com.yugentech.jigaku.authentication

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.yugentech.jigaku.auth.AuthResult
import com.yugentech.jigaku.authentication.authUtils.AuthState
import com.yugentech.jigaku.authentication.authUtils.UserData
import com.yugentech.jigaku.repositories.authRepository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    init {
        when (val result = repository.getCurrentUser()) {
            is AuthResult.Success -> updateAuthState(result.data)
            else -> _authState.value = AuthState(isUserLoggedIn = false)
        }
    }

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

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState(isUserLoggedIn = false)
    }

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