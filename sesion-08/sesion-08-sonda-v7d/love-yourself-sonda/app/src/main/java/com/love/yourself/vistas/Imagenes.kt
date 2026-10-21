package com.love.yourself.vistas

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Los cerebros vienen a 745 px de ancho y casi siempre se dibujan mucho mas
 * chicos. Cargarlos a la mitad o a un cuarto ahorra memoria sin que se note:
 * las capas viven encima de Instagram, que ya ocupa bastante.
 */
object Imagenes {

    /** La imagen a la menor escala que siga midiendo al menos anchoPx. */
    fun cargar(res: Resources, id: Int, anchoPx: Int): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(res, id, medidas)
        var muestra = 1
        while (anchoPx > 0 && medidas.outWidth / (muestra * 2) >= anchoPx) muestra *= 2
        return runCatching {
            BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply { inSampleSize = muestra })
        }.getOrNull()
    }
}
