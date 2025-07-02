package com.yugentech.jigaku.user.userRepository

import com.yugentech.jigaku.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getAllUsers(): Result<List<User>>
    suspend fun getAllUsersWithStatuses(): Result<List<User>>
    fun observeUserStatuses(): Flow<Map<String, Boolean>>
}