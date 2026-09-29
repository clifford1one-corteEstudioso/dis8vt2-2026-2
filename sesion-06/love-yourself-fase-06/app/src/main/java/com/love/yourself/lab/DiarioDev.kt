package com.love.yourself.lab

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lo que el servicio va viendo, para leerlo en la pantalla de la app (modo
 * dev) sin Logcat. Vive en memoria: se borra si el sistema cierra la app.
 */
object DiarioDev {
    private const val MAX = 40
    private val lineas = ArrayDeque<String>()
    private val hora = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun anotar(texto: String) {
        Log.i(TAG, texto)
        lineas.addFirst("${hora.format(Date())}  $texto")
        while (lineas.size > MAX) lineas.removeLast()
    }

    @Synchronized
    fun texto(): String = if (lineas.isEmpty()) "(vacío: todavía no pasa nada)" else lineas.joinToString("\n")

    @Synchronized
    fun borrar() = lineas.clear()
}
