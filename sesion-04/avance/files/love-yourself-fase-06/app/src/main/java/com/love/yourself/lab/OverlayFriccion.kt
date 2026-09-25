package com.love.yourself.lab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Las tres etapas de friccion. Ninguna bloquea Instagram.
 *
 *   ESPEJO     discreto, informa
 *   PRESENCIA  mas grande, cuesta ignorarlo
 *   DECISION   pantalla completa, obliga a elegir
 *
 * La tercera es el corazon del proyecto: no prohibe, devuelve la decision.
 * Por eso "Seguir" siempre esta disponible y no cuesta mas que "Salir".
 * Una friccion que castiga se desinstala; una que interrumpe, se piensa.
 */
class OverlayFriccion(private val context: Context) {

    enum class Etapa { OCULTO, ESPEJO, PRESENCIA }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var caja: TextView? = null
    private var etapaActual = Etapa.OCULTO
    private var pantallaDecision: LinearLayout? = null

    private val tipoVentana = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    // ---- caja de datos ----

    fun mostrarCaja(etapa: Etapa, segundos: Long, swipesPorMinuto: Double) {
        if (etapa == Etapa.OCULTO) {
            quitarCaja()
            return
        }
        val v = caja ?: crearCaja().also { caja = it }
        if (etapa != etapaActual) {
            aplicarEtapa(v, etapa)
            etapaActual = etapa
        }
        val mm = segundos / 60
        val ss = segundos % 60
        v.text = "%02d:%02d   ·   %.0f swipes/min".format(mm, ss, swipesPorMinuto)
    }

    private fun crearCaja(): TextView {
        val v = TextView(context).apply {
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoVentana,
            // NOT_TOUCHABLE: los toques la atraviesan. Instagram se usa igual.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }
        wm.addView(v, params)
        aplicarEtapa(v, Etapa.ESPEJO)
        return v
    }

    /** La escalada es de peso visual, no de texto. El dato es el mismo. */
    private fun aplicarEtapa(v: TextView, etapa: Etapa) {
        when (etapa) {
            Etapa.ESPEJO -> {
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                v.setPadding(26, 12, 26, 12)
                v.alpha = 0.75f
                v.background = GradientDrawable().apply {
                    cornerRadius = 24f
                    setColor(Color.argb(170, 0, 0, 0))
                }
            }
            Etapa.PRESENCIA -> {
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                v.setPadding(44, 26, 44, 26)
                v.alpha = 1f
                v.background = GradientDrawable().apply {
                    cornerRadius = 28f
                    setColor(Color.argb(232, 0, 0, 0))
                    setStroke(3, Color.argb(220, 255, 255, 255))
                }
            }
            Etapa.OCULTO -> {}
        }
    }

    private fun quitarCaja() {
        caja?.let { runCatching { wm.removeView(it) } }
        caja = null
        etapaActual = Etapa.OCULTO
    }

    // ---- momento de decision ----

    fun decisionVisible(): Boolean = pantallaDecision != null

    fun mostrarDecision(minutos: Long, alSeguir: () -> Unit, alSalir: () -> Unit) {
        if (pantallaDecision != null) return

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
        fondo.addView(boton("Seguir") {
            quitarDecision()
            alSeguir()
        })
        fondo.addView(boton("Salir") {
            quitarDecision()
            alSalir()
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            tipoVentana,
            // Esta si recibe toques: hay que poder elegir.
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        wm.addView(fondo, params)
        pantallaDecision = fondo
    }

    private fun boton(texto: String, alTocar: () -> Unit) = Button(context).apply {
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

    private fun quitarDecision() {
        pantallaDecision?.let { runCatching { wm.removeView(it) } }
        pantallaDecision = null
    }

    fun ocultarTodo() {
        quitarCaja()
        quitarDecision()
    }
}
