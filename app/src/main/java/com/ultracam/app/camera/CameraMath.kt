package com.ultracam.app.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.RggbChannelVector
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Pure camera math: exposure stops, kelvin -> RGGB gains, aspect labels,
 * compass helpers. All functions are side-effect free so they are trivially
 * testable and safe to call from any thread.
 */
object CameraMath {

    /** Standard ISO third-stop ladder. */
    val ISO_STOPS: List<Int> = listOf(
        50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000,
        1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 8000, 10000, 12800,
        16000, 20000, 25600, 32000, 40000, 51200, 64000, 80000, 102400
    )

    /** Standard shutter-speed ladder in nanoseconds, 1/8000s .. 30s. */
    val SHUTTER_STOPS_NS: List<Long> = run {
        val denominators = listOf(
            8000, 6400, 5000, 4000, 3200, 2500, 2000, 1600, 1250, 1000,
            800, 640, 500, 400, 320, 250, 200, 160, 125, 100, 80, 64,
            50, 40, 30, 25, 20, 16, 13, 10, 8
        )
        val seconds = listOf(1.3, 1.6, 2.0, 2.5, 3.2, 4.0, 5.0, 6.0, 8.0, 10.0, 13.0, 15.0, 20.0, 25.0, 30.0)
        denominators.map { (1_000_000_000.0 / it).roundToLong() } +
            seconds.map { (it * 1_000_000_000.0).roundToLong() }
    }

    fun isoStopsInRange(range: IntRange): List<Int> =
        ISO_STOPS.filter { it in range }.ifEmpty { listOf(range.first, range.last) }

    fun snapIso(value: Int, range: IntRange): Int {
        val steps = isoStopsInRange(range)
        return steps.minByOrNull { abs(it - value) } ?: value.coerceIn(range.first, range.last)
    }

    fun shutterStopsInRange(range: LongRange): List<Long> =
        SHUTTER_STOPS_NS.filter { it in range }.ifEmpty { listOf(range.first, range.last) }

    fun snapShutter(ns: Long, range: LongRange): Long {
        val steps = shutterStopsInRange(range)
        return steps.minByOrNull { abs(it - ns) } ?: ns.coerceIn(range.first, range.last)
    }

    /** Normalized 0..1 position of [iso] on the log ladder inside [range]. */
    fun isoToNorm(iso: Int, range: IntRange): Float {
        val steps = isoStopsInRange(range)
        if (steps.size < 2) return 0.5f
        val idx = steps.indexOfFirst { it >= iso }.let { if (it < 0) steps.lastIndex else it }
        return idx.toFloat() / (steps.size - 1)
    }

    fun normToIso(norm: Float, range: IntRange): Int {
        val steps = isoStopsInRange(range)
        if (steps.size < 2) return steps.first()
        val idx = (norm.coerceIn(0f, 1f) * (steps.size - 1)).roundToInt()
        return steps[idx.coerceIn(0, steps.lastIndex)]
    }

    fun shutterToNorm(ns: Long, range: LongRange): Float {
        val steps = shutterStopsInRange(range)
        if (steps.size < 2) return 0.5f
        val idx = steps.indexOfFirst { it >= ns }.let { if (it < 0) steps.lastIndex else it }
        return idx.toFloat() / (steps.size - 1)
    }

    fun normToShutter(norm: Float, range: LongRange): Long {
        val steps = shutterStopsInRange(range)
        if (steps.size < 2) return steps.first()
        val idx = (norm.coerceIn(0f, 1f) * (steps.size - 1)).roundToInt()
        return steps[idx.coerceIn(0, steps.lastIndex)]
    }

    fun formatShutter(ns: Long): String {
        val seconds = ns / 1_000_000_000.0
        return if (seconds >= 0.95) {
            String.format(Locale.US, "%.1fs", seconds)
        } else {
            val denom = (1.0 / seconds).roundToInt()
            "1/$denom"
        }
    }

    fun formatKelvin(k: Int): String = "${k}K"

    fun formatDiopters(d: Float): String =
        if (d <= 0.01f) "INF" else String.format(Locale.US, "%.2fm", 1f / d)

