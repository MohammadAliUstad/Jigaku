package com.yugentech.jigaku.session.sessionRepository

import com.yugentech.jigaku.models.Session
import com.yugentech.jigaku.session.sessionUtils.SessionResult
import com.yugentech.jigaku.session.SessionService

/**
 * SessionRepositoryImpl implements the SessionRepository interface.
 * It serves as an abstraction layer between the ViewModel/UseCase layer and the Firebase-backed SessionService.
 *
 * This structure enables testability, separation of concerns, and flexibility for future changes.
 */
class SessionRepositoryImpl(
    private val sessionService: SessionService // Injected dependency for actual Firebase operations
) : SessionRepository {

    /**
     * Saves a study session to Firestore.
     *
     * @param userId The ID of the user
     * @param session The session data to be saved
     * @return SessionResult<Unit> indicating success or error
     */
    override suspend fun saveSession(userId: String, session: Session): SessionResult<Unit> {
        return sessionService.saveSession(userId, session)
    }

    /**
     * Retrieves a list of all study sessions for the specified user.
     *
     * @param userId The ID of the user
     * @return SessionResult containing a list of sessions or an error
     */
    override suspend fun getSessions(userId: String): SessionResult<List<Session>> {
        return sessionService.getSessions(userId)
    }

    /**
     * Updates the user's total study time by adding a given number of seconds.
     *
     * @param userId The ID of the user
     * @param additionalSeconds Number of seconds to add
     * @return SessionResult<Unit> indicating success or error
     */
    override suspend fun updateTotalTime(
        userId: String,
        additionalSeconds: Int
    ): SessionResult<Unit> {
        return sessionService.updateTotalTime(userId, additionalSeconds)
    }

    /**
     * Retrieves the total study time for the user.
     *
     * @param userId The ID of the user
     * @return SessionResult<Long> containing the total seconds studied or an error
     */
    override suspend fun getTotalTime(userId: String): SessionResult<Long> {
        return sessionService.getTotalTime(userId)
    }
}