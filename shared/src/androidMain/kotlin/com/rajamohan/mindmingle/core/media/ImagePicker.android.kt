package com.rajamohan.mindmingle.core.media

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.rajamohan.mindmingle.core.AppContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object ImagePicker {
    private const val RC_PICK_IMAGES = 9002

    private var pendingContinuation: CancellableContinuation<List<ByteArray>>? = null
    private var pendingMaxCount: Int = 5

    actual suspend fun pickImages(maxCount: Int): List<ByteArray> = suspendCancellableCoroutine { continuation ->
        val activity = AppContext.get() as? Activity
        if (activity == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        pendingContinuation = continuation
        pendingMaxCount = maxCount

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        activity.startActivityForResult(Intent.createChooser(intent, "Select Photos"), RC_PICK_IMAGES)

        continuation.invokeOnCancellation { pendingContinuation = null }
    }

    /** Wired from MainActivity.onActivityResult. Returns true if this result belonged to the picker. */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != RC_PICK_IMAGES) return false
        val continuation = pendingContinuation ?: return true
        pendingContinuation = null

        if (resultCode != Activity.RESULT_OK || data == null) {
            continuation.resume(emptyList())
            return true
        }

        val uris = mutableListOf<Uri>()
        val clipData = data.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount.coerceAtMost(pendingMaxCount)) {
                uris.add(clipData.getItemAt(i).uri)
            }
        } else {
            data.data?.let { uris.add(it) }
        }

        val activity = AppContext.get() as? Activity
        val bytesList = uris.mapNotNull { uri ->
            try {
                activity?.contentResolver?.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
        }
        continuation.resume(bytesList)
        return true
    }
}
