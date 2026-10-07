package com.love.yourself.vistas

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

/** Una visita en la lista de Inicio. */
data class SesionInicio(
    val finMs: Long,
    /** El nombre visible de la app ("Instagram"). */
    val app: String,
    val totalMs: Long,
    val arrastradoMs: Long,
    val videos: Int,
    /** Como termino: INICIO, BLOQUEO, SALIR u OTRA_APP. */
    val motivo: String,
    val seguir: Int = 0,
    val recuerdas: String = ""
)

/** Lo que muestra Inicio. Lo arma InicioActivity con el historial; la app de diseno usa ejemplo(). */
data class DatosInicio(
    val ahora: Long,
    val hoyArrastreMs: Long,
    val hoyBuscadoMs: Long,
    val hoySesiones: Int,
    val hoyVideos: Int,
    /** Arrastre de cada dia de esta semana, de lunes a domingo. */
    val semana: List<Long>,
    /** Que dia es hoy: 0 = lunes. */
    val hoy: Int,
    /** La semana pasada hasta el mismo dia, para comparar. -1 si no hay con que. */
    val semanaPasadaMs: Long,
    val actividad: String,
    /** Las ultimas visitas, la mas nueva primero. */
    val sesiones: List<SesionInicio>,
    /** Las apps vigiladas, por nombre: para decir donde mira mientras no hay sesiones. */
    val apps: List<String>,
    /** "datos dev ×10" si vienen de pruebas con el reloj acelerado. Vacio en uso normal. */
    val etiqueta: String = ""
) {
    /** De lunes a hoy. */
    val semanaMs get() = semana.take(hoy + 1).sum()

    companion object {
        /** Una semana inventada, para ver Inicio en la app de diseno. */
        fun ejemplo(ahora: Long): DatosInicio {
            val min = 60_000L
            val hora = 60 * min
            val c = Calendar.getInstance().apply { timeInMillis = ahora }
            val hoy = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val inventada = listOf(95 * min, 130 * min, 62 * min, 48 * min, 110 * min, 75 * min, 40 * min)
            return DatosInicio(
                ahora = ahora,
                hoyArrastreMs = 42 * min,
                hoyBuscadoMs = 14 * min,
                hoySesiones = 2,
                hoyVideos = 135,
                semana = List(7) { if (it < hoy) inventada[it] else if (it == hoy) 42 * min else 0L },
                hoy = hoy,
                semanaPasadaMs = (4 * 60 + 41) * min,
                actividad = "hacer música",
                sesiones = listOf(
                    SesionInicio(ahora - 25 * min, "Instagram", 22 * min, 19 * min + 45_000L, 71, "SALIR", recuerdas = "1–3"),
                    SesionInicio(ahora - 3 * hora, "TikTok", 26 * min, 22 * min + 15_000L, 64, "INICIO", seguir = 1),
                    SesionInicio(ahora - 5 * hora, "Instagram", 6 * min, 0L, 0, "OTRA_APP"),
                    SesionInicio(ahora - 26 * hora, "YouTube", 48 * min, 31 * min, 40, "BLOQUEO", seguir = 2)
                ),
                apps = listOf("Instagram", "TikTok", "YouTube")
            )
        }
    }
}

/**
 * Inicio: lo que se ve al abrir la app, y a donde lleva "ver detalle".
 *
 * Es el agregado que el resumen solo alcanza a insinuar: como quedo tu
 * cerebro hoy, la semana dia por dia y las ultimas visitas, con como
 * terminaron. Las visitas sin arrastre tambien estan: son el uso con
 * intencion, que no se toca.
 *
 * No hay metas, rachas ni puntajes. Muestra, igual que la mascota, y la
 * conclusion queda para quien lo mira.
 *
 * Identico en la app real y en la de diseno: se copia entero.
 */
class VistaInicio(private val context: Context) {

    private val densidad = context.resources.displayMetrics.density

