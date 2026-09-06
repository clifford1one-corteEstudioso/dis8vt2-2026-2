package com.love.yourself.lab

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Convierte un [Image] del ImageReader en Bitmap y lo guarda como PNG.
 *
 * El buffer que entrega Android suele ser mas ancho que la pantalla por
 * alineacion de memoria (rowStride > pixelStride * width). Si se ignora ese
 * padding la imagen sale con una diagonal de basura y parece captura fallida
 * cuando en realidad los pixeles llegaron bien.
 */
object FrameWriter {

    fun guardar(image: Image, destino: File): File {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmapConPadding = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmapConPadding.copyPixelsFromBuffer(plane.buffer)

        // Recortar el padding sobrante de la derecha.
        val bitmap = if (rowPadding == 0) {
            bitmapConPadding
        } else {
            Bitmap.createBitmap(bitmapConPadding, 0, 0, image.width, image.height)
        }

        FileOutputStream(destino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
        }

        Log.i(TAG, "rowStride=$rowStride pixelStride=$pixelStride padding=$rowPadding")
        Log.i(TAG, diagnosticoDeNegro(bitmap))

        if (bitmap !== bitmapConPadding) bitmapConPadding.recycle()
        bitmap.recycle()
        return destino
    }

    /**
     * Respuesta directa a la pregunta de la prueba: si la ventana esta marcada
     * FLAG_SECURE, MediaProjection no falla, entrega un rectangulo negro.
     * Muestrea una grilla en vez de recorrer todo el bitmap.
     */
    private fun diagnosticoDeNegro(bitmap: Bitmap): String {
        val pasos = 20
        var noNegros = 0
        var total = 0
        for (i in 0 until pasos) {
            for (j in 0 until pasos) {
                val x = bitmap.width * i / pasos
                val y = bitmap.height * j / pasos
                val pixel = bitmap.getPixel(x, y)
                total++
                if (pixel and 0x00FFFFFF != 0) noNegros++
            }
        }
        return if (noNegros == 0) {
            "VEREDICTO: todos los $total pixeles muestreados son negros -> posible FLAG_SECURE"
        } else {
            "VEREDICTO: $noNegros de $total pixeles muestreados tienen color -> hay captura real"
        }
    }
}
