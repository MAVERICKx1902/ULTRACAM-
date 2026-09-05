package com.ultracam.app.util

import android.content.Context
import android.util.Size

/**
 * Tiny persistence layer for user preferences. Everything is in-memory first,
 * mirrored to SharedPreferences so settings survive restarts.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("ultracam_prefs", Context.MODE_PRIVATE)

    var mode: String
        get() = sp.getString(KEY_MODE, "AUTO") ?: "AUTO"
        set(value) = sp.edit().putString(KEY_MODE, value).apply()

    var flashMode: String
        get() = sp.getString(KEY_FLASH, "OFF") ?: "OFF"
        set(value) = sp.edit().putString(KEY_FLASH, value).apply()

    var gridMode: String
        get() = sp.getString(KEY_GRID, "OFF") ?: "OFF"
        set(value) = sp.edit().putString(KEY_GRID, value).apply()

    var timerSec: Int
        get() = sp.getInt(KEY_TIMER, 0)
        set(value) = sp.edit().putInt(KEY_TIMER, value).apply()

    var soundOn: Boolean
        get() = sp.getBoolean(KEY_SOUND, true)
        set(value) = sp.edit().putBoolean(KEY_SOUND, value).apply()

    var histogramOn: Boolean
        get() = sp.getBoolean(KEY_HISTOGRAM, false)
        set(value) = sp.edit().putBoolean(KEY_HISTOGRAM, value).apply()

    var peakingOn: Boolean
        get() = sp.getBoolean(KEY_PEAKING, false)
        set(value) = sp.edit().putBoolean(KEY_PEAKING, value).apply()

    var levelOn: Boolean
        get() = sp.getBoolean(KEY_LEVEL, false)
        set(value) = sp.edit().putBoolean(KEY_LEVEL, value).apply()

    var quality: String
        get() = sp.getString(KEY_QUALITY, "QUALITY") ?: "QUALITY"
        set(value) = sp.edit().putString(KEY_QUALITY, value).apply()

    var fpsTarget: Int
        get() = sp.getInt(KEY_FPS, 0)
        set(value) = sp.edit().putInt(KEY_FPS, value).apply()

    var lensId: String?
        get() = sp.getString(KEY_LENS, null)
        set(value) { if (value == null) sp.edit().remove(KEY_LENS).apply() else sp.edit().putString(KEY_LENS, value).apply() }

    var resolution: Size?
        get() {
            val w = sp.getInt(KEY_RES_W, 0)
            val h = sp.getInt(KEY_RES_H, 0)
            return if (w > 0 && h > 0) Size(w, h) else null
        }
        set(value) {
            val e = sp.edit()
            if (value == null) {
                e.remove(KEY_RES_W).remove(KEY_RES_H)
            } else {
                e.putInt(KEY_RES_W, value.width).putInt(KEY_RES_H, value.height)
            }
            e.apply()
        }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_FLASH = "flash"
        const val KEY_GRID = "grid"
        const val KEY_TIMER = "timer"
        const val KEY_SOUND = "sound"
        const val KEY_HISTOGRAM = "histogram"
        const val KEY_PEAKING = "peaking"
        const val KEY_LEVEL = "level"
        const val KEY_QUALITY = "quality"
        const val KEY_FPS = "fps"
        const val KEY_LENS = "lens_id"
        const val KEY_RES_W = "res_w"
        const val KEY_RES_H = "res_h"
    }
}
