package com.rajamohan.mindmingle.presentation.home.component.mobile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.rajamohan.mindmingle.presentation.common.icon.CrossIcon
import com.rajamohan.mindmingle.presentation.common.icon.HeartIcon
import com.rajamohan.mindmingle.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Which way the card left the deck. Left is a pass, right is a connect. */
internal enum class SwipeDirection { LEFT, RIGHT }

/** Fraction of the card's width a drag must pass before releasing commits it. */
private const val CommitFraction = 0.28f

/** Release speed, in px/s, that commits a card regardless of how far it actually travelled. */
private const val FlickVelocity = 1_200f

/** How far off-screen a committed card flies, as a multiple of its own width. */
private const val FlyOffFactor = 1.6f

/** Tilt at full drag. Small — a card that spins reads as a bug, not as physics. */
private const val MaxRotationDegrees = 12f

/**
 * Drag state for the top card of the deck.
 *
 * It is hoisted above the card rather than remembered inside it: the card composable is replaced
 * the moment a swipe commits, and state living inside it would be destroyed mid-animation. This
 * object outlives every individual card and is reset by [fling] instead.
 *
 * The same [fling] backs the Pass/Connect buttons, so a tap and a swipe run the identical motion
 * and cannot drift apart.
 */
@Stable
internal class SwipeDeckState(private val scope: CoroutineScope) {

    val offsetX = Animatable(0f)

    /** Measured width of the card slot; the commit threshold and fly-off distance derive from it. */
    var cardWidthPx by mutableStateOf(0f)

    /** True from the moment a swipe commits until the next card is centred. Input is dead here. */
    var isSettling by mutableStateOf(false)
        private set

    private val commitThresholdPx: Float get() = cardWidthPx * CommitFraction

    /** -1 at a full pass, +1 at a full connect. Every drag-driven visual reads this. */
    val progress: Float
        get() = if (commitThresholdPx <= 0f) 0f else (offsetX.value / commitThresholdPx).coerceIn(-1f, 1f)

    fun onDrag(delta: Float) {
        if (isSettling) return
        scope.launch { offsetX.snapTo(offsetX.value + delta) }
    }

    /**
     * Decides what a released drag meant. Distance alone would ignore a fast flick that barely
     * travelled, which is the most common way people actually swipe, so velocity commits too —
     * but only when it points the same way the card is already offset.
     */
    fun onDragStopped(velocity: Float, onCommit: (SwipeDirection) -> Unit) {
        if (isSettling) return
        val offset = offsetX.value
        val travelled = commitThresholdPx > 0f && abs(offset) > commitThresholdPx
        val flicked = abs(velocity) > FlickVelocity && offset != 0f && (velocity > 0f) == (offset > 0f)

        if (travelled || flicked) {
            fling(if (offset > 0f) SwipeDirection.RIGHT else SwipeDirection.LEFT, onCommit)
        } else {
            scope.launch {
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
                )
            }
        }
    }

    /** Throws the card off-screen, then reports the direction. Also the buttons' entry point. */
    fun fling(direction: SwipeDirection, onCommit: (SwipeDirection) -> Unit) {
        if (isSettling) return
        isSettling = true
        scope.launch {
            val width = cardWidthPx
            if (width > 0f) {
                val target = if (direction == SwipeDirection.RIGHT) width * FlyOffFactor else -width * FlyOffFactor
                offsetX.animateTo(target, tween(durationMillis = 260, easing = LinearOutSlowInEasing))
            }
            // Recentred before the event fires: the next profile arrives on the recomposition that
            // `onCommit` triggers, and it must not be drawn sitting at the old card's fly-off offset.
            offsetX.snapTo(0f)
            onCommit(direction)
            isSettling = false
        }
    }
}

/**
 * The draggable top card plus the stack behind it.
 *
 * There is deliberately no `AnimatedContent` here. A container-driven enter/exit transition and a
 * drag both want to own the card's transform, and running them together makes the card jump on
 * every commit. The incoming card's entrance is instead the peek layer rising into place.
 */
@Composable
internal fun SwipeDeck(
    profile: User,
    distanceKm: Double?,
    behindDepth: Int,
    enabled: Boolean,
    state: SwipeDeckState,
    onPass: () -> Unit,
    onConnect: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val progress = state.progress

    // One tick as the drag crosses the point of no return, so the commit is felt before release.
    val atThreshold = abs(progress) >= 1f
    LaunchedEffect(atThreshold) {
        if (atThreshold) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val dragState = rememberDraggableState { delta -> state.onDrag(delta) }

    Box(
        modifier = modifier.onSizeChanged { size -> state.cardWidthPx = size.width.toFloat() }
    ) {
        DeckPeekLayer(depth = behindDepth, progress = abs(progress))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = DeckStackReserve)
                .graphicsLayer {
                    translationX = state.offsetX.value
                    rotationZ = progress * MaxRotationDegrees
                    // Pivot below the card's own bottom edge, so it swings the way a card held at
                    // the bottom of a stack does rather than spinning about its middle.
                    transformOrigin = TransformOrigin(0.5f, 1.2f)
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled && !state.isSettling,
                    onDragStopped = { velocity ->
                        state.onDragStopped(velocity) { direction ->
                            if (direction == SwipeDirection.RIGHT) onConnect() else onPass()
                        }
                    }
                )
        ) {
            ProfileDeckCard(
                profile = profile,
                distanceKm = distanceKm,
                onOpenDetails = onOpenDetails,
                modifier = Modifier.fillMaxSize()
            )

            SwipeTint(progress = progress)
            SwipeStamps(progress = progress)
        }
    }
}

/** Washes the whole card toward the colour of the action being chosen. */
@Composable
private fun BoxScope.SwipeTint(progress: Float) {
    val colors = MaterialTheme.colorScheme
    if (progress == 0f) return

    val tint = if (progress > 0f) colors.tertiary else colors.error
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(DeckCardShape)
            .background(tint.copy(alpha = 0.12f * abs(progress)))
    )
}

/**
 * Connect and pass stamps.
 *
 * Icon badges rather than words: the shape reads at a glance mid-drag, and it matches the two
 * buttons under the deck so a tap and a swipe show the same symbol. Each sits on the edge the card
 * is moving *away* from, which is where every app in the category puts them — the stamp stays in
 * view under the thumb instead of disappearing off-screen first.
 */
@Composable
private fun BoxScope.SwipeStamps(progress: Float) {
    val colors = MaterialTheme.colorScheme

    SwipeStamp(
        color = colors.tertiary,
        rotationDegrees = -14f,
        alpha = progress.coerceAtLeast(0f),
        modifier = Modifier.align(Alignment.TopStart).padding(22.dp)
    ) { tint ->
        HeartIcon(color = tint, modifier = Modifier.size(StampIconSize), filled = true)
    }
    SwipeStamp(
        color = colors.error,
        rotationDegrees = 14f,
        alpha = (-progress).coerceAtLeast(0f),
        modifier = Modifier.align(Alignment.TopEnd).padding(22.dp)
    ) { tint ->
        CrossIcon(color = tint, modifier = Modifier.size(StampIconSize))
    }
}

/** Icon size inside a stamp badge. */
private val StampIconSize = 34.dp

/** Diameter of the ring drawn around a stamp icon. */
private val StampBadgeSize = 68.dp

@Composable
private fun SwipeStamp(
    color: Color,
    rotationDegrees: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit
) {
    if (alpha <= 0.01f) return

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = rotationDegrees
            }
            .size(StampBadgeSize)
            .border(3.dp, color, CircleShape)
            .background(color.copy(alpha = 0.14f), CircleShape)
    ) {
        icon(color)
    }
}
