package com.yugentech.jigaku.user

import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.jigaku.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Service class responsible for interacting with:
 * - Firestore for static user data
 * - Realtime Database for dynamic study status (online/offline)
 */
class UserService {

    // Firestore instance (used for fetching user profile and study data)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Realtime Database instance (used for tracking live user study statuses)
    private val realtimeDb: FirebaseDatabase = FirebaseDatabase.getInstance()

    // Reference to user statuses (e.g., userId → isStudying)
    private val statusRef = realtimeDb.getReference("studyStatus")

    // Reference to Firestore "users" collection
    private val usersRef = firestore.collection("users")

    /**
     * Fetches all users from Firestore.
     * Converts documents into User model using fromMap.
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersRef.get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                User.fromMap(doc.id, doc.data ?: return@mapNotNull null)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches users from Firestore and merges their study statuses from Realtime DB.
     * Useful for leaderboards or presence-aware UIs.
     */
    suspend fun getAllUsersWithStatuses(): Result<List<User>> {
        return try {
            // Load Firestore users and Realtime Database statuses
            val usersSnapshot = usersRef.get().await()
            val statusSnapshot = statusRef.get().await()

            // Convert Realtime DB snapshot to userId → isStudying map
            val statusMap = statusSnapshot.children.associate { child ->
                val userId = child.key ?: return@associate null to false
                userId to (child.getValue(Boolean::class.java) == true)
            }

            // Merge status into each User object
            val combinedUsers = usersSnapshot.documents.mapNotNull { doc ->
                val userId = doc.id
                val data = doc.data ?: return@mapNotNull null
                val baseUser = User.fromMap(userId, data)
                val isStudying = statusMap[userId] == true
                baseUser.copy(isStudying = isStudying)
            }

            Result.success(combinedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Provides a real-time flow of user statuses using Firebase's ValueEventListener.
     * This keeps your UI in sync with current study activity across users.
     */
    fun observeUserStatuses(): Flow<Map<String, Boolean>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Map the snapshot to userId → isStudying
                val statusUpdates = snapshot.children.mapNotNull { child ->
                    val userId = child.key ?: return@mapNotNull null
                    val isStudying = child.getValue(Boolean::class.java) == true
                    userId to isStudying
                }.toMap()

                trySend(statusUpdates) // Emit update to Flow
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException()) // Propagate error through flow
            }
        }

        // Attach listener to Realtime DB
        statusRef.addValueEventListener(listener)

        // Clean up when flow is closed (e.g., on ViewModel clear)
        awaitClose {
            statusRef.removeEventListener(listener)
        }
    }
}