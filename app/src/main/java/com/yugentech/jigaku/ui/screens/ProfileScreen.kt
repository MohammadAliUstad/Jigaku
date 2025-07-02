package com.yugentech.jigaku.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yugentech.jigaku.authentication.AuthViewModel
import com.yugentech.jigaku.session.SessionViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToAbout: () -> Unit,
    authViewModel: AuthViewModel,
    sessionViewModel: SessionViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val formattedStudyTime by sessionViewModel.formattedStudyTime.collectAsStateWithLifecycle()
    val user = authState.userData

    // Load total time only once when the user is available
    LaunchedEffect(Unit) {
        user?.userId?.let { userId ->
            sessionViewModel.loadTotalTime(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Your Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (user != null) {
                // Profile Picture
                user.profilePictureUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Username
                Text(
                    text = user.username ?: "Anonymous",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Email
                Text(
                    text = user.email ?: "No email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Study Time Display
                StudyTimeSection(
                    isLoaded = sessionState.isTotalTimeLoaded,
                    formattedTime = formattedStudyTime
                )
            } else {
                Text(
                    text = "User not logged in",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StudyTimeSection(
    isLoaded: Boolean,
    formattedTime: String
) {
    if (!isLoaded) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
    } else {
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}