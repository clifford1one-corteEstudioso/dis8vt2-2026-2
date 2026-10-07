package com.love.yourself.vistas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.love.yourself.vistas.Animacion.acelera
import com.love.yourself.vistas.Animacion.frena
import com.love.yourself.vistas.Animacion.lerp
import com.love.yourself.vistas.Animacion.rebota
import com.love.yourself.vistas.Animacion.suave
import com.love.yourself.vistas.Animacion.tramo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * La burbuja de arriba (Figma: "animacion burbuja oficial").
 *
 * Recogida es una pildora con un cerebro chico y dos datos: tiempo en la app
 * y swipes por minuto. Cuando pasa un momento da un saltito, se agranda, entra
 * la mascota y escribe lo que dice; a los pocos segundos se recoge sola. No
 * recibe toques: Instagram se sigue usando por debajo.
 *
 * Que pasa y en que milisegundo es lo del prototipo de Figma. Las curvas le
 * dan peso: el salto sube frenando y cae acelerando, y al crecer se pasa un
 * poco y vuelve.
 *
 * Todo se dibuja en onDraw a partir de un reloj, sin animadores: asi este
 * archivo es identico en la app real y en la de diseno, y cuando no pasa nada
 * no se redibuja.
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
    private val anchoConCara = anchoSinCara + 44f * d
    private val altoRecogida = 32f * d
    private val anchoExpandida = 361f * d
    private val altoExpandida = 88f * d
    private val radio = 16f * d
    private val margenSuperior = MARGEN_SUPERIOR_DP * d
    private val salto = 17f * d
    private val anchoCerebro = 93f * d
    private val altoCaraChica = 24f * d
    /** Lugar de sobra alrededor, para que lo que se pasa al crecer no se corte. */
    private val holgura = 14f * d

    // ---- lo que muestra recogida ----
    private var tiempo = "0:00"
    private var ritmo = "0"
    private var cara = 0
    private var caraAnterior = 0
    private var cambioCaraMs = -1L
    private var anchoDesde = anchoSinCara
    private var cambioAnchoMs = -1L

    private val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val negrita = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(12f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val normal = TextPaint(negrita).apply { typeface = Typeface.DEFAULT }
    private val dialogo = TextPaint(negrita).apply { textSize = sp(18f) }
    private val imagen = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private val pildora = RectF()
    private val destino = RectF()
    private val imagenes = mutableMapOf<Int, Bitmap?>()

    // ---- el momento en curso ----
    private var momento: Momento? = null
    private var inicioMs = 0L
    private var frase = ""
    private var textoDialogo: StaticLayout? = null
    private var letrasDibujadas = -1

    /** Cuando aparecio en pantalla: entra bajando desde arriba. -1 = sin entrada. */
    private var aparecioMs = -1L
    private var yaDibujo = false

    /** @param cara el cerebro chico de la pildora (ver CaraProgresiva). 0 = sin cara. */
    fun mostrarDatos(tiempo: String, ritmo: String, cara: Int = 0) {
        if (tiempo == this.tiempo && ritmo == this.ritmo && cara == this.cara) return
        if (cara != this.cara) {
            val ahora = SystemClock.uptimeMillis()
            // Antes de dibujarse la primera vez no hay nada que animar: ya entra
            // con la cara puesta.
            val animar = yaDibujo && Animacion.activas()
            anchoDesde = anchoRecogida(ahora)
            if (animar && (cara == 0) != (this.cara == 0)) cambioAnchoMs = ahora
            caraAnterior = this.cara
            cambioCaraMs = if (animar) ahora else -1L
            this.cara = cara
        }
        this.tiempo = tiempo
        this.ritmo = ritmo
        invalidate()
    }

    fun expandir(m: Momento) {
        val ahora = SystemClock.uptimeMillis()
        val t = if (momento == null) -1L else ahora - inicioMs
        momento = m
        frase = m.frases.random()
        textoDialogo = null
        letrasDibujadas = -1
        // Si ya estaba abierta no se recoge para volver a abrirse: cambia lo
        // que dice y la mascota entra de nuevo.
        val abierta = t >= CONTENIDO_MS.toLong() && t < RECOGER_MS.toLong()
        inicioMs = if (abierta) ahora - CONTENIDO_MS.toLong() else ahora
        invalidate()
    }

    fun expandida(): Boolean = momento != null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        aparecioMs = if (Animacion.activas()) SystemClock.uptimeMillis() else -1L
    }

    override fun onMeasure(anchoSpec: Int, altoSpec: Int) {
        setMeasuredDimension(
            resolveSize((anchoExpandida + 2 * holgura).toInt(), anchoSpec),
            resolveSize((margenSuperior + altoExpandida + holgura).toInt(), altoSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val ahora = SystemClock.uptimeMillis()
        var moviendo = false

        if (momento != null && ahora - inicioMs >= FIN) momento = null
        val m = momento
        val t = if (m == null) FIN else (ahora - inicioMs).toFloat()
        val f = fotograma(t)

        // Al aparecer, baja desde arriba de la pantalla.
        val entrada = if (aparecioMs < 0) 1f else tramo((ahora - aparecioMs).toFloat(), 0f, ENTRADA_MS)
        if (entrada < 1f) moviendo = true
        val caida = (1f - frena(entrada)) * (margenSuperior + altoRecogida + holgura)

        if (cambioAnchoMs >= 0 && ahora - cambioAnchoMs < CAMBIO_ANCHO_MS) moviendo = true
        val w = lerp(anchoRecogida(ahora), anchoExpandida, f.ancho)
        val h = lerp(altoRecogida, altoExpandida, f.alto)
        val izq = (width - w) / 2f
        val arriba = margenSuperior - salto * f.salto - caida
        pildora.set(izq, arriba, izq + w, arriba + h)
        canvas.drawRoundRect(pildora, radio, radio, fondo)

        if (f.datos > 0f && datos(canvas, f.datos, ahora)) moviendo = true
        if (m != null && f.contenido > 0f) contenido(canvas, m, t, f.contenido)

        yaDibujo = true
        if (m != null || moviendo) postInvalidateOnAnimation()
    }

    /** El ancho de la pildora recogida: se estira suave cuando aparece o se va la cara. */
    private fun anchoRecogida(ahora: Long): Float {
        val meta = if (cara == 0) anchoSinCara else anchoConCara
        if (cambioAnchoMs < 0) return meta
        return lerp(anchoDesde, meta, rebota(tramo((ahora - cambioAnchoMs).toFloat(), 0f, CAMBIO_ANCHO_MS), 1.2f))
    }

    /**
     * La cara chica a la izquierda y los dos datos, centrados en lo que queda.
     * Cuando la cara cambia, la anterior se encoge y la nueva entra con un
     * saltito. Devuelve true mientras eso se esta moviendo.
     */
    private fun datos(c: Canvas, alfa: Float, ahora: Long): Boolean {
        val a = (alfa * 255).toInt()
        negrita.alpha = a
        normal.alpha = a
        var izquierda = pildora.left
        var moviendo = false
        if (cara != 0) {
            val x = if (cambioCaraMs < 0) 1f else tramo((ahora - cambioCaraMs).toFloat(), 0f, CAMBIO_CARA_CHICA_MS)
            if (x < 1f) {
                moviendo = true
                if (caraAnterior != 0) caraChica(c, caraAnterior, 1f - 0.5f * frena(x), alfa * (1f - frena(x)))
            }
            izquierda = caraChica(c, cara, if (x < 1f) rebota(x, 2.4f) else 1f, alfa * frena(minOf(1f, x * 2.5f)))
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
        return moviendo
    }

    /**
     * Un cerebro chico, centrado en su lugar de la pildora. El lugar no cambia
     * con la escala: asi los datos no se corren mientras la cara salta.
     * Devuelve donde termina ese lugar.
     */
    private fun caraChica(c: Canvas, id: Int, escala: Float, alfa: Float): Float {
        val b = bitmap(id) ?: return pildora.left
        val ancho = altoCaraChica * b.width / b.height
        val cx = pildora.left + 10f * d + ancho / 2f
        val cy = pildora.centerY()
        if (alfa > 0f && escala > 0f) {
            val mw = ancho * escala / 2f
            val mh = altoCaraChica * escala / 2f
            destino.set(cx - mw, cy - mh, cx + mw, cy + mh)
            imagen.alpha = (alfa * 255).toInt().coerceIn(0, 255)
            c.drawBitmap(b, null, destino, imagen)
        }
        return pildora.left + 10f * d + ancho
    }

    /** La mascota a la derecha, escribiendo lo que dice a la izquierda. */
    private fun contenido(c: Canvas, m: Momento, t: Float, alfa: Float) {
        c.save()
        c.clipRect(pildora)

        // Entra creciendo y pasandose un poco. Mientras escribe da saltitos,
        // como si hablara; despues respira despacio.
        val desde = t - CONTENIDO_MS
        val brote = rebota(tramo(desde, 0f, 380f), 2.0f)
        val vaiven = when {
            !Animacion.activas() -> 0f
            letrasVisibles(t) < frase.length -> -abs(sin(desde / 1000f * 2f * PI.toFloat() * 3.2f)) * 2.2f * d
            else -> sin(desde / 1000f * 2f * PI.toFloat() / 2.2f) * 1.6f * d
        }
        // Cambio de cara (desde -> hasta): se funden con un apreton, como una reaccion.
        val cambio = if (m.hasta == null) 0f else tramo(t, CAMBIO_CARA_MS, CAMBIO_CARA_MS + 320f)
        val apreton = if (cambio <= 0f || cambio >= 1f) 0f else sin(cambio * PI.toFloat()) * 0.12f
        cerebro(c, m.desde, alfa * (1f - cambio), brote, vaiven, apreton)
        m.hasta?.let { cerebro(c, it, alfa * cambio, brote, vaiven, apreton) }

        // Dialogo: lo que queda a la izquierda del cerebro, letra por letra.
        val anchoTexto = (pildora.width() - 23f * d - anchoCerebro - 32f * d).toInt().coerceAtLeast(1)
        val letras = letrasVisibles(t)
        val capa = textoDialogo?.takeIf { it.width == anchoTexto && letras == letrasDibujadas }
            ?: armarDialogo(anchoTexto, letras).also {
                textoDialogo = it
                letrasDibujadas = letras
            }
        dialogo.alpha = (alfa * 255).toInt()
        c.translate(pildora.left + 20f * d, pildora.centerY() - capa.height / 2f)
        capa.draw(c)
        dialogo.alpha = 255
        c.restore()
    }

    /** Un cerebro grande: 93 dp de ancho como en Figma, a 23 dp del borde derecho. */
    private fun cerebro(c: Canvas, id: Int, alfa: Float, escala: Float, dy: Float, apreton: Float) {
        if (alfa <= 0f || escala <= 0f) return
        val b = bitmap(id) ?: return
        val alto = (anchoCerebro * b.height / b.width).coerceAtMost(altoExpandida - 8f * d)
        val ancho = alto * b.width / b.height
        val cx = pildora.right - 23f * d - ancho / 2f
        val cy = pildora.centerY() + dy
        val mw = ancho * escala * (1f + apreton) / 2f
        val mh = alto * escala * (1f - apreton) / 2f
        destino.set(cx - mw, cy - mh, cx + mw, cy + mh)
        imagen.alpha = (alfa * 255).toInt().coerceIn(0, 255)
        c.drawBitmap(b, null, destino, imagen)
    }

    /** Cuantas letras de la frase ya se escribieron. */
    private fun letrasVisibles(t: Float): Int {
        if (!Animacion.activas()) return frase.length
        return ((t - ESCRIBE_DESDE_MS) / MS_POR_LETRA).toInt().coerceIn(0, frase.length)
    }

    /**
     * La frase entera, con lo que falta escribir en transparente: asi las
     * palabras no saltan de linea mientras se escriben.
     */
    private fun armarDialogo(ancho: Int, letras: Int): StaticLayout {
        val texto = SpannableString(frase)
        if (letras < frase.length) {
            texto.setSpan(ForegroundColorSpan(Color.TRANSPARENT), letras, frase.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return StaticLayout.Builder.obtain(texto, 0, texto.length, dialogo, ancho)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
    }

    private fun bitmap(id: Int): Bitmap? =
        imagenes.getOrPut(id) { Imagenes.cargar(resources, id, anchoCerebro.toInt()) }

    // ---- la linea de tiempo ----

    /** Cada valor va de 0 a 1 (alto y ancho pueden pasarse un poco al crecer). */
    private class Fotograma(
        val datos: Float,
        val salto: Float,
        val alto: Float,
        val ancho: Float,
        val contenido: Float
    )

    /**
     * Expandir sigue los tiempos del prototipo de Figma (en ms): se apagan los
     * datos, salta, crece en alto y despues en ancho, y entra la mascota.
     * Recoger es lo mismo al reves, sin el salto.
     */
    private fun fotograma(t: Float): Fotograma {
        if (t < RECOGER_MS) {
            val salto = if (t < 701f) frena(tramo(t, 595f, 701f)) else 1f - acelera(tramo(t, 701f, 801f))
            return Fotograma(
                datos = 1f - suave(tramo(t, 192f, 475f)),
                salto = salto,
                alto = rebota(tramo(t, 801f, 997f), 1.1f),
                ancho = rebota(tramo(t, 993f, CONTENIDO_MS), 1.1f),
                contenido = frena(tramo(t, CONTENIDO_MS, EXPANDIDA_MS))
            )
        }
        val r = t - RECOGER_MS
        return Fotograma(
            datos = suave(tramo(r, 600f, 850f)),
            salto = 0f,
            alto = 1f - suave(tramo(r, 380f, 600f)),
            ancho = 1f - suave(tramo(r, 180f, 420f)),
            contenido = 1f - frena(tramo(r, 0f, 200f))
        )
    }

    companion object {
        /** Distancia del borde de arriba a la pildora. Puede chocar con la barra de estado. */
        const val MARGEN_SUPERIOR_DP = 21f

        /** Cuanto se queda expandida antes de recogerse sola. */
        const val DURACION_EXPANDIDA_MS = 4000f

        const val UNIDAD_RITMO = "s/m"

        /** Cuando termina de crecer y entra la mascota. */
        private const val CONTENIDO_MS = 1187f
        private const val EXPANDIDA_MS = 1400f
        private const val RECOGER_MS = EXPANDIDA_MS + DURACION_EXPANDIDA_MS
        private const val FIN = RECOGER_MS + 850f

        /** Cuando cambia la cara (de "desde" a "hasta"), contado desde que empieza a expandirse. */
        private const val CAMBIO_CARA_MS = EXPANDIDA_MS + 1200f

        /** El dialogo se escribe a esta velocidad, desde que entra la mascota. */
        private const val ESCRIBE_DESDE_MS = 1300f
        private const val MS_POR_LETRA = 32f

        private const val ENTRADA_MS = 480f
        private const val CAMBIO_CARA_CHICA_MS = 520f
        private const val CAMBIO_ANCHO_MS = 420f
    }
}
