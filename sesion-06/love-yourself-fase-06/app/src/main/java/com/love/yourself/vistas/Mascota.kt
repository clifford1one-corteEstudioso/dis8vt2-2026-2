package com.love.yourself.vistas

import com.love.yourself.R
import com.love.yourself.config.Config

/*
 * La mascota es tu cerebro: no te reta, muestra lo que el arrastre le hace.
 * Aca estan sus caras y lo que dice; cuando habla lo decide el servicio.
 *
 * Los textos son provisorios: el tono esta por decidirse.
 */

/**
 * Los momentos que agrandan la burbuja. Cada uno: lo que puede decir (se elige
 * una frase al azar, para que no se vuelva invisible) y como cambia su cara
 * (desde -> hasta).
 */
enum class Momento(val frases: List<String>, val desde: Int, val hasta: Int?) {
    /** Primer swipe a pantalla completa. */
    PRIMER_SWIPE(listOf("¿ya?", "¿empezamos?", "mmm…"), R.drawable.brain_sus, R.drawable.brain_sad),

    /** Un minuto de arrastre sin parar. */
    ARRASTRE_SEGUIDO(listOf("otro más…", "¿y este?", "sigue, sigue…"), R.drawable.brain_pfff, null),

    /** Muchos swipes por minuto durante un rato. */
    RITMO_ALTO(listOf("más lento, porfa", "no alcanzo a ver nada", "¿viste alguno?"), R.drawable.brain_pfff, R.drawable.brain_f),

    /** Cinco minutos en la app. */
    CINCO_MIN(listOf("llevamos 5 min", "me está dando sueño", "5 minutos ya"), R.drawable.brain_drowzy, null),

    /** Volver a la app poco despues de haberla cerrado. */
    VOLVISTE(listOf("¿otra vez?", "¿se te olvidó algo?", "hola de nuevo"), R.drawable.brain_sus, null),

    /** Un minuto antes del cielo. */
    ANTES_DEL_CIELO(listOf("necesito aire", "¿salimos un rato?", "ya casi…"), R.drawable.brain_serio, R.drawable.brain_f),

    /** Justo despues de elegir "Seguir" en el cielo. */
    TRAS_SEGUIR(listOf("ok…", "bueno, sigamos", "ahí vamos"), R.drawable.brain_sad, null)
}

/**
 * La cara que lleva la pildora todo el rato: el cerebro se va gastando con el
 * tiempo en la app. Tranquilo antes de arrastrar, frito al llegar al cielo.
 * Cambiar los minutos o el orden aca.
 */
object CaraProgresiva {
    fun para(sesionIniciada: Boolean, totalMs: Long): Int {
        if (!sesionIniciada) return R.drawable.brain_relief
        val min = totalMs / 60_000.0
        return when {
            min < 1 -> R.drawable.brain_sus
            min < 3 -> R.drawable.brain_pfff
            min < 5 -> R.drawable.brain_serio
            min < 7 -> R.drawable.brain_drowzy
            min < Config.MIN_CIELO -> R.drawable.brain_sad
            else -> R.drawable.brain_f
        }
    }
}

/**
 * La cara del dia, arriba en Inicio: como quedo el cerebro con todo el
 * arrastre de hoy, sumando las sesiones. Dice como esta el, no como lo hiciste
 * tu. Cambiar los minutos o las frases aca.
 */
object CaraDelDia {

    private class Estado(val hastaMin: Int, val cara: Int, val frase: String)

    private val estados = listOf(
        Estado(1, R.drawable.brain_relief, "hoy estoy entero"),
        Estado(15, R.drawable.brain_sus, "por ahora, bien"),
        Estado(30, R.drawable.brain_pfff, "un poco cansado"),
        Estado(45, R.drawable.brain_serio, "ya fue bastante"),
        Estado(60, R.drawable.brain_drowzy, "me está dando sueño"),
        Estado(90, R.drawable.brain_sad, "hoy me cuesta"),
        Estado(Int.MAX_VALUE, R.drawable.brain_f, "hoy quedé frito")
    )

    private fun estado(arrastreMs: Long) = estados.first { arrastreMs / 60_000 < it.hastaMin }

    fun para(arrastreMs: Long): Int = estado(arrastreMs).cara

    fun frase(arrastreMs: Long): String = estado(arrastreMs).frase
}

/** Lo que dice en el resumen: aliviado si saliste antes del cielo, frito si llegaste a verlo. */
object FrasesCierre {
    val ALIVIO = listOf("uf, gracias", "qué alivio", "aire, por fin")
    val FRITO = listOf("la próxima salimos antes", "quedé frito", "necesito descansar")
}
