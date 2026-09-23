package com.rajamohan.mindmingle.core.payments

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rajamohan.mindmingle.core.AppContext

actual object UrlOpener {
    actual fun open(url: String): Boolean {
        val context = AppContext.get() as? Context ?: return false
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    // The context may not be an Activity when this is called from a non-UI path.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
