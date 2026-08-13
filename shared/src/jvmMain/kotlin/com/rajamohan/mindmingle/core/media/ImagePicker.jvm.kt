package com.rajamohan.mindmingle.core.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual object ImagePicker {
    actual suspend fun pickImages(maxCount: Int): List<ByteArray> = withContext(Dispatchers.IO) {
        val chooser = JFileChooser().apply {
            isMultiSelectionEnabled = true
            fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
            dialogTitle = "Select up to $maxCount photos"
        }

        val result = chooser.showOpenDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return@withContext emptyList()

        chooser.selectedFiles.take(maxCount).mapNotNull { file ->
            try {
                file.readBytes()
            } catch (e: Exception) {
                null
            }
        }
    }
}
