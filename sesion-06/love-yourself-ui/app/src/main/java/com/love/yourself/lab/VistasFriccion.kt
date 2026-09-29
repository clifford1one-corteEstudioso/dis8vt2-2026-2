package com.love.yourself.lab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
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

    /**
     * La pantalla de decision. No prohibe, devuelve la decision: por eso
     * "Seguir" siempre esta disponible y no cuesta mas que "Salir".
     */
    fun decision(
        context: Context,
        minutos: Long,
        alSeguir: () -> Unit,
        alSalir: () -> Unit
    ): LinearLayout {
        val fondo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(242, 8, 8, 10))
            setPadding(80, 0, 80, 0)
        }

        fondo.addView(TextView(context).apply {
            text = "Llevas $minutos minutos aquí."
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            gravity = Gravity.CENTER
        })

        fondo.addView(TextView(context).apply {
            text = "\n¿Querías estar todo este rato?\n"
            setTextColor(Color.argb(170, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 56)
        })

        // Los dos botones pesan lo mismo a proposito. El proyecto devuelve la
        // decision; no la toma por el usuario ni lo empuja hacia un lado.
        fondo.addView(boton(context, "Seguir", alSeguir))
        fondo.addView(boton(context, "Salir", alSalir))
        return fondo
    }

    private fun boton(context: Context, texto: String, alTocar: () -> Unit) = Button(context).apply {
        text = texto
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        setPadding(0, 34, 0, 34)
        background = GradientDrawable().apply {
            cornerRadius = 18f
            setColor(Color.TRANSPARENT)
            setStroke(3, Color.argb(200, 255, 255, 255))
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 18, 0, 18) }
        setOnClickListener { alTocar() }
    }
}