    /**
     * @param animar si entra con animacion. Solo al abrir: al volver de
     *   Ajustes no tiene que repetirse.
     * @param alAjustes el engranaje de arriba.
     */
    fun crear(d: DatosInicio, animar: Boolean, alAjustes: () -> Unit): View {
        val columna = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(32))
        }
        columna.addView(barraSuperior(d, alAjustes))
        columna.addView(hoy(d, animar), conMargenArriba(dp(8)))
        columna.addView(semana(d, animar), conMargenArriba(dp(16)))
        columna.addView(visitas(d), conMargenArriba(dp(16)))
        columna.addView(
            texto("No tienes que volver aquí: la app trabaja sola.", 12f, Colores.TEXTO_SUAVE).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
            conMargenArriba(dp(20))
        )
        if (animar) {
            for (i in 0 until columna.childCount) Animacion.entrar(columna.getChildAt(i), 60L + i * 90L)
        }
        return ScrollView(context).apply {
            setBackgroundColor(Colores.PAGINA)
            isVerticalScrollBarEnabled = false
            addView(columna)
        }
    }

    // ---- secciones ----

    private fun barraSuperior(d: DatosInicio, alAjustes: () -> Unit): View {
        val titulo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto("Love Yourself", 22f).apply { setTypeface(typeface, Typeface.BOLD) })
            if (d.etiqueta.isNotEmpty()) addView(texto(d.etiqueta, 12f, Colores.TEXTO_SUAVE))
        }
        val engranaje = texto("⚙", 22f).apply {
            gravity = Gravity.CENTER
            contentDescription = "Ajustes"
            Animacion.presionable(this)
            setOnClickListener { alAjustes() }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(titulo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(engranaje, LinearLayout.LayoutParams(dp(48), dp(48)))
        }
    }

    /** El cerebro del dia y lo de hoy: cuanto arrastre, en cuantas sesiones, y cuanto fue buscado. */
    private fun hoy(d: DatosInicio, animar: Boolean): View {
        val cerebro = CerebroFlotante(context).apply {
            Imagenes.cargar(resources, CaraDelDia.para(d.hoyArrastreMs), dp(180))?.let { setImageBitmap(it) }
            adjustViewBounds = true
            contentDescription = "tu cerebro hoy: ${CaraDelDia.frase(d.hoyArrastreMs)}"
        }
        if (animar) Animacion.brotar(cerebro, 320L)

        val numero = texto(Formato.minutos(d.hoyArrastreMs), 34f).apply { setTypeface(typeface, Typeface.BOLD) }
        if (animar) Animacion.contar(numero, d.hoyArrastreMs, 500L, 1000L) { Formato.minutos(it) }

        val detalle = seccion().apply {
            addView(texto("hoy", 13f, Colores.TEXTO_SUAVE))
            addView(fila(numero, "en arrastre"), conMargenArriba(dp(4)))
            addView(texto(Formato.sesionesYVideos(d.hoySesiones, d.hoyVideos), 13f, Colores.TEXTO_SUAVE), conMargenArriba(dp(2)))
            val total = d.hoyArrastreMs + d.hoyBuscadoMs
            if (total > 0) {
                val barra = BarraOrigen(context).apply { fraccionArrastrada = d.hoyArrastreMs.toFloat() / total }
                if (animar) barra.llenar(520L)
                addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12)).apply { topMargin = dp(12) })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(texto("${Formato.minutos(d.hoyArrastreMs)} arrastrado", 14f), envolver(0))
                    addView(texto("${Formato.minutos(d.hoyBuscadoMs)} buscado", 14f).apply {
                        gravity = Gravity.END
                        unaLinea()
                    }, peso(dp(8)))
                }, conMargenArriba(dp(6)))
            }
        }

        return tarjeta().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(cerebro, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(112)))
            addView(texto(CaraDelDia.frase(d.hoyArrastreMs), 18f).apply { gravity = Gravity.CENTER_HORIZONTAL }, conMargenArriba(dp(6)))
            addView(detalle, conMargenArriba(dp(14)))
        }
    }

    /** La semana dia por dia, el total y como iba la anterior a esta altura. */
    private fun semana(d: DatosInicio, animar: Boolean): View {
        val barras = BarrasSemana(context).apply { mostrar(d.semana, d.hoy, animar, demoraMs = 380L) }
        val total = texto(Formato.semana(d.semanaMs), 34f).apply { setTypeface(typeface, Typeface.BOLD) }
        if (animar) Animacion.contar(total, d.semanaMs, 700L, 1100L) { Formato.semana(it) }
        return tarjeta().apply {
            addView(texto("Esta semana", 16f))
            addView(barras, conMargenArriba(dp(12)))
            addView(fila(total, "en arrastre"), conMargenArriba(dp(10)))
            if (d.semanaPasadaMs > 0) {
                addView(texto(Formato.semanaPasada(d.semanaPasadaMs), 13f, Colores.TEXTO_SUAVE), conMargenArriba(dp(4)))
            }
            // Igual que en el resumen: el tiempo de lo que te gustaria hacer
            // no se puede medir, y la pregunta queda abierta.
            if (d.actividad.isNotEmpty()) {
                val signo = texto("?", 34f).apply { setTypeface(typeface, Typeface.BOLD) }
                addView(fila(signo, "en ${d.actividad}"), conMargenArriba(dp(10)))
                addView(texto("lo que dijiste que te gustaría hacer más", 12f, Colores.TEXTO_SUAVE))
            }
        }
    }

    /** Las ultimas visitas: cuanto duraron, cuanto fue arrastre y como terminaron. */
    private fun visitas(d: DatosInicio): View = tarjeta().apply {
        addView(texto("Últimas sesiones", 16f))
        if (d.sesiones.isEmpty()) {
            val donde = if (d.apps.isEmpty()) "las apps que elegiste" else d.apps.joinToString(", ")
            addView(
                texto("Todavía no hay. Aparecen cuando deslizas a pantalla completa en $donde.", 14f, Colores.TEXTO_SUAVE),
                conMargenArriba(dp(10))
            )
        }
        for (s in d.sesiones.take(MAX_VISITAS)) addView(visita(s, d.ahora), conMargenArriba(dp(10)))
    }

    private fun visita(s: SesionInicio, ahora: Long): View = seccion().apply {
        // Lo de la izquierda mide lo que mide; lo de la derecha usa el resto.
        // Asi "19:45 arrastrado · 71 videos" no se parte en dos lineas.
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(texto(s.app, 16f).apply {
                setTypeface(typeface, Typeface.BOLD)
                unaLinea()
            }, peso())
            addView(texto(Formato.minutos(s.totalMs), 16f), envolver(dp(8)))
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(texto(Formato.cuando(s.finMs, ahora), 13f, Colores.TEXTO_SUAVE), envolver(0))
            addView(texto(Formato.detalleSesion(s.arrastradoMs, s.videos), 13f, Colores.TEXTO_SUAVE).apply {
                gravity = Gravity.END
                unaLinea()
            }, peso(dp(8)))
        }, conMargenArriba(dp(2)))
        if (s.totalMs > 0) {
            addView(BarraOrigen(context).apply { fraccionArrastrada = s.arrastradoMs.toFloat() / s.totalMs },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5)).apply { topMargin = dp(8) })
        }
        val cierre = Formato.comoTermino(s.motivo, s.seguir, s.recuerdas)
        if (cierre.isNotEmpty()) addView(texto(cierre, 12f, Colores.TEXTO_SUAVE), conMargenArriba(dp(6)))
    }

    // ---- piezas ----

    private fun tarjeta() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(Colores.FONDO)
        }
        setPadding(dp(14), dp(16), dp(14), dp(16))
    }

    private fun seccion() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(Colores.SECCION)
        }
        setPadding(dp(12), dp(10), dp(12), dp(12))
    }

    /** Un numero grande y su etiqueta, alineados por la base del texto. */
    private fun fila(numero: TextView, etiqueta: String) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        isBaselineAligned = true
        addView(numero)
        addView(texto(etiqueta, 18f), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8) })
    }

    private fun texto(contenido: String, sp: Float, color: Int = Colores.TEXTO) = TextView(context).apply {
        text = contenido
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    private fun conMargenArriba(margen: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = margen }

    private fun peso(margenInicio: Int = 0) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        .apply { marginStart = margenInicio }

    private fun envolver(margenInicio: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { marginStart = margenInicio }

    /** Si no cabe, se corta con "…" en vez de saltar de linea. */
    private fun TextView.unaLinea() {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun dp(valor: Int) = (valor * densidad).toInt()

    private companion object {
        const val MAX_VISITAS = 8
    }
}

/**
 * El cerebro de Inicio: flota despacio mientras la pantalla se ve, y se
 * queda quieto cuando no (no gasta bateria detras de otra app).
 */
private class CerebroFlotante(context: Context) : ImageView(context) {

    private var vaiven: ObjectAnimator? = null

    override fun onWindowVisibilityChanged(visibilidad: Int) {
        super.onWindowVisibilityChanged(visibilidad)
        if (visibilidad == View.VISIBLE) empezar() else parar()
    }

    override fun onDetachedFromWindow() {
        parar()
        super.onDetachedFromWindow()
    }

    private fun empezar() {
        if (vaiven != null || !Animacion.activas()) return
        vaiven = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, -6f * resources.displayMetrics.density).apply {
            duration = 1800L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = Animacion.SUAVE
            start()
        }
    }

    private fun parar() {
        vaiven?.cancel()
        vaiven = null
        translationY = 0f
    }
}
