package com.yugentech.jigaku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.jigaku.models.User
import com.yugentech.jigaku.user.UserViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: UserViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Set up real-time updates
    LaunchedEffect(Unit) {
        viewModel.startListeningToUserStatuses()
    }

    // Cleanup listeners when leaving screen
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
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
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
    Text(
        text = error ?: "An unknown error occurred",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}

@Composable
private fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Text(
        text = "No users found",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun LeaderboardContent(
    users: List<User>,
    viewModel: UserViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        itemsIndexed(
            items = users.sortedByDescending { it.totalTimeStudied },
            key = { _, user -> user.userId } // Stable key for better performance
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RankNumber(rank)
            UserInfo(
                user.name,
                formattedTime,
                modifier = Modifier.weight(1f)
            )
            if (user.isStudying) {
                StudyingIndicator()
            }
        }
    }
}

@Composable
private fun RankNumber(rank: Int) {
    Text(
        text = "#$rank",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.width(48.dp)
    )
}

@Composable
private fun UserInfo(
    name: String,
    formattedTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudyingIndicator() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}