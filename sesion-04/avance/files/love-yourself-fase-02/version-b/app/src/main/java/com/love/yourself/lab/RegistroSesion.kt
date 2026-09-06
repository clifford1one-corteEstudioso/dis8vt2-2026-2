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
const val TAG = "LYSFase02b"

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
}
