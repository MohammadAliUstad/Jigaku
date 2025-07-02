package com.yugentech.jigaku.authentication.authUtils

data class UserData(
    val userId: String = "",
    val username: String? = null,
    val email: String? = null,
    val profilePictureUrl: String? = null
)