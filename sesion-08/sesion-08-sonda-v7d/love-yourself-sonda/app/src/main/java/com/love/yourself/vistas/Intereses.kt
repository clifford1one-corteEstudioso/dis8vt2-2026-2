package com.love.yourself.vistas

import com.love.yourself.R

/** Algo que la persona quiere hacer mas: el nombre que se guarda y su icono. */
class Interes(val nombre: String, val icono: Int)

/**
 * Los intereses de la bienvenida ("¿Qué te gustaría hacer más?"), en una
 * grilla de a tres. Los iconos son Material Symbols provisorios: se cambian
 * por dibujos propios reemplazando los drawables ic_interes_*.
 */
object Intereses {
    val TODOS = listOf(
        Interes("Música", R.drawable.ic_interes_music_note),
        Interes("Ejercicio", R.drawable.ic_interes_fitness_center),
        Interes("Leer", R.drawable.ic_interes_menu_book),
        Interes("Dibujar", R.drawable.ic_interes_palette),
        Interes("Cocinar", R.drawable.ic_interes_restaurant),
        Interes("Caminar", R.drawable.ic_interes_directions_walk),
        Interes("Amigos", R.drawable.ic_interes_group),
        Interes("Estudiar", R.drawable.ic_interes_school),
        Interes("Jugar", R.drawable.ic_interes_sports_esports)
    )
}
