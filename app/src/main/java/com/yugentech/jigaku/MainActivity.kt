package com.yugentech.jigaku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.yugentech.jigaku.authentication.AuthViewModel
import com.yugentech.jigaku.navigation.AppNavHost
import com.yugentech.jigaku.session.SessionViewModel
import com.yugentech.jigaku.ui.theme.JigakuTheme
import com.yugentech.jigaku.user.UserViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            JigakuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val webClientId = getString(R.string.web_client_id)
                    val authViewModel: AuthViewModel = koinViewModel()
                    val sessionViewModel: SessionViewModel = koinViewModel()
                    val userViewModel: UserViewModel = koinViewModel()

                    AppNavHost(
                        navController = navController,
                        webClientId = webClientId,
                        authViewModel = authViewModel,
                        sessionViewModel = sessionViewModel,
                        userViewModel = userViewModel
                    )
                }
            }
        }
    }
}