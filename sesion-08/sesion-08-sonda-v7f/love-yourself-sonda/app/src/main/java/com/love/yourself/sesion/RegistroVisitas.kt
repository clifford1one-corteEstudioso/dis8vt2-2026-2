package com.love.yourself.sesion

import com.love.yourself.config.Config

/** Lo que esta haciendo la mascota. */
enum class EstadoMascota {
    /** No hay app vigilada adelante: no se muestra. */
    OCULTA,

    /** En una app vigilada, sin arrastre: hace lo suyo (lee). */
    TRANQUILA,

    /** Empezo el scroll a pantalla completa: levanta la vista. Todavia no deja nada. */
    ALERTA,

    /** Un minuto de arrastre seguido: deja el libro. No se pone triste, solo no puede seguir. */
    DETENIDA
}

/**
 * Una estadia en una app vigilada, desde que se abre hasta que se cierra.
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

    /** Cuando aparece el cielo, en tiempo en la app (totalMs). */
    var proximoCieloMs = Config.MIN_CIELO * 60_000L

    /** Cuantas veces aparecio el cielo y cuantas eligio seguir. Los anota el servicio. */
    var cielos = 0
    var seguir = 0

    /**
     * Cuando empezo el arrastre actual, en tiempo acumulado. -1 si no hay
     * arrastre en curso. Es lo que mira la mascota.
     */
    internal var inicioTiradaMs = -1L

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
     * elegir "Salir" en el cielo. Irse a otra app cierra en silencio:
     * mostrarlo encima de WhatsApp medio minuto despues seria interrumpir justo
     * lo que el proyecto no quiere interrumpir.
     */
    val muestraBrief: Boolean
        get() = motivo != Motivo.OTRA_APP &&
            visita.sesionIniciada &&
            visita.arrastradoMs >= Config.MIN_ARRASTRE_BRIEF_S * 1000L
}

/**
 * Las reglas de sesion, sin nada de Android adentro para poder probarlas solas.
 *
 *   abre    = una app vigilada pasa a primer plano (empieza a contar el tiempo)
 *   inicia  = primer swipe sobre contenido a pantalla completa
 *   cierra  = inicio / apps recientes, bloqueo > 3 min, o "Salir"
 *
 * Solo cuentan las apps vigiladas, y dentro de ellas solo el scroll a pantalla
 * completa: bajar por los mensajes o buscar algo es uso con intencion, no
 * arrastre. Esa es la diferencia con apps como Focus Friend, que tratan igual
 * cualquier uso del telefono.
 *
 * Irse a otra app deja la visita en espera: si se vuelve antes de la
 * tolerancia, sigue la misma. Asi responder un WhatsApp no parte la sesion.
 */
class RegistroVisitas(private val paquetePropio: String) {

    var actual: Visita? = null
        private set
    private var enEspera: Visita? = null

    /** La ultima app que paso adelante. Una misma app avisa varias veces. */
    private var adelante: String? = null
    private var ultimoTicMs = 0L

    /**
     * Velocidad del reloj (modo dev). Acelera los contadores, no a la persona:
     * los intervalos que vienen del cuerpo (un gesto, mirar un reel, responder
     * un mensaje, dejar el celular) se escalan con esto y siguen midiendose en
     * segundos reales. Si no, a x60 un swipe valdria por diez.
     */
    var factorReloj = 1

    /** Un intervalo del mundo real, expresado en el reloj (acelerado o no). */
    private fun real(ms: Long) = ms * factorReloj

    private fun ignorado(paquete: String) =
        paquete == paquetePropio || paquete in Config.PAQUETES_DE_SISTEMA

    /** Suma el tiempo transcurrido a la visita actual, si corresponde. */
    private fun acumular(ahora: Long) {
        if (ultimoTicMs == 0L) {
            ultimoTicMs = ahora
            return
        }
        // Tope: si el sistema congelo el proceso un rato, ese hueco no se
        // cuenta como uso.
        val dt = (ahora - ultimoTicMs).coerceIn(0L, real(5_000L))
        ultimoTicMs = ahora
        val v = actual ?: return
        if (v.pausada || v.afuera) return
        v.totalMs += dt
        val enArrastre = v.sesionIniciada && ahora - v.ultimoSwipeMs <= real(Config.VENTANA_ARRASTRE_S * 1000L)
        if (enArrastre) {
            v.arrastradoMs += dt
        } else {
            v.inicioTiradaMs = -1L
        }
    }

