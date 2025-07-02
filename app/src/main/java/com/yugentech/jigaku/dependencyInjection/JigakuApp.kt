package com.yugentech.jigaku.di

import android.app.Application
import com.google.firebase.FirebaseApp
import com.yugentech.jigaku.dependencyInjection.sessionModule
import com.yugentech.jigaku.dependencyInjection.userModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class JigakuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        startKoin {
            androidLogger()
            androidContext(this@JigakuApp)
            modules(
                authModule,
                sessionModule,
                statusModule,
                userModule
            )
        }
    }
}