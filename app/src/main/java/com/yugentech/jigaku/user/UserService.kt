package com.yugentech.jigaku.user

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.jigaku.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val realtimeDb: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val statusRef = realtimeDb.getReference("studyStatus")
    private val usersRef = firestore.collection("users")

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

    suspend fun getAllUsersWithStatuses(): Result<List<User>> {
        return try {
            // Get users from Firestore
            val usersSnapshot = usersRef.get().await()

            // Get current status from Realtime Database
            val statusSnapshot = statusRef.get().await()

            // Create a map of user IDs to their study status
            val statusMap = statusSnapshot.children.associate { child ->
                val userId = child.key ?: return@associate null to false
                userId to (child.getValue(Boolean::class.java) == true)
            }

            // Combine user data with their current status
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

    fun observeUserStatuses(): Flow<Map<String, Boolean>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusUpdates = snapshot.children.associate { child ->
                    val userId = child.key ?: return@associate null to false
                    userId to (child.getValue(Boolean::class.java) == true)
                }.filterKeys { it != null } as Map<String, Boolean>

                trySend(statusUpdates)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        statusRef.addValueEventListener(listener)

        // Clean up listener when Flow collection is cancelled
        awaitClose {
            statusRef.removeEventListener(listener)
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val STATUS_REF = "studyStatus"
    }
}