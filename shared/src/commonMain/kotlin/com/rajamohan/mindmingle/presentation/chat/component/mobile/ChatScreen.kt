package com.rajamohan.mindmingle.presentation.chat.component.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.presentation.chat.viewmodel.ChatEvent
import com.rajamohan.mindmingle.presentation.chat.viewmodel.ChatViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.common.icon.SendIcon
import com.rajamohan.mindmingle.presentation.common.icon.WaveIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private val chatAvatarPalettes = listOf(
    listOf(Color(0xFFFF4081), Color(0xFFFF80AB)),
    listOf(Color(0xFF00C853), Color(0xFFB9F6CA)),
    listOf(Color(0xFF651FFF), Color(0xFFB388FF)),
    listOf(Color(0xFFFF6D00), Color(0xFFFFD180))
)

private fun chatPaletteFor(uid: String): List<Color> =
    chatAvatarPalettes[(uid.hashCode().let { if (it < 0) -it else it }) % chatAvatarPalettes.size]

@Composable
fun ChatScreen(uid: String) {
    val viewModel: ChatViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.onEvent(ChatEvent.LoadConversations(uid))
    }

    val activeConversation = uiState.activeConversation
    if (activeConversation != null) {
        ChatThreadContent(
            uid = uid,
            conversation = activeConversation,
            messages = uiState.messages,
            onSend = { text ->
                viewModel.onEvent(
                    ChatEvent.SendMessage(matchId = activeConversation.matchId, senderId = uid, text = text)
                )
            },
            onBack = { viewModel.onEvent(ChatEvent.CloseConversation) }
        )
    } else {
        ChatConversationListContent(
            isLoading = uiState.isLoadingConversations,
            conversations = uiState.conversations,
            onOpenConversation = { matchId -> viewModel.onEvent(ChatEvent.OpenConversation(matchId)) }
        )
    }
}

@Composable
private fun ChatConversationListContent(
    isLoading: Boolean,
    conversations: List<ChatConversation>,
    onOpenConversation: (matchId: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical)
        ) {
            Text(
                text = "Messages & Chat",
                style = typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                conversations.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ChatBubbleIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No conversations yet",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Match with a tech partner in Discover to start chatting",
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(conversations, key = { it.matchId }) { item ->
                            Surface(
                                onClick = { onOpenConversation(item.matchId) },
                                shape = RoundedCornerShape(18.dp),
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
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(chatPaletteFor(item.otherUser.uid))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(24.dp))
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.otherUser.name,
                                            style = typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.lastMessage.ifBlank { "You matched — say hi!" },
                                            style = typography.bodyMedium,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1
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

@Composable
private fun ChatThreadContent(
    uid: String,
    conversation: ChatConversation,
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var draftText by remember { mutableStateOf("") }
    val listState: LazyListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
        ) {
            // Thread header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(chatPaletteFor(conversation.otherUser.uid))),
                    contentAlignment = Alignment.Center
                ) {
                    DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = conversation.otherUser.name,
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = conversation.otherUser.occupation,
                        style = typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WaveIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You matched with ${conversation.otherUser.name}. Say hi!",
                                style = typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    val isMine = message.senderId == uid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
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

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    text = "Message ${conversation.otherUser.name}…",
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
                            onSend(draftText)
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
