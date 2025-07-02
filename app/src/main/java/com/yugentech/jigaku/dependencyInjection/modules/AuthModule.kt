package com.yugentech.jigaku.dependencyInjection.modules

import com.yugentech.jigaku.authentication.AuthService
import com.yugentech.jigaku.authentication.authRepository.AuthRepository
import com.yugentech.jigaku.authentication.authRepository.AuthRepositoryImpl
import com.yugentech.jigaku.authentication.AuthViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {

    single { AuthService(androidContext()) }

    single<AuthRepository> { AuthRepositoryImpl(get()) }

    viewModel { AuthViewModel(get()) }
}