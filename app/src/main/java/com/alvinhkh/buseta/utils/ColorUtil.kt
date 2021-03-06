package com.alvinhkh.buseta.utils

import android.graphics.Color
import androidx.annotation.ColorInt

object ColorUtil {

    @JvmStatic
    @ColorInt
    fun darkenColor(@ColorInt color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        return Color.HSVToColor(hsv)
    }
}