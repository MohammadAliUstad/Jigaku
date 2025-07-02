package com.yugentech.jigaku.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.jigaku.session.SessionViewModel
import com.yugentech.jigaku.status.StatusViewModel

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    sessionViewModel: SessionViewModel,
    statusViewModel: StatusViewModel,
    userId: String
) {
    val isStudying by sessionViewModel.isStudying.collectAsStateWithLifecycle()
    val selectedDuration by sessionViewModel.selectedDuration.collectAsStateWithLifecycle()
    val currentTime by sessionViewModel.currentTime.collectAsStateWithLifecycle()

    // Animations
    val animatedProgress by animateFloatAsState(
        targetValue = 1f - (currentTime / selectedDuration.toFloat()),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "TimerProgress"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "Pulse")
    val scale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    LaunchedEffect(isStudying) {
        if (isStudying) {
            statusViewModel.setUserStatus(userId, true)
        } else {
            statusViewModel.setUserStatus(userId, false)
        }
    }

    LaunchedEffect(currentTime) {
        if (currentTime == 0 && isStudying) {
            sessionViewModel.stopTimer()
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
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToLeaderboard,
                    icon = {
                        Icon(Icons.Default.Leaderboard, "Leaderboard")
                    },
                    label = { Text("Leaderboard") }
                )
                NavigationBarItem(
                    selected = isStudying,
                    onClick = {
                        if (isStudying) {
                            sessionViewModel.stopTimer()
                        } else {
                            sessionViewModel.startTimer()
                        }
                    },
                    icon = {
                        Icon(
                            if (isStudying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isStudying) "Stop" else "Start"
                        )
                    },
                    label = { Text(if (isStudying) "Stop" else "Start") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = {
                        Icon(Icons.Default.AccountCircle, "Profile")
                    },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer Section
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Background Progress
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(280.dp),
                        strokeWidth = 16.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    // Actual Progress
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(280.dp),
                        strokeWidth = 16.dp,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )

                    // Timer Display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(if (isStudying) scale else 1f)
                    ) {
                        AnimatedContent(
                            targetState = currentTime,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "TimeDisplay"
                        ) { time ->
                            Text(
                                text = String.format("%02d:%02d", time / 60, time % 60),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            "minutes remaining",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                }

                // Duration Selection
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Select Duration",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(25, 50).forEach { min ->
                                Button(
                                    onClick = {
                                        sessionViewModel.updateSelectedDuration(min)
                                        sessionViewModel.resetTimer()
                                    },
                                    enabled = !isStudying,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedDuration == min * 60)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContainerColor = if (selectedDuration == min * 60)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        "$min min",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selectedDuration == min * 60)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Control Buttons
                AnimatedVisibility(
                    visible = isStudying,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sessionViewModel.stopTimer() },
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}