package com.love.yourself.lab

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Escribe la senal de diferencia a un CSV, una fila por muestra.
 *
 * El CSV es el producto de esta fase: se abre en una planilla, se grafica la
 * columna de diferencia contra el tiempo, y se elige el umbral mirando donde
 * caen los cortes que contaste a mano en el video.
 */
class RegistroCsv(context: Context) {

    val archivo: File
    private val escritor: FileWriter

    init {
        val carpeta = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "registros")
        carpeta.mkdirs()
        val sello = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        archivo = File(carpeta, "fase03-$sello.csv")
        escritor = FileWriter(archivo, true)
        escritor.write("ms_desde_inicio,diferencia,sobre_umbral,muestras_perdidas\n")
        escritor.flush()
    }

    fun fila(msDesdeInicio: Long, diferencia: Double, sobreUmbral: Boolean, perdidas: Int) {
        escritor.write("$msDesdeInicio,${"%.2f".format(Locale.US, diferencia)},${if (sobreUmbral) 1 else 0},$perdidas\n")
        // Sin flush por fila: a 4 muestras por segundo durante 20 minutos son
        // 4800 filas y flushear cada una castiga la bateria sin ganar nada.
    }

    fun sincronizar() {
        escritor.flush()
    }

    fun cerrar() {
        try {
            escritor.flush()
            escritor.close()
        } catch (_: Exception) {
        }
    }
}
