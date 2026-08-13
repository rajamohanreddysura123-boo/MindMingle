package com.rajamohan.mindmingle.core.media

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.gitlive.firebase.storage.Data

actual fun ByteArray.toStorageData(): Data = Data(this)

actual fun ByteArray.decodeToImageBitmapOrNull(): ImageBitmap? {
    return try {
        BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
