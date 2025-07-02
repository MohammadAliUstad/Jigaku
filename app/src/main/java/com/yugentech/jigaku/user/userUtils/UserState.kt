package com.yugentech.jigaku.user.userUtils

import com.yugentech.jigaku.models.User

data class UserState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val formattedTimes: Map<String, String> = emptyMap(),
    val refreshing: Boolean = false
)