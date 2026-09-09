package com.love.yourself.lab

/**
 * La regla de sesion, sin nada de Android adentro para poder razonarla sola.
 *
 *   inicia  = primer scroll dentro de una app
 *   termina = se sale de esa app y no se vuelve antes de la tolerancia
 */
class EstadoSesion(private val paquetePropio: String) {

    enum class Cambio { NADA, INICIA, SIGUE, TERMINA }

    var appActual: String? = null
        private set
    var activa = false
        private set
    var swipes = 0
        private set
    var inicioMs = 0L
        private set

    private var salidaMs = 0L
    private val instantesSwipe = ArrayDeque<Long>()

    /** Segundos transcurridos desde que arranco la sesion. */
    fun segundos(ahora: Long): Long =
        if (activa) (ahora - inicioMs) / 1000 else 0

    /** Swipes por minuto en la ventana movil. Lo que se muestra como ritmo. */
    fun ritmoPorMinuto(ahora: Long): Double {
        val corte = ahora - Config.VENTANA_RITMO_S * 1000L
        while (instantesSwipe.isNotEmpty() && instantesSwipe.first() < corte) {
            instantesSwipe.removeFirst()
        }
        if (!activa) return 0.0
        val transcurrido = (ahora - inicioMs) / 1000
        // Al principio la ventana no esta llena; dividir por 60 daria un ritmo
        // falsamente bajo justo cuando el usuario esta mas activo.
        val ventana = minOf(Config.VENTANA_RITMO_S.toLong(), maxOf(1L, transcurrido))
        return instantesSwipe.size * 60.0 / ventana
    }

    fun cambioDeApp(paquete: String, ahora: Long): Cambio {
        if (paquete == paquetePropio || paquete in Config.PAQUETES_DE_SISTEMA) return Cambio.NADA

        if (paquete == appActual) {
            // Volvio a la misma app. Si fue rapido, la sesion nunca se corto.
            if (!activa && salidaMs > 0L &&
                ahora - salidaMs <= Config.TOLERANCIA_REGRESO_S * 1000L
            ) {
                activa = true
                salidaMs = 0L
                return Cambio.SIGUE
            }
            return Cambio.NADA
        }

        val terminaba = activa
        if (activa) {
            activa = false
            salidaMs = ahora
        }
        appActual = paquete
        return if (terminaba) Cambio.TERMINA else Cambio.NADA
    }

    fun scroll(ahora: Long): Cambio {
        instantesSwipe.addLast(ahora)
        swipes++
        if (activa) return Cambio.NADA

        // Reanudar dentro de la tolerancia conserva el contador; si no, empieza
        // de cero. Es lo que separa una sesion interrumpida de una nueva.
        val reanuda = salidaMs > 0L && ahora - salidaMs <= Config.TOLERANCIA_REGRESO_S * 1000L
        activa = true
        salidaMs = 0L
        if (!reanuda) {
            inicioMs = ahora
            swipes = 1
            instantesSwipe.clear()
            instantesSwipe.addLast(ahora)
        }
        return Cambio.INICIA
    }

    fun cerrar() {
        activa = false
        salidaMs = 0L
    }
}
