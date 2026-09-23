package com.rajamohan.mindmingle.presentation.premium.component.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajamohan.mindmingle.core.payments.UrlOpener
import com.rajamohan.mindmingle.presentation.common.component.AppDialog
import com.rajamohan.mindmingle.domain.model.PaymentLink
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/**
 * How a desktop user pays.
 *
 * Razorpay ships checkout SDKs for Android and iOS and nothing for desktop, so there is no sheet to
 * present here. This offers the same purchase two ways off one hosted link: scan the QR and pay on
 * a phone — which is where UPI lives, and where nobody has to type card details — or open the page
 * in a browser on this machine.
 *
 * There is deliberately no "I have paid" button. The screen behind this is already streaming
 * `subscriptions/{uid}`, so when the webhook grants the plan this sheet is dismissed by its own
 * caller. Asking the user to confirm a payment the server already knows about is how people end up
 * pressing a button that lies.
 */
@Composable
internal fun PaymentLinkSheet(
    link: PaymentLink,
    planLabel: String,
    priceLabel: String,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var openFailed by remember(link.linkId) { mutableStateOf(false) }

    AppDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 18.dp,
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(28.dp)
            ) {
                Text(
                    text = "Scan to pay",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$planLabel · $priceLabel",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // White plate under the code on purpose: a QR needs light quiet space around it to
                // scan, and in dark theme the surface behind it is nearly black.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(232.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    Image(
                        painter = rememberQrCodePainter(link.url),
                        contentDescription = "Payment QR code",
                        modifier = Modifier.size(196.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Open your camera or any UPI app and scan. The moment it goes through, " +
                        "MindMingle+ switches on here — you don't need to come back and confirm.",
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Waiting for payment",
                        style = typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    onClick = { openFailed = !UrlOpener.open(link.url) },
                    shape = RoundedCornerShape(50),
                    color = colors.primary,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Pay in browser instead",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }

                if (openFailed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    // No browser to hand it to — headless JVM, or a Linux box with no xdg-open.
                    // The URL is shown so it can be copied, and the QR above still works.
                    Text(
                        text = link.url,
                        style = typography.bodySmall,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    modifier = Modifier.height(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                        Text(
                            text = "Cancel",
                            style = typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
