package com.rajamohan.mindmingle.presentation.chat.component.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.presentation.chat.viewmodel.ChatEvent
import com.rajamohan.mindmingle.presentation.chat.viewmodel.ChatViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
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

/** Row entrance/removal used by both the conversation list and the message thread — a fade
 *  paired with a soft spring settle reads as "arrived", not "popped in". Reused so both lists
 *  move to the same rhythm. */
private val rowFadeInSpec = tween<Float>(durationMillis = 260, easing = LinearEasing)
private val rowFadeOutSpec = tween<Float>(durationMillis = 150, easing = LinearEasing)
private val rowPlacementSpec = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

@Composable
fun ChatScreen(
    uid: String,
    onOpenAnonymousChat: () -> Unit = {},
    /** Set when a notification tap named a conversation; opened once the list has loaded. */
    openConversationId: String = "",
    onOpenConversationHandled: () -> Unit = {},
    /** Told every time an open thread's presence flips, so the host can hide its bottom nav
     *  while a conversation is open — a thread is full-screen messaging, not a tab. */
    onThreadOpenChanged: (Boolean) -> Unit = {}
) {
    val viewModel: ChatViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.onEvent(ChatEvent.LoadConversations(uid))
    }

    LaunchedEffect(uiState.activeConversation) {
        onThreadOpenChanged(uiState.activeConversation != null)
    }
    // Covers leaving the Chat tab entirely (host disposes this composable) while a thread was
    // still open — without this the bottom nav would stay hidden on every other tab.
    DisposableEffect(Unit) {
        onDispose { onThreadOpenChanged(false) }
    }

    // The conversation cannot be opened before its row exists — OpenConversation looks the
    // conversation up in the loaded list — so this waits for the list rather than firing with uid.
    LaunchedEffect(openConversationId, uiState.conversations) {
        if (openConversationId.isBlank()) return@LaunchedEffect
        if (uiState.conversations.none { it.conversationId == openConversationId }) return@LaunchedEffect
        viewModel.onEvent(ChatEvent.OpenConversation(openConversationId))
        onOpenConversationHandled()
    }

    // The list and an open thread are two different screens wearing the same composable, so they
    // get a real navigation transition between them — the thread pushes in from the right and the
    // list slides back out from the left on the way back, each cross-faded against the other
    // rather than just cutting. Keyed on conversationId so switching straight from one open thread
    // to another (e.g. via a notification tap) still reads as a fresh push, not a no-op.
    AnimatedContent(
        targetState = uiState.activeConversation,
        transitionSpec = {
            val opening = targetState != null && initialState?.conversationId != targetState?.conversationId
            if (opening) {
                (slideInHorizontally(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) { it / 3 } + fadeIn(tween(220)))
                    .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { -it / 6 })
            } else {
                (slideInHorizontally(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) { -it / 3 } + fadeIn(tween(220)))
                    .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { it / 3 })
            }
        },
        label = "chat_thread_transition"
    ) { target ->
        if (target != null) {
            ChatThreadContent(
                uid = uid,
                conversation = target,
                messages = uiState.messages,
                onSend = { text ->
                    viewModel.onEvent(
                        ChatEvent.SendMessage(conversationId = target.conversationId, senderId = uid, text = text)
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
                    text = "Messages",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                BouncyPill(onClick = onOpenAnonymousChat) {
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

            val searchInteraction = remember { MutableInteractionSource() }
            val searchFocused by searchInteraction.collectIsFocusedAsState()
            val searchBg by animateColorAsState(
                targetValue = if (searchFocused) colors.surfaceVariant.copy(alpha = 0.65f) else colors.surfaceVariant.copy(alpha = 0.4f),
                animationSpec = tween(200),
                label = "search_bg"
            )
            val searchBorder by animateColorAsState(
                targetValue = if (searchFocused) colors.primary.copy(alpha = 0.6f) else Color.Transparent,
                animationSpec = tween(200),
                label = "search_border"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(searchBg)
                    .border(1.dp, searchBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                    interactionSource = searchInteraction,
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
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(conversations, key = { it.conversationId }) { item ->
                            ConversationRow(
                                uid = uid,
                                item = item,
                                onClick = { onOpenConversation(item.conversationId) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = rowFadeInSpec,
                                    placementSpec = rowPlacementSpec,
                                    fadeOutSpec = rowFadeOutSpec
                                )
                            )
                        }

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

/** A conversation row: settles in with the shared list rhythm, dips slightly under a finger,
 *  and pulses its unread dot so a new match doesn't just sit there unnoticed. */
@Composable
private fun ConversationRow(
    uid: String,
    item: ChatConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "row_press_scale"
    )

    val isUnread = item.hasUnreadFor(uid)

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth().scale(pressScale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp)
            ) {
                RemoteProfileImage(
                    url = item.otherUserAvatarUrl,
                    uid = item.otherUid,
                    contentDescription = item.displayName,
                    placeholderIconSize = 24.dp,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUnread) {
                        PulsingDot(color = colors.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = when {
                            isUnread -> "New message"
                            item.lastMessageAtSeconds > 0L -> "Opened"
                            else -> "Say hi!"
                        },
                        style = typography.bodyMedium,
                        color = if (isUnread) colors.primary else colors.onSurfaceVariant,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** A small dot that breathes — the least intrusive way to say "this one's new" without a badge
 *  shouting over the whole row. */
@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "unread_pulse")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "unread_pulse_scale"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

/** Shared button chrome for a pill that should feel like it's actually being pressed. */
@Composable
private fun BouncyPill(onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pill_press_scale"
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(50),
        color = colors.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.scale(scale)
    ) {
        content()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val backPressed by backInteraction.collectIsPressedAsState()
                val backScale by animateFloatAsState(
                    targetValue = if (backPressed) 0.85f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "back_press_scale"
                )

                Surface(
                    onClick = onBack,
                    interactionSource = backInteraction,
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp).scale(backScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    RemoteProfileImage(
                        url = conversation.otherUserAvatarUrl,
                        uid = conversation.otherUid,
                        contentDescription = conversation.displayName,
                        placeholderIconSize = 18.dp,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
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

            // Messages — each bubble fades and settles into place via animateItem(), which only
            // fires for genuinely new items (a real send/receive), not for ones merely scrolling
            // back into view, so re-scrolling the thread never replays the entrance.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        ) {
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
                }

                items(messages, key = { it.id }) { message ->
                    val isMine = message.senderId == uid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = rowFadeInSpec,
                                placementSpec = rowPlacementSpec,
                                fadeOutSpec = rowFadeOutSpec
                            ),
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
                val fieldInteraction = remember { MutableInteractionSource() }
                val fieldFocused by fieldInteraction.collectIsFocusedAsState()
                val fieldBorder by animateColorAsState(
                    targetValue = if (fieldFocused) colors.primary.copy(alpha = 0.5f) else Color.Transparent,
                    animationSpec = tween(200),
                    label = "input_border"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface)
                        .border(1.dp, fieldBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                        interactionSource = fieldInteraction,
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

                val canSend = draftText.isNotBlank()
                val sendInteraction = remember { MutableInteractionSource() }
                val sendPressed by sendInteraction.collectIsPressedAsState()
                val sendScale by animateFloatAsState(
                    targetValue = if (sendPressed && canSend) 0.85f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "send_press_scale"
                )
                val sendColor by animateColorAsState(
                    targetValue = if (canSend) colors.primary else colors.surfaceVariant,
                    animationSpec = tween(200),
                    label = "send_color"
                )

                Surface(
                    onClick = {
                        if (draftText.isNotBlank()) {
                            onSend(draftText)
                            draftText = ""
                        }
                    },
                    interactionSource = sendInteraction,
                    shape = CircleShape,
                    color = sendColor,
                    modifier = Modifier.size(48.dp).scale(sendScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SendIcon(
                            color = if (canSend) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
