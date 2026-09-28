package com.love.yourself.lab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/** Lo que esta haciendo la mascota. */
enum class EstadoMascota {
    /** No hay app vigilada adelante: no se muestra. */
    OCULTA,

    /** En una app vigilada, sin arrastre: hace lo suyo (lee). */
    TRANQUILA,

    /** Empezo el scroll a pantalla completa: levanta la vista. Todavia no deja nada. */
    ALERTA,

    /** Un minuto de arrastre seguido: deja el libro. No se pone triste, solo no puede seguir. */
    DETENIDA
}

/**
 * La mascota, dibujada en codigo: no necesita assets.
 *
 * Hace lo suyo (leer) mientras la persona usa la app con intencion. Cuando
 * empieza el arrastre levanta la vista, y si dura un minuto deja el libro.
 * Nunca se pone triste ni enojada: no juzga, solo no puede seguir con lo suyo.
 *
 * Todo se dibuja en proporciones de 0 a 1 del cuadrado de la vista, asi que
 * cambiar el tamano no desarma nada. Igual que CieloView, este archivo es
 * identico en la app real y en la de diseno.
 */
class MascotaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var estado = EstadoMascota.TRANQUILA
        set(valor) {
            if (field == valor) return
            field = valor
            invalidate()
        }

    // ---- colores: cambiar aca ----
    private val cuerpo = pintura(Color.rgb(246, 196, 120))
    private val sombraCuerpo = pintura(Color.rgb(226, 170, 96))
    private val tinta = pintura(Color.rgb(60, 45, 40))
    private val tapa = pintura(Color.rgb(120, 150, 210))
    private val paginas = pintura(Color.rgb(252, 250, 244))
    private val lineas = pintura(Color.rgb(200, 196, 188))
    private val sombraSuelo = pintura(Color.argb(60, 0, 0, 0))
    private val contorno = pintura(Color.rgb(200, 140, 70)).apply { style = Paint.Style.STROKE }

    private fun pintura(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c }

    private val caja = RectF()
    private val trazo = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (estado == EstadoMascota.OCULTA) return
        val lado = minOf(width, height).toFloat()
        if (lado <= 0f) return

        // Centrar el cuadrado de dibujo dentro de la vista.
        canvas.save()
        canvas.translate((width - lado) / 2f, (height - lado) / 2f)

        val t = System.currentTimeMillis() / 1000.0
        // Leyendo se mece apenas; detenida respira lento. Alerta se queda quieta.
        val vaiven = when (estado) {
            EstadoMascota.TRANQUILA -> (sin(t * 2 * PI / 3.0) * 0.012).toFloat()
            EstadoMascota.DETENIDA -> (sin(t * 2 * PI / 5.0) * 0.006).toFloat()
            else -> 0f
        }

        suelo(canvas, lado)
        if (estado == EstadoMascota.DETENIDA) libroCerrado(canvas, lado)
        cuerpo(canvas, lado, vaiven)
        cara(canvas, lado, vaiven)
        when (estado) {
            EstadoMascota.TRANQUILA -> libroAbierto(canvas, lado, 0.80f + vaiven, 0f)
            EstadoMascota.ALERTA -> libroAbierto(canvas, lado, 0.88f, -8f)
            else -> manosQuietas(canvas, lado, vaiven)
        }
        canvas.restore()

        // Solo se anima mientras se ve y tiene algo que mover.
        if (estado != EstadoMascota.ALERTA) postInvalidateOnAnimation()
    }

    private fun suelo(c: Canvas, l: Float) {
        caja.set(l * 0.18f, l * 0.88f, l * 0.82f, l * 0.96f)
        c.drawOval(caja, sombraSuelo)
    }

    private fun cuerpo(c: Canvas, l: Float, dy: Float) {
        // Una gota ancha abajo: sentada.
        caja.set(l * 0.20f, l * (0.30f + dy), l * 0.80f, l * (0.92f + dy))
        c.drawOval(caja, cuerpo)
        caja.set(l * 0.26f, l * (0.70f + dy), l * 0.74f, l * (0.92f + dy))
        c.drawOval(caja, sombraCuerpo)
    }

    private fun cara(c: Canvas, l: Float, dy: Float) {
        // Leyendo mira hacia abajo, al libro. Alerta y detenida miran al frente.
        val (yOjos, mirada) = when (estado) {
            EstadoMascota.TRANQUILA -> 0.52f to 0.018f
            EstadoMascota.ALERTA -> 0.47f to -0.012f
            else -> 0.50f to 0f
        }
        val rOjo = l * if (estado == EstadoMascota.ALERTA) 0.045f else 0.038f
        val y = l * (yOjos + dy)
        c.drawCircle(l * 0.40f, y, rOjo, paginas)
        c.drawCircle(l * 0.60f, y, rOjo, paginas)
        c.drawCircle(l * 0.40f, y + l * mirada, rOjo * 0.55f, tinta)
        c.drawCircle(l * 0.60f, y + l * mirada, rOjo * 0.55f, tinta)

        // Boca: una linea corta y recta en todos los estados. Neutral a proposito.
        tinta.strokeWidth = l * 0.014f
        tinta.strokeCap = Paint.Cap.ROUND
        val yBoca = l * (yOjos + 0.09f + dy)
        c.drawLine(l * 0.46f, yBoca, l * 0.54f, yBoca, tinta)
    }

    /** Libro abierto sostenido frente al cuerpo. y es el lomo; giro en grados. */
    private fun libroAbierto(c: Canvas, l: Float, y: Float, giro: Float) {
        c.save()
        c.rotate(giro, l * 0.5f, l * y)
        val cx = l * 0.5f
        val cy = l * y
        val ancho = l * 0.22f
        val alto = l * 0.16f

        // Tapa: un poco mas grande que las paginas.
        caja.set(cx - ancho - l * 0.015f, cy - alto - l * 0.01f, cx + ancho + l * 0.015f, cy + l * 0.02f)
        c.drawRoundRect(caja, l * 0.02f, l * 0.02f, tapa)
        // Dos paginas en V, abiertas hacia arriba.
        pagina(c, cx, cy, -ancho, alto)
        pagina(c, cx, cy, ancho, alto)

        // Manos a los costados, sujetando.
        c.drawCircle(cx - ancho, cy - alto * 0.3f, l * 0.045f, cuerpo)
        c.drawCircle(cx + ancho, cy - alto * 0.3f, l * 0.045f, cuerpo)
        c.restore()
    }

    private fun pagina(c: Canvas, cx: Float, cy: Float, ancho: Float, alto: Float) {
        trazo.reset()
        trazo.moveTo(cx, cy)
        trazo.lineTo(cx + ancho, cy - alto * 0.15f)
        trazo.lineTo(cx + ancho, cy - alto)
        trazo.lineTo(cx, cy - alto * 0.85f)
        trazo.close()
        c.drawPath(trazo, paginas)
        lineas.strokeWidth = alto * 0.05f
        for (i in 1..3) {
            val f = i / 4f
            c.drawLine(cx + ancho * 0.15f, cy - alto * (0.15f + f * 0.6f), cx + ancho * 0.85f, cy - alto * (0.2f + f * 0.6f), lineas)
        }
    }

    /** El libro cerrado, dejado en el suelo a un lado. */
    private fun libroCerrado(c: Canvas, l: Float) {
        caja.set(l * 0.66f, l * 0.84f, l * 0.94f, l * 0.92f)
        c.drawRoundRect(caja, l * 0.015f, l * 0.015f, tapa)
        caja.set(l * 0.67f, l * 0.85f, l * 0.93f, l * 0.875f)
        c.drawRect(caja, paginas)
    }

    /** Sin libro: las manos descansan sobre las piernas. */
    private fun manosQuietas(c: Canvas, l: Float, dy: Float) {
        contorno.strokeWidth = l * 0.01f
        for (x in floatArrayOf(0.36f, 0.64f)) {
            c.drawCircle(l * x, l * (0.76f + dy), l * 0.045f, cuerpo)
            c.drawCircle(l * x, l * (0.76f + dy), l * 0.045f, contorno)
        }
    }
}
