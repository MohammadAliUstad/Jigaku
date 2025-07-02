package com.yugentech.jigaku.dependencyInjection

import com.yugentech.jigaku.user.userRepository.UserRepository
import com.yugentech.jigaku.user.userRepository.UserRepositoryImpl
import com.yugentech.jigaku.user.UserService
import com.yugentech.jigaku.user.UserViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val userModule = module {

    single { UserService() }

    single<UserRepository> { UserRepositoryImpl(get()) }

    viewModel { UserViewModel(get()) }
}