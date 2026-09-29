package com.love.yourself.lab

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.love.yourself.R
import kotlin.math.PI
import kotlin.math.cos

/**
 * Los momentos que agrandan la burbuja. Cada uno: lo que puede decir la
 * mascota (se elige una frase al azar, para que no se vuelva invisible) y
 * como cambia su cara (de -> a). Cuando ocurre cada uno lo decide el servicio.
 *
 * La mascota es tu cerebro: no te reta, muestra lo que el arrastre le hace.
 * Los textos son provisorios: el tono esta por decidirse.
 */
enum class Momento(val frases: List<String>, val desde: Int, val hasta: Int?) {
    /** Primer swipe a pantalla completa. */
    PRIMER_SWIPE(listOf("¿ya?", "¿empezamos?", "mmm…"), R.drawable.brain_sus, R.drawable.brain_sad),

    /** Un minuto de arrastre sin parar. */
    ARRASTRE_SEGUIDO(listOf("otro más…", "¿y este?", "sigue, sigue…"), R.drawable.brain_pfff, null),

    /** Muchos swipes por minuto durante un rato. */
    RITMO_ALTO(listOf("más lento, porfa", "no alcanzo a ver nada", "¿viste alguno?"), R.drawable.brain_pfff, R.drawable.brain_f),

    /** Cinco minutos en la app. */
    CINCO_MIN(listOf("llevamos 5 min", "me está dando sueño", "5 minutos ya"), R.drawable.brain_drowzy, null),

    /** Volver a la app poco despues de haberla cerrado. */
    VOLVISTE(listOf("¿otra vez?", "¿se te olvidó algo?", "hola de nuevo"), R.drawable.brain_sus, null),

    /** Un minuto antes del cielo. */
    ANTES_DEL_CIELO(listOf("necesito aire", "¿salimos un rato?", "ya casi…"), R.drawable.brain_serio, R.drawable.brain_f),

    /** Justo despues de elegir "Seguir" en el cielo. */
    TRAS_SEGUIR(listOf("ok…", "bueno, sigamos", "ahí vamos"), R.drawable.brain_sad, null)
}

/**
 * La cara que lleva la pildora todo el rato: el cerebro se va deteriorando
 * con el tiempo en la app. Tranquilo antes de arrastrar, frito al llegar al
 * cielo. Cambiar los minutos o el orden aca.
 */
object CaraProgresiva {
    fun para(sesionIniciada: Boolean, totalMs: Long): Int {
        if (!sesionIniciada) return R.drawable.brain_relief
        val min = totalMs / 60_000.0
        return when {
            min < 1 -> R.drawable.brain_sus
            min < 3 -> R.drawable.brain_pfff
            min < 5 -> R.drawable.brain_serio
            min < 7 -> R.drawable.brain_drowzy
            min < Config.MIN_CIELO -> R.drawable.brain_sad
            else -> R.drawable.brain_f
        }
    }
}

/**
 * La burbuja de arriba (Figma: "animacion burbuja oficial").
 *
 * Recogida es una pildora con dos datos: tiempo arrastrado y swipes por
 * minuto. Cuando pasa un momento, da un saltito, se agranda, muestra a la
 * mascota con lo que dice, y a los pocos segundos se recoge sola. No recibe
 * toques: Instagram se sigue usando por debajo.
 *
 * Todo se dibuja en onDraw a partir de un reloj, sin animadores: asi este
 * archivo es identico en la app real y en la de diseno.
 */
class BurbujaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private fun sp(valor: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, valor, resources.displayMetrics)

    // ---- medidas, en dp, sacadas del frame de Figma (402 de ancho) ----
    private val anchoSinCara = 153f * d
    /** Con la cara chica adentro, la pildora crece lo justo para que quepa. */
    private val anchoRecogida get() = if (cara == 0) anchoSinCara else anchoSinCara + 44f * d
    private val altoRecogida = 32f * d
    private val anchoExpandida = 361f * d
    private val altoExpandida = 88f * d
    private val radio = 16f * d
    private val margenSuperior = MARGEN_SUPERIOR_DP * d
    private val salto = 17f * d

    // ---- lo que muestra recogida ----
    private var tiempo = "0:00"
    private var ritmo = "0"
    private var cara = 0

    private val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val negrita = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(12f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val normal = TextPaint(negrita).apply { typeface = Typeface.DEFAULT }
    private val dialogo = TextPaint(negrita).apply { textSize = sp(18f) }
    private val imagen = Paint(Paint.FILTER_BITMAP_FLAG)

    private val pildora = RectF()
    private val destino = RectF()
    private val imagenes = mutableMapOf<Int, Bitmap?>()

    // ---- animacion ----
    private var momento: Momento? = null
    private var inicioMs = 0L
    private var textoDialogo: StaticLayout? = null
    private var frase = ""

    /** @param cara el cerebro chico de la pildora (ver CaraProgresiva). 0 = sin cara. */
    fun mostrarDatos(tiempo: String, ritmo: String, cara: Int = 0) {
        if (tiempo == this.tiempo && ritmo == this.ritmo && cara == this.cara) return
        this.tiempo = tiempo
        this.ritmo = ritmo
        this.cara = cara
        invalidate()
    }

    fun expandir(m: Momento) {
        momento = m
        frase = m.frases.random()
        inicioMs = SystemClock.uptimeMillis()
        textoDialogo = null
        invalidate()
    }

    fun expandida(): Boolean = momento != null

    override fun onMeasure(anchoSpec: Int, altoSpec: Int) {
        setMeasuredDimension(
            resolveSize(anchoExpandida.toInt(), anchoSpec),
            resolveSize((margenSuperior + altoExpandida + 4 * d).toInt(), altoSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val m = momento
        val t = if (m == null) FIN else SystemClock.uptimeMillis() - inicioMs
        if (t >= FIN) momento = null

        val f = fotograma(t.toFloat())

        val w = lerp(anchoRecogida, anchoExpandida, f.ancho)
        val h = lerp(altoRecogida, altoExpandida, f.alto)
        val izq = (width - w) / 2f
        val arriba = margenSuperior - salto * f.salto
        pildora.set(izq, arriba, izq + w, arriba + h)
        canvas.drawRoundRect(pildora, radio, radio, fondo)

        if (f.datos > 0f) datos(canvas, f.datos)
        if (m != null && f.contenido > 0f) contenido(canvas, m, t, f.contenido)

        if (momento != null) postInvalidateOnAnimation()
    }

    /** La cara chica a la izquierda y los dos datos, centrados en lo que queda. */
    private fun datos(c: Canvas, alfa: Float) {
        val a = (alfa * 255).toInt()
        negrita.alpha = a
        normal.alpha = a
        var izquierda = pildora.left
        if (cara != 0) {
            imagenes.getOrPut(cara) { BitmapFactory.decodeResource(resources, cara) }?.let { b ->
                val alto = 24f * d
                val ancho = alto * b.width / b.height
                destino.set(pildora.left + 10f * d, pildora.centerY() - alto / 2f, pildora.left + 10f * d + ancho, pildora.centerY() + alto / 2f)
                imagen.alpha = a
                c.drawBitmap(b, null, destino, imagen)
                izquierda = destino.right
            }
        }
        val separacion = 22f * d
        val anchoTiempo = negrita.measureText(tiempo)
        val anchoRitmo = negrita.measureText(ritmo)
        val anchoUnidad = normal.measureText(UNIDAD_RITMO)
        val total = anchoTiempo + separacion + anchoRitmo + anchoUnidad
        var x = (izquierda + pildora.right) / 2f - total / 2f
        val base = pildora.centerY() - (negrita.descent() + negrita.ascent()) / 2f
        c.drawText(tiempo, x, base, negrita)
        x += anchoTiempo + separacion
        c.drawText(ritmo, x, base, negrita)
        c.drawText(UNIDAD_RITMO, x + anchoRitmo, base, normal)
        negrita.alpha = 255
        normal.alpha = 255
    }

    /** La mascota a la derecha y lo que dice a la izquierda. */
    private fun contenido(c: Canvas, m: Momento, t: Long, alfa: Float) {
        c.save()
        c.clipRect(pildora)

        // Cerebro: 93 dp de ancho como en Figma, a 23 dp del borde derecho. El
        // alto sale de la proporcion de cada imagen, centrado en la burbuja.
        val cambio = if (m.hasta == null) 0f else tramo(t.toFloat(), CAMBIO_CARA_MS, CAMBIO_CARA_MS + 300f)
        dibujar(c, m.desde, alfa * (1f - cambio))
        m.hasta?.let { dibujar(c, it, alfa * cambio) }

        // Dialogo: lo que queda a la izquierda del cerebro.
        val anchoTexto = (pildora.width() - 23f * d - 93f * d - 32f * d).toInt().coerceAtLeast(1)
        val capa = textoDialogo?.takeIf { it.width == anchoTexto }
            ?: StaticLayout.Builder.obtain(frase, 0, frase.length, dialogo, anchoTexto)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
                .also { textoDialogo = it }
        dialogo.alpha = (alfa * 255).toInt()
        c.translate(pildora.left + 20f * d, pildora.centerY() - capa.height / 2f)
        capa.draw(c)
        dialogo.alpha = 255
        c.restore()
    }

    private fun dibujar(c: Canvas, id: Int, alfa: Float) {
        if (alfa <= 0f) return
        val b = imagenes.getOrPut(id) { BitmapFactory.decodeResource(resources, id) } ?: return
        val ancho = 93f * d
        val alto = (ancho * b.height / b.width).coerceAtMost(pildora.height() - 8f * d)
        val der = pildora.right - 23f * d
        destino.set(der - alto * b.width / b.height, pildora.centerY() - alto / 2f, der, pildora.centerY() + alto / 2f)
        imagen.alpha = (alfa * 255).toInt()
        c.drawBitmap(b, null, destino, imagen)
    }

    // ---- la linea de tiempo ----

    /** Cada valor va de 0 a 1. */
    private class Fotograma(
        val datos: Float,
        val salto: Float,
        val alto: Float,
        val ancho: Float,
        val contenido: Float
    )

    /**
     * Expandir copia los tiempos del prototipo de Figma (en ms): se apagan los
     * datos, salta, crece en alto y despues en ancho. Recoger es lo mismo al
     * reves, sin el salto.
     */
    private fun fotograma(t: Float): Fotograma {
        if (t < RECOGER_MS) {
            val salto = when {
                t < 595f -> 0f
                t < 701f -> tramo(t, 595f, 701f)
                else -> 1f - tramo(t, 701f, 801f)
            }
            return Fotograma(
                datos = 1f - suave(tramo(t, 192f, 475f)),
                salto = salto,
                alto = tramo(t, 801f, 997f),
                ancho = tramo(t, 993f, 1187f),
                contenido = tramo(t, 1187f, EXPANDIDA_MS)
            )
        }
        val r = t - RECOGER_MS
        return Fotograma(
            datos = suave(tramo(r, 600f, 850f)),
            salto = 0f,
            alto = 1f - tramo(r, 400f, 600f),
            ancho = 1f - tramo(r, 200f, 400f),
            contenido = 1f - tramo(r, 0f, 200f)
        )
    }

    private fun tramo(t: Float, desde: Float, hasta: Float) = ((t - desde) / (hasta - desde)).coerceIn(0f, 1f)
    private fun suave(x: Float) = ((1 - cos(x * PI)) / 2).toFloat()
    private fun lerp(a: Float, b: Float, x: Float) = a + (b - a) * x

    companion object {
        /** Distancia del borde de arriba a la pildora. Puede chocar con la barra de estado. */
        const val MARGEN_SUPERIOR_DP = 21f

        /** Cuanto se queda expandida antes de recogerse sola. */
        const val DURACION_EXPANDIDA_MS = 4000f

        const val UNIDAD_RITMO = "s/m"

        private const val EXPANDIDA_MS = 1400f
        private const val RECOGER_MS = EXPANDIDA_MS + DURACION_EXPANDIDA_MS
        private const val FIN = (RECOGER_MS + 850f).toLong()

        /** Cuando cambia la cara (de "desde" a "hasta"), contado desde que empieza a expandirse. */
        private const val CAMBIO_CARA_MS = EXPANDIDA_MS + 1200f
    }
}