    fun formatEv(index: Int, step: Float): String =
        String.format(Locale.US, "%+.1f", index * step)

    /**
     * Kelvin -> RGGB white-balance gains.
     *
     * Uses the Tanner Helland blackbody approximation to get the chromaticity of
     * an illuminant at [kelvin], then inverts it into green-referenced gains that
     * neutralize that illuminant (gain = G / channel).
     */
    fun kelvinToRggb(kelvin: Int): RggbChannelVector {
        val t = kelvin.coerceIn(1500, 12000) / 100f
        var r: Float
        var g: Float
        var b: Float
        if (t <= 66f) {
            r = 255f
            g = 99.4708025861f * ln(t) - 161.1195681661f
        } else {
            r = 329.698727446f * (t - 60f).pow(-0.1332047592f)
            g = 288.1221695283f * (t - 60f).pow(-0.0755148492f)
        }
        b = when {
            t >= 66f -> 255f
            t <= 19f -> 0f
            else -> 138.5177312231f * ln(t - 10f) - 305.0447927307f
        }
        fun channel(v: Float): Float = v.coerceIn(0f, 255f) / 255f
        val rc = channel(r)
        val gc = channel(g)
        val bc = channel(b)
        fun gain(x: Float): Float = (if (x <= 0.001f) 4f else gc / x).coerceIn(0.25f, 4f)
        return RggbChannelVector(gain(rc), 1f, 1f, gain(bc))
    }

    fun aspectLabel(width: Int, height: Int): String {
        val w = maxOf(width, height)
        val h = minOf(width, height)
        val divisor = gcd(w, h)
        val rw = w / divisor
        val rh = h / divisor
        val ratio = w.toFloat() / h
        return when {
            abs(ratio - 4f / 3f) < 0.02f -> "4:3"
            abs(ratio - 16f / 9f) < 0.02f -> "16:9"
            abs(ratio - 3f / 2f) < 0.02f -> "3:2"
            abs(ratio - 1f) < 0.02f -> "1:1"
            abs(ratio - 21f / 9f) < 0.05f -> "21:9"
            else -> "$rw:$rh"
        }
    }

    fun aspectStrategyForOption(option: AspectRatioOption, size: Size?): AspectRatioStrategy {
        val aspect = when (option) {
            AspectRatioOption.RATIO_4_3, AspectRatioOption.RATIO_1_1 -> AspectRatio.RATIO_4_3
            AspectRatioOption.RATIO_16_9, AspectRatioOption.RATIO_FULL -> AspectRatio.RATIO_16_9
        }
        return AspectRatioStrategy(aspect, AspectRatioStrategy.FALLBACK_RULE_AUTO)
    }

    /** Chooses the CameraX aspect strategy closest to the still-capture size. */
    fun aspectStrategyFor(size: Size?): AspectRatioStrategy {
        if (size == null) {
            return AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO)
        }
        val ratio = size.width.toFloat() / size.height
        val d43 = abs(ratio - 4f / 3f)
        val d169 = abs(ratio - 16f / 9f)
        val ratioValue = if (d43 <= d169) AspectRatio.RATIO_4_3 else AspectRatio.RATIO_16_9
        return AspectRatioStrategy(ratioValue, AspectRatioStrategy.FALLBACK_RULE_AUTO)
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private val COMPASS_16 = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    fun compassLabel(azimuthDeg: Float): String {
        val normalized = ((azimuthDeg % 360f) + 360f) % 360f
        val index = (normalized / 22.5f).roundToInt() % 16
        return COMPASS_16[index]
    }

    fun focalBucket(equiv35mm: Float): String = when {
        equiv35mm < 24f -> "ULTRAWIDE"
        equiv35mm < 34f -> "WIDE"
        equiv35mm < 68f -> "MAIN"
        else -> "TELE"
    }

    /** sqrt-space normalization for histogram visibility. */
    fun sqrtNorm(value: Float, max: Float): Float {
        if (max <= 0f) return 0f
        return sqrt(value.coerceAtLeast(0f) / max)
    }
}
