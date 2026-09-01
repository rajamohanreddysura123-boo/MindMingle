package com.rajamohan.mindmingle.core.media

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
        pendingMaxCount = maxCount.coerceAtLeast(1)

        activity.startActivityForResult(pickerIntent(pendingMaxCount), RC_PICK_IMAGES)

        continuation.invokeOnCancellation { pendingContinuation = null }
    }

    /**
     * Android 13+ has a system photo picker that enforces a maximum selection itself: the user is
     * told "select up to 3" and simply cannot pick a fourth. That is the whole reason to prefer it
     * — the older ACTION_GET_CONTENT chooser lets someone pick ten and then silently loses seven,
     * which reads as the app dropping their photos.
     *
     * Below 33 (minSdk here is 29) the chooser is the only option and the count is enforced by
     * truncation in [handleActivityResult].
     */
    private fun pickerIntent(maxCount: Int): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
                // The extra is only valid for multi-select; asking for one photo means the
                // single-select picker, which rejects the extra outright.
                if (maxCount > 1) {
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxCount)
                }
            }
        }

        val chooser = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, maxCount > 1)
        }
        return Intent.createChooser(chooser, "Select Photos")
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
