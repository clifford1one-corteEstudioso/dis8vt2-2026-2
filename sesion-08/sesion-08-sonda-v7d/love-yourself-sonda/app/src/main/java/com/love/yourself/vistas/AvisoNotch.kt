package com.love.yourself.vistas

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.love.yourself.config.Config

/**
 * El aviso tipo notch: arriba, al tercer swipe de arrastre. Pregunta si salir
 * y cuenta los segundos que hay que quedarse para seguir. No recibe toques: el
 * swipe pasa a Instagram y el servicio lo lee. Solo arma la vista.
 */
object AvisoNotch {

    const val GRAVEDAD = Gravity.TOP or Gravity.CENTER_HORIZONTAL

    /** Debajo de la burbuja y de la caja del modo dev. */
    const val MARGEN_SUPERIOR_DP = BurbujaView.MARGEN_SUPERIOR_DP + 150f

    fun crear(context: Context): TextView = TextView(context).apply {
        val d = resources.displayMetrics.density
        setTextColor(Colores.TEXTO)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
        setPadding((18 * d).toInt(), (10 * d).toInt(), (18 * d).toInt(), (10 * d).toInt())
        background = GradientDrawable().apply {
            cornerRadius = 24 * d
            setColor(Color.argb(230, 0, 0, 0))
        }
    }

    fun texto(segundos: Int) = "${Config.TEXTO_AVISO}\nQuédate en este reel para seguir: $segundos"
}
