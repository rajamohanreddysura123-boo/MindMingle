package com.rajamohan.mindmingle.presentation.admin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.SendIcon
import com.rajamohan.mindmingle.presentation.support.viewmodel.SupportChatEvent
import com.rajamohan.mindmingle.presentation.support.viewmodel.SupportChatViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/** Admin-desktop-only: reply to one user's Help & Support thread. Replies are written with senderId="support". */
@Composable
fun AdminSupportChatScreen(uid: String, userName: String, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: SupportChatViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.onEvent(SupportChatEvent.Open(uid))
    }

    var draftText by remember { mutableStateOf("") }
    val listState: LazyListState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
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
                Column {
                    Text(
                        text = userName.ifBlank { "(no name)" },
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(text = "Support conversation", style = typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No messages yet", style = typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    val isFromAdmin = message.senderId == "support"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isFromAdmin) Arrangement.End else Arrangement.Start
                    ) {
                        Column(horizontalAlignment = if (isFromAdmin) Alignment.End else Alignment.Start) {
                            Text(
                                text = if (isFromAdmin) "You" else userName.ifBlank { "User" },
                                style = typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isFromAdmin) 16.dp else 4.dp,
                                    bottomEnd = if (isFromAdmin) 4.dp else 16.dp
                                ),
                                color = if (isFromAdmin) colors.primary else colors.surface,
                                modifier = Modifier.widthIn(max = 420.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    style = typography.bodyMedium,
                                    color = if (isFromAdmin) colors.onPrimary else colors.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (draftText.isEmpty()) {
                                Text(
                                    text = "Reply to $userName…",
                                    style = typography.bodyMedium,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    onClick = {
                        if (draftText.isNotBlank()) {
                            viewModel.onEvent(SupportChatEvent.Send(uid = uid, userName = userName, senderId = "support", text = draftText))
                            draftText = ""
                        }
                    },
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SendIcon(color = colors.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
