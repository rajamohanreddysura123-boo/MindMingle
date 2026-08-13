package com.rajamohan.mindmingle.core.media

// Native PHPickerViewController wiring isn't implemented yet — returns no photos on iOS.
actual object ImagePicker {
    actual suspend fun pickImages(maxCount: Int): List<ByteArray> = emptyList()
}
