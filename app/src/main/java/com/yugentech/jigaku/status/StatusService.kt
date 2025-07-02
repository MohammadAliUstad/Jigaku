package com.yugentech.jigaku.status

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class StatusService(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    suspend fun setStudyStatus(userId: String, isStudying: Boolean): Boolean {
        return try {
            db.getReference("studyStatus")
                .child(userId)
                .setValue(isStudying)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStudyStatus(userId: String): Boolean {
        return try {
            val snapshot = db.getReference("studyStatus")
                .child(userId)
                .get()
                .await()
            snapshot.getValue(Boolean::class.java) == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllStatuses(): Map<String, Boolean> {
        return try {
            val snapshot = db.getReference("studyStatus")
                .get()
                .await()
            snapshot.children.associate {
                it.key!! to (it.getValue(Boolean::class.java) == true)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}