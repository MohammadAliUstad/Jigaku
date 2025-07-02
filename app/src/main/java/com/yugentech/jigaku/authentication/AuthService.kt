@file:Suppress("DEPRECATION")

package com.yugentech.jigaku.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthService(context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    suspend fun signUp(name: String, email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            user.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            ).await()

            addUser(user, name)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("Sign up failed: ${e.message}")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            AuthResult.Error("Sign in failed: ${e.message}")
        }
    }

    fun getCurrentUser(): AuthResult<FirebaseUser> {
        val user = auth.currentUser
        return if (user != null) AuthResult.Success(user)
        else AuthResult.Error("No user is currently signed in")
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun getGoogleSignInIntent(webClientId: String): AuthResult<PendingIntent> {
        return try {
            val signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .setAutoSelectEnabled(false)
                .build()

            val result = oneTapClient.beginSignIn(signInRequest).await()
            AuthResult.Success(result.pendingIntent)
        } catch (e: Exception) {
            AuthResult.Error("Google Sign-In failed: ${e.message}")
        }
    }

    suspend fun handleGoogleSignInResult(data: Intent?): AuthResult<FirebaseUser> {
        return try {
            if (data == null) return AuthResult.Error("No data received from Google Sign-In")

            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken ?: throw Exception("Google ID token is null")
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            val user = result.user!!
            val isNewUser = result.additionalUserInfo?.isNewUser == true

            if (isNewUser) {
                addUser(user, user.displayName ?: "No Name")
            }

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("Google Sign-In failed: ${e.message}")
        }
    }

    private suspend fun addUser(user: FirebaseUser, name: String) {
        val userData = mapOf(
            "uid" to user.uid,
            "name" to name,
            "email" to user.email,
            "totalTimeStudied" to 0L,
        )
        firestore.collection("users").document(user.uid).set(userData).await()
    }
}