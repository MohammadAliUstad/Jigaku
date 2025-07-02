package com.yugentech.jigaku.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.jigaku.models.User
import com.yugentech.jigaku.user.userRepository.UserRepository
import com.yugentech.jigaku.user.userUtils.UserState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing user list and their real-time study status.
 * Supports caching, formatting, and observing user status updates from the repository.
 */
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    // State holding the UI data for the user leaderboard
    private val _uiState = MutableStateFlow(UserState())
    val uiState: StateFlow<UserState> = _uiState.asStateFlow()

    private var hasInitialLoad = false                // Tracks if first data load is done
    private var lastLoadTime: Long = 0                // Last successful load timestamp
    private val cacheValidityPeriod = 30_000L         // Cache lifespan: 30 seconds
    private var statusListenerJob: Job? = null        // Coroutine job for observing status

    init {
        loadUsers(withStatus = true) // Load users and their study statuses initially
    }

    /**
     * Formats total study time into a user-friendly string (e.g., "2 hr 15 min").
     */
    private fun formatTimeStudied(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 && minutes > 0 -> "$hours hr $minutes min"
            hours > 0 -> "$hours hr"
            minutes > 0 -> "$minutes min"
            else -> "Just started"
        }
    }

    /**
     * Starts listening to real-time user study status updates.
     * It updates the isStudying flag on each user based on Firestore data.
     */
    fun startListeningToUserStatuses() {
        if (statusListenerJob != null) return // Already listening

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
     * Stops the status listener coroutine to prevent memory leaks.
     */
    fun stopListeningToUserStatuses() {
        statusListenerJob?.cancel()
        statusListenerJob = null
    }

    /**
     * Updates the formatted time strings for each user in the list.
     */
    private fun updateFormattedTimes(users: List<User>) {
        val newFormattedTimes = users.associate { user ->
            user.userId to formatTimeStudied(user.totalTimeStudied)
        }

        _uiState.update { it.copy(formattedTimes = newFormattedTimes) }
    }

    /**
     * Returns formatted study time string for a given user ID.
     */
    fun getFormattedTimeForUser(userId: String): String {
        return _uiState.value.formattedTimes[userId] ?: "Just started"
    }

    /**
     * Loads the user list from the repository with optional force refresh.
     * Also handles showing proper loading states and error handling.
     */
    private fun loadUsers(withStatus: Boolean = true, forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()

        if (!forceRefresh &&
            hasInitialLoad &&
            now - lastLoadTime < cacheValidityPeriod &&
            _uiState.value.users.isNotEmpty()
        ) return // Use cache if valid

        // Update UI state for loading or refreshing
        _uiState.update {
            it.copy(
                isLoading = !hasInitialLoad,
                refreshing = hasInitialLoad
            )
        }

        viewModelScope.launch {
            try {
                // Fetch users with or without real-time status data
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
                    lastLoadTime = now

                    // Begin listening to live status updates
                    if (withStatus) startListeningToUserStatuses()
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
                // Handle unexpected exceptions
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
     * Forces a refresh of the user list and their current statuses.
     */
    fun refreshUserStatuses() {
        loadUsers(withStatus = true, forceRefresh = true)
    }

    /**
     * Cleans up resources (stops status listeners) when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        stopListeningToUserStatuses()
    }
}