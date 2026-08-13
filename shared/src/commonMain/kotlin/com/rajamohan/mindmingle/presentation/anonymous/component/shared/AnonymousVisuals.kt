package com.rajamohan.mindmingle.presentation.anonymous.component.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rajamohan.mindmingle.presentation.theme.AnonymousColors
import com.rajamohan.mindmingle.presentation.theme.LocalAnonymousColors

internal val anonymousColors: AnonymousColors
    @Composable get() = LocalAnonymousColors.current

@Composable
internal fun anonymousAccentAt(index: Int): List<Color> {
    val accents = LocalAnonymousColors.current.accents
    val safeIndex = if (index < 0) 0 else index % accents.size
    return accents[safeIndex]
}

internal const val ANONYMOUS_TITLE = "Anonymous Chat"
internal const val ANONYMOUS_TAGLINE = "No names, no history. Messages burn the moment you walk away."
internal const val ANONYMOUS_SEARCHING = "Finding someone awake right now"
internal const val ANONYMOUS_EMPTY_TITLE = "No one found"
internal const val ANONYMOUS_EMPTY_BODY = "Nobody is waiting in the anonymous room at the moment. Try again in a few seconds."
internal const val ANONYMOUS_PARTNER_LEFT_TITLE = "They disappeared"
internal const val ANONYMOUS_PARTNER_LEFT_BODY = "Your partner left the room. Everything they said is already gone."
