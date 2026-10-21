package com.love.yourself.servicio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
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
 * Con REGISTRAR_AUTORES apagado no trae nombres de cuentas. Se saca con
 * Ajustes > Exportar registro.
 */
object RegistroEstudio {
    private const val TAG = "ESTUDIO"
    private val hora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private var archivo: File? = null

    fun iniciar(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        archivo = File(dir, "estudio.tsv")
    }

    /**
     * Abre el menu de compartir con una copia del registro, nombrada con el
     * modelo del telefono y la fecha para no confundir participantes.
     * Devuelve false si todavia no hay registro.
     */
    @Synchronized
    fun exportar(context: Context): Boolean {
        // Ajustes puede abrirse antes de que el servicio haya iniciado el registro.
        if (archivo == null) iniciar(context)
        val origen = archivo?.takeIf { it.exists() } ?: return false
        val carpeta = File(context.cacheDir, "exportar").apply { mkdirs() }
        carpeta.listFiles()?.forEach { it.delete() }
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val modelo = Build.MODEL.replace(Regex("[^A-Za-z0-9]"), "")
        val copia = File(carpeta, "estudio-$modelo-$fecha.tsv")
        origen.copyTo(copia, overwrite = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", copia)
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, copia.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(enviar, "Enviar registro").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    @Synchronized
    fun anotar(tipo: String, vararg campos: Any?) {
        val linea = (listOf(hora.format(Date()), tipo) + campos.map { it?.toString()?.replace('\t', ' ') ?: "" })
            .joinToString("\t")
        Log.d(TAG, linea)
        runCatching { archivo?.appendText(linea + "\n") }
    }
}
