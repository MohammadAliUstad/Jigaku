package com.yugentech.jigaku.status.statusRepository

import com.yugentech.jigaku.status.StatusService

class StatusRepositoryImpl(
    private val service: StatusService
) : StatusRepository {

    override suspend fun setStudyStatus(userId: String, isStudying: Boolean): Boolean {
        return service.setStudyStatus(userId, isStudying)
    }

    override suspend fun getStudyStatus(userId: String): Boolean {
        return service.getStudyStatus(userId)
    }

    override suspend fun getAllStatuses(): Map<String, Boolean> {
        return service.getAllStatuses()
    }
}