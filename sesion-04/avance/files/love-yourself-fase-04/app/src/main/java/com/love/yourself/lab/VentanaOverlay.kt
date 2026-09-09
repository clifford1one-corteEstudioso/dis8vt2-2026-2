package com.love.yourself.lab

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * El sello visual: una caja chica sobre Instagram con el tiempo de sesion y el
 * ritmo de cortes.
 *
 * Dos decisiones que la hacen un espejo y no un obstaculo:
 *
 *  - FLAG_NOT_TOUCHABLE: los toques la atraviesan. Instagram se sigue usando
 *    igual. Esta fase mide y muestra; no interrumpe.
 *  - El ritmo es de los ultimos [VENTANA_S] segundos, no el acumulado. Un
 *    promedio de veinte minutos se aplana y deja de decir nada; lo que importa
 *    es a que velocidad te esta estimulando AHORA.
 */
class VentanaOverlay(private val context: Context) {

    private var vista: TextView? = null
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun mostrar() {
        if (vista != null) return

        val texto = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTypeface(android.graphics.Typeface.MONOSPACE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(28, 14, 28, 14)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.argb(190, 0, 0, 0))
            }
            text = "00:00  ·  --"
        }

        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipo,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        wm.addView(texto, params)
        vista = texto
    }

    /** Llamar desde el hilo principal. */
    fun actualizar(segundos: Long, ritmoPorSegundo: Double) {
        val v = vista ?: return
        val mm = segundos / 60
        val ss = segundos % 60
        v.text = "%02d:%02d  ·  %.1f cortes/s".format(mm, ss, ritmoPorSegundo)
        v.visibility = View.VISIBLE
    }

    fun ocultar() {
        vista?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        vista = null
    }

    companion object {
        const val VENTANA_S = 15
    }
}
