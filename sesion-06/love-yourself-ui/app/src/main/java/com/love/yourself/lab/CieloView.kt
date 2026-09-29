package com.love.yourself.lab

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.love.yourself.R

/**
 * El cielo del frame "sky" de Figma: degradado azul a celeste, un sol con
 * halo y tres nubes. Todo va en proporciones del lienzo de Figma (1440 x
 * 3120), asi que se estira a cualquier pantalla.
 */
class CieloView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val fondo = Paint()
    private val sol = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barraEstado = Paint().apply { color = Color.argb(56, 0, 0, 0) }
    private val pinturaNube = Paint(Paint.FILTER_BITMAP_FLAG)
    private val nube: Bitmap? by lazy { BitmapFactory.decodeResource(resources, R.drawable.nube) }
    private val destino = RectF()

    // x, y, ancho, alto de cada nube, en px del lienzo de Figma.
    private val nubes = arrayOf(
        floatArrayOf(720f, 515f, 1032f, 561f),
        floatArrayOf(-297f, 887f, 1032f, 561f),
        floatArrayOf(482f, 529f, 773f, 420f)
    )

    private fun x(v: Float) = v / 1440f * width
    private fun y(v: Float) = v / 3120f * height

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // De #497FFF (arriba) a #82E3F0, que llega a un tercio del alto y sigue igual.
        fondo.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#497FFF"), Color.parseColor("#497FFF"), Color.parseColor("#82E3F0"), Color.parseColor("#82E3F0")),
            floatArrayOf(0f, 0.013f, 0.349f, 1f),
            Shader.TileMode.CLAMP
        )
        // Sol: centro amarillo palido y un halo que se desvanece.
        val r = x(352.5f)
        sol.shader = RadialGradient(
            x(1087.5f), y(562.5f), r,
            intArrayOf(Color.parseColor("#FFF6CC"), Color.parseColor("#FFF6CC"), Color.argb(90, 255, 246, 204), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 0.35f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fondo)
        canvas.drawCircle(x(1087.5f), y(562.5f), x(352.5f), sol)
        nube?.let { b ->
            for (n in nubes) {
                destino.set(x(n[0]), y(n[1]), x(n[0] + n[2]), y(n[1] + n[3]))
                canvas.drawBitmap(b, null, destino, pinturaNube)
            }
        }
        canvas.drawRect(0f, 0f, width.toFloat(), y(72f), barraEstado)
    }
}
