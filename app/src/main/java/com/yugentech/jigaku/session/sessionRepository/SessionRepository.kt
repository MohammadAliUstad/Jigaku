package com.yugentech.jigaku.session.sessionRepository

import com.yugentech.jigaku.models.Session
import com.yugentech.jigaku.session.SessionResult

interface SessionRepository {
    suspend fun saveSession(userId: String, session: Session): SessionResult<Unit>
    suspend fun getSessions(userId: String): SessionResult<List<Session>>
    suspend fun updateTotalTime(userId: String, additionalSeconds: Int): SessionResult<Unit>
    suspend fun getTotalTime(userId: String): SessionResult<Long>
}