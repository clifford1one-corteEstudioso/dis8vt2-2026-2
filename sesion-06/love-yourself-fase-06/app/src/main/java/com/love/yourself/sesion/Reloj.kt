package com.love.yourself.sesion

/**
 * La hora que usa toda la app. Normalmente es la hora real; en el modo dev
 * puede correr 10 o 60 veces mas rapido, para ver lo que pasa a los 15
 * minutos sin esperar 15 minutos.
 *
 * Al cambiar la velocidad se re-ancla en el instante actual: el tiempo ya
 * transcurrido no salta.
 */
object Reloj {

    var factor = 1
        private set

    private var baseReal = System.currentTimeMillis()
    private var baseReloj = baseReal

    fun ahora(): Long = baseReloj + (System.currentTimeMillis() - baseReal) * factor

    fun cambiarFactor(nuevo: Int) {
        if (nuevo == factor) return
        val instante = ahora()
        baseReal = System.currentTimeMillis()
        baseReloj = instante
        factor = nuevo.coerceAtLeast(1)
    }
}
