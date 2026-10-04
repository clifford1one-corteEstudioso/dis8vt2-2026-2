package com.love.yourself.vistas

import android.animation.LayoutTransition
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
    /** La semana pasada hasta el mismo dia, para comparar. -1 si no hay con que. */
    val semanaPasadaMs: Long = -1L,
    /** Va antes de "sesión cerrada": "ejemplo", "dev ×10". Vacio en uso normal. */
    val etiqueta: String = "",
    /** Lo que declaro en la bienvenida ("hacer música"). Vacio si la omitio. */
    val actividad: String = "",
    /** La cara del cerebro al cierre (un drawable) y lo que dice. 0 = sin mascota. */
    val mascota: Int = 0,
    val fraseMascota: String = "",
    /** Lo que ya respondio en "recuerdas". Vacio: todavia es "?". */
    val recuerdas: String = ""
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
            semanaPasadaMs = (12 * 60 + 5) * 60_000L,
            etiqueta = "ejemplo",
            actividad = "hacer música",
            mascota = R.drawable.brain_f,
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
 * Por eso los numeros se cuentan al aparecer: el agregado se ve juntarse.
 *
 * Identico en la app real y en la de diseno: se copia entero.
 */
class VistaBrief(private val context: Context) {

    private val densidad = context.resources.displayMetrics.density

