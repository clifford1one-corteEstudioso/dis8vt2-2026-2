package com.love.yourself.lab

import android.media.Image
import kotlin.math.abs

/**
 * Mide cuanto cambio la pantalla entre un frame y el anterior.
 *
 * Decision de diseno importante: esto NO decide si hubo un corte. Devuelve el
 * numero crudo de diferencia y deja que el umbral se elija despues, comparando
 * contra un video con los cortes contados a mano. Un umbral inventado ahora
 * seria un supuesto disfrazado de medicion.
 *
 * Para que aguante veinte minutos sin fundir la bateria, no construye un Bitmap
 * por frame. Lee [LADO] x [LADO] puntos sueltos directo del buffer y compara esa
 * huella. Son 1024 lecturas por frame en vez de 2.5 millones.
 */
class DetectorCortes {

    private var huellaPrevia: IntArray? = null

    /**
     * Diferencia media por punto respecto al frame anterior, de 0 a 255.
     * Devuelve -1.0 en el primer frame, cuando no hay con que comparar.
     */
    fun diferencia(image: Image): Double {
        val huella = huellaDe(image)
        val previa = huellaPrevia
        huellaPrevia = huella
        if (previa == null) return -1.0

        var suma = 0L
        for (i in huella.indices) suma += abs(huella[i] - previa[i])
        return suma.toDouble() / huella.size
    }

    fun reiniciar() {
        huellaPrevia = null
    }

    private fun huellaDe(image: Image): IntArray {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val huella = IntArray(LADO * LADO)
        var i = 0
        for (fy in 0 until LADO) {
            val y = image.height * fy / LADO
            val filaBase = y * rowStride
            for (fx in 0 until LADO) {
                val x = image.width * fx / LADO
                val pos = filaBase + x * pixelStride
                val r = buffer.get(pos).toInt() and 0xFF
                val g = buffer.get(pos + 1).toInt() and 0xFF
                val b = buffer.get(pos + 2).toInt() and 0xFF
                // Luminancia aproximada, entera, sin coma flotante por punto.
                huella[i++] = (r * 77 + g * 151 + b * 28) shr 8
            }
        }
        return huella
    }

    companion object {
        private const val LADO = 32
    }
}
