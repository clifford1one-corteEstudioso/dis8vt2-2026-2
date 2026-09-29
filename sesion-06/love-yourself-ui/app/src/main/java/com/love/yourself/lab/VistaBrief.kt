package com.love.yourself.lab

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lo que el resumen necesita saber de una sesion cerrada. */
data class DatosBrief(
    val cierreMs: Long,
    val totalMs: Long,
    val arrastradoMs: Long,
    val videos: Int,
    val semanaArrastreMs: Long,
    /** Va antes de "sesión cerrada": "ejemplo", "dev ×10". Vacio en uso normal. */
    val etiqueta: String = "",
    /** Lo que declaro en la bienvenida ("hacer música"). Vacio si la omitio. */
    val actividad: String = "",
    /** La cara del cerebro al cierre (un drawable) y lo que dice. 0 = sin mascota. */
    val mascota: Int = 0,
    val fraseMascota: String = ""
) {
    val buscadoMs get() = (totalMs - arrastradoMs).coerceAtLeast(0L)

    companion object {
        /** Los numeros del wireframe, para mostrar el resumen sin esperar una sesion. */
        fun ejemplo(ahora: Long) = DatosBrief(
            cierreMs = ahora,
            totalMs = 22 * 60_000L,
            arrastradoMs = (19 * 60 + 45) * 1000L,
            videos = 71,
            semanaArrastreMs = (14 * 60 + 20) * 60_000L,
            etiqueta = "ejemplo",
            actividad = "hacer música",
            mascota = com.love.yourself.R.drawable.brain_f,
            fraseMascota = "la próxima salimos antes"
        )
    }
}

/**
 * WF3, el resumen de cierre de sesion. Solo arma la vista: la ventana que la
 * pone encima de todo esta en OverlayBrief.kt.
 *
 * Divide la sesion por origen del contenido y contrasta con el acumulado de la
 * semana, porque el costo del uso no se percibe por episodio sino por agregado.
 *
 * Identico en la app real y en la de diseno: se copia entero.
 */
class VistaBrief(private val context: Context) {

    private val densidad = context.resources.displayMetrics.density

