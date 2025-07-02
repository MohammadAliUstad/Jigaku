package com.yugentech.jigaku.session

import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.jigaku.models.Session
import com.yugentech.jigaku.session.sessionUtils.SessionResult
import kotlinx.coroutines.tasks.await

/**
 * SessionService is responsible for all Firestore operations related to user study sessions.
 * This includes saving sessions, retrieving session history, updating total study time, etc.
 */
class SessionService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Saves a single study session to the Firestore under the user's "sessions" subcollection.
     *
     * @param userId ID of the user the session belongs to
     * @param session The session data to be saved
     * @return SessionResult indicating success or failure
     */
    suspend fun saveSession(userId: String, session: Session): SessionResult<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .add(session.toMap()) // Converts session object to a map
                .await()

            SessionResult.Success(Unit)
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Failed to save session")
        }
    }

    /**
     * Retrieves all study sessions associated with the given user.
     *
     * @param userId The ID of the user whose sessions are to be retrieved
     * @return SessionResult containing a list of Session objects or an error
     */
    suspend fun getSessions(userId: String): SessionResult<List<Session>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .get()
                .await()

            // Convert each document into a Session object
            val sessions = snapshot.documents.mapNotNull { doc ->
                Session.fromMap(doc.data ?: emptyMap())
            }

            SessionResult.Success(sessions)
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Failed to fetch sessions")
        }
    }

    /**
     * Atomically updates the user's total study time in Firestore.
     *
     * @param userId The ID of the user whose total time is being updated
     * @param additionalSeconds Number of seconds to add to the total
     * @return SessionResult indicating success or failure
     */
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

    /**
     * Retrieves the total time studied by the user from Firestore.
     *
     * @param userId ID of the user
     * @return SessionResult containing the total study time in seconds, or an error
     */
    suspend fun getTotalTime(userId: String): SessionResult<Long> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val total = doc.getLong("totalTimeStudied") ?: 0
            SessionResult.Success(total)
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Failed to retrieve total time")
        }
    }
}