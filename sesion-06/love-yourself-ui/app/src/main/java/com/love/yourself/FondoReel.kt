package com.love.yourself

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * Un reel de mentira, para ver la friccion sobre algo parecido a Instagram.
 *
 * Las posiciones salen del wireframe WF2 (lienzo de 1440 x 3120): la columna
 * de iconos a la derecha, el avatar y el texto abajo, y la barra de pestanas.
 * No es Instagram: es el esqueleto que ocupa la pantalla mientras se usa.
 */
class FondoReel(context: Context) : View(context) {

    private val fondo = Paint()
    private val gris = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 255, 255, 255) }
    private val barra = Paint().apply { color = Color.rgb(20, 20, 20) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        fondo.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            Color.rgb(96, 62, 120),
            Color.rgb(36, 70, 92),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fondo)

        // Columna de iconos (me gusta, comentar, compartir...).
        for (i in 0 until 5) rect(canvas, 1277f, 1123f + i * 254f, 98f, 88f, 20f)
        // Avatar y nombre de cuenta.
        circulo(canvas, 108f, 2509f, 40f)
        rect(canvas, 222f, 2470f, 520f, 50f, 25f)
        // Descripcion del reel.
        rect(canvas, 56f, 2611f, 1000f, 40f, 20f)
        // Barra de pestanas de Instagram.
        canvas.drawRect(0f, y(2763f), width.toFloat(), height.toFloat(), barra)
        for (i in 0 until 5) rect(canvas, 102f + i * 288f, 2808f, 83f, 83f, 16f)
    }

    private fun x(v: Float) = v / 1440f * width
    private fun y(v: Float) = v / 3120f * height

    private fun rect(c: Canvas, x0: Float, y0: Float, w: Float, h: Float, radio: Float) {
        c.drawRoundRect(RectF(x(x0), y(y0), x(x0 + w), y(y0 + h)), x(radio), x(radio), gris)
    }

    private fun circulo(c: Canvas, cx: Float, cy: Float, r: Float) {
        c.drawCircle(x(cx), y(cy), x(r), gris)
    }
}
