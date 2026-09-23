package com.rajamohan.mindmingle.core.media

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max

private const val TAG = "ImageCompressor"

actual object ImageCompressor {

    actual suspend fun compress(bytes: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        if (bytes.size <= SKIP_BELOW_BYTES) return@withContext bytes

        try {
            val source = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@withContext bytes

            val longEdge = max(source.width, source.height)
            val ratio = if (longEdge > MAX_DIMENSION) MAX_DIMENSION.toDouble() / longEdge else 1.0
            val targetWidth = (source.width * ratio).toInt().coerceAtLeast(1)
            val targetHeight = (source.height * ratio).toInt().coerceAtLeast(1)

            // TYPE_INT_RGB, not ARGB: the output is JPEG, which has no alpha channel, and writing
            // an image that carries one produces the notorious pink-tinted JPEG.
            val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
            scaled.createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                drawImage(source, 0, 0, targetWidth, targetHeight, null)
                dispose()
            }

            val result = scaled.toJpeg()
            if (result.size >= bytes.size) {
                Napier.d(tag = TAG) { "compression made it larger; keeping the original" }
                return@withContext bytes
            }

            Napier.d(tag = TAG) { "photo ${bytes.size / 1024}KB -> ${result.size / 1024}KB" }
            result
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "compression failed; uploading the original" }
            bytes
        }
    }

    /** ImageIO's default JPEG quality is unspecified, so the writer is configured explicitly. */
    private fun BufferedImage.toJpeg(): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val out = ByteArrayOutputStream()

        ImageIO.createImageOutputStream(out).use { stream ->
            writer.output = stream
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = JPEG_QUALITY / 100f
            }
            writer.write(null, IIOImage(this, null, null), params)
        }
        writer.dispose()

        return out.toByteArray()
    }
}
