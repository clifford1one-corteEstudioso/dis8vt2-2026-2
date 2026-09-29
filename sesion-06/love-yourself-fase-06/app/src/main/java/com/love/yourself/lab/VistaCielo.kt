package com.love.yourself.lab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * El cielo a pantalla completa (Figma: "sky"). Aparece a los MIN_CIELO
 * minutos y pregunta sin decidir por la persona: "Seguir" y "Salir" pesan lo
 * mismo. Solo arma la vista; la ventana la pone OverlayFriccion.
 *
 * Todo se ubica en proporciones del lienzo de Figma (1440 x 3120).
 * Identico en la app real y en la de diseno: se copia entero.
 */
class VistaCielo(private val context: Context) {

    private val ancho = context.resources.displayMetrics.widthPixels.toFloat()
    private val alto = context.resources.displayMetrics.heightPixels.toFloat()
    private fun x(v: Float) = (v / 1440f * ancho).toInt()
    private fun y(v: Float) = (v / 3120f * alto).toInt()

    fun crear(minutos: Long, alSeguir: () -> Unit, alSalir: () -> Unit): FrameLayout {
        val raiz = FrameLayout(context).apply { isClickable = true }
        raiz.addView(CieloView(context), completo())

        // Mensaje, arriba a la izquierda.
        raiz.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(texto(Config.TEXTO_CIELO, 70f, negrita = false))
                addView(texto("Llevas $minutos minutos aquí.", 38f, negrita = false).apply { alpha = 0.7f },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = y(40f)
                    })
            },
            FrameLayout.LayoutParams(x(620f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = x(123f)
                topMargin = y(341f)
            }
        )

        // Tarjeta del clima, a la derecha.
        raiz.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = x(45f).toFloat()
                    setColor(Color.argb(79, 255, 255, 255))
                }
                setPadding(x(50f), x(25f), x(50f), x(25f))
                addView(View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#FFF6CC"))
                    }
                }, LinearLayout.LayoutParams(x(144f), x(144f)))
                addView(texto(Config.CLIMA_TEMPERATURA, 50f, negrita = true).apply { gravity = Gravity.CENTER },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = x(12f)
                    })
                addView(texto(Config.CLIMA_LUGAR, 35f, negrita = false).apply { gravity = Gravity.CENTER })
            },
            FrameLayout.LayoutParams(x(504f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = x(835f)
                topMargin = y(1396f)
            }
        )

        // Seguir / Salir, abajo. Pesan lo mismo a proposito: el proyecto
        // devuelve la decision, no la toma.
        raiz.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(boton("Seguir", alSeguir), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(boton("Salir", alSalir), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = x(40f)
                })
            },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                leftMargin = x(123f)
                rightMargin = x(123f)
                bottomMargin = y(380f)
            }
        )
        return raiz
    }

    private fun texto(contenido: String, pxFigma: Float, negrita: Boolean) = TextView(context).apply {
        text = contenido
        setTextColor(Color.parseColor("#010101"))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, x(pxFigma).toFloat())
        if (negrita) setTypeface(typeface, Typeface.BOLD)
    }

    private fun boton(texto: String, alTocar: () -> Unit) = TextView(context).apply {
        text = texto
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor("#010101"))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, x(50f).toFloat())
        setPadding(0, x(40f), 0, x(40f))
        background = GradientDrawable().apply {
            cornerRadius = x(45f).toFloat()
            setColor(Color.argb(79, 255, 255, 255))
            setStroke(x(4f), Color.argb(160, 1, 1, 1))
        }
        setOnClickListener { alTocar() }
    }

    private fun completo() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
}
