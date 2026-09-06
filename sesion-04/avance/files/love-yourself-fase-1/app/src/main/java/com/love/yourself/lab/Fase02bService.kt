package com.love.yourself.lab

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Fase 02b — en vivo a Logcat.
 *
 * Responde una sola pregunta: ¿esto esta corriendo? Es la falla mas probable,
 * porque un AccessibilityService mal declarado nunca se activa y no avisa.
 *
 * Estrangula la salida: como maximo una tanda cada [INTERVALO_MS]. Logcat es un
 * buffer circular y el scroll dispara eventos decenas de veces por segundo; sin
 * el freno se llena y bota justo lo que se quiere leer.
 *
 * Las lineas de sesion (inicia / termina) nunca se estrangulan: son pocas y son
 * las que importan.
 *
 *   adb logcat -s LYSFase1
 */
class Fase02bService : AccessibilityService() {

    private var registro: RegistroSesion? = null
    private var ultimaEmision = 0L
    private var descartados = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        registro = RegistroSesion(packageName)
        Log.i(TAG, "Fase 02b conectada. Estrangulando a 1 tanda cada $INTERVALO_MS ms")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val e = evento ?: return
        val lineas = registro?.procesar(e) ?: return
        if (lineas.isEmpty()) return

        val ahora = System.currentTimeMillis()
        val esDeSesion = lineas.any { it.contains("SESION") }

        if (!esDeSesion && ahora - ultimaEmision < INTERVALO_MS) {
            descartados++
            return
        }

        if (descartados > 0) {
            Log.i(TAG, "... $descartados eventos descartados por el estrangulador")
            descartados = 0
        }
        lineas.forEach { Log.i(TAG, it) }
        ultimaEmision = ahora
    }

    override fun onInterrupt() {
        Log.i(TAG, "Fase 02b interrumpida")
    }

    companion object {
        private const val INTERVALO_MS = 800L
    }
}
