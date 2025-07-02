package com.yugentech.jigaku.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.jigaku.models.User
import com.yugentech.jigaku.user.userRepository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val formattedTimes: Map<String, String> = emptyMap()
)

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private var hasInitialLoad = false
    private var lastLoadTime: Long = 0
    private val cacheValidityPeriod = 30_000L // 30 seconds cache
    private var statusListenerJob: Job? = null

    init {
        loadUsers(withStatus = true)
    }

    /**
     * Starts listening to real-time user status updates
     */
    fun startListeningToUserStatuses() {
        if (statusListenerJob != null) return

        statusListenerJob = viewModelScope.launch {
            repository.observeUserStatuses().collect { statusUpdates ->
                _uiState.update { currentState ->
                    val updatedUsers = currentState.users.map { user ->
                        statusUpdates[user.userId]?.let { newStatus ->
                            user.copy(isStudying = newStatus)
                        } ?: user
                    }
                    currentState.copy(users = updatedUsers)
                }
            }
        }
    }

    /**
     * Stops listening to real-time user status updates
     */
    fun stopListeningToUserStatuses() {
        statusListenerJob?.cancel()
        statusListenerJob = null
    }

    /**
     * Formats study time into a human-readable string
     */
    private fun formatTimeStudied(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 && minutes > 0 -> "Studied: $hours hour${if (hours != 1L) "s" else ""} $minutes minute${if (minutes != 1L) "s" else ""}"
            hours > 0 -> "Studied: $hours hour${if (hours != 1L) "s" else ""}"
            minutes > 0 -> "Studied: $minutes minute${if (minutes != 1L) "s" else ""}"
            else -> "No sessions yet!"
        }
    }

    /**
     * Updates formatted times for all users
     */
    private fun updateFormattedTimes(users: List<User>) {
        val newFormattedTimes = users.associate { user ->
            user.userId to formatTimeStudied(user.totalTimeStudied)
        }
        _uiState.update { it.copy(formattedTimes = newFormattedTimes) }
    }

    /**
     * Gets formatted time for a specific user
     */
    fun getFormattedTimeForUser(userId: String): String {
        return _uiState.value.formattedTimes[userId] ?: "No sessions yet!"
    }

    /**
     * Loads users with caching
     */
    fun loadUsers(withStatus: Boolean = true, forceRefresh: Boolean = false) {
        // Skip if data is already loaded and cache is still valid
        if (!forceRefresh &&
            hasInitialLoad &&
            System.currentTimeMillis() - lastLoadTime < cacheValidityPeriod &&
            _uiState.value.users.isNotEmpty()
        ) {
            return
        }

        // Only show loading on initial load
        if (!hasInitialLoad) {
            _uiState.update { it.copy(isLoading = true) }
        }

        viewModelScope.launch {
            try {
                val result = if (withStatus) {
                    repository.getAllUsersWithStatuses()
                } else {
                    repository.getAllUsers()
                }

                result.onSuccess { users ->
                    updateFormattedTimes(users)
                    _uiState.update {
                        it.copy(
                            users = users,
                            isLoading = false,
                            error = null
                        )
                    }
                    hasInitialLoad = true
                    lastLoadTime = System.currentTimeMillis()

                    // Start listening to status updates after initial load
                    if (withStatus) {
                        startListeningToUserStatuses()
                    }
                }.onFailure { e ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            error = e.message ?: "Unknown error occurred",
                            isLoading = false,
                            users = currentState.users,
                            formattedTimes = currentState.formattedTimes
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Unknown error occurred",
                        isLoading = false,
                        users = currentState.users,
                        formattedTimes = currentState.formattedTimes
                    )
                }
            }
        }
    }

    /**
     * Forces a refresh of user data
     */
    fun refreshUsers() {
        loadUsers(withStatus = true, forceRefresh = true)
    }

    /**
     * Cleans up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        stopListeningToUserStatuses()
    }
}