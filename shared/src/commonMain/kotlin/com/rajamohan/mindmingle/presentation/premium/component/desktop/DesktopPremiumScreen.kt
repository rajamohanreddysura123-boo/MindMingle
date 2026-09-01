package com.rajamohan.mindmingle.presentation.premium.component.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.CheckIcon
import com.rajamohan.mindmingle.presentation.common.icon.SparkleBurstIcon
import com.rajamohan.mindmingle.presentation.premium.viewmodel.PremiumEvent
import com.rajamohan.mindmingle.presentation.premium.viewmodel.PremiumViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private data class PlusFeature(val title: String, val description: String)

private val plusFeatures = listOf(
    PlusFeature("Unlimited Connects", "Like as many tech partners as you want, no daily cap"),
    PlusFeature("See Who Liked You", "Skip the guesswork and match instantly"),
    PlusFeature(
        "Advanced Filters",
        "Filter Discover by occupation, experience, interests, languages, distance and lifestyle — free search covers age, gender and intent"
    ),
    PlusFeature("Priority in Discover", "Your profile surfaces first to relevant matches"),
    PlusFeature("Verified Developer Badge", "Boost trust with a GitHub-verified checkmark"),
    PlusFeature("Unlimited Rewinds", "Undo an accidental pass anytime")
)

/** Two-column desktop layout: feature list on the left, sticky price/CTA card on the right (typical SaaS pricing page shape). */
@Composable
fun DesktopPremiumScreen(
    uid: String,
    onBack: () -> Unit,
    onUpgraded: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: PremiumViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val isAnnual = uiState.selectedPlan == PremiumPlan.ANNUAL
    val isProcessing = uiState.isProcessing
    val isUpgraded = uiState.isPremium

    LaunchedEffect(uid) {
        viewModel.onEvent(PremiumEvent.Load(uid))
    }

    LaunchedEffect(uiState.justUpgraded) {
        if (uiState.justUpgraded) {
            delay(900)
            onUpgraded()
            onBack()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.desktopScreenPadding, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = colors.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Subscription Plans", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 1100.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.desktopScreenPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // Left: pitch + features
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(text = "Enhance Your Experience", style = typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
                    Row {
                        Text(text = "with ", style = typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
                        Text(text = "MindMingle+", style = typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = colors.secondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Unlock advanced matching filters, unlimited connects and priority visibility with tech partners.",
                        style = typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        plusFeatures.forEach { feature -> FeatureRow(feature) }
                    }
                }

                // Right: sticky price card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.weight(0.8f).widthIn(min = 320.dp)
                ) {
                    Column(modifier = Modifier.padding(28.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                .background(colors.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PlanToggleOption(label = "Monthly", isSelected = !isAnnual, modifier = Modifier.weight(1f)) {
                                viewModel.onEvent(PremiumEvent.SelectPlan(PremiumPlan.MONTHLY))
                            }
                            PlanToggleOption(label = uiState.annualToggleLabel, isSelected = isAnnual, modifier = Modifier.weight(1f)) {
                                viewModel.onEvent(PremiumEvent.SelectPlan(PremiumPlan.ANNUAL))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = uiState.priceLabel,
                                style = typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.onBackground
                            )
                            Text(
                                text = uiState.periodLabel,
                                style = typography.titleMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (uiState.error.isNotBlank()) {
                            Text(
                                text = uiState.error,
                                style = typography.bodySmall,
                                color = colors.error,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            )
                        }

                        Surface(
                            onClick = { viewModel.onEvent(PremiumEvent.Checkout) },
                            enabled = uiState.canCheckout,
                            shape = RoundedCornerShape(50),
                            color = if (isUpgraded) colors.tertiary else colors.primary,
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                when {
                                    isProcessing -> {
                                        CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
                                    }
                                    isUpgraded -> {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            SparkleBurstIcon(color = colors.onTertiary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "You're on MindMingle+", style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onTertiary)
                                        }
                                    }
                                    else -> {
                                        Text(text = "Upgrade to Plus", style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (uiState.isCheckoutAvailable) {
                                "Cancel anytime. No hidden fees."
                            } else {
                                "Payments are only available in the Android and iOS apps."
                            },
                            style = typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanToggleOption(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.surface else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.primary else colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureRow(feature: PlusFeature) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(30.dp).background(colors.tertiaryContainer.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CheckIcon(color = colors.tertiary, modifier = Modifier.size(15.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = feature.title, style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(text = feature.description, style = typography.bodyMedium, color = colors.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}
