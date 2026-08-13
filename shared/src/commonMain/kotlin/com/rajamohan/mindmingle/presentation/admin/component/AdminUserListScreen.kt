package com.rajamohan.mindmingle.presentation.admin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminUserListViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminUserListScreen(
    onUserClick: (uid: String) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: AdminUserListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .widthIn(max = 900.dp)
                .padding(Spacing.desktopScreenPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Manage Users",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    singleLine = true,
                    textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (uiState.query.isEmpty()) {
                            Text(
                                text = "Search by name, email, or tech stack…",
                                style = typography.bodyMedium,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                uiState.filteredUsers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No users found",
                            style = typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredUsers, key = { it.uid }) { user ->
                            Surface(
                                onClick = { onUserClick(user.uid) },
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DeveloperAvatarIcon(color = colors.primary, modifier = Modifier.size(22.dp))
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name.ifBlank { "(no name)" },
                                            style = typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = user.email.ifBlank { user.phoneNumber },
                                            style = typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }

                                    if (user.isDisabled) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.errorContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "Disabled",
                                                style = typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.error,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
