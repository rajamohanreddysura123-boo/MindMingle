package com.rajamohan.mindmingle.presentation.premium.component.mobile

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private data class PlusFeature(val title: String, val description: String)

private val plusFeatures = listOf(
    PlusFeature("Unlimited Connects", "Like as many tech partners as you want, no daily cap"),
    PlusFeature("See Who Liked You", "Skip the guesswork and match instantly"),
    PlusFeature("Advanced Filters", "Filter Discover by exact tech stack and experience level"),
    PlusFeature("Priority in Discover", "Your profile surfaces first to relevant matches"),
    PlusFeature("Verified Developer Badge", "Boost trust with a GitHub-verified checkmark"),
    PlusFeature("Unlimited Rewinds", "Undo an accidental pass anytime")
)

@Composable
fun PremiumScreen(
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .safeContentPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Text(
                    text = "Subscription Plans",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Box(modifier = Modifier.size(40.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enhance Your Experience",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "with ",
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onBackground
                    )
                    Text(
                        text = "MindMingle+",
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Unlock advanced matching filters, unlimited connects and priority visibility with tech partners.",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Monthly / Annual pill toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.4f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlanToggleOption(
                        label = "Monthly",
                        isSelected = !isAnnual,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onEvent(PremiumEvent.SelectPlan(PremiumPlan.MONTHLY)) }
                    )
                    PlanToggleOption(
                        label = uiState.annualToggleLabel,
                        isSelected = isAnnual,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onEvent(PremiumEvent.SelectPlan(PremiumPlan.ANNUAL)) }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Price
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

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "What's included",
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    plusFeatures.forEach { feature ->
                        FeatureRow(feature = feature)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Sticky CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical)
            ) {
                if (uiState.error.isNotBlank()) {
                    Text(
                        text = uiState.error,
                        style = typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                if (!uiState.isCheckoutAvailable && !isUpgraded) {
                    Text(
                        text = "Payments are only available in the Android and iOS apps.",
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                Surface(
                    onClick = { viewModel.onEvent(PremiumEvent.Checkout) },
                    enabled = uiState.canCheckout,
                    shape = RoundedCornerShape(50),
                    color = if (isUpgraded) colors.tertiary else colors.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        when {
                            isProcessing -> {
                                CircularProgressIndicator(
                                    color = colors.onPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            isUpgraded -> {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SparkleBurstIcon(color = colors.onTertiary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "You're on MindMingle+",
                                        style = typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onTertiary
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "Upgrade to Plus",
                                    style = typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanToggleOption(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
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
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.tertiaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            CheckIcon(color = colors.tertiary, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = feature.title,
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = feature.description,
                style = typography.bodySmall,
                color = colors.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

