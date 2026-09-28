package com.love.yourself.lab

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View

/**
 * La barra de "de esos N minutos": claro es arrastrado, oscuro es buscado.
 *
 * El paso entre uno y otro es un degradado y no un corte, como en el wireframe:
 * la frontera entre elegir y ser empujado no es nitida, y la barra no deberia
 * fingir que lo es.
 */
class BarraOrigen(context: Context) : View(context) {

    /** Fraccion arrastrada, de 0 a 1. */
    var fraccionArrastrada = 0f
        set(valor) {
            field = valor.coerceIn(0f, 1f)
            invalidate()
        }

    private val pintura = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // En el wireframe la transicion ocupa ~24% del ancho (de 69% a 93%).
        val medio = MEDIA_TRANSICION
        val desde = (fraccionArrastrada - medio).coerceIn(0f, 1f)
        val hasta = (fraccionArrastrada + medio).coerceIn(0f, 1f)
        pintura.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(ColoresBrief.ARRASTRADO, ColoresBrief.ARRASTRADO, ColoresBrief.BUSCADO, ColoresBrief.BUSCADO),
            floatArrayOf(0f, desde, hasta, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, pintura)
    }

    private companion object {
        const val MEDIA_TRANSICION = 0.12f
    }
}
