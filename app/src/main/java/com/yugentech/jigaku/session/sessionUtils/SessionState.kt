package com.yugentech.jigaku.session.sessionUtils

data class SessionState(
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val totalTime: Long = 0L,
    val isTotalTimeLoaded: Boolean = false,
    val error: String? = null
)