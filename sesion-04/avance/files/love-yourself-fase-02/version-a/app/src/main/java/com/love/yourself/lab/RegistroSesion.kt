package com.love.yourself.lab

import android.view.accessibility.AccessibilityEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Traduce eventos de accesibilidad a lineas legibles y aplica la regla de
 * sesion acordada:
 *
 *   inicia = primer scroll dentro de una app
 *   termina = se sale de esa app
 *
 * No recorre el arbol de nodos. Solo usa lo que viene en el evento mismo:
 * tipo, paquete y clase. La condicion de "pantalla completa" no se resuelve
 * aqui, sale de los pixeles (fase 01).
 *
 * Compartido por Fase02aService (archivo) y Fase02bService (Logcat) para que
 * ambas salidas describan exactamente lo mismo.
 */
const val TAG = "LYSFase02a"

class RegistroSesion(private val paquetePropio: String) {

    private var appActual: String? = null
    private var sesionActiva = false
    private var scrolls = 0
    private var inicioSesionMs = 0L

    private val reloj = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    /** Devuelve las lineas a emitir para este evento. Vacia si hay que ignorarlo. */
    fun procesar(evento: AccessibilityEvent): List<String> {
        val paquete = evento.packageName?.toString() ?: return emptyList()
        if (paquete == paquetePropio) return emptyList()
        // La barra de notificaciones y el teclado se dibujan encima sin que el
        // usuario salga de la app. Si se dejaran pasar, bajar la cortina para ver
        // la hora cerraria la sesion y la siguiente cuenta como nueva: una sesion
        // de 40 minutos apareceria partida en pedazos.
        if (paquete in PAQUETES_DE_SISTEMA) return emptyList()

        val ahora = System.currentTimeMillis()
        val hora = reloj.format(Date(ahora))
        val clase = evento.className?.toString() ?: "?"
        val lineas = mutableListOf<String>()

        // Cambio de app: cierra la sesion anterior si estaba abierta.
        if (paquete != appActual) {
            if (sesionActiva) {
                val duracion = (ahora - inicioSesionMs) / 1000.0
                lineas += "$hora  === SESION TERMINA  $appActual  " +
                    "scrolls=$scrolls  duracion=${"%.1f".format(duracion)}s"
            }
            appActual = paquete
            sesionActiva = false
            scrolls = 0
            lineas += "$hora  APP -> $paquete"
        }

        when (evento.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                scrolls++
                if (!sesionActiva) {
                    sesionActiva = true
                    inicioSesionMs = ahora
                    lineas += "$hora  === SESION INICIA  $paquete  (primer scroll)"
                } else {
                    lineas += "$hora  scroll #$scrolls  $clase"
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                lineas += "$hora  ventana  $clase"
            }
        }

        return lineas
    }

    companion object {
        /**
         * Superficies del sistema que aparecen sobre la app sin que el usuario
         * la abandone. La lista es corta a proposito: si en el registro aparece
         * otro paquete cortando sesiones, se agrega aca con la evidencia a mano.
         */
        private val PAQUETES_DE_SISTEMA = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey"
        )
    }
}
