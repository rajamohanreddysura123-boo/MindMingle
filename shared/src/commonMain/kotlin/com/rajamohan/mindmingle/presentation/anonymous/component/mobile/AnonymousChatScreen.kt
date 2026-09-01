package com.rajamohan.mindmingle.presentation.anonymous.component.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.domain.model.AnonymousMessage
import com.rajamohan.mindmingle.domain.model.AnonymousSession
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.ANONYMOUS_EMPTY_BODY
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.ANONYMOUS_EMPTY_TITLE
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.ANONYMOUS_PARTNER_LEFT_BODY
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.ANONYMOUS_PARTNER_LEFT_TITLE
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.ANONYMOUS_SEARCHING
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.ANONYMOUS_TAGLINE
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.ANONYMOUS_TITLE
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.anonymousAccentAt
import com.rajamohan.mindmingle.presentation.anonymous.component.shared.anonymousColors
import com.rajamohan.mindmingle.presentation.anonymous.viewmodel.AnonymousChatEvent
import com.rajamohan.mindmingle.presentation.anonymous.viewmodel.AnonymousChatViewModel
import com.rajamohan.mindmingle.presentation.anonymous.viewmodel.AnonymousPhase
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.BurnIcon
import com.rajamohan.mindmingle.presentation.common.icon.GhostIcon
import com.rajamohan.mindmingle.presentation.common.icon.MaskIcon
import com.rajamohan.mindmingle.presentation.common.icon.RadarIcon
import com.rajamohan.mindmingle.presentation.common.icon.SendIcon
import com.rajamohan.mindmingle.presentation.common.icon.SkipIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnonymousChatScreen(
    uid: String,
    onBack: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val viewModel: AnonymousChatViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(uid) {
        viewModel.onEvent(AnonymousChatEvent.VisibilityChanged(true))
        viewModel.onEvent(AnonymousChatEvent.LoadPending)
        onDispose {
            viewModel.onEvent(AnonymousChatEvent.VisibilityChanged(false))
            viewModel.onEvent(AnonymousChatEvent.Leave(uid))
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(anonymousColors.backdrop))
        ) {
            AnimatedContent(
                targetState = uiState.phase,
                transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
                label = "anonymous_phase"
            ) { phase ->
                when (phase) {
                    AnonymousPhase.Idle -> AnonymousLobby(
                        pendingCount = uiState.pendingMessages.size,
                        isPendingRevealed = uiState.isPendingRevealed,
                        pendingMessages = uiState.pendingMessages,
                        onReveal = { viewModel.onEvent(AnonymousChatEvent.RevealPending) },
                        onDismissPending = { viewModel.onEvent(AnonymousChatEvent.DismissPending) },
                        onStart = { viewModel.onEvent(AnonymousChatEvent.Start(uid)) }
                    )
                    AnonymousPhase.Searching -> AnonymousSearching(
                        skippedCount = uiState.skippedCount,
                        onCancel = { viewModel.onEvent(AnonymousChatEvent.Leave(uid)) }
                    )
                    AnonymousPhase.NoOneFound -> AnonymousNoOneFound(
                        onRetry = { viewModel.onEvent(AnonymousChatEvent.Start(uid)) },
                        onExit = { viewModel.onEvent(AnonymousChatEvent.Leave(uid)) }
                    )
                    AnonymousPhase.Chatting -> AnonymousRoom(
                        session = uiState.session,
                        messages = uiState.messages,
                        onSend = { text -> viewModel.onEvent(AnonymousChatEvent.Send(uid, text)) },
                        onSkip = { viewModel.onEvent(AnonymousChatEvent.Skip(uid)) },
                        onExit = { viewModel.onEvent(AnonymousChatEvent.Leave(uid)) }
                    )
                    AnonymousPhase.PartnerLeft -> AnonymousPartnerLeft(
                        onFindNew = { viewModel.onEvent(AnonymousChatEvent.Skip(uid)) },
                        onExit = { viewModel.onEvent(AnonymousChatEvent.Leave(uid)) }
                    )
                }
            }

            // Anonymous chat is reached from inside the Chat tab rather than the bottom nav, so
            // it needs its own way back. Only drawn when a host supplied one.
            if (onBack != null) {
                Surface(
                    shape = CircleShape,
                    color = colors.surface.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .safeDrawingPadding()
                        .padding(start = Spacing.screenHorizontal, top = 8.dp)
                        .size(40.dp)
                        .clickable(onClick = onBack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnonymousLobby(
    pendingCount: Int,
    isPendingRevealed: Boolean,
    pendingMessages: List<AnonymousMessage>,
    onReveal: () -> Unit,
    onDismissPending: () -> Unit,
    onStart: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        AnonymousEmblem(accent = anonymousAccentAt(0), size = 132.dp)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = ANONYMOUS_TITLE,
            style = typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ANONYMOUS_TAGLINE,
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnonymousTag(text = "Nothing stored")
            AnonymousTag(text = "Alias only")
            AnonymousTag(text = "Skip anytime")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (pendingCount > 0) {
            PendingWhispersCard(
                pendingCount = pendingCount,
                isRevealed = isPendingRevealed,
                pendingMessages = pendingMessages,
                onReveal = onReveal,
                onDismiss = onDismissPending
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            onClick = onStart,
            shape = RoundedCornerShape(28.dp),
            color = colors.primary,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaskIcon(color = colors.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Start anonymous chat",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun PendingWhispersCard(
    pendingCount: Int,
    isRevealed: Boolean,
    pendingMessages: List<AnonymousMessage>,
    onReveal: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BurnIcon(color = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (pendingCount == 1) "1 unread whisper" else "$pendingCount unread whispers",
                        style = typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Kept on this device only, until you read them",
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (isRevealed) {
                Spacer(modifier = Modifier.height(14.dp))
                pendingMessages.forEach { message ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Text(
                            text = message.text,
                            style = typography.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                onClick = if (isRevealed) onDismiss else onReveal,
                shape = RoundedCornerShape(16.dp),
                color = if (isRevealed) colors.errorContainer else colors.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (isRevealed) "Burn them now" else "Read and burn",
                        style = typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isRevealed) colors.onErrorContainer else colors.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AnonymousSearching(
    skippedCount: Int,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RadarPulse(accent = anonymousAccentAt(3))

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = ANONYMOUS_SEARCHING,
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (skippedCount > 0) "Skipped $skippedCount so far" else "Hold tight, this takes a moment",
            style = typography.bodySmall,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(36.dp))

        Surface(
            onClick = onCancel,
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)),
            modifier = Modifier.height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 28.dp).fillMaxSize()) {
                Text(
                    text = "Cancel",
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnonymousNoOneFound(
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.surfaceContainerHigh,
                            colors.surfaceContainerLow
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            GhostIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(54.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = ANONYMOUS_EMPTY_TITLE,
            style = typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ANONYMOUS_EMPTY_BODY,
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Surface(
            onClick = onRetry,
            shape = RoundedCornerShape(24.dp),
            color = colors.primary,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadarIcon(color = colors.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Search again",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            onClick = onExit,
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Back to lobby",
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnonymousRoom(
    session: AnonymousSession?,
    messages: List<AnonymousMessage>,
    onSend: (String) -> Unit,
    onSkip: () -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val accent = anonymousAccentAt(session?.peerAccentIndex ?: 0)
    var draftText by remember { mutableStateOf("") }
    val listState: LazyListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(accent)),
                contentAlignment = Alignment.Center
            ) {
                MaskIcon(color = anonymousColors.onAccent, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session?.peerAlias.orEmpty(),
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.tertiary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Connected anonymously",
                        style = typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Surface(
                onClick = onExit,
                shape = CircleShape,
                color = colors.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BurnIcon(color = colors.error, modifier = Modifier.size(18.dp))
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = "Messages live only on your screen. Leaving this room erases them.",
                style = typography.labelSmall,
                color = colors.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnonymousEmblem(accent = accent, size = 96.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "You are talking to ${session?.peerAlias.orEmpty()}",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Neither of you can see who the other is",
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (message.isMine) 18.dp else 4.dp,
                            bottomEnd = if (message.isMine) 4.dp else 18.dp
                        ),
                        color = if (message.isMine) colors.primary else colors.surface,
                        border = if (message.isMine) null else BorderStroke(1.dp, accent.first().copy(alpha = 0.35f)),
                        shadowElevation = if (message.isMine) 2.dp else 0.dp,
                        modifier = Modifier.widthIn(max = 284.dp)
                    ) {
                        Text(
                            text = message.text,
                            style = typography.bodyMedium,
                            color = if (message.isMine) colors.onPrimary else colors.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                onClick = onSkip,
                shape = CircleShape,
                color = colors.surface,
                border = BorderStroke(1.2.dp, colors.outline.copy(alpha = 0.5f)),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SkipIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

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
                                text = "Say something anonymous…",
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
                shadowElevation = 6.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SendIcon(color = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AnonymousPartnerLeft(
    onFindNew: () -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.errorContainer, colors.surfaceContainerLow))),
            contentAlignment = Alignment.Center
        ) {
            GhostIcon(color = colors.error, modifier = Modifier.size(50.dp))
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = ANONYMOUS_PARTNER_LEFT_TITLE,
            style = typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ANONYMOUS_PARTNER_LEFT_BODY,
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            onClick = onFindNew,
            shape = RoundedCornerShape(24.dp),
            color = colors.primary,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Meet someone else",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            onClick = onExit,
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Back to lobby",
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnonymousTag(text: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            style = typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AnonymousEmblem(accent: List<Color>, size: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "anonymous_emblem")
    val glow by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anonymous_emblem_scale"
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(glow)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.first().copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(size * 0.58f)
                .clip(CircleShape)
                .background(Brush.linearGradient(accent))
                .border(2.dp, anonymousColors.onAccent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaskIcon(color = anonymousColors.onAccent, modifier = Modifier.size(size * 0.26f))
        }
    }
}

@Composable
private fun RadarPulse(accent: List<Color>) {
    val transition = rememberInfiniteTransition(label = "anonymous_radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anonymous_radar_sweep"
    )
    val ripple by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anonymous_radar_ripple"
    )

    val colors = MaterialTheme.colorScheme

    Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(
                color = accent.first().copy(alpha = 0.18f),
                radius = radius * ripple,
                style = Stroke(width = 2.5f * density)
            )
            drawCircle(
                color = accent.first().copy(alpha = 0.10f),
                radius = radius * 0.72f
            )
            drawCircle(
                color = colors.outline.copy(alpha = 0.25f),
                radius = radius * 0.46f,
                style = Stroke(width = 1.5f * density)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(sweep)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(accent.first(), Color.Transparent),
                        start = Offset(size.width / 2f, size.height / 2f),
                        end = Offset(size.width / 2f, size.height / 2f - radius)
                    ),
                    start = Offset(size.width / 2f, size.height / 2f),
                    end = Offset(size.width / 2f, size.height / 2f - radius),
                    strokeWidth = 3f * density
                )
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(accent)),
            contentAlignment = Alignment.Center
        ) {
            RadarIcon(color = anonymousColors.onAccent, modifier = Modifier.size(32.dp))
        }
    }
}
