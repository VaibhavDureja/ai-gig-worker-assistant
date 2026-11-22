package com.parkcast.mirror

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.roundToInt

/** Captures screen frames and produces downscaled JPEGs for the local stream. */
internal class CaptureFrames(
    private val projection: MediaProjection,
    private val dpi: Int,
) {
    private val worker = HandlerThread("ParkCastJpegEncoder").apply { start() }
    private val handler = Handler(worker.looper)
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val latest = AtomicReference<ByteArray?>(null)
    @Volatile private var lastCaptureTime = 0L
    @Volatile private var stopped = false

    fun latestJpeg(): ByteArray? = latest.get()

    /** Called on the service's main thread. Video is capped at ~960 pixels wide / 8fps. */
    fun configure(screenWidth: Int, screenHeight: Int) {
        if (stopped || screenWidth < 16 || screenHeight < 16) return
        val width = minOf(screenWidth, 960)
        val height = max(1, (screenHeight.toDouble() * width / screenWidth).roundToInt())
        val next = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        next.setOnImageAvailableListener({ imageReader -> encode(imageReader) }, handler)
        val oldReader = reader
        if (virtualDisplay == null) {
            virtualDisplay = projection.createVirtualDisplay(
                "ParkCast-view-only", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                next.surface, null, handler
            )
        } else {
            virtualDisplay?.resize(width, height, dpi)
            virtualDisplay?.surface = next.surface
        }
        reader = next
        oldReader?.setOnImageAvailableListener(null, null)
        oldReader?.close()
    }

    private fun encode(imageReader: ImageReader) {
        val image = try { imageReader.acquireLatestImage() } catch (_: Exception) { null } ?: return
        try {
            if (stopped) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastCaptureTime < 125) return
            lastCaptureTime = now
            val plane = image.planes.firstOrNull() ?: return
            if (plane.pixelStride != 4) return
            val width = image.width
            val height = image.height
            val rowStride = plane.rowStride
            val paddedWidth = rowStride / plane.pixelStride
            if (paddedWidth < width) return
            val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
            try {
                plane.buffer.rewind()
                padded.copyPixelsFromBuffer(plane.buffer)
                val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
                try {
                    val bytes = ByteArrayOutputStream()
                    if (cropped.compress(Bitmap.CompressFormat.JPEG, 67, bytes)) {
                        latest.set(bytes.toByteArray())
                    }
                } finally {
                    cropped.recycle()
                }
            } finally {
                padded.recycle()
            }
        } catch (_: Exception) {
            // Rotation or protected content can invalidate a frame; keep the last good one.
        } finally {
            image.close()
        }
    }

    fun close() {
        stopped = true
        reader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        reader?.close()
        reader = null
        virtualDisplay = null
        latest.set(null)
        worker.quitSafely()
    }
}
