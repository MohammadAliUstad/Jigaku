@file:Suppress("DEPRECATION")

package com.yugentech.jigaku.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.yugentech.jigaku.authentication.AuthViewModel
import com.yugentech.jigaku.session.SessionViewModel
import com.yugentech.jigaku.ui.screens.AboutScreen
import com.yugentech.jigaku.ui.screens.DashboardScreen
import com.yugentech.jigaku.ui.screens.LeaderboardScreen
import com.yugentech.jigaku.ui.screens.LoginScreen
import com.yugentech.jigaku.ui.screens.ProfileScreen
import com.yugentech.jigaku.user.UserViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    sessionViewModel: SessionViewModel,
    userViewModel: UserViewModel,
    webClientId: String
) {
    val state by authViewModel.authState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                authViewModel.handleGoogleSignInResult(result.data)
            }
        }
    )

    LaunchedEffect(state.pendingIntent) {
        state.pendingIntent?.let {
            launcher.launch(IntentSenderRequest.Builder(it).build())
        }
    }

    val startDestination =
        if (state.isUserLoggedIn) {
            Screens.Dashboard.route
        } else {
            Screens.Login.route
        }

    val enterTransition = fadeIn(
        animationSpec = tween(300)
    ) + slideInHorizontally(
        animationSpec = tween(300),
        initialOffsetX = { it }
    )

    val exitTransition = fadeOut(
        animationSpec = tween(300)
    ) + slideOutHorizontally(
        animationSpec = tween(300),
        targetOffsetX = { -it }
    )

    val popEnterTransition = fadeIn(
        animationSpec = tween(300)
    ) + slideInHorizontally(
        animationSpec = tween(300),
        initialOffsetX = { -it }
    )

    val popExitTransition = fadeOut(
        animationSpec = tween(300)
    ) + slideOutHorizontally(
        animationSpec = tween(300),
        targetOffsetX = { it }
    )

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition }
    ) {
        composable(
            route = Screens.Login.route,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            LoginScreen(
                state = state,
                onSignInClick = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onSignUpClick = { name, email, password ->
                    authViewModel.signUp(name = name, email = email, password = password)
                },
                onGoogleSignInClick = {
                    authViewModel.getGoogleSignInIntent(webClientId)
                }
            )
        }

        composable(
            route = Screens.Dashboard.route,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val userId = state.userId
            if (userId != null) {
                DashboardScreen(
                    sessionViewModel = sessionViewModel,
                    userId = userId,
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screens.Login.route) {
                            popUpTo(Screens.Dashboard.route) { inclusive = true }
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screens.Profile.route)
                    },
                    onNavigateToLeaderboard = {
                        navController.navigate(Screens.Leaderboard.route)
                    }
                )
            }
        }

        composable(
            route = Screens.Profile.route,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            ProfileScreen(
                onNavigateToAbout = {
                    navController.navigate(Screens.About.route)
                },
                authViewModel = authViewModel,
                sessionViewModel = sessionViewModel,
            )
        }

        composable(
            route = Screens.Leaderboard.route,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            LeaderboardScreen(viewModel = userViewModel)
        }

        composable(
            route = Screens.About.route,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            AboutScreen(onNavigateBack = navController::popBackStack)
        }
    }
}