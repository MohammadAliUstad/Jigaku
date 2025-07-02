package com.yugentech.jigaku.dependencyInjection

import android.app.Application
import com.google.firebase.FirebaseApp
import com.yugentech.jigaku.dependencyInjection.modules.authModule
import com.yugentech.jigaku.dependencyInjection.modules.sessionModule
import com.yugentech.jigaku.dependencyInjection.modules.statusModule
import com.yugentech.jigaku.dependencyInjection.modules.userModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

/**
 * Custom Application class for Jigaku.
 *
 * This class initializes:
 * - Firebase SDK (for Auth, Firestore, etc.)
 * - Koin DI framework with all required modules
 */
class JigakuApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase before any Firebase service is used
        FirebaseApp.initializeApp(this)

        // Start Koin for Dependency Injection
        startKoin {
            androidLogger() // Optional: logs Koin messages for debugging
            androidContext(this@JigakuApp) // Provide Android context to Koin

            // Load DI modules: separates concerns by feature
            modules(
                authModule,    // AuthService, AuthRepository, AuthViewModel
                sessionModule, // SessionService, SessionRepository, SessionViewModel
                statusModule,  // Realtime presence/status handling
                userModule     // UserService, UserRepository, UserViewModel
            )
        }
    }
}