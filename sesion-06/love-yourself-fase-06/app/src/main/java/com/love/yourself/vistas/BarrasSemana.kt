package com.love.yourself.vistas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import com.love.yourself.vistas.Animacion.frena
import com.love.yourself.vistas.Animacion.tramo

/**
 * "Esta semana" en Inicio: el arrastre de cada dia, de lunes a domingo. Hoy
 * va en blanco y con su numero encima; los dias que vienen, apenas marcados.
 * Al aparecer, las barras crecen una despues de otra.
 */
class BarrasSemana(context: Context) : View(context) {

    private val d = resources.displayMetrics.density
    private fun sp(valor: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, valor, resources.displayMetrics)

    private var dias: List<Long> = List(7) { 0L }
    private var hoy = 0
    private var inicioMs = -1L

    private val pasado = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colores.ARRASTRADO }
    private val deHoy = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colores.TEXTO }
    private val futuro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colores.TARJETA }
    private val letra = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Colores.TEXTO_SUAVE
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
    }
    private val letraHoy = TextPaint(letra).apply {
        color = Colores.TEXTO
        typeface = Typeface.DEFAULT_BOLD
    }
    private val barra = RectF()

    /**
     * @param dias arrastre de cada dia, de lunes a domingo.
     * @param hoy que dia es hoy: 0 = lunes.
     * @param demoraMs cuanto esperar antes de que crezcan (si animar).
     */
    fun mostrar(dias: List<Long>, hoy: Int, animar: Boolean, demoraMs: Long = 0L) {
        this.dias = List(7) { dias.getOrElse(it) { 0L } }
        this.hoy = hoy.coerceIn(0, 6)
        inicioMs = if (animar && Animacion.activas()) SystemClock.uptimeMillis() + demoraMs else -1L
        contentDescription = "esta semana: " + (0..this.hoy).joinToString(", ") {
            "${NOMBRES[it]} ${Formato.minutos(this.dias[it])}"
        }
        invalidate()
    }

    override fun onMeasure(anchoSpec: Int, altoSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, anchoSpec),
            resolveSize((150 * d).toInt(), altoSpec)
        )
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val ahora = SystemClock.uptimeMillis()
        // Una hora llena el alto, o el dia mas largo si se paso: un dia de 5
        // minutos no deberia verse como uno de 3 horas.
        val maximo = maxOf(dias.maxOrNull() ?: 0L, 60 * 60_000L).toFloat()
        val base = height - 22 * d
        val altoUtil = base - 20 * d
        val columna = width / 7f
        val ancho = minOf(columna * 0.55f, 30 * d)
        var creciendo = false
        for (i in 0 until 7) {
            val cx = columna * (i + 0.5f)
            val progreso = if (inicioMs < 0) 1f else frena(tramo((ahora - inicioMs - i * 70L).toFloat(), 0f, 700f))
            if (progreso < 1f) creciendo = true
            if (i > hoy) {
                // Lo que viene: solo la marca en el piso.
                barra.set(cx - ancho / 2f, base - 3 * d, cx + ancho / 2f, base)
                c.drawRoundRect(barra, 1.5f * d, 1.5f * d, futuro)
            } else {
                val alto = maxOf(dias[i] / maximo * altoUtil * progreso, 3 * d)
                val radio = minOf(ancho / 2f, 6 * d)
                barra.set(cx - ancho / 2f, base - alto, cx + ancho / 2f, base)
                c.drawRoundRect(barra, radio, radio, if (i == hoy) deHoy else pasado)
                if (i == hoy && dias[i] > 0) {
                    c.drawText(Formato.minutos((dias[i] * progreso).toLong()), cx, base - alto - 6 * d, letraHoy)
                }
            }
            c.drawText(LETRAS[i], cx, height - 6 * d, if (i == hoy) letraHoy else letra)
        }
        if (creciendo) postInvalidateOnAnimation()
    }

    private companion object {
        val LETRAS = listOf("L", "M", "M", "J", "V", "S", "D")
        val NOMBRES = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    }
}
