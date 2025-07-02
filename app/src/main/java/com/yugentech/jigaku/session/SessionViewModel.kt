package com.yugentech.jigaku.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.jigaku.models.Session
import com.yugentech.jigaku.session.sessionRepository.SessionRepository
import com.yugentech.jigaku.session.sessionUtils.SessionResult
import com.yugentech.jigaku.session.sessionUtils.SessionState
import kotlinx.coroutines.*

import kotlinx.coroutines.flow.*

/**
 * ViewModel responsible for managing study sessions, timer state, and total study time.
 * It coordinates with the SessionRepository to persist data and exposes state via StateFlow for UI consumption.
 */
class SessionViewModel(
    private val repository: SessionRepository
) : ViewModel() {

    // State representing all session-related UI data
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // Indicates if the user is currently studying (i.e., timer is running)
    private val _isStudying = MutableStateFlow(false)
    val isStudying: StateFlow<Boolean> = _isStudying.asStateFlow()

    // Duration selected by user (in seconds), default = 25 min
    private val _selectedDuration = MutableStateFlow(25 * 60)
    val selectedDuration: StateFlow<Int> = _selectedDuration.asStateFlow()

    // Current countdown time (in seconds)
    private val _currentTime = MutableStateFlow(_selectedDuration.value)
    val currentTime: StateFlow<Int> = _currentTime.asStateFlow()

    // UI-friendly string of total time studied
    private val _formattedStudyTime = MutableStateFlow("No sessions yet!")
    val formattedStudyTime: StateFlow<String> = _formattedStudyTime.asStateFlow()

    private var timerJob: Job? = null // Reference to the active timer coroutine
    private var hasInitialLoad = false
    private var hasLoadedSessions = false
    private var currentUserId: String? = null

    init {
        // Observes totalTime changes and updates formatted time string for UI
        viewModelScope.launch {
            sessionState
                .map { it.totalTime }
                .distinctUntilChanged()
                .collect { totalTime ->
                    _formattedStudyTime.value = formatTimeStudied(totalTime)
                }
        }
    }

    /**
     * Converts raw seconds into a human-readable format for display.
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
     * Loads all study sessions for a user, unless cached or forceRefresh is true.
     */
    fun loadSessions(userId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh &&
            hasLoadedSessions &&
            currentUserId == userId &&
            !sessionState.value.isSessionsLoading
        ) return

        viewModelScope.launch {
            currentUserId = userId
            _sessionState.update { it.copy(isSessionsLoading = true) }

            when (val result = repository.getSessions(userId)) {
                is SessionResult.Success -> {
                    _sessionState.update {
                        it.copy(
                            sessions = result.data.sortedByDescending { s -> s.timestamp },
                            isSessionsLoading = false,
                            error = null
                        )
                    }
                    hasLoadedSessions = true
                }

                is SessionResult.Error -> {
                    _sessionState.update {
                        it.copy(
                            error = result.message,
                            isSessionsLoading = false
                        )
                    }
                }

                is SessionResult.Loading -> {
                    _sessionState.update { it.copy(isSessionsLoading = true) }
                }
            }
        }
    }

    /**
     * Loads total time studied for a user, unless already loaded or forceRefresh is true.
     */
    fun loadTotalTime(userId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh &&
            hasInitialLoad &&
            currentUserId == userId &&
            sessionState.value.isTotalTimeLoaded
        ) return

        viewModelScope.launch {
            currentUserId = userId

            if (!hasInitialLoad) {
                _sessionState.update { it.copy(isTotalTimeLoaded = false) }
            }

            when (val result = repository.getTotalTime(userId)) {
                is SessionResult.Success -> {
                    _sessionState.update {
                        it.copy(
                            totalTime = result.data,
                            isTotalTimeLoaded = true,
                            error = null
                        )
                    }
                    hasInitialLoad = true
                }

                is SessionResult.Error -> {
                    _sessionState.update {
                        it.copy(
                            error = result.message,
                            isTotalTimeLoaded = true
                        )
                    }
                }

                is SessionResult.Loading -> {
                    // Optional: Handle loading indicator
                }
            }
        }
    }

    /**
     * Triggers reloading of both session list and total time for the given user.
     */
    fun refreshData(userId: String) {
        refreshTotalTime(userId)
        refreshSessions(userId)
    }

    fun refreshTotalTime(userId: String) = loadTotalTime(userId, forceRefresh = true)
    fun refreshSessions(userId: String) = loadSessions(userId, forceRefresh = true)

    /**
     * Updates the selected timer duration in minutes.
     */
    fun updateSelectedDuration(minutes: Int) {
        _selectedDuration.value = minutes * 60
        _currentTime.value = _selectedDuration.value
    }

    /**
     * Starts the countdown timer, decrementing every second.
     */
    fun startTimer() {
        if (timerJob?.isActive == true) return

        _isStudying.value = true
        timerJob = viewModelScope.launch {
            try {
                while (_currentTime.value > 0 && _isStudying.value) {
                    delay(1000)
                    _currentTime.update { it - 1 }
                }
            } finally {
                if (_currentTime.value <= 0) stopTimer()
            }
        }
    }

    /**
     * Stops the current timer coroutine and sets studying flag to false.
     */
    fun stopTimer() {
        _isStudying.value = false
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Resets the timer back to the originally selected duration.
     */
    fun resetTimer() {
        stopTimer()
        _currentTime.value = _selectedDuration.value
    }

    /**
     * Returns how much time (in seconds) has elapsed since the timer started.
     */
    fun getElapsedTime(): Int = _selectedDuration.value - _currentTime.value

    /**
     * Saves a study session to Firestore and updates total time.
     *
     * @param userId The user who completed the session
     * @param durationStudied The number of seconds studied
     */
    fun saveSession(userId: String, durationStudied: Int) {
        if (durationStudied <= 0) return

        viewModelScope.launch {
            _sessionState.update {
                it.copy(isSaving = true, saveSuccess = false, error = null)
            }

            try {
                val session = Session(duration = durationStudied)

                when (val result = repository.saveSession(userId, session)) {
                    is SessionResult.Success -> {
                        repository.updateTotalTime(userId, durationStudied)
                        refreshData(userId)

                        _sessionState.update {
                            it.copy(isSaving = false, saveSuccess = true, error = null)
                        }
                    }

                    is SessionResult.Error -> {
                        _sessionState.update {
                            it.copy(isSaving = false, saveSuccess = false, error = result.message)
                        }
                    }

                    is SessionResult.Loading -> {
                        _sessionState.update { it.copy(isSaving = true) }
                    }
                }
            } catch (e: Exception) {
                _sessionState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    /**
     * Ensures the timer is stopped when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}