    /**
     * El velo con la tarjeta al centro.
     *
     * @param alCerrar se llama cuando el resumen termino de irse (Cerrar, atras o ver detalle).
     * @param alVerDetalle abre el detalle (Inicio). Sin esto, "ver detalle" se ve apagado.
     * @param alResponder guarda lo que contesto en "recuerdas". Sin esto, el "?" no se toca.
     */
    fun crear(
        datos: DatosBrief,
        alCerrar: () -> Unit,
        alVerDetalle: (() -> Unit)? = null,
        alResponder: ((String) -> Unit)? = null
    ): Pantalla {
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
                cornerRadius = dp(18).toFloat()
                setColor(Colores.FONDO)
            }
            setPadding(dp(14), dp(19), dp(14), dp(19))
        }
        if (datos.mascota != 0) tarjeta.addView(mascota(datos, alEntrar))
        tarjeta.addView(cabecera(datos, alResponder, alEntrar), if (datos.mascota != 0) separado() else conMargenArriba(0))
        tarjeta.addView(origen(datos, alEntrar), separado())
        tarjeta.addView(semana(datos, alEntrar), separado())
        tarjeta.addView(acciones(pantalla, alCerrar, alVerDetalle), separado())

        // En el wireframe la tarjeta ocupa 1204 de 1440 px de ancho.
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
            Imagenes.cargar(resources, d.mascota, dp(110))?.let { setImageBitmap(it) }
            adjustViewBounds = true
        }
        alEntrar += { Animacion.brotar(cerebro, 300L) }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(cerebro, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(56)))
            addView(texto(d.fraseMascota, 18f), LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(12) })
        }
    }

    private fun cabecera(d: DatosBrief, alResponder: ((String) -> Unit)?, alEntrar: MutableList<() -> Unit>): View {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(d.cierreMs))
        val titulo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Colores.TARJETA)
            setPadding(dp(8), dp(6), dp(8), dp(10))
            val prefijo = if (d.etiqueta.isEmpty()) "" else "${d.etiqueta} · "
            addView(texto("${prefijo}sesión cerrada · $hora", 13f))
            addView(texto(Formato.estuviste(d.totalMs), 24f), conMargenArriba(dp(10)))
        }

        val videos = texto(d.videos.toString(), 30f, centrado = true)
        // "recuerdas" no se mide: es una pregunta, y el signo es la respuesta
        // hasta que la persona quiera contestarla.
        val recuerdas = texto(d.recuerdas.ifEmpty { "?" }, 30f, centrado = true)
        val cajaRecuerdas = metrica("recuerdas", recuerdas)
        val metricas = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(metrica("videos", videos, Formato.porVideo(d.arrastradoMs, d.videos)), igual(0))
            addView(cajaRecuerdas, igual(dp(8)))
            // Sin porcentaje hasta que "patron oscuro" tenga definicion operativa.
            addView(metrica("oscuridad", texto("—", 30f, centrado = true)), igual(dp(8)))
        }

        val seccion = seccion(dp(10), dp(9)).apply {
            addView(titulo)
            addView(metricas, conMargenArriba(dp(9)))
        }
        if (alResponder != null) {
            val pregunta = preguntaRecuerdas(recuerdas, alResponder)
            seccion.addView(pregunta, conMargenArriba(dp(9)))
            // Abrir y cerrar la pregunta estira la tarjeta suave, no de golpe.
            seccion.layoutTransition = LayoutTransition()
            cajaRecuerdas.setOnClickListener {
                pregunta.visibility = if (pregunta.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
            Animacion.presionable(cajaRecuerdas)
            // Un saltito despues de entrar: dice que el "?" se toca, sin explicarlo.
            if (d.recuerdas.isEmpty()) alEntrar += { Animacion.latido(recuerdas, 1500L) }
        }
        alEntrar += { Animacion.contar(videos, d.videos.toLong(), 380L) { it.toString() } }
        return seccion
    }

    /** "¿cuántos de esos videos recuerdas?" y cuatro respuestas. Escondida hasta tocar el "?". */
    private fun preguntaRecuerdas(valor: TextView, alResponder: (String) -> Unit): View {
        val caja = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setBackgroundColor(Colores.TARJETA)
            setPadding(dp(8), dp(8), dp(8), dp(10))
        }
        val fila = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        OPCIONES_RECUERDAS.forEachIndexed { i, opcion ->
            fila.addView(respuesta(opcion) {
                valor.text = opcion
                alResponder(opcion)
                caja.visibility = View.GONE
                Animacion.latido(valor)
            }, igual(if (i == 0) 0 else dp(6)))
        }
        caja.addView(texto("¿cuántos de esos videos recuerdas?", 13f))
        caja.addView(fila, conMargenArriba(dp(8)))
        return caja
    }

    private fun respuesta(contenido: String, alTocar: () -> Unit) = texto(contenido, 16f, centrado = true).apply {
        setPadding(0, dp(8), 0, dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Colores.SECCION)
        }
        Animacion.presionable(this)
        setOnClickListener { alTocar() }
    }

    private fun metrica(etiqueta: String, valor: TextView, pie: String = "") = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(Colores.TARJETA)
        setPadding(dp(4), dp(6), dp(4), dp(8))
        addView(texto(etiqueta, 11f, centrado = true))
        addView(valor, conMargenArriba(dp(6)))
        if (pie.isNotEmpty()) addView(texto(pie, 11f, centrado = true).apply { setTextColor(Colores.TEXTO_SUAVE) })
    }

    private fun origen(d: DatosBrief, alEntrar: MutableList<() -> Unit>): View {
        val barra = BarraOrigen(context).apply {
            fraccionArrastrada = if (d.totalMs > 0) d.arrastradoMs.toFloat() / d.totalMs else 0f
        }
        // Arrastrado mide lo que mide y buscado usa el resto: asi ninguno salta
        // de linea mientras los numeros se cuentan.
        val arrastrado = texto("${Formato.reloj(d.arrastradoMs)} arrastrado", 18f).apply { maxLines = 1 }
        val buscado = texto("${Formato.reloj(d.buscadoMs)} buscado", 18f).apply {
            gravity = Gravity.END
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val etiquetas = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(arrastrado, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(buscado, peso(dp(8)))
        }
        alEntrar += {
            barra.llenar(450L)
            Animacion.contar(arrastrado, d.arrastradoMs, 450L) { "${Formato.reloj(it)} arrastrado" }
            Animacion.contar(buscado, d.buscadoMs, 450L) { "${Formato.reloj(it)} buscado" }
        }
        return seccion(dp(12), dp(10)).apply {
            addView(texto(Formato.deEsos(d.totalMs), 16f))
            addView(
                barra,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(15)).apply { topMargin = dp(10) }
            )
            addView(etiquetas, conMargenArriba(dp(10)))
        }
    }

    private fun semana(d: DatosBrief, alEntrar: MutableList<() -> Unit>): View {
        val total = texto(Formato.semana(d.semanaArrastreMs), 34f).apply { setTypeface(typeface, Typeface.BOLD) }
        val seccion = seccion(dp(17), dp(13)).apply {
            addView(texto("Esta semana", 16f))
            addView(fila(total, "en arrastre"), conMargenArriba(dp(16)))
        }
        if (d.semanaPasadaMs > 0) {
            seccion.addView(
                texto(Formato.semanaPasada(d.semanaPasadaMs), 13f).apply { setTextColor(Colores.TEXTO_SUAVE) },
                conMargenArriba(dp(6))
            )
        }
        // La segunda linea del wireframe ("3h 40 en [actividad]"): la app no
        // puede medir ese tiempo, asi que va como pregunta, igual que "recuerdas ?".
        if (d.actividad.isNotEmpty()) {
            val signo = texto("?", 34f).apply { setTypeface(typeface, Typeface.BOLD) }
            seccion.addView(fila(signo, "en ${d.actividad}"), conMargenArriba(dp(12)))
        }
        alEntrar += { Animacion.contar(total, d.semanaArrastreMs, 600L, 1100L) { Formato.semana(it) } }
        return seccion
    }

    /** Un numero grande y su etiqueta, alineados por la base del texto. */
    private fun fila(numero: TextView, etiqueta: String) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        isBaselineAligned = true
        addView(numero)
        addView(
            texto(etiqueta, 18f),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) }
        )
    }

    private fun acciones(pantalla: Pantalla, alCerrar: () -> Unit, alVerDetalle: (() -> Unit)?): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Colores.SECCION)
            setPadding(dp(20), dp(17), dp(20), dp(17))
            addView(texto("ver detalle", 18f).apply {
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
            addView(texto("Cerrar", 18f).apply {
                gravity = Gravity.END
                Animacion.presionable(this)
                setOnClickListener { pantalla.salir(alCerrar) }
            }, peso(0))
        }

    // ---- utilidades ----

    private fun seccion(horizontal: Int, vertical: Int) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Colores.SECCION)
        setPadding(horizontal, vertical, horizontal, vertical)
    }

    private fun texto(contenido: String, sp: Float, centrado: Boolean = false) = TextView(context).apply {
        text = contenido
        setTextColor(Colores.TEXTO)
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

    /** Mismo ancho y mismo alto que sus hermanas: las tres cajas de metricas. */
    private fun igual(margenInicio: Int) = LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.MATCH_PARENT,
        1f
    ).apply { marginStart = margenInicio }

    private fun dp(valor: Int) = (valor * densidad).toInt()

    companion object {
        /** Las respuestas a "¿cuántos recuerdas?". */
        val OPCIONES_RECUERDAS = listOf("0", "1–3", "4–10", "10+")

        private const val ANCHO_TARJETA = 1204f / 1440f
    }
}
