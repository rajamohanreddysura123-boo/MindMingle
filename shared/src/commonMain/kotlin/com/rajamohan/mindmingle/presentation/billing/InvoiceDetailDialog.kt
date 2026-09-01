package com.rajamohan.mindmingle.presentation.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajamohan.mindmingle.domain.model.Invoice

/**
 * The full invoice behind an order row.
 *
 * The same numbers are already emailed when the payment lands; this exists because an email is
 * easy to lose and a receipt is the thing people go looking for months later. Everything shown
 * is copied straight off the stored invoice — nothing is recomputed on the device, so what the
 * user reads here is what the accounting record says.
 */
@Composable
internal fun InvoiceDetailDialog(
    invoice: Invoice,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", fontWeight = FontWeight.SemiBold)
            }
        },
        title = {
            Column {
                Text(
                    text = "Invoice ${invoice.invoiceNumber}",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.issuedLabel,
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InvoiceLine(label = invoice.description, value = invoice.subtotalLabel)

                // One row per tax component: a split tax (CGST + SGST) has to stay itemised,
                // and a market with no tax registration shows none at all.
                invoice.taxRows().forEach { (label, amount) ->
                    InvoiceLine(label = label, value = amount, muted = true)
                }

                HorizontalDivider(color = colors.outlineVariant)

                InvoiceLine(
                    label = "Total paid",
                    value = invoice.totalLabel,
                    emphasised = true
                )

                if (invoice.taxNote.isNotBlank()) {
                    Text(
                        text = invoice.taxNote,
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = colors.outlineVariant)

                InvoiceLine(label = "Plan", value = invoice.planLabel, muted = true)
                InvoiceLine(label = "Active until", value = invoice.activeUntilLabel, muted = true)
                InvoiceLine(label = "Payment reference", value = invoice.paymentId, muted = true)

                val sellerLines = listOf(
                    invoice.sellerLegalName,
                    invoice.sellerAddress,
                    invoice.sellerTaxId
                ).filter { it.isNotBlank() }

                if (sellerLines.isNotEmpty()) {
                    HorizontalDivider(color = colors.outlineVariant)
                    sellerLines.forEach { line ->
                        Text(
                            text = line,
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun InvoiceLine(
    label: String,
    value: String,
    muted: Boolean = false,
    emphasised: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = if (emphasised) typography.bodyMedium else typography.bodySmall,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
            color = if (muted) colors.onSurfaceVariant else colors.onSurface,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Text(
            text = value,
            style = if (emphasised) typography.bodyMedium else typography.bodySmall,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.SemiBold,
            color = if (muted) colors.onSurfaceVariant else colors.onSurface
        )
    }
}
