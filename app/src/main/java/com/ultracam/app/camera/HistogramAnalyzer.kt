package com.ultracam.app.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Luminance histogram + clipping stats for one analysis frame. */
class HistogramData(
    val bins: IntArray,
    val clipLowPct: Float,
    val clipHighPct: Float
)

/**
 * ImageAnalysis.Analyzer that walks the Y plane of every frame to produce:
 *  - a 256-bin luminance histogram with clipping percentages
 *  - (optionally) a small focus-peaking edge map rendered into a reused Bitmap
 *
 * All work happens on the analysis executor, never the main thread.
 */
class HistogramAnalyzer : ImageAnalysis.Analyzer {

    @Volatile
    var peakingEnabled: Boolean = false

    @Volatile
    var onHistogram: ((HistogramData) -> Unit)? = null

    @Volatile
    var onPeaking: ((Bitmap) -> Unit)? = null

    private var frame = 0
    private var luma: ByteArray = ByteArray(0)
    private var pixels: IntArray = IntArray(0)
    private var edgeBitmap: Bitmap? = null

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val w = image.width
            val h = image.height
            if (w <= 0 || h <= 0) return

            // ---- histogram (sampled) ----
            val step = max(1, sqrt(w.toFloat() * h / 60_000f).toInt())
            val hist = IntArray(256)
            var total = 0
            var y = 0
            while (y < h) {
                val rowStart = y * rowStride
                var x = 0
                while (x < w) {
                    hist[buffer.get(rowStart + x).toInt() and 0xFF]++
                    total++
                    x += step
                }
                y += step
            }
            var low = 0
            var high = 0
            for (i in 0..2) low += hist[i]
            for (i in 253..255) high += hist[i]
            if (total > 0) {
                onHistogram?.invoke(
                    HistogramData(hist, low * 100f / total, high * 100f / total)
                )
            }

            // ---- focus peaking (every other frame while enabled) ----
            frame++
            if (peakingEnabled && frame % 2 == 0) {
                buildPeaking(buffer, w, h, rowStride)
            }
        } catch (_: Throwable) {
            // A single bad frame must never take the camera down.
        } finally {
            image.close()
        }
    }

    private fun buildPeaking(buffer: java.nio.ByteBuffer, w: Int, h: Int, rowStride: Int) {
        val pw = 320.coerceAtMost(w)
        val ph = (h * pw / w).coerceAtLeast(1)

        if (luma.size != pw * ph) {
            luma = ByteArray(pw * ph)
            pixels = IntArray(pw * ph)
            edgeBitmap = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
        }
        val sx = w.toFloat() / pw
        val sy = h.toFloat() / ph

        var jy = 0
        while (jy < ph) {
            val srcY = (jy * sy).toInt().coerceAtMost(h - 1)
            val base = srcY * rowStride
            var ix = 0
            while (ix < pw) {
                val srcX = (ix * sx).toInt().coerceAtMost(w - 1)
                luma[jy * pw + ix] = buffer.get(base + srcX)
                ix++
            }
            jy++
        }

        var py = 1
        while (py < ph - 1) {
            var px = 1
            while (px < pw - 1) {
                val idx = py * pw + px
                val c = luma[idx].toInt() and 0xFF
                val l = luma[idx - 1].toInt() and 0xFF
                val r = luma[idx + 1].toInt() and 0xFF
                val u = luma[idx - pw].toInt() and 0xFF
                val d = luma[idx + pw].toInt() and 0xFF
                val gx = abs(r - l)
                val gy = abs(d - u)
                // center term keeps a faint local-contrast component
                val mag = gx + gy + (abs(r - c) + abs(l - c) + abs(u - c) + abs(d - c)) / 2
                pixels[idx] = if (mag > 46) {
                    val alpha = ((mag - 46) * 3).coerceAtMost(225)
                    if (mag > 120) {
                        (alpha shl 24) or 0x00FF5FD2 // strong edge: prism magenta
                    } else {
                        (alpha shl 24) or 0x0066E5FF // edge: prism cyan
                    }
                } else {
                    0
                }
                px++
            }
            py++
        }

        val bmp = edgeBitmap ?: return
        bmp.setPixels(pixels, 0, pw, 0, 0, pw, ph)
        onPeaking?.invoke(bmp)
    }
}
