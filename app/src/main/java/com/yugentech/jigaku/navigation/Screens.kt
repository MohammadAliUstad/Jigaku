package com.yugentech.jigaku.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screens(val route: String, val title: String, val icon: ImageVector) {
    data object Login : Screens("login", "Login", Icons.AutoMirrored.Filled.Login)
    data object Dashboard : Screens("dashboard", "Dashboard", Icons.Filled.Dashboard)
    data object Profile : Screens("profile", "Profile", Icons.Filled.AccountCircle)
    data object Leaderboard : Screens("leaderboard", "Leaderboard", Icons.Filled.Leaderboard)
    data object About : Screens("about", "About", Icons.Filled.Info)
}