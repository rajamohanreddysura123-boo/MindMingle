package com.rajamohan.mindmingle.presentation.support.component

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
import com.rajamohan.mindmingle.presentation.common.icon.HelpCircleIcon
import com.rajamohan.mindmingle.presentation.common.icon.SendIcon
import com.rajamohan.mindmingle.presentation.support.viewmodel.SupportChatEvent
import com.rajamohan.mindmingle.presentation.support.viewmodel.SupportChatViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/** Reached from Profile > Help & Support. One thread per user with the MindMingle team; replies land in the admin desktop's Support Chats inbox. */
@Composable
fun SupportChatScreen(uid: String, userName: String, onBack: () -> Unit) {
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
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .safeContentPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    HelpCircleIcon(color = colors.primary, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(text = "Help & Support", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Text(text = "We usually reply within a day", style = typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HelpCircleIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tell us what's going on — a real person on the MindMingle team will reply here.",
                                style = typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    val isMine = message.senderId == uid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                            if (!isMine) {
                                Text(
                                    text = "Support",
                                    style = typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 14.dp, bottom = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMine) 16.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 16.dp
                                ),
                                color = if (isMine) colors.primary else colors.surface,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    style = typography.bodyMedium,
                                    color = if (isMine) colors.onPrimary else colors.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface)
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
                                    text = "Describe your issue…",
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
                            viewModel.onEvent(SupportChatEvent.Send(uid = uid, userName = userName, senderId = uid, text = draftText))
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
