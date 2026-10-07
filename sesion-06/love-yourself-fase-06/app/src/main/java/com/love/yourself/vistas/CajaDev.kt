package com.love.yourself.vistas

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView

/**
 * La caja del modo dev, debajo de la burbuja: lo que mide cada scroll, para
 * calibrar la deteccion en el celular sin Logcat. Solo arma la vista.
 */
object CajaDev {

    const val GRAVEDAD = Gravity.TOP or Gravity.CENTER_HORIZONTAL

    /** Distancia del borde de arriba, en dp: debajo de la burbuja expandida. */
    const val MARGEN_SUPERIOR_DP = BurbujaView.MARGEN_SUPERIOR_DP + 96f

    fun crear(context: Context): TextView = TextView(context).apply {
        val d = resources.displayMetrics.density
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        alpha = 0.75f
        setPadding((9 * d).toInt(), (4 * d).toInt(), (9 * d).toInt(), (4 * d).toInt())
        background = GradientDrawable().apply {
            cornerRadius = 8 * d
            setColor(Color.argb(170, 0, 0, 0))
        }
    }
}
