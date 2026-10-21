package com.love.yourself.vistas

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.love.yourself.R

/** Lo que el resumen necesita saber de una sesion cerrada. */
data class DatosBrief(
    val cierreMs: Long,
    val totalMs: Long,
    /** Tiempo en arrastre segun el clasificador (pestaña Reels, sugerencias, anuncios). */
    val arrastreMs: Long,
    /** Las ultimas sesiones de hoy, de la mas vieja a la mas nueva, como mucho cinco. */
    val sesionesHoy: List<SesionDelDia> = emptyList(),
    /** Todo lo de hoy en la app, incluida esta sesion. */
    val totalHoyMs: Long = 0L,
    /** Va antes de "sesión cerrada": "ejemplo", "dev ×10". Vacio en uso normal. */
    val etiqueta: String = "",
    /** La cara del cerebro al cierre (un drawable) y lo que dice. 0 = sin mascota. */
    val mascota: Int = 0,
    val fraseMascota: String = ""
) {
    val dirigidoMs get() = (totalMs - arrastreMs).coerceAtLeast(0L)

    companion object {
        /** Numeros de ejemplo, para mostrar el resumen sin esperar una sesion. */
        fun ejemplo(ahora: Long): DatosBrief {
            val min = 60_000L
            return DatosBrief(
                cierreMs = ahora,
                totalMs = 22 * min,
                arrastreMs = 17 * min,
                sesionesHoy = listOf(
                    SesionDelDia(ahora - 9 * 60 * min, 6 * min),
                    SesionDelDia(ahora - 6 * 60 * min, 14 * min),
                    SesionDelDia(ahora - 4 * 60 * min, 3 * min),
                    SesionDelDia(ahora - 2 * 60 * min, 31 * min),
                    SesionDelDia(ahora - 22 * min, 22 * min)
                ),
                totalHoyMs = 81 * min,
                etiqueta = "ejemplo",
                mascota = R.drawable.brain_f,
                fraseMascota = "la próxima salimos antes"
            )
        }
    }
}

/**
 * El resumen de cierre de sesion: poco y grande, para leerlo de un vistazo.
 *
 *   - la mascota y lo que dice
 *   - el tiempo de esta sesion y una barra arrastre / dirigido
 *   - hoy: el total y las ultimas sesiones (como mucho cinco) en barras
 *
 * Solo arma la vista: la ventana que la pone encima de todo esta en OverlayBrief.kt.
 */
class VistaBrief(private val context: Context) {

    private val densidad = context.resources.displayMetrics.density

