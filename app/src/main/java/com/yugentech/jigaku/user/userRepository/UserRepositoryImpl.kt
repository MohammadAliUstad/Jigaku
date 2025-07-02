package com.yugentech.jigaku.user.userRepository

import com.yugentech.jigaku.models.User
import com.yugentech.jigaku.user.UserService
import kotlinx.coroutines.flow.Flow

class UserRepositoryImpl(
    private val userService: UserService
) : UserRepository {

    override suspend fun getAllUsers(): Result<List<User>> {
        return userService.getAllUsers()
    }

    override suspend fun getAllUsersWithStatuses(): Result<List<User>> {
        return userService.getAllUsersWithStatuses()
    }

    override fun observeUserStatuses(): Flow<Map<String, Boolean>> {
        return userService.observeUserStatuses()
    }
}