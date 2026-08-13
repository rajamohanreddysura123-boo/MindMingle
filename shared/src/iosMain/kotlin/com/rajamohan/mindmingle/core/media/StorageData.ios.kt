package com.rajamohan.mindmingle.core.media

import androidx.compose.ui.graphics.ImageBitmap
import dev.gitlive.firebase.storage.Data
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.toStorageData(): Data {
    val nsData = this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
    return Data(nsData)
}

// Native bitmap preview decoding isn't wired up on iOS yet — thumbnails simply won't render there.
actual fun ByteArray.decodeToImageBitmapOrNull(): ImageBitmap? = null
