package com.rajamohan.mindmingle.core.media

// No on-device face detection wired on iOS yet (mirrors ImagePicker.ios's not-yet-implemented
// native picker) — accepts by default rather than blocking uploads on an unimplemented check.
actual object FaceDetector {
    actual suspend fun containsFace(bytes: ByteArray): Boolean = true
}
