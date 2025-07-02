package com.yugentech.jigaku.user.userRepository

import com.yugentech.jigaku.models.User
import com.yugentech.jigaku.user.UserService
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of UserRepository.
 *
 * Acts as a middle layer between ViewModels and the UserService,
 * encapsulating logic for user retrieval and real-time status updates.
 */
class UserRepositoryImpl(
    private val userService: UserService // Injected service that handles Firebase operations
) : UserRepository {

    /**
     * Retrieves all users from Firestore.
     */
    override suspend fun getAllUsers(): Result<List<User>> {
        return userService.getAllUsers()
    }

    /**
     * Retrieves all users along with their current 'isStudying' status
     * by combining Firestore user data with Realtime Database statuses.
     */
    override suspend fun getAllUsersWithStatuses(): Result<List<User>> {
        return userService.getAllUsersWithStatuses()
    }

    /**
     * Observes real-time updates to users' study statuses using a Flow.
     * This can be collected to keep UI synced with status changes.
     */
    override fun observeUserStatuses(): Flow<Map<String, Boolean>> {
        return userService.observeUserStatuses()
    }
}