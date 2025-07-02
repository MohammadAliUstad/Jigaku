package com.yugentech.jigaku.session

import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.jigaku.models.Session
import kotlinx.coroutines.tasks.await

class SessionService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun saveSession(userId: String, session: Session): SessionResult<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .add(session.toMap())
                .await()
            SessionResult.Success(Unit)
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Failed to save session")
        }
    }

    suspend fun getSessions(userId: String): SessionResult<List<Session>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { doc ->
                Session.fromMap(doc.data ?: emptyMap())
            }

            SessionResult.Success(sessions)
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Failed to fetch sessions")
        }
    }

    suspend fun updateTotalTime(userId: String, additionalSeconds: Int): SessionResult<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentTotal = snapshot.getLong("totalTimeStudied") ?: 0
                transaction.update(userRef, "totalTimeStudied", currentTotal + additionalSeconds)
            }.await()
            SessionResult.Success(Unit)
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Failed to update total time")
        }
    }

    suspend fun getTotalTime(userId: String): SessionResult<Long> {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val total = doc.getLong("totalTimeStudied") ?: 0
            SessionResult.Success(total)
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Failed to retrieve total time")
        }
    }
}