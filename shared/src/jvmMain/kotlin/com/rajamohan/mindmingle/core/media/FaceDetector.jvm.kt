package com.rajamohan.mindmingle.core.media

// ML Kit has no JVM/desktop artifact — desktop photo uploads skip the face check.
actual object FaceDetector {
    actual suspend fun containsFace(bytes: ByteArray): Boolean = true
}
