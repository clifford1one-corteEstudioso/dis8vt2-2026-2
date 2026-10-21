package com.love.yourself.vistas

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Una barra del grafico: cuando empezo la sesion y cuanto duro. */
class SesionDelDia(val inicioMs: Long, val totalMs: Long)

/**
 * Las ultimas sesiones del dia como barras: arriba los minutos, abajo la hora
 * en que empezo cada una. La ultima (la que se acaba de cerrar) va en claro.
 * Las barras crecen desde abajo al aparecer.
 */
class GraficoSesiones(context: Context) : View(context) {

    var sesiones: List<SesionDelDia> = emptyList()
        set(valor) {
            field = valor
            invalidate()
        }

    private var progreso = 1f
    private val d = resources.displayMetrics.density
    private val hora = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val barra = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val textoArriba = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Colores.TEXTO
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = sp(18f)
    }
    private val textoAbajo = Paint(textoArriba).apply {
        color = Colores.TEXTO_SUAVE
        typeface = Typeface.DEFAULT
        textSize = sp(15f)
    }

    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    fun crecer(demoraMs: Long) {
        if (!Animacion.activas()) return
        progreso = 0f
        invalidate()
        ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = demoraMs
            duration = 700L
            interpolator = Animacion.FRENA
            addUpdateListener {
                progreso = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = sesiones.size
        if (n == 0 || width <= 0) return
        val maximo = sesiones.maxOf { it.totalMs }.coerceAtLeast(60_000L).toFloat()
        val arriba = textoArriba.textSize + 8 * d
        val abajo = textoAbajo.textSize + 10 * d
        val altoUtil = height - arriba - abajo
        // Columnas del mismo ancho, siempre cinco lugares: una sola sesion no ocupa todo.
        val columna = width / COLUMNAS.toFloat()
        val anchoBarra = columna * 0.56f
        val inicio = (width - n * columna) / 2f
        sesiones.forEachIndexed { i, s ->
            val cx = inicio + columna * (i + 0.5f)
            val alto = (altoUtil * s.totalMs / maximo * progreso).coerceAtLeast(4 * d)
            val base = height - abajo
            rect.set(cx - anchoBarra / 2, base - alto, cx + anchoBarra / 2, base)
            barra.color = if (i == n - 1) Colores.ARRASTRADO else Colores.BUSCADO
            canvas.drawRoundRect(rect, 6 * d, 6 * d, barra)
            canvas.drawText(Formato.minutos(s.totalMs), cx, rect.top - 8 * d, textoArriba)
            canvas.drawText(hora.format(Date(s.inicioMs)), cx, height - 4 * d, textoAbajo)
        }
    }

    companion object {
        const val COLUMNAS = 5
    }
}
