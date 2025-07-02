package com.yugentech.jigaku.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.jigaku.models.Session
import com.yugentech.jigaku.session.sessionRepository.SessionRepository
import com.yugentech.jigaku.session.sessionUtils.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val repository: SessionRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _isStudying = MutableStateFlow(false)
    val isStudying: StateFlow<Boolean> = _isStudying.asStateFlow()

    private val _selectedDuration = MutableStateFlow(25 * 60)
    val selectedDuration: StateFlow<Int> = _selectedDuration.asStateFlow()

    private val _currentTime = MutableStateFlow(_selectedDuration.value)
    val currentTime: StateFlow<Int> = _currentTime.asStateFlow()

    private val _formattedStudyTime = MutableStateFlow("No sessions yet!")
    val formattedStudyTime: StateFlow<String> = _formattedStudyTime.asStateFlow()

    private var timerJob: Job? = null
    private var hasInitialLoad = false
    private var hasLoadedSessions = false
    private var currentUserId: String? = null

    init {
        // Update formatted time whenever total time changes
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
     * Formats the total study time into a human-readable string
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
     * Loads sessions for a user.
     * @param userId The ID of the user
     * @param forceRefresh Whether to force a refresh of the data even if cached
     */
    fun loadSessions(userId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh &&
            hasLoadedSessions &&
            currentUserId == userId &&
            !sessionState.value.isSessionsLoading
        ) {
            return
        }

        viewModelScope.launch {
            currentUserId = userId
            _sessionState.update { it.copy(isSessionsLoading = true) }

            when (val result = repository.getSessions(userId)) {
                is SessionResult.Success -> {
                    _sessionState.update {
                        it.copy(
                            sessions = result.data.sortedByDescending { session -> session.timestamp },
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
     * Loads total study time for a user.
     * @param userId The ID of the user
     * @param forceRefresh Whether to force a refresh of the data even if cached
     */
    fun loadTotalTime(userId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh &&
            hasInitialLoad &&
            currentUserId == userId &&
            sessionState.value.isTotalTimeLoaded
        ) {
            return
        }

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
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Forces a refresh of both total time and sessions data
     */
    fun refreshData(userId: String) {
        refreshTotalTime(userId)
        refreshSessions(userId)
    }

    /**
     * Forces a refresh of the total time data
     */
    fun refreshTotalTime(userId: String) {
        loadTotalTime(userId, forceRefresh = true)
    }

    /**
     * Forces a refresh of the sessions data
     */
    fun refreshSessions(userId: String) {
        loadSessions(userId, forceRefresh = true)
    }

    /**
     * Updates the selected duration for the study session
     */
    fun updateSelectedDuration(minutes: Int) {
        _selectedDuration.value = minutes * 60
        _currentTime.value = _selectedDuration.value
    }

    /**
     * Starts the study timer if not already running
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
     * Stops the running timer
     */
    fun stopTimer() {
        _isStudying.value = false
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Resets the timer to the selected duration
     */
    fun resetTimer() {
        stopTimer()
        _currentTime.value = _selectedDuration.value
    }

    /**
     * Returns the elapsed time in the current session
     */
    fun getElapsedTime(): Int = _selectedDuration.value - _currentTime.value

    /**
     * Saves a completed study session
     * @param userId The ID of the user
     * @param durationStudied Duration of the completed session in seconds
     */
    fun saveSession(userId: String, durationStudied: Int) {
        if (durationStudied <= 0) return

        viewModelScope.launch {
            _sessionState.update {
                it.copy(
                    isSaving = true,
                    saveSuccess = false,
                    error = null
                )
            }

            try {
                val session = Session(duration = durationStudied)
                when (val result = repository.saveSession(userId, session)) {
                    is SessionResult.Success -> {
                        // Update total time in repository
                        repository.updateTotalTime(userId, durationStudied)

                        // Refresh both total time and sessions
                        refreshData(userId)

                        _sessionState.update {
                            it.copy(
                                isSaving = false,
                                saveSuccess = true,
                                error = null
                            )
                        }
                    }

                    is SessionResult.Error -> {
                        _sessionState.update {
                            it.copy(
                                isSaving = false,
                                saveSuccess = false,
                                error = result.message
                            )
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

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}