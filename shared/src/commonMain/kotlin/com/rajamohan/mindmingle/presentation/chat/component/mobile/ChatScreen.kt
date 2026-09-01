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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import com.rajamohan.mindmingle.presentation.common.icon.MaskIcon
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
fun ChatScreen(
    uid: String,
    onOpenAnonymousChat: () -> Unit = {},
    /** Set when a notification tap named a conversation; opened once the list has loaded. */
    openConversationId: String = "",
    onOpenConversationHandled: () -> Unit = {}
) {
    val viewModel: ChatViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.onEvent(ChatEvent.LoadConversations(uid))
    }

    // The conversation cannot be opened before its row exists — OpenConversation looks the
    // conversation up in the loaded list — so this waits for the list rather than firing with uid.
    LaunchedEffect(openConversationId, uiState.conversations) {
        if (openConversationId.isBlank()) return@LaunchedEffect
        if (uiState.conversations.none { it.conversationId == openConversationId }) return@LaunchedEffect
        viewModel.onEvent(ChatEvent.OpenConversation(openConversationId))
        onOpenConversationHandled()
    }

    val activeConversation = uiState.activeConversation
    if (activeConversation != null) {
        ChatThreadContent(
            uid = uid,
            conversation = activeConversation,
            messages = uiState.messages,
            onSend = { text ->
                viewModel.onEvent(
                    ChatEvent.SendMessage(conversationId = activeConversation.conversationId, senderId = uid, text = text)
                )
            },
            onBack = { viewModel.onEvent(ChatEvent.CloseConversation) }
        )
    } else {
        ChatConversationListContent(
            uid = uid,
            isLoading = uiState.isLoadingConversations,
            isLoadingMore = uiState.isLoadingMore,
            hasMore = uiState.hasMore,
            query = uiState.query,
            conversations = uiState.visibleConversations,
            onQueryChange = { viewModel.onEvent(ChatEvent.SearchChanged(it)) },
            onLoadMore = { viewModel.onEvent(ChatEvent.LoadMoreConversations) },
            onOpenConversation = { conversationId -> viewModel.onEvent(ChatEvent.OpenConversation(conversationId)) },
            onOpenAnonymousChat = onOpenAnonymousChat
        )
    }
}

@Composable
private fun ChatConversationListContent(
    uid: String,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    query: String,
    conversations: List<ChatConversation>,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onOpenConversation: (conversationId: String) -> Unit,
    onOpenAnonymousChat: () -> Unit
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
                .safeDrawingPadding()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Messages & Chat",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                // Anonymous chat used to be its own bottom-nav tab. It lives here now: it is a way
                // of chatting, not a separate destination, and the nav bar is down to four items.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.clickable(onClick = onOpenAnonymousChat)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        MaskIcon(color = colors.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Anonymous",
                            style = typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
            }

            // Filters the pages already loaded — no query per keystroke.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search chats",
                                style = typography.bodyMedium,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                )
            }

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
                                text = "Like someone in Discover to start chatting",
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
                        items(conversations, key = { it.conversationId }) { item ->
                            Surface(
                                onClick = { onOpenConversation(item.conversationId) },
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
                                            .background(Brush.linearGradient(chatPaletteFor(item.otherUid))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(24.dp))
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayName,
                                            style = typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        // No preview text: messages disappear once seen, so a copy
                                        // of one here would outlive the message itself.
                                        Text(
                                            text = when {
                                                item.hasUnreadFor(uid) -> "New message"
                                                item.lastMessageAtSeconds > 0L -> "Opened"
                                                else -> "Say hi!"
                                            },
                                            style = typography.bodyMedium,
                                            color = if (item.hasUnreadFor(uid)) colors.primary else colors.onSurfaceVariant,
                                            fontWeight = if (item.hasUnreadFor(uid)) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Conversations arrive a page at a time; search covers what is loaded.
                        if (hasMore && query.isBlank()) {
                            item {
                                Surface(
                                    onClick = onLoadMore,
                                    enabled = !isLoadingMore,
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(
                                                color = colors.primary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Load older chats",
                                                style = typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
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
                .safeDrawingPadding()
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
                        .background(Brush.linearGradient(chatPaletteFor(conversation.otherUid))),
                    contentAlignment = Alignment.Center
                ) {
                    DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = conversation.displayName,
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = if (conversation.isOtherUserDeleted) "Account deleted" else "Connected",
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
                                text = "Start the conversation with ${conversation.displayName}.",
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
                                    text = "Message ${conversation.displayName}…",
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
