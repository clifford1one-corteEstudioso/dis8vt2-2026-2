package com.love.yourself.servicio

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * El registro del estudio: una linea por evento (segmentos, reels, aviso,
 * cielo, visitas), separada por tabuladores. Queda en
 * Android/data/com.love.yourself.sonda/files/estudio.tsv, que se copia al
 * computador por cable. A diferencia de Logcat, sobrevive dias.
 *
 * Trae nombres de cuentas publicas (autores de reels): anonimizar antes de
 * compartir.
 */
object RegistroEstudio {
    private const val TAG = "ESTUDIO"
    private val hora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private var archivo: File? = null

    fun iniciar(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        archivo = File(dir, "estudio.tsv")
    }

    @Synchronized
    fun anotar(tipo: String, vararg campos: Any?) {
        val linea = (listOf(hora.format(Date()), tipo) + campos.map { it?.toString()?.replace('\t', ' ') ?: "" })
            .joinToString("\t")
        Log.d(TAG, linea)
        runCatching { archivo?.appendText(linea + "\n") }
    }
}
