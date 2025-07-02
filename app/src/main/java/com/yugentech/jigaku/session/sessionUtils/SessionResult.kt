package com.yugentech.jigaku.session

sealed class SessionResult<out T> {
    data class Success<out T>(val data: T) : SessionResult<T>()
    data class Error(val message: String) : SessionResult<Nothing>()
    data object Loading : SessionResult<Nothing>()
}