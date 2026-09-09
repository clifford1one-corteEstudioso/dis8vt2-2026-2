package com.love.yourself.lab

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * El corazon de la app.
 *
 * Vive siempre encendido una vez activado en Ajustes, y no necesita permiso de
 * grabar pantalla: mide tiempo de sesion y ritmo de swipes, que es lo que se
 * puede obtener sin pedirle nada al usuario en el momento.
 *
 * Esa es la razon de que exista: el conteo de cortes exige un dialogo de
 * consentimiento en cada medicion, asi que jamas podria arrancar solo. Y una
 * intervencion que hay que encender a mano no sirve para alguien que se pierde
 * cuarenta minutos sin darse cuenta.
 */
class SesionService : AccessibilityService() {

    private lateinit var estado: EstadoSesion
    private var overlay: OverlayFriccion? = null
    private val reloj = Handler(Looper.getMainLooper())

    /** Momento a partir del cual se puede volver a preguntar. */
    private var proximaDecisionMs = 0L

    private val tic = object : Runnable {
        override fun run() {
            refrescar()
            reloj.postDelayed(this, 1000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        estado = EstadoSesion(packageName)
        overlay = OverlayFriccion(this)
        reloj.post(tic)
        Log.i(TAG, "Love Yourself activo. Presencia a los ${Config.MIN_PRESENCIA} min, decision a los ${Config.MIN_DECISION}.")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val e = evento ?: return
        val paquete = e.packageName?.toString() ?: return
        val ahora = System.currentTimeMillis()

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (estado.cambioDeApp(paquete, ahora) == EstadoSesion.Cambio.TERMINA) {
                    Log.i(TAG, "sesion termina: ${estado.segundos(ahora)}s, ${estado.swipes} swipes")
                    overlay?.ocultarTodo()
                    proximaDecisionMs = 0L
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (paquete in Config.PAQUETES_DE_SISTEMA || paquete == packageName) return
                // Un scroll implica que esta app esta en primer plano, aunque no
                // haya llegado el evento de ventana.
                estado.cambioDeApp(paquete, ahora)
                if (estado.scroll(ahora) == EstadoSesion.Cambio.INICIA) {
                    Log.i(TAG, "sesion inicia en $paquete")
                    proximaDecisionMs = ahora + Config.MIN_DECISION * 60_000L
                }
            }
        }
    }

    /**
     * Un tic por segundo en vez de reaccionar solo a eventos: el reloj tiene que
     * seguir corriendo aunque el usuario se quede mirando un reel sin deslizar.
     * Quedarse quieto viendo tambien es estar ahi.
     */
    private fun refrescar() {
        val ov = overlay ?: return
        val ahora = System.currentTimeMillis()

        if (!estado.activa) {
            ov.mostrarCaja(OverlayFriccion.Etapa.OCULTO, 0, 0.0)
            return
        }

        val segundos = estado.segundos(ahora)
        val minutos = segundos / 60

        if (proximaDecisionMs > 0L && ahora >= proximaDecisionMs && !ov.decisionVisible()) {
            ov.mostrarDecision(
                minutos = minutos,
                alSeguir = {
                    // No castiga la eleccion: solo aplaza. Preguntar de nuevo al
                    // tiro convertiria la friccion en hostigamiento.
                    proximaDecisionMs = System.currentTimeMillis() + Config.MIN_REPREGUNTA * 60_000L
                    Log.i(TAG, "eligio seguir a los $minutos min")
                },
                alSalir = {
                    Log.i(TAG, "eligio salir a los $minutos min")
                    estado.cerrar()
                    ov.ocultarTodo()
                    proximaDecisionMs = 0L
                    // Devolverlo al inicio; no se puede cerrar Instagram por el.
                    val casa = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(casa)
                }
            )
            return
        }

        val etapa = if (minutos >= Config.MIN_PRESENCIA) {
            OverlayFriccion.Etapa.PRESENCIA
        } else {
            OverlayFriccion.Etapa.ESPEJO
        }
        ov.mostrarCaja(etapa, segundos, estado.ritmoPorMinuto(ahora))
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        reloj.removeCallbacks(tic)
        overlay?.ocultarTodo()
        overlay = null
    }
}