    /** El velo con la tarjeta al centro. alCerrar lo llama el boton "Cerrar". */
    fun crear(datos: DatosBrief, alCerrar: () -> Unit): FrameLayout {
        val tarjeta = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(ColoresBrief.FONDO)
            }
            setPadding(dp(14), dp(19), dp(14), dp(19))
        }
        if (datos.mascota != 0) tarjeta.addView(mascota(datos))
        tarjeta.addView(cabecera(datos), if (datos.mascota != 0) separado() else LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        tarjeta.addView(origen(datos), separado())
        tarjeta.addView(semana(datos), separado())
        tarjeta.addView(acciones(alCerrar), separado())

        val velo = FrameLayout(context).apply {
            setBackgroundColor(ColoresBrief.VELO)
            // Tocar fuera de la tarjeta no hace nada: no queremos que se cierre
            // por accidente, sin haberlo leido.
            isClickable = true
        }
        // En el wireframe la tarjeta ocupa 1204 de 1440 px de ancho.
        val ancho = (context.resources.displayMetrics.widthPixels * ANCHO_TARJETA).toInt()
        velo.addView(
            tarjeta,
            FrameLayout.LayoutParams(ancho, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        return velo
    }

    // ---- secciones ----

    /** El cerebro al cierre: aliviado si saliste antes del cielo, frito si no. */
    private fun mascota(d: DatosBrief): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(context).apply {
            setImageResource(d.mascota)
            adjustViewBounds = true
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(56)))
        addView(texto(d.fraseMascota, 18f), LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginStart = dp(12) })
    }

    private fun cabecera(d: DatosBrief): View {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(d.cierreMs))
        val titulo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ColoresBrief.TARJETA)
            setPadding(dp(8), dp(6), dp(8), dp(10))
            val prefijo = if (d.etiqueta.isEmpty()) "" else "${d.etiqueta} · "
            addView(texto("${prefijo}sesión cerrada · $hora", 13f))
            addView(texto(FormatoBrief.estuviste(d.totalMs), 24f), conMargenArriba(dp(10)))
        }

        val metricas = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(metrica("videos", d.videos.toString()), peso(0))
            // "recuerdas" no se mide: es una pregunta, y el signo es la respuesta.
            addView(metrica("recuerdas", "?"), peso(dp(8)))
            // Sin porcentaje hasta que "patron oscuro" tenga definicion operativa.
            addView(metrica("oscuridad", "—"), peso(dp(8)))
        }

        return seccion(dp(10), dp(9)).apply {
            addView(titulo)
            addView(metricas, conMargenArriba(dp(9)))
        }
    }

    private fun metrica(etiqueta: String, valor: String) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(ColoresBrief.TARJETA)
        setPadding(dp(4), dp(6), dp(4), dp(8))
        addView(texto(etiqueta, 11f, centrado = true))
        addView(texto(valor, 30f, centrado = true), conMargenArriba(dp(6)))
    }

    private fun origen(d: DatosBrief): View {
        val barra = BarraOrigen(context).apply {
            fraccionArrastrada = if (d.totalMs > 0) d.arrastradoMs.toFloat() / d.totalMs else 0f
        }
        val etiquetas = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(texto("${FormatoBrief.reloj(d.arrastradoMs)} arrastrado", 18f), peso(0))
            addView(
                texto("${FormatoBrief.reloj(d.buscadoMs)} buscado", 18f).apply { gravity = Gravity.END },
                peso(0)
            )
        }
        return seccion(dp(12), dp(10)).apply {
            addView(texto(FormatoBrief.deEsos(d.totalMs), 16f))
            addView(
                barra,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(15)).apply { topMargin = dp(10) }
            )
            addView(etiquetas, conMargenArriba(dp(10)))
        }
    }

    private fun semana(d: DatosBrief): View {
        val fila = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = true
            addView(texto(FormatoBrief.semana(d.semanaArrastreMs), 34f).apply { setTypeface(typeface, Typeface.BOLD) })
            addView(
                texto("en arrastre", 18f),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(8) }
            )
        }
        val seccion = seccion(dp(17), dp(13)).apply {
            addView(texto("Esta semana", 16f))
            addView(fila, conMargenArriba(dp(16)))
        }
        // La segunda linea del wireframe ("3h 40 en [actividad]"): la app no
        // puede medir ese tiempo, asi que va como pregunta, igual que "recuerdas ?".
        if (d.actividad.isNotEmpty()) {
            val otra = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                isBaselineAligned = true
                addView(texto("?", 34f).apply { setTypeface(typeface, Typeface.BOLD) })
                addView(
                    texto("en ${d.actividad}", 18f),
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = dp(8) }
                )
            }
            seccion.addView(otra, conMargenArriba(dp(12)))
        }
        return seccion
    }

    private fun acciones(alCerrar: () -> Unit): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(ColoresBrief.SECCION)
        setPadding(dp(20), dp(17), dp(20), dp(17))
        // Todavia no existe la pantalla de detalle: se ve, pero apagado.
        addView(texto("ver detalle", 18f).apply { alpha = 0.4f }, peso(0))
        addView(
            texto("Cerrar", 18f).apply {
                gravity = Gravity.END
                setOnClickListener { alCerrar() }
            },
            peso(0)
        )
    }

    // ---- utilidades ----

    private fun seccion(horizontal: Int, vertical: Int) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(ColoresBrief.SECCION)
        setPadding(horizontal, vertical, horizontal, vertical)
    }

    private fun texto(contenido: String, sp: Float, centrado: Boolean = false) = TextView(context).apply {
        text = contenido
        setTextColor(ColoresBrief.TEXTO)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (centrado) gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun separado() = conMargenArriba(dp(16))

    private fun conMargenArriba(margen: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = margen }

    private fun peso(margenInicio: Int) = LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1f
    ).apply { marginStart = margenInicio }

    private fun dp(valor: Int) = (valor * densidad).toInt()

    private companion object {
        const val ANCHO_TARJETA = 1204f / 1440f
    }
}
