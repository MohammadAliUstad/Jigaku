package com.yugentech.jigaku.session.sessionRepository

import com.yugentech.jigaku.models.Session
import com.yugentech.jigaku.session.SessionResult
import com.yugentech.jigaku.session.SessionService

class SessionRepositoryImpl(
    private val sessionService: SessionService
) : SessionRepository {

    override suspend fun saveSession(userId: String, session: Session): SessionResult<Unit> {
        return sessionService.saveSession(userId, session)
    }

    override suspend fun getSessions(userId: String): SessionResult<List<Session>> {
        return sessionService.getSessions(userId)
    }

    override suspend fun updateTotalTime(
        userId: String,
        additionalSeconds: Int
    ): SessionResult<Unit> {
        return sessionService.updateTotalTime(userId, additionalSeconds)
    }

    override suspend fun getTotalTime(userId: String): SessionResult<Long> {
        return sessionService.getTotalTime(userId)
    }
}