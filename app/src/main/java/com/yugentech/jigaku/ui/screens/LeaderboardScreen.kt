package com.yugentech.jigaku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.jigaku.models.User
import com.yugentech.jigaku.ui.components.RankNumber
import com.yugentech.jigaku.ui.components.StudyingIndicator
import com.yugentech.jigaku.ui.components.UserInfo
import com.yugentech.jigaku.user.UserViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: UserViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startListeningToUserStatuses()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListeningToUserStatuses()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshUserStatuses() }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                when {
                    uiState.isLoading && uiState.users.isEmpty() -> {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    }

                    uiState.error != null && uiState.users.isEmpty() -> {
                        ErrorMessage(
                            error = uiState.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.users.isEmpty() -> {
                        EmptyStateMessage(Modifier.align(Alignment.Center))
                    }

                    else -> {
                        LeaderboardContent(
                            users = uiState.users,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 2.dp
    )
}

@Composable
private fun ErrorMessage(error: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = error ?: "An unknown error occurred",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "No users found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LeaderboardContent(
    users: List<User>,
    viewModel: UserViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        itemsIndexed(
            items = users.sortedByDescending { it.totalTimeStudied },
            key = { _, user -> user.userId }
        ) { index, user ->
            UserLeaderboardCard(
                rank = index + 1,
                user = user,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun UserLeaderboardCard(
    rank: Int,
    user: User,
    viewModel: UserViewModel
) {
    val formattedTime = viewModel.getFormattedTimeForUser(user.userId)
    val isTopThree = rank <= 3

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isTopThree)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isTopThree) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RankNumber(rank, isTopThree)
            UserInfo(
                user.name,
                formattedTime,
                isTopThree,
                modifier = Modifier.weight(1f)
            )
            if (user.isStudying) {
                StudyingIndicator(isTopThree)
            }
        }
    }
}