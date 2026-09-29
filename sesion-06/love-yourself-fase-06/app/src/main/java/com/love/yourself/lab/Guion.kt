package com.love.yourself.lab

/**
 * Cuando puede hablar la mascota. Sin Android adentro, para probarlo solo.
 *
 * Dos reglas, para que no se vuelva ruido:
 *  - cada momento, una sola vez por sesion;
 *  - entre dos momentos, al menos una pausa (salvo el primero de la sesion).
 *
 * Se crea uno nuevo por cada visita.
 */
class Guion<T>(private val pausaMs: Long) {

    private val dichos = mutableSetOf<T>()
    private var ultimoMs = Long.MIN_VALUE

    /**
     * Devuelve true si el momento puede mostrarse ahora, y lo anota.
     * @param sinPausa para respuestas inmediatas a algo que hizo la persona.
     */
    fun pedir(momento: T, ahora: Long, sinPausa: Boolean = false): Boolean {
        if (momento in dichos) return false
        if (!sinPausa && ultimoMs != Long.MIN_VALUE && ahora - ultimoMs < pausaMs) return false
        dichos += momento
        ultimoMs = ahora
        return true
    }

    fun yaDicho(momento: T) = momento in dichos
}
