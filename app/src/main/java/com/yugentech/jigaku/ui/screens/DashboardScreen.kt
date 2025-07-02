package com.yugentech.jigaku.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.jigaku.session.SessionViewModel
import com.yugentech.jigaku.status.StatusViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    sessionViewModel: SessionViewModel = koinViewModel(),
    statusViewModel: StatusViewModel = koinViewModel(),
    userId: String
) {
    val isStudying by sessionViewModel.isStudying.collectAsStateWithLifecycle()
    val selectedDuration by sessionViewModel.selectedDuration.collectAsStateWithLifecycle()
    val currentTime by sessionViewModel.currentTime.collectAsStateWithLifecycle()

    val animatedProgress by animateFloatAsState(
        targetValue = 1f - (currentTime / selectedDuration.toFloat()),
        animationSpec = tween(500),
        label = "TimerProgress"
    )

    // Only load total time once on initial composition
    LaunchedEffect(Unit) {
        sessionViewModel.loadTotalTime(userId, forceRefresh = false)
    }

    // Handle study status changes
    LaunchedEffect(isStudying) {
        if (isStudying) {
            statusViewModel.setUserStatus(userId, true)
            sessionViewModel.startTimer()
        } else {
            statusViewModel.setUserStatus(userId, false)
            sessionViewModel.stopTimer()
        }
    }

    // Handle timer completion
    LaunchedEffect(currentTime) {
        if (currentTime == 0 && isStudying) {
            sessionViewModel.stopTimer()
            // Save session will automatically update total time
            sessionViewModel.saveSession(userId, selectedDuration)
            sessionViewModel.resetTimer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Jigaku",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = onNavigateToLeaderboard) {
                    Icon(
                        Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onNavigateToProfile) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile"
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isStudying) {
                        sessionViewModel.stopTimer()
                    } else {
                        sessionViewModel.resetTimer()
                        sessionViewModel.startTimer()
                    }
                }
            ) {
                Icon(
                    imageVector = if (isStudying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isStudying) "Stop Timer" else "Start Timer"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration Selection
            Text(
                "Select Duration",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Duration Buttons
            Row(horizontalArrangement = Arrangement.Center) {
                listOf(25, 50).forEach { min ->
                    OutlinedButton(
                        onClick = {
                            sessionViewModel.updateSelectedDuration(min)
                            sessionViewModel.resetTimer()
                        },
                        enabled = !isStudying,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("${min}m")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Display
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(240.dp),
                    strokeWidth = 12.dp
                )
                Text(
                    text = String.format("%02d:%02d", currentTime / 60, currentTime % 60),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Buttons
            if (isStudying) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(
                        onClick = { sessionViewModel.stopTimer() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("Stop")
                    }

                    Button(
                        onClick = {
                            sessionViewModel.stopTimer()
                            val elapsedTime = sessionViewModel.getElapsedTime()
                            if (elapsedTime > 0) {
                                sessionViewModel.saveSession(userId, elapsedTime)
                            }
                            sessionViewModel.resetTimer()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}