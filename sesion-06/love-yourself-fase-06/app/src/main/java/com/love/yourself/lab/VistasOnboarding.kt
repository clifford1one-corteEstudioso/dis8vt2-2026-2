package com.love.yourself.lab

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Una app que se puede vigilar: paquete y nombre visible. */
class AppInstalada(val paquete: String, val nombre: String)

/**
 * Las pantallas de bienvenida (Figma: WF1a, WF1b y "¿A qué quieres que le
 * pongamos ojo?"). Solo arman vistas: que pasa al tocar lo decide quien las
 * usa (OnboardingActivity en la app real, un simulador en la de diseno).
 *
 * Identico en las dos apps: se copia entero.
 */
class VistasOnboarding(private val context: Context) {

    private val d = context.resources.displayMetrics.density
    private fun dp(v: Float) = (v * d).toInt()

    // ---- colores de los wireframes: cambiar aca ----
    private val fondo = Color.parseColor("#262626")
    private val modal = Color.parseColor("#3A3A3A")
    private val linea = Color.parseColor("#5A5A5A")
    private val campo = Color.parseColor("#2E2E2E")
    private val bordeCampo = Color.parseColor("#8C8C8C")
    private val ayuda = Color.parseColor("#A6A6A6")
    private val panel = Color.parseColor("#4D4D4D")
    private val bordePanel = Color.parseColor("#737373")

    /**
     * WF1a: el modal de un permiso. El enlace "¿Por qué...?" despliega la
     * explicacion dentro del mismo modal, sin salir.
     */
    fun permiso(
        texto: String,
        porQue: String,
        alSalir: () -> Unit,
        alActivar: () -> Unit
    ): View {
        val explicacion = texto(porQue, 14f, ayuda).apply { visibility = View.GONE }
        val enlace = texto("¿Por qué requerimos este permiso?", 14f, Color.WHITE).apply {
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                explicacion.visibility = if (explicacion.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        val cuerpo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto(texto, 17f, Color.WHITE))
            addView(enlace, margenArriba(dp(18f)))
            addView(explicacion, margenArriba(dp(10f)))
        }
        return pantalla(tarjeta(cuerpo, "Salir" to alSalir, "Activar" to alActivar))
    }

    /** "¿A qué quieres que le pongamos ojo?": elegir las apps donde se mide. */
    fun apps(
        instaladas: List<AppInstalada>,
        marcadas: Set<String>,
        alCambiar: (paquete: String, si: Boolean) -> Unit,
        alContinuar: () -> Unit
    ): View {
        val lista = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            for (app in instaladas) {
                addView(CheckBox(context).apply {
                    text = app.nombre
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    isChecked = app.paquete in marcadas
                    setOnCheckedChangeListener { _, si -> alCambiar(app.paquete, si) }
                })
            }
        }
        val caja = ScrollView(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(30f).toFloat()
                setColor(panel)
                setStroke(dp(2f), bordePanel)
            }
            addView(lista)
        }
        val columna = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(35f), dp(90f), dp(35f), dp(40f))
            addView(texto("¿A qué quieres que le pongamos ojo?", 18f, Color.WHITE))
            addView(texto("Solo cuenta el scroll a pantalla completa en estas apps.", 13f, ayuda), margenArriba(dp(6f)))
            addView(caja, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(16f) })
            addView(boton("Continuar", alContinuar).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(30f).toFloat()
                    setColor(modal)
                }
            }, margenArriba(dp(16f)))
        }
        return FrameLayout(context).apply {
            setBackgroundColor(fondo)
            addView(columna, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    /** WF1b: la actividad que la persona quiere hacer mas. Aparece en el resumen. */
    fun declaracion(
        actual: String,
        alOmitir: () -> Unit,
        alContinuar: (actividad: String) -> Unit
    ): View {
        val entrada = EditText(context).apply {
            setText(actual)
            hint = "hacer música, ejercicio, leer…"
            setHintTextColor(Color.parseColor("#CCCCCC"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = true
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = dp(4f).toFloat()
                setColor(campo)
                setStroke(dp(1f), bordeCampo)
            }
        }
        val cuerpo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto("¿Qué te gustaría hacer más y no alcanzas?", 17f, Color.WHITE))
            addView(entrada, margenArriba(dp(22f)))
            addView(texto("Aparecerá en tu resumen semanal como punto de comparación.", 11f, ayuda).apply {
                gravity = Gravity.START
            }, margenArriba(dp(8f)))
        }
        return pantalla(tarjeta(cuerpo, "Omitir" to alOmitir, "Continuar" to { alContinuar(entrada.text.toString().trim()) }))
    }

    // ---- piezas ----

    /** El modal: contenido arriba y dos botones abajo, separados por lineas. */
    private fun tarjeta(cuerpo: View, izq: Pair<String, () -> Unit>, der: Pair<String, () -> Unit>): View {
        val botones = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(boton(izq.first, izq.second), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(View(context).apply { setBackgroundColor(linea) }, LinearLayout.LayoutParams(dp(1f), ViewGroup.LayoutParams.MATCH_PARENT))
            addView(boton(der.first, der.second), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(22f).toFloat()
                setColor(modal)
            }
            addView(cuerpo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(26f), dp(40f), dp(26f), dp(30f))
            })
            addView(View(context).apply { setBackgroundColor(linea) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f)).apply {
                leftMargin = dp(18f)
                rightMargin = dp(18f)
            })
            addView(botones)
        }
    }

    /** Fondo oscuro con el modal centrado. */
    private fun pantalla(contenido: View) = FrameLayout(context).apply {
        setBackgroundColor(fondo)
        addView(contenido, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            leftMargin = dp(20f)
            rightMargin = dp(20f)
        })
    }

    private fun texto(contenido: String, sp: Float, color: Int) = TextView(context).apply {
        text = contenido
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        gravity = Gravity.CENTER
    }

    private fun boton(texto: String, alTocar: () -> Unit) = texto(texto, 16f, Color.WHITE).apply {
        setPadding(0, dp(18f), 0, dp(18f))
        setOnClickListener { alTocar() }
    }

    private fun margenArriba(m: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = m }
}
