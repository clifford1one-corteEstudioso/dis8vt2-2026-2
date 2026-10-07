package com.love.yourself.vistas

import android.graphics.Color

/**
 * Los grises de los wireframes, en un solo lugar. Es una paleta de
 * wireframe, no la final: cuando exista la identidad visual se cambia aca.
 */
object Colores {
    /** El fondo de las pantallas de la app: Inicio y la bienvenida. */
    val PAGINA = Color.parseColor("#262626")

    // WF3, el resumen. Inicio usa los mismos, para que se lean como parte de lo mismo.
    val FONDO = Color.parseColor("#414141")
    val SECCION = Color.parseColor("#545454")
    val TARJETA = Color.parseColor("#636363")
    val ARRASTRADO = Color.parseColor("#D9D9D9")
    val BUSCADO = Color.parseColor("#737373")
    val VELO = Color.argb(110, 0, 0, 0)
    const val TEXTO = Color.WHITE

    /** Etiquetas, fechas, lo que acompana a un numero. */
    val TEXTO_SUAVE = Color.parseColor("#B3B3B3")
}