    /**
     * @param vigilada si la app esta en la lista de la persona. Las demas se
     *   tratan como "salir un rato": no abren visita.
     */
    fun enPrimerPlano(paquete: String, esLauncher: Boolean, vigilada: Boolean, ahora: Long): List<Cierre> {
        if (ignorado(paquete)) return emptyList()
        acumular(ahora)
        // La misma app ajena avisando de nuevo (un dialogo, otra pantalla suya)
        // no es "irse a otra app" otra vez: no debe cerrar la visita en espera.
        if (!esLauncher && paquete == adelante && actual?.app != paquete) return emptyList()
        adelante = paquete
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

        // Otra app: la actual queda en espera; la que estaba en espera antes, se cierra.
        enEspera?.let { cierres += Cierre(it, Motivo.OTRA_APP, ahora) }
        enEspera = a?.also { it.afueraDesde = ahora }
        actual = if (vigilada) Visita(paquete, ahora) else null
        return cierres
    }

    /**
     * Devuelve true si este swipe inicia la sesion.
     *
     * @param pantallaCompleta si la zona que se deslizo ocupa casi toda la
     *   pantalla. Los scrolls que no, se ignoran: son mensajes o busqueda.
     */
    fun scroll(paquete: String, pantallaCompleta: Boolean, ahora: Long): Boolean {
        val v = actual ?: return false
        if (v.app != paquete || v.pausada || !pantallaCompleta) return false

        val esNuevoSwipe = ahora - v.ultimoEventoScrollMs > real(Config.DEBOUNCE_SWIPE_MS)
        v.ultimoEventoScrollMs = ahora
        if (!esNuevoSwipe) return false

        // Cerrar la cuenta del tramo anterior antes de mover el ultimo swipe,
        // para que ese tramo se clasifique con la ventana que tenia.
        acumular(ahora)
        v.videos++
        v.ultimoSwipeMs = ahora
        v.swipesRecientes.addLast(ahora)
        if (v.inicioTiradaMs < 0L) v.inicioTiradaMs = v.totalMs
        if (!v.sesionIniciada) {
            v.sesionIniciada = true
            return true
        }
        return false
    }

    fun tic(ahora: Long): List<Cierre> {
        acumular(ahora)
        val e = enEspera ?: return emptyList()
        if (ahora - e.afueraDesde <= real(Config.TOLERANCIA_REGRESO_S * 1000L)) return emptyList()
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
        if (pausa < real(Config.PAUSA_BLOQUEO_MIN * 60_000L)) return emptyList()
        actual = null
        return listOf(Cierre(v, Motivo.BLOQUEO, ahora))
    }

    fun cerrarActual(motivo: Motivo, ahora: Long): List<Cierre> {
        acumular(ahora)
        val v = actual ?: return emptyList()
        actual = null
        return listOf(Cierre(v, motivo, ahora))
    }

    fun mascota(): EstadoMascota {
        val v = actual ?: return EstadoMascota.OCULTA
        if (v.pausada || v.afuera) return EstadoMascota.OCULTA
        if (v.inicioTiradaMs < 0L) return EstadoMascota.TRANQUILA
        val tirada = v.totalMs - v.inicioTiradaMs
        return if (tirada >= Config.LIMITE_ARRASTRE_S * 1000L) EstadoMascota.DETENIDA else EstadoMascota.ALERTA
    }

    /** Swipes por minuto real en la ventana movil. El ritmo es del cuerpo: no se acelera. */
    fun ritmoPorMinuto(ahora: Long): Double {
        val v = actual ?: return 0.0
        val corte = ahora - real(Config.VENTANA_RITMO_S * 1000L)
        while (v.swipesRecientes.isNotEmpty() && v.swipesRecientes.first() < corte) {
            v.swipesRecientes.removeFirst()
        }
        // Al principio la ventana no esta llena; dividir por 60 daria un ritmo
        // falsamente bajo justo cuando el usuario esta mas activo.
        val ventana = minOf(Config.VENTANA_RITMO_S.toLong(), maxOf(1L, v.totalMs / real(1000L)))
        return v.swipesRecientes.size * 60.0 / ventana
    }
}
