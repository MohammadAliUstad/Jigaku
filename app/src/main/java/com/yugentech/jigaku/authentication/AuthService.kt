@file:Suppress("DEPRECATION") // Suppresses deprecated API warnings in this file

package com.yugentech.jigaku.authentication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.jigaku.authentication.authUtils.AuthResult
import kotlinx.coroutines.tasks.await

/**
 * AuthService handles all authentication-related functionality,
 * including email/password sign up, sign in, Google sign-in, and user Firestore setup.
 */
class AuthService(context: Context) {

    // Firebase Authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Firestore instance for saving user metadata
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Google One Tap sign-in client
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    /**
     * Registers a new user using email and password and stores additional metadata in Firestore.
     *
     * @param name Display name of the user
     * @param email Email address
     * @param password Password
     * @return AuthResult containing FirebaseUser on success or error message
     */
    suspend fun signUp(name: String, email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            // Create a user with email and password
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            // Set display name
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            ).await()

            // Add user data to Firestore
            addUser(user, name)

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("Sign up failed: ${e.message}")
        }
    }

    /**
     * Logs in the user with email and password.
     *
     * @return AuthResult containing FirebaseUser on success or error message
     */
    suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            AuthResult.Error("Sign in failed: ${e.message}")
        }
    }

    /**
     * Returns the currently signed-in Firebase user.
     *
     * @return AuthResult containing FirebaseUser or error if not signed in
     */
    fun getCurrentUser(): AuthResult<FirebaseUser> {
        val user = auth.currentUser
        return if (user != null) AuthResult.Success(user)
        else AuthResult.Error("No user is currently signed in")
    }

    /**
     * Signs out the currently authenticated user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Initiates the Google One Tap Sign-In flow and returns a PendingIntent.
     *
     * @param webClientId The server-side Web Client ID from Google Cloud console
     * @return AuthResult containing PendingIntent for Google Sign-In or error
     */
    suspend fun getGoogleSignInIntent(webClientId: String): AuthResult<PendingIntent> {
        return try {
            // Build the sign-in request with ID token options
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

            // Get pending intent to launch Google Sign-In UI
            val result = oneTapClient.beginSignIn(signInRequest).await()
            AuthResult.Success(result.pendingIntent)
        } catch (e: Exception) {
            AuthResult.Error("Google Sign-In failed: ${e.message}")
        }
    }

    /**
     * Handles the result from Google One Tap Sign-In and authenticates with Firebase.
     *
     * @param data The intent returned by One Tap
     * @return AuthResult containing FirebaseUser or error
     */
    suspend fun handleGoogleSignInResult(data: Intent?): AuthResult<FirebaseUser> {
        return try {
            if (data == null) return AuthResult.Error("No data received from Google Sign-In")

            // Extract ID token and authenticate with Firebase
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken ?: throw Exception("Google ID token is null")
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            val user = result.user!!
            val isNewUser = result.additionalUserInfo?.isNewUser == true

            // If new user, add to Firestore
            if (isNewUser) {
                addUser(user, user.displayName ?: "No Name")
            }

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("Google Sign-In failed: ${e.message}")
        }
    }

    /**
     * Adds user information to Firestore under "users/{uid}".
     *
     * @param user The FirebaseUser object
     * @param name Display name of the user
     */
    private suspend fun addUser(user: FirebaseUser, name: String) {
        val userData = mapOf(
            "uid" to user.uid,
            "name" to name,
            "email" to user.email,
            "totalTimeStudied" to 0L // Default value for study time
        )
        firestore.collection("users").document(user.uid).set(userData).await()
    }
}