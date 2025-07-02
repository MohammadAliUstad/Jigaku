package com.yugentech.jigaku.status.statusRepository

interface StatusRepository {
    suspend fun setStudyStatus(userId: String, isStudying: Boolean): Boolean
    suspend fun getStudyStatus(userId: String): Boolean
    suspend fun getAllStatuses(): Map<String, Boolean>
}