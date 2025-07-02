package com.yugentech.jigaku.status.statusRepository

import com.yugentech.jigaku.status.StatusService

/**
 * Implementation of StatusRepository.
 *
 * Acts as an abstraction layer between the app's domain layer (e.g. ViewModels)
 * and the StatusService that directly interacts with Firebase Realtime Database.
 */
class StatusRepositoryImpl(
    private val service: StatusService // Injected service that does the actual Firebase work
) : StatusRepository {

    /**
     * Updates the study status (true/false) of a user in the backend.
     *
     * @param userId ID of the user.
     * @param isStudying Whether the user is currently studying.
     * @return Boolean indicating success or failure of the operation.
     */
    override suspend fun setStudyStatus(userId: String, isStudying: Boolean): Boolean {
        return service.setStudyStatus(userId, isStudying)
    }

    /**
     * Fetches the current study status of a given user.
     *
     * @param userId ID of the user.
     * @return true if the user is studying, false otherwise (including on failure).
     */
    override suspend fun getStudyStatus(userId: String): Boolean {
        return service.getStudyStatus(userId)
    }

    /**
     * Retrieves the study status of all users in the system.
     *
     * @return A map of userId → isStudying (true/false). Returns emptyMap() on failure.
     */
    override suspend fun getAllStatuses(): Map<String, Boolean> {
        return service.getAllStatuses()
    }
}