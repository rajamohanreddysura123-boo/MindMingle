package com.rajamohan.mindmingle.presentation.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.core.ads.PlatformBannerAd
import com.rajamohan.mindmingle.presentation.home.viewmodel.AdGate
import com.rajamohan.mindmingle.presentation.home.viewmodel.AdSlotKind

/**
 * The ad that takes over the Discover deck's card slot every Nth profile.
 *
 * It deliberately occupies the same footprint as a profile card so the deck doesn't jump, and it
 * carries its own "Sponsored" label — AdMob policy requires ad content to be distinguishable from
 * app content, and an unlabelled ad card sitting where a person's profile normally sits is exactly
 * the ambiguity that gets an app's serving disabled.
 *
 * Swipe actions are locked by the caller for as long as this is on screen; [gate] decides when
 * "Continue" lights up.
 */
@Composable
internal fun SponsoredAdCard(
    gate: AdGate,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "Sponsored",
                    style = typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                when (gate.kind) {
                    // A 300x250 unit sits inside the card itself.
                    AdSlotKind.BANNER -> PlatformBannerAd(
                        unitId = gate.unitId,
                        modifier = Modifier.fillMaxSize()
                    )

                    // The real ad is full-screen and owned by the SDK; this is just the deck
                    // placeholder behind it while it loads and plays.
                    AdSlotKind.INTERSTITIAL -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!gate.failed) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                        Text(
                            text = if (gate.failed) "Ad unavailable" else "Loading ad…",
                            style = typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            Text(
                text = if (gate.canContinue) {
                    "Thanks for supporting MindMingle"
                } else {
                    "Ad in progress — swiping resumes in ${gate.secondsLeft}s"
                },
                style = typography.bodySmall,
                color = colors.onSurfaceVariant,
                fontSize = 12.sp
            )

            Surface(
                onClick = onContinue,
                enabled = gate.canContinue,
                shape = RoundedCornerShape(50),
                color = if (gate.canContinue) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (gate.canContinue) "Continue" else "Continue in ${gate.secondsLeft}s",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (gate.canContinue) colors.onPrimary else colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
