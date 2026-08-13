package com.rajamohan.mindmingle.core.media

/** Gates profile photo uploads: true only if the image has a detectable human face. */
expect object FaceDetector {
    suspend fun containsFace(bytes: ByteArray): Boolean
}
