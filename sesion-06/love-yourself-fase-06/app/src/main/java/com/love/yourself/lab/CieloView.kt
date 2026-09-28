package com.love.yourself.lab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View

/** Cielo con nubes, dibujado en código: no necesita assets. */
class CieloView(context: Context) : View(context) {

    private val fondo = Paint()
    private val nube = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        fondo.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(90, 160, 230),   // arriba
            Color.rgb(200, 230, 255),  // abajo
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fondo)
        nubeEn(canvas, 0.25f, 0.18f, 1.0f)
        nubeEn(canvas, 0.70f, 0.35f, 0.8f)
        nubeEn(canvas, 0.40f, 0.62f, 1.2f)
    }

    /** Una nube = tres círculos. x e y van de 0 a 1 (proporción de pantalla). */
    private fun nubeEn(c: Canvas, x: Float, y: Float, escala: Float) {
        val cx = width * x
        val cy = height * y
        val r = width * 0.08f * escala
        c.drawCircle(cx - r, cy, r * 0.8f, nube)
        c.drawCircle(cx, cy - r * 0.4f, r, nube)
        c.drawCircle(cx + r, cy, r * 0.8f, nube)
    }
}