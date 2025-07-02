package com.yugentech.jigaku.dependencyInjection

import com.yugentech.jigaku.session.sessionRepository.SessionRepository
import com.yugentech.jigaku.session.sessionRepository.SessionRepositoryImpl
import com.yugentech.jigaku.session.SessionService
import com.yugentech.jigaku.session.SessionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sessionModule = module {

    single { SessionService() }

    single<SessionRepository> { SessionRepositoryImpl(get()) }

    viewModel { SessionViewModel(get()) }
}