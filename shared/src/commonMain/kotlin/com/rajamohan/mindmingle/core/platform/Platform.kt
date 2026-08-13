package com.rajamohan.mindmingle.core.platform

enum class AppPlatform {
    MOBILE,
    DESKTOP
}

expect fun getCurrentPlatform(): AppPlatform