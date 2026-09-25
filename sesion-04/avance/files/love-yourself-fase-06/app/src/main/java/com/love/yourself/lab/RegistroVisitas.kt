package com.love.yourself.lab

/**
 * Una estadia en una app, desde que se abre hasta que se cierra.
 *
 * El tiempo se acumula en vez de restar horas de reloj: asi las pausas (la
 * pantalla bloqueada, un rato en otra app) no cuentan como estar ahi.
 */
class Visita(val app: String, val entradaMs: Long) {
    var totalMs = 0L
        internal set
    var arrastradoMs = 0L
        internal set
    var videos = 0
        internal set
    var sesionIniciada = false
        internal set

    /** En tiempo acumulado (totalMs), no de reloj. */
    var proximaDecisionMs = Config.MIN_DECISION * 60_000L

    internal var ultimoEventoScrollMs = 0L
    internal var ultimoSwipeMs = 0L
    internal var pausadaDesde = 0L
    internal var afueraDesde = 0L
    internal val swipesRecientes = ArrayDeque<Long>()

    val buscadoMs get() = totalMs - arrastradoMs
    val pausada get() = pausadaDesde > 0L
    val afuera get() = afueraDesde > 0L
}

enum class Motivo { INICIO, BLOQUEO, SALIR, OTRA_APP }

class Cierre(val visita: Visita, val motivo: Motivo, val cierreMs: Long) {
    /**
     * El resumen aparece al salir por el inicio, al bloquear mas de 3 min, o al
     * elegir "Salir" en la pantalla de decision. Irse a otra app cierra en
     * silencio: mostrar un resumen encima de WhatsApp medio minuto despues
     * seria interrumpir justo lo que el proyecto no quiere interrumpir.
     */
    val muestraBrief: Boolean
        get() = motivo != Motivo.OTRA_APP &&
            visita.sesionIniciada &&
            visita.arrastradoMs >= Config.MIN_ARRASTRE_BRIEF_S * 1000L
}

/**
 * Las reglas de sesion, sin nada de Android adentro para poder probarlas solas.
 *
 *   abre    = la app pasa a primer plano (empieza a contar el tiempo)
 *   inicia  = primer swipe (desde aca hay sesion, y overlay)
 *   cierra  = inicio / apps recientes, bloqueo > 3 min, o "Salir"
 *
 * Irse a otra app deja la visita en espera: si se vuelve antes de la
 * tolerancia, sigue la misma. Asi responder un WhatsApp no parte la sesion.
 */
class RegistroVisitas(private val paquetePropio: String) {

    var actual: Visita? = null
        private set
    private var enEspera: Visita? = null
    private var ultimoTicMs = 0L

    private fun ignorado(paquete: String) =
        paquete == paquetePropio || paquete in Config.PAQUETES_DE_SISTEMA

    /** Suma el tiempo transcurrido a la visita actual, si corresponde. */
    private fun acumular(ahora: Long) {
        if (ultimoTicMs == 0L) {
            ultimoTicMs = ahora
            return
        }
        // Tope de 5 s: si el sistema congelo el proceso un rato, ese hueco no
        // se cuenta como uso.
        val dt = (ahora - ultimoTicMs).coerceIn(0L, 5_000L)
        ultimoTicMs = ahora
        val v = actual ?: return
        if (v.pausada || v.afuera) return
        v.totalMs += dt
        if (v.sesionIniciada && ahora - v.ultimoSwipeMs <= Config.VENTANA_ARRASTRE_S * 1000L) {
            v.arrastradoMs += dt
        }
    }

    fun enPrimerPlano(paquete: String, esLauncher: Boolean, ahora: Long): List<Cierre> {
        if (ignorado(paquete)) return emptyList()
        acumular(ahora)
        val cierres = mutableListOf<Cierre>()

        if (esLauncher) {
            actual?.let { cierres += Cierre(it, Motivo.INICIO, ahora) }
            enEspera?.let { cierres += Cierre(it, Motivo.OTRA_APP, ahora) }
            actual = null
            enEspera = null
            return cierres
        }

        val a = actual
        if (a != null && a.app == paquete) {
            a.afueraDesde = 0L
            return cierres
        }

        val e = enEspera
        if (e != null && e.app == paquete) {
            // Volvio dentro de la tolerancia: sigue la misma visita.
            a?.let { cierres += Cierre(it, Motivo.OTRA_APP, ahora) }
            e.afueraDesde = 0L
            actual = e
            enEspera = null
            return cierres
        }

        enEspera?.let { cierres += Cierre(it, Motivo.OTRA_APP, ahora) }
        enEspera = a?.also { it.afueraDesde = ahora }
        actual = Visita(paquete, ahora)
        return cierres
    }

    /** Devuelve true si este swipe inicia la sesion. */
    fun scroll(paquete: String, ahora: Long): Boolean {
        val v = actual ?: return false
        if (v.app != paquete || v.pausada) return false

        val esNuevoSwipe = ahora - v.ultimoEventoScrollMs > Config.DEBOUNCE_SWIPE_MS
        v.ultimoEventoScrollMs = ahora
        if (!esNuevoSwipe) return false

        // Cerrar la cuenta del tramo anterior antes de mover el ultimo swipe,
        // para que ese tramo se clasifique con la ventana que tenia.
        acumular(ahora)
        v.videos++
        v.ultimoSwipeMs = ahora
        v.swipesRecientes.addLast(ahora)
        if (!v.sesionIniciada) {
            v.sesionIniciada = true
            return true
        }
        return false
    }

    fun tic(ahora: Long): List<Cierre> {
        acumular(ahora)
        val e = enEspera ?: return emptyList()
        if (ahora - e.afueraDesde <= Config.TOLERANCIA_REGRESO_S * 1000L) return emptyList()
        enEspera = null
        return listOf(Cierre(e, Motivo.OTRA_APP, ahora))
    }

    fun pantallaApagada(ahora: Long) {
        acumular(ahora)
        actual?.let { if (!it.pausada) it.pausadaDesde = ahora }
    }

    fun desbloqueo(ahora: Long): List<Cierre> {
        acumular(ahora)
        val v = actual ?: return emptyList()
        if (!v.pausada) return emptyList()
        val pausa = ahora - v.pausadaDesde
        v.pausadaDesde = 0L
        if (pausa < Config.PAUSA_BLOQUEO_MIN * 60_000L) return emptyList()
        actual = null
        return listOf(Cierre(v, Motivo.BLOQUEO, ahora))
    }

    fun cerrarActual(motivo: Motivo, ahora: Long): List<Cierre> {
        acumular(ahora)
        val v = actual ?: return emptyList()
        actual = null
        return listOf(Cierre(v, motivo, ahora))
    }

    /** Swipes por minuto en la ventana movil. */
    fun ritmoPorMinuto(ahora: Long): Double {
        val v = actual ?: return 0.0
        val corte = ahora - Config.VENTANA_RITMO_S * 1000L
        while (v.swipesRecientes.isNotEmpty() && v.swipesRecientes.first() < corte) {
            v.swipesRecientes.removeFirst()
        }
        // Al principio la ventana no esta llena; dividir por 60 daria un ritmo
        // falsamente bajo justo cuando el usuario esta mas activo.
        val ventana = minOf(Config.VENTANA_RITMO_S.toLong(), maxOf(1L, v.totalMs / 1000))
        return v.swipesRecientes.size * 60.0 / ventana
    }
}
