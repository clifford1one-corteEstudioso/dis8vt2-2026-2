package com.love.yourself.lab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView

enum class EtapaCaja { OCULTO, ESPEJO, PRESENCIA }

/**
 * Como SE VE la friccion. Solo arma vistas: no sabe de ventanas, permisos ni
 * servicios.
 *
 * Por eso este archivo es identico en la app real (sesion-04/.../fase-06) y en
 * la app de diseno (sesion-06/love-yourself-ui). Lo que se ajuste en el
 * emulador se copia entero de vuelta, sin mezclar.
 */
object VistasFriccion {

    /** Donde va la caja y a que distancia del borde superior, en px. */
    const val GRAVEDAD_CAJA = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    const val MARGEN_SUPERIOR_CAJA_PX = 120


    fun caja(context: Context): TextView = TextView(context).apply {
        setTextColor(Color.WHITE)
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        aplicarEtapa(this, EtapaCaja.ESPEJO)
    }

    fun texto(segundos: Long, swipesPorMinuto: Double): String {
        val mm = segundos / 60
        val ss = segundos % 60
        return "%02d:%02d   ·   %.0f swipes/min".format(mm, ss, swipesPorMinuto)
    }

    /** La escalada es de peso visual, no de texto. El dato es el mismo. */
    fun aplicarEtapa(v: TextView, etapa: EtapaCaja) {
        when (etapa) {
            EtapaCaja.ESPEJO -> {
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                v.setPadding(26, 12, 26, 12)
                v.alpha = 0.75f
                v.background = GradientDrawable().apply {
                    cornerRadius = 24f
                    setColor(Color.argb(170, 0, 0, 0))
                }
            }
            EtapaCaja.PRESENCIA -> {
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                v.setPadding(44, 26, 44, 26)
                v.alpha = 1f
                v.background = GradientDrawable().apply {
                    cornerRadius = 28f
                    setColor(Color.argb(232, 0, 0, 0))
                    setStroke(3, Color.argb(220, 255, 255, 255))
                }
            }
            EtapaCaja.OCULTO -> {}
        }
    }
}
