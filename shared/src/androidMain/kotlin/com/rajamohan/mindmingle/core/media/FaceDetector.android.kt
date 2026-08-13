package com.rajamohan.mindmingle.core.media

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual object FaceDetector {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()

    actual suspend fun containsFace(bytes: ByteArray): Boolean {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
        val image = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { continuation ->
            FaceDetection.getClient(options).process(image)
                .addOnSuccessListener { faces -> continuation.resume(faces.isNotEmpty()) }
                // A detector/model failure isn't proof the photo lacks a face — don't block
                // the upload over an infra hiccup the user can't do anything about.
                .addOnFailureListener { continuation.resume(true) }
        }
    }
}
