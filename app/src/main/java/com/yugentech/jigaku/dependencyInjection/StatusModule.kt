package com.yugentech.jigaku.di

import com.yugentech.jigaku.status.statusRepository.StatusRepository
import com.yugentech.jigaku.status.statusRepository.StatusRepositoryImpl
import com.yugentech.jigaku.status.StatusService
import com.yugentech.jigaku.status.StatusViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val statusModule = module {

    single { StatusService() }

    single<StatusRepository> { StatusRepositoryImpl(get()) }

    viewModel { StatusViewModel(get()) }
}