    /**
     * El velo con la tarjeta al centro.
     *
     * @param alCerrar se llama cuando el resumen termino de irse (Cerrar, atras o ver detalle).
     * @param alVerDetalle abre el detalle (Inicio). Sin esto, "ver detalle" se ve apagado.
     */
    fun crear(datos: DatosBrief, alCerrar: () -> Unit, alVerDetalle: (() -> Unit)? = null): Pantalla {
        // Tocar fuera de la tarjeta no hace nada: no queremos que se cierre
        // por accidente, sin haberlo leido.
        val pantalla = Pantalla(context).apply { isClickable = true }
        val velo = View(context).apply { setBackgroundColor(Colores.VELO) }
        pantalla.addView(velo, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // Cada seccion agrega aca lo que hace al entrar (contar, llenar la barra...).
        val alEntrar = mutableListOf<() -> Unit>()
        val tarjeta = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Colores.FONDO)
            }
            setPadding(dp(16), dp(22), dp(16), dp(16))
        }
        if (datos.mascota != 0) tarjeta.addView(mascota(datos, alEntrar))
        tarjeta.addView(sesion(datos, alEntrar), if (datos.mascota != 0) separado() else conMargenArriba(0))
        tarjeta.addView(hoy(datos, alEntrar), separado())
        tarjeta.addView(acciones(pantalla, alCerrar, alVerDetalle), separado())

        val ancho = (context.resources.displayMetrics.widthPixels * ANCHO_TARJETA).toInt()
        pantalla.addView(tarjeta, FrameLayout.LayoutParams(ancho, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        pantalla.entrada = {
            velo.alpha = 0f
            velo.animate().alpha(1f).setStartDelay(0L).setDuration(260L).setInterpolator(Animacion.SUAVE).start()
            tarjeta.alpha = 0f
            tarjeta.translationY = dp(48).toFloat()
            tarjeta.scaleX = 0.96f
            tarjeta.scaleY = 0.96f
            tarjeta.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(60L).setDuration(460L).setInterpolator(Animacion.FRENA)
                .start()
            for (i in 0 until tarjeta.childCount) {
                Animacion.entrar(tarjeta.getChildAt(i), demoraMs = 140L + i * 70L, desdeDp = 14f)
            }
            alEntrar.forEach { it() }
        }
        pantalla.salida = { fin ->
            velo.animate().alpha(0f).setStartDelay(0L).setDuration(220L).setInterpolator(Animacion.SUAVE).start()
            tarjeta.animate().alpha(0f).translationY(dp(28).toFloat()).scaleX(0.98f).scaleY(0.98f)
                .setStartDelay(0L).setDuration(220L).setInterpolator(Animacion.ACELERA)
                .withEndAction(fin)
                .start()
        }
        return pantalla
    }

    // ---- secciones ----

    /** El cerebro al cierre: aliviado si saliste antes del cielo, frito si no. */
    private fun mascota(d: DatosBrief, alEntrar: MutableList<() -> Unit>): View {
        val cerebro = ImageView(context).apply {
            Imagenes.cargar(resources, d.mascota, dp(140))?.let { setImageBitmap(it) }
            adjustViewBounds = true
        }
        alEntrar += { Animacion.brotar(cerebro, 300L) }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(cerebro, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(76)))
            addView(texto(d.fraseMascota, 21f), LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(14) })
        }
    }

    /** Esta sesion: el total grande y, debajo, cuanto fue arrastre y cuanto dirigido. */
    private fun sesion(d: DatosBrief, alEntrar: MutableList<() -> Unit>): View {
        val total = texto(Formato.minutos(d.totalMs), 46f).apply { setTypeface(typeface, Typeface.BOLD) }
        val barra = BarraOrigen(context).apply {
            fraccionArrastrada = if (d.totalMs > 0) d.arrastreMs.toFloat() / d.totalMs else 0f
        }
        val arrastre = texto("${Formato.minutos(d.arrastreMs)} arrastre", 19f).apply { maxLines = 1 }
        val dirigido = texto("${Formato.minutos(d.dirigidoMs)} dirigido", 19f).apply {
            gravity = Gravity.END
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val etiquetas = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(arrastre, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(dirigido, peso(dp(8)))
        }
        alEntrar += {
            Animacion.contar(total, d.totalMs, 380L) { Formato.minutos(it) }
            barra.llenar(450L)
        }
        val prefijo = if (d.etiqueta.isEmpty()) "" else "${d.etiqueta} · "
        return seccion().apply {
            addView(texto("${prefijo}Esta sesión", 17f).apply { setTextColor(Colores.TEXTO_SUAVE) })
            addView(total, conMargenArriba(dp(4)))
            addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)).apply { topMargin = dp(14) })
            addView(etiquetas, conMargenArriba(dp(10)))
        }
    }

    /** Hoy: el total del dia y las ultimas sesiones en barras. */
    private fun hoy(d: DatosBrief, alEntrar: MutableList<() -> Unit>): View {
        val total = texto(Formato.minutos(d.totalHoyMs), 34f).apply { setTypeface(typeface, Typeface.BOLD) }
        val grafico = GraficoSesiones(context).apply { sesiones = d.sesionesHoy }
        alEntrar += {
            Animacion.contar(total, d.totalHoyMs, 600L) { Formato.minutos(it) }
            grafico.crecer(650L)
        }
        return seccion().apply {
            addView(texto("Hoy", 17f).apply { setTextColor(Colores.TEXTO_SUAVE) })
            addView(total, conMargenArriba(dp(4)))
            if (d.sesionesHoy.isNotEmpty()) {
                addView(grafico, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170)).apply {
                    topMargin = dp(16)
                })
            }
        }
    }

    private fun acciones(pantalla: Pantalla, alCerrar: () -> Unit, alVerDetalle: (() -> Unit)?): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Colores.SECCION)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            addView(texto("ver detalle", 20f).apply {
                if (alVerDetalle == null) {
                    alpha = 0.4f
                } else {
                    Animacion.presionable(this)
                    setOnClickListener {
                        alVerDetalle()
                        pantalla.salir(alCerrar)
                    }
                }
            }, peso(0))
            addView(texto("Cerrar", 20f).apply {
                gravity = Gravity.END
                Animacion.presionable(this)
                setOnClickListener { pantalla.salir(alCerrar) }
            }, peso(0))
        }

    // ---- utilidades ----

    private fun seccion() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Colores.SECCION)
        setPadding(dp(18), dp(16), dp(18), dp(18))
    }

    private fun texto(contenido: String, sp: Float) = TextView(context).apply {
        text = contenido
        setTextColor(Colores.TEXTO)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    private fun separado() = conMargenArriba(dp(14))

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

    companion object {
        private const val ANCHO_TARJETA = 1300f / 1440f
    }
}
