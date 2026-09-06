package com.love.yourself.lab

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fase 02a — guarda a archivo.
 *
 * Escribe cada evento a un .txt en la carpeta de la app. No pierde nada y
 * permite usar Instagram con el celular desconectado del PC, que es la unica
 * forma de observar el scroll en condiciones reales.
 *
 * El archivo se saca despues con:
 *   adb pull /sdcard/Android/data/com.love.yourself.fase1/files/registros/
 */
class Fase02aService : AccessibilityService() {

    private var registro: RegistroSesion? = null
    private var escritor: FileWriter? = null
    private var archivo: File? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        registro = RegistroSesion(packageName)

        val carpeta = File(getExternalFilesDir(null), "registros")
        carpeta.mkdirs()
        val sello = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val destino = File(carpeta, "fase02a-$sello.txt")
        archivo = destino
        escritor = FileWriter(destino, true)

        // Esta linea si va a Logcat: es la unica forma de saber que arranco.
        Log.i(TAG, "Fase 02a conectada. Escribiendo en: ${destino.absolutePath}")
        Log.i(TAG, "adb pull \"${destino.absolutePath}\"")
        escribir("--- fase 02a iniciada $sello ---")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val e = evento ?: return
        registro?.procesar(e)?.forEach { escribir(it) }
    }

    private fun escribir(linea: String) {
        try {
            escritor?.apply {
                write(linea)
                write("\n")
                // Flush en cada linea: si Android mata el service, no se pierde nada.
                flush()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "No se pudo escribir el registro", ex)
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Fase 02a interrumpida")
    }

    override fun onDestroy() {
        super.onDestroy()
        escribir("--- fase 02a detenida ---")
        try {
            escritor?.close()
        } catch (ex: Exception) {
            Log.e(TAG, "Error cerrando el registro", ex)
        }
        escritor = null
    }
}
