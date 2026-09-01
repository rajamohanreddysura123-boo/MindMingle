package com.rajamohan.mindmingle.core.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

/**
 * The iOS photo picker.
 *
 * `PHPickerViewController` is part of PhotosUI, which ships with the OS, so unlike ads, payments
 * and push this needs no Swift package and no bridge object in `iosApp` — it can be driven straight
 * from Kotlin/Native. It also runs out of process: the app never gets photo-library permission and
 * never has to ask for it, which is why there is no permission prompt anywhere in this file.
 *
 * `selectionLimit` is what enforces [MAX_PROFILE_PHOTOS] here — the system UI refuses the extra
 * selection itself rather than the app quietly dropping it afterwards.
 */
@OptIn(ExperimentalForeignApi::class)
actual object ImagePicker {

    /** The uniform type identifier every still image conforms to. */
    private const val IMAGE_TYPE = "public.image"

    /** Retains the delegate for as long as the picker is on screen; ARC would otherwise free it. */
    private var activeDelegate: NSObject? = null

    actual suspend fun pickImages(maxCount: Int): List<ByteArray> {
        val presenter = topViewController() ?: return emptyList()

        val configuration = PHPickerConfiguration().apply {
            setSelectionLimit(maxCount.toLong().coerceAtLeast(1L))
        }

        val picked = CompletableDeferred<List<ByteArray>>()
        val controller = PHPickerViewController(configuration)

        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)
                activeDelegate = null

                @Suppress("UNCHECKED_CAST")
                val results = didFinishPicking as List<PHPickerResult>
                if (results.isEmpty()) {
                    picked.complete(emptyList())
                    return
                }

                // Each item is loaded asynchronously and they finish in any order, so the results
                // are collected against a countdown rather than assumed to arrive in sequence.
                val collected = mutableListOf<ByteArray>()
                var remaining = results.size

                results.forEach { result ->
                    // The raw file data, not a decoded UIImage: the bytes go straight to Firebase
                    // Storage, so decoding to a bitmap and re-encoding would cost memory and a
                    // generation of quality for nothing.
                    result.itemProvider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE) { data, _ ->
                        data?.toByteArray()?.let { bytes -> collected.add(bytes) }

                        remaining -= 1
                        if (remaining == 0) picked.complete(collected.take(maxCount))
                    }
                }
            }
        }

        activeDelegate = delegate
        controller.delegate = delegate
        presenter.presentViewController(controller, animated = true, completion = null)

        return picked.await()
    }

    /** Whatever is on screen right now — the picker has to be presented from a live controller. */
    private fun topViewController(): UIViewController? {
        val window: UIWindow = UIApplication.sharedApplication.keyWindow ?: return null
        var controller = window.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}
