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
    val formattedTimes: Map<String, String> = emptyMap(),
    val refreshing: Boolean = false
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
     * Formats study time into a human-readable string with improved formatting
     */
    private fun formatTimeStudied(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 && minutes > 0 -> "$hours hr ${minutes} min"
            hours > 0 -> "$hours hr"
            minutes > 0 -> "$minutes min"
            else -> "Just started"
        }
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
                    }.sortedByDescending { it.totalTimeStudied }
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
        return _uiState.value.formattedTimes[userId] ?: "Just started"
    }

    /**
     * Loads users with caching
     */
    private fun loadUsers(withStatus: Boolean = true, forceRefresh: Boolean = false) {
        // Skip if data is already loaded and cache is still valid
        if (!forceRefresh &&
            hasInitialLoad &&
            System.currentTimeMillis() - lastLoadTime < cacheValidityPeriod &&
            _uiState.value.users.isNotEmpty()
        ) {
            return
        }

        // Show loading state appropriately
        _uiState.update {
            it.copy(
                isLoading = !hasInitialLoad,
                refreshing = hasInitialLoad
            )
        }

        viewModelScope.launch {
            try {
                val result = if (withStatus) {
                    repository.getAllUsersWithStatuses()
                } else {
                    repository.getAllUsers()
                }

                result.onSuccess { users ->
                    val sortedUsers = users.sortedByDescending { it.totalTimeStudied }
                    updateFormattedTimes(sortedUsers)
                    _uiState.update {
                        it.copy(
                            users = sortedUsers,
                            isLoading = false,
                            refreshing = false,
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
                            error = e.message ?: "Failed to load users",
                            isLoading = false,
                            refreshing = false,
                            users = currentState.users,
                            formattedTimes = currentState.formattedTimes
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "An unexpected error occurred",
                        isLoading = false,
                        refreshing = false,
                        users = currentState.users,
                        formattedTimes = currentState.formattedTimes
                    )
                }
            }
        }
    }

    /**
     * Forces a refresh of user data and their statuses
     */
    fun refreshUserStatuses() {
        loadUsers(withStatus = true, forceRefresh = true)
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningToUserStatuses()
    }
}