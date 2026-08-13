package com.rajamohan.mindmingle.core.media

import androidx.compose.ui.graphics.ImageBitmap
import dev.gitlive.firebase.storage.Data

/** Wraps raw bytes into gitlive Storage's platform-specific upload payload type. */
expect fun ByteArray.toStorageData(): Data

/** Decodes picked image bytes into a paintable bitmap for thumbnail previews, or null if invalid. */
expect fun ByteArray.decodeToImageBitmapOrNull(): ImageBitmap?
