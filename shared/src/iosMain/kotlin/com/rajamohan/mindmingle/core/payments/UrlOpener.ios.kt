package com.rajamohan.mindmingle.core.payments

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object UrlOpener {
    actual fun open(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        if (!UIApplication.sharedApplication.canOpenURL(nsUrl)) return false
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any?>(), null)
        return true
    }
}
