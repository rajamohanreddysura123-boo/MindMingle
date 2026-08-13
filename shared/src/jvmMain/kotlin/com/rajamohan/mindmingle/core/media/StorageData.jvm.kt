package com.rajamohan.mindmingle.core.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import dev.gitlive.firebase.storage.Data
import org.jetbrains.skia.Image

actual fun ByteArray.toStorageData(): Data = Data(this)

actual fun ByteArray.decodeToImageBitmapOrNull(): ImageBitmap? {
    return try {
        Image.makeFromEncoded(this).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
