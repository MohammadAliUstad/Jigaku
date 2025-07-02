package com.yugentech.jigaku.status

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Service class responsible for managing user study status
 * in Firebase Realtime Database.
 *
 * Status is stored under the "studyStatus" node in the format:
 * "studyStatus/{userId}" → true | false
 */
class StatusService(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance() // Default Realtime DB instance
) {

    /**
     * Sets the study status (true/false) for a specific user.
     *
     * @param userId The ID of the user.
     * @param isStudying Whether the user is currently studying.
     * @return true if the operation was successful, false otherwise.
     */
    suspend fun setStudyStatus(userId: String, isStudying: Boolean): Boolean {
        return try {
            db.getReference("studyStatus")
                .child(userId)
                .setValue(isStudying)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Retrieves the current study status of a specific user.
     *
     * @param userId The ID of the user.
     * @return true if the user is currently studying, false otherwise or if failed.
     */
    suspend fun getStudyStatus(userId: String): Boolean {
        return try {
            val snapshot = db.getReference("studyStatus")
                .child(userId)
                .get()
                .await()
            snapshot.getValue(Boolean::class.java) == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Retrieves the study status of all users.
     *
     * @return A map of userId to isStudying boolean. Empty map on failure.
     */
    suspend fun getAllStatuses(): Map<String, Boolean> {
        return try {
            val snapshot = db.getReference("studyStatus")
                .get()
                .await()
            snapshot.children.associate {
                it.key!! to (it.getValue(Boolean::class.java) == true)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}