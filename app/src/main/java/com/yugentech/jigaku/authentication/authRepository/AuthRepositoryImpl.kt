package com.yugentech.jigaku.authentication.authRepository

import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.auth.FirebaseUser
import com.yugentech.jigaku.authentication.authUtils.AuthResult
import com.yugentech.jigaku.authentication.AuthService

/**
 * Implementation of the AuthRepository interface.
 * Delegates authentication operations to AuthService.
 *
 * Acts as an abstraction layer between the ViewModel and AuthService,
 * allowing easier testing, mocking, and separation of concerns.
 */
class AuthRepositoryImpl(
    private val authService: AuthService // Dependency injected AuthService instance
) : AuthRepository {

    /**
     * Signs up a new user with the provided credentials.
     * Delegates to AuthService.
     */
    override suspend fun signUp(name: String, email: String, password: String): AuthResult<FirebaseUser> {
        return authService.signUp(name, email, password)
    }

    /**
     * Signs in a user with email and password.
     * Delegates to AuthService.
     */
    override suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        return authService.signIn(email, password)
    }

    /**
     * Retrieves the currently authenticated Firebase user, if any.
     */
    override fun getCurrentUser(): AuthResult<FirebaseUser> {
        return authService.getCurrentUser()
    }

    /**
     * Signs out the currently authenticated user.
     */
    override fun signOut() {
        authService.signOut()
    }

    /**
     * Initiates Google One Tap Sign-In and returns a PendingIntent.
     */
    override suspend fun getGoogleSignInIntent(webClientId: String): AuthResult<PendingIntent> {
        return authService.getGoogleSignInIntent(webClientId)
    }

    /**
     * Handles the result returned from Google One Tap Sign-In and logs the user into Firebase.
     */
    override suspend fun handleGoogleSignInResult(data: Intent?): AuthResult<FirebaseUser> {
        return authService.handleGoogleSignInResult(data)
    }
}