package com.yugentech.jigaku.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.jigaku.status.statusRepository.StatusRepository
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and updating the study status of a user.
 *
 * This ViewModel interacts with the StatusRepository to update the study status
 * in Firebase Realtime Database. It uses a coroutine scope tied to the ViewModel lifecycle.
 */
class StatusViewModel(
    private val repository: StatusRepository // Injected repository for status-related operations
) : ViewModel() {

    /**
     * Updates the study status of a user in the repository.
     * This triggers a Firebase Realtime DB update via the StatusRepository and StatusService.
     *
     * @param userId ID of the user whose status is being updated.
     * @param isStudying True if the user is currently studying, false otherwise.
     */
    fun setUserStatus(userId: String, isStudying: Boolean) {
        viewModelScope.launch {
            repository.setStudyStatus(userId, isStudying)
        }
    }
}