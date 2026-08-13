package com.rajamohan.mindmingle.presentation.anonymous.component.desktop

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.rajamohan.mindmingle.presentation.anonymous.viewmodel.AnonymousChatUiState
import com.rajamohan.mindmingle.presentation.anonymous.viewmodel.AnonymousChatViewModel
import com.rajamohan.mindmingle.presentation.anonymous.viewmodel.AnonymousPhase
import com.rajamohan.mindmingle.presentation.common.icon.BurnIcon
import com.rajamohan.mindmingle.presentation.common.icon.GhostIcon
import com.rajamohan.mindmingle.presentation.common.icon.MaskIcon
import com.rajamohan.mindmingle.presentation.common.icon.RadarIcon
import com.rajamohan.mindmingle.presentation.common.icon.SendIcon
import com.rajamohan.mindmingle.presentation.common.icon.SkipIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopAnonymousChatScreen(uid: String) {
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
        Row(modifier = Modifier.fillMaxSize()) {
            DesktopAnonymousControlPanel(
                uiState = uiState,
                onStart = { viewModel.onEvent(AnonymousChatEvent.Start(uid)) },
                onSkip = { viewModel.onEvent(AnonymousChatEvent.Skip(uid)) },
                onLeave = { viewModel.onEvent(AnonymousChatEvent.Leave(uid)) },
                onReveal = { viewModel.onEvent(AnonymousChatEvent.RevealPending) },
                onDismissPending = { viewModel.onEvent(AnonymousChatEvent.DismissPending) },
                modifier = Modifier.width(340.dp).fillMaxHeight()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(anonymousColors.backdrop))
            ) {
                AnimatedContent(
                    targetState = uiState.phase,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "desktop_anonymous_phase"
                ) { phase ->
                    when (phase) {
                        AnonymousPhase.Idle -> DesktopAnonymousStage(
                            accentIndex = 0,
                            title = ANONYMOUS_TITLE,
                            body = ANONYMOUS_TAGLINE,
                            actionLabel = "Start anonymous chat",
                            onAction = { viewModel.onEvent(AnonymousChatEvent.Start(uid)) }
                        )
                        AnonymousPhase.Searching -> DesktopAnonymousSearching(
                            skippedCount = uiState.skippedCount
                        )
                        AnonymousPhase.NoOneFound -> DesktopAnonymousNoOneFound(
                            onRetry = { viewModel.onEvent(AnonymousChatEvent.Start(uid)) }
                        )
                        AnonymousPhase.Chatting -> DesktopAnonymousRoom(
                            session = uiState.session,
                            messages = uiState.messages,
                            onSend = { text -> viewModel.onEvent(AnonymousChatEvent.Send(uid, text)) }
                        )
                        AnonymousPhase.PartnerLeft -> DesktopAnonymousStage(
                            accentIndex = 2,
                            title = ANONYMOUS_PARTNER_LEFT_TITLE,
                            body = ANONYMOUS_PARTNER_LEFT_BODY,
                            actionLabel = "Meet someone else",
                            onAction = { viewModel.onEvent(AnonymousChatEvent.Skip(uid)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopAnonymousControlPanel(
    uiState: AnonymousChatUiState,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onLeave: () -> Unit,
    onReveal: () -> Unit,
    onDismissPending: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val accent = anonymousAccentAt(uiState.session?.peerAccentIndex ?: 0)

    Column(
        modifier = modifier
            .background(colors.surfaceContainerLow)
            .border(width = 1.dp, color = colors.outlineVariant.copy(alpha = 0.4f))
            .safeContentPadding()
            .padding(Spacing.desktopScreenPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MaskIcon(color = colors.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = ANONYMOUS_TITLE,
                style = typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(accent)),
                        contentAlignment = Alignment.Center
                    ) {
                        MaskIcon(color = anonymousColors.onAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = uiState.session?.peerAlias ?: "No one connected",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = phaseLabel(uiState.phase),
                            style = typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DesktopPolicyLine(text = "Messages are never stored on our servers")
                DesktopPolicyLine(text = "Unread lines stay on this device only")
                DesktopPolicyLine(text = "Leaving the room erases what you read")
                DesktopPolicyLine(text = "Sessions are logged by user id for safety")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.pendingMessages.isNotEmpty()) {
            DesktopPendingWhispers(
                pendingMessages = uiState.pendingMessages,
                isRevealed = uiState.isPendingRevealed,
                onReveal = onReveal,
                onDismiss = onDismissPending
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        if (uiState.phase == AnonymousPhase.Chatting) {
            DesktopPanelButton(
                label = "Skip this person",
                container = colors.surface,
                content = colors.onSurface,
                border = BorderStroke(1.2.dp, colors.outline.copy(alpha = 0.5f)),
                onClick = onSkip
            ) {
                SkipIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            DesktopPanelButton(
                label = "Leave and burn",
                container = colors.errorContainer,
                content = colors.onErrorContainer,
                border = null,
                onClick = onLeave
            ) {
                BurnIcon(color = colors.onErrorContainer, modifier = Modifier.size(16.dp))
            }
        } else {
            DesktopPanelButton(
                label = if (uiState.phase == AnonymousPhase.Searching) "Cancel search" else "Start anonymous chat",
                container = if (uiState.phase == AnonymousPhase.Searching) colors.surface else colors.primary,
                content = if (uiState.phase == AnonymousPhase.Searching) colors.onSurfaceVariant else colors.onPrimary,
                border = if (uiState.phase == AnonymousPhase.Searching) BorderStroke(1.2.dp, colors.outline.copy(alpha = 0.5f)) else null,
                onClick = if (uiState.phase == AnonymousPhase.Searching) onLeave else onStart
            ) {
                if (uiState.phase == AnonymousPhase.Searching) {
                    RadarIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                } else {
                    MaskIcon(color = colors.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (uiState.skippedCount > 0) "Skipped ${uiState.skippedCount} this session" else "Nobody skipped yet",
            style = typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun DesktopPanelButton(
    label: String,
    container: Color,
    content: Color,
    border: BorderStroke?,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = border,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

@Composable
private fun DesktopPolicyLine(text: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.7f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun DesktopPendingWhispers(
    pendingMessages: List<AnonymousMessage>,
    isRevealed: Boolean,
    onReveal: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = anonymousColors.whisper,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BurnIcon(color = anonymousColors.onWhisper, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${pendingMessages.size} unread on this device",
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = anonymousColors.onWhisper
                )
            }

            if (isRevealed) {
                Spacer(modifier = Modifier.height(12.dp))
                pendingMessages.forEach { message ->
                    Text(
                        text = message.text,
                        style = typography.bodySmall,
                        color = anonymousColors.onWhisper,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                onClick = if (isRevealed) onDismiss else onReveal,
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (isRevealed) "Burn them now" else "Read and burn",
                        style = typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopAnonymousStage(
    accentIndex: Int,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val accent = anonymousAccentAt(accentIndex)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 460.dp).padding(32.dp)
        ) {
            DesktopEmblem(accent = accent)

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = title,
                style = typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = body,
                style = typography.bodyLarge,
                color = colors.onSurfaceVariant,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(22.dp),
                color = colors.primary,
                shadowElevation = 10.dp,
                modifier = Modifier.height(52.dp).widthIn(min = 240.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaskIcon(color = colors.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = actionLabel,
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopAnonymousSearching(skippedCount: Int) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val accent = anonymousAccentAt(3)

    val transition = rememberInfiniteTransition(label = "desktop_anonymous_search")
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "desktop_anonymous_pulse"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accent.first().copy(alpha = 0.32f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(accent)),
                    contentAlignment = Alignment.Center
                ) {
                    RadarIcon(color = anonymousColors.onAccent, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = ANONYMOUS_SEARCHING,
                style = typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (skippedCount > 0) "Skipped $skippedCount so far" else "Matching you with another anonymous member",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DesktopAnonymousNoOneFound(onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 440.dp).padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.surfaceContainerHigh, colors.surfaceContainerLowest)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                GhostIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(58.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = ANONYMOUS_EMPTY_TITLE,
                style = typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = ANONYMOUS_EMPTY_BODY,
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            Surface(
                onClick = onRetry,
                shape = RoundedCornerShape(20.dp),
                color = colors.primary,
                shadowElevation = 8.dp,
                modifier = Modifier.height(50.dp).widthIn(min = 220.dp)
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
        }
    }
}

@Composable
private fun DesktopAnonymousRoom(
    session: AnonymousSession?,
    messages: List<AnonymousMessage>,
    onSend: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val accent = anonymousAccentAt(session?.peerAccentIndex ?: 0)

    var draftText by remember { mutableStateOf("") }
    val listState: LazyListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.6f))
                .padding(horizontal = 32.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(accent)),
                contentAlignment = Alignment.Center
            ) {
                MaskIcon(color = anonymousColors.onAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session?.peerAlias.orEmpty(),
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "Anonymous room, nothing is written down",
                    style = typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = anonymousColors.whisper
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    BurnIcon(color = anonymousColors.onWhisper, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ephemeral",
                        style = typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = anonymousColors.onWhisper
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DesktopEmblem(accent = accent)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "You are matched with ${session?.peerAlias.orEmpty()}",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Say the first word. Neither side sees a name.",
                                style = typography.bodyMedium,
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
                                bottomStart = if (message.isMine) 18.dp else 6.dp,
                                bottomEnd = if (message.isMine) 6.dp else 18.dp
                            ),
                            color = if (message.isMine) colors.primary else colors.surface,
                            border = if (message.isMine) null else BorderStroke(1.dp, accent.first().copy(alpha = 0.35f)),
                            modifier = Modifier.widthIn(max = 520.dp)
                        ) {
                            Text(
                                text = message.text,
                                style = typography.bodyMedium,
                                color = if (message.isMine) colors.onPrimary else colors.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().background(colors.surface.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.surfaceContainerHigh)
                        .padding(horizontal = 20.dp, vertical = 15.dp)
                ) {
                    BasicTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (draftText.isEmpty()) {
                                Text(
                                    text = "Type anonymously…",
                                    style = typography.bodyMedium,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SendIcon(color = colors.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopEmblem(accent: List<Color>) {
    val transition = rememberInfiniteTransition(label = "desktop_anonymous_emblem")
    val glow by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "desktop_anonymous_glow"
    )

    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(glow)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(accent.first().copy(alpha = 0.3f), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(accent)),
            contentAlignment = Alignment.Center
        ) {
            MaskIcon(color = anonymousColors.onAccent, modifier = Modifier.size(40.dp))
        }
    }
}

private fun phaseLabel(phase: AnonymousPhase): String = when (phase) {
    AnonymousPhase.Idle -> "Idle"
    AnonymousPhase.Searching -> "Searching"
    AnonymousPhase.NoOneFound -> "No one found"
    AnonymousPhase.Chatting -> "Live and anonymous"
    AnonymousPhase.PartnerLeft -> "Partner left"
}
