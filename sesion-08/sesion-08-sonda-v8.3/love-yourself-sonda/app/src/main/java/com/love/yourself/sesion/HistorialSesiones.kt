package com.love.yourself.sesion

import android.content.Context

/** Una visita cerrada, como queda en el historial. Es lo que muestra Inicio. */
data class RegistroSesion(
    /** Cuando se cerro, en hora real. Tambien sirve para encontrarla despues. */
    val finMs: Long,
    val app: String,
    val totalMs: Long,
    val arrastradoMs: Long,
    val videos: Int,
    val motivo: Motivo,
    val cielos: Int = 0,
    val seguir: Int = 0,
    /** Lo que respondio en "recuerdas" del resumen. Vacio si no respondio. */
    val recuerdas: String = ""
) {
    val buscadoMs get() = (totalMs - arrastradoMs).coerceAtLeast(0L)

    /** Si hubo arrastre (al menos un swipe a pantalla completa) o fue solo uso con intencion. */
    val fueSesion get() = videos > 0

    /** Una linea de texto, con los campos separados por tabs. */
    fun aLinea(): String = listOf(
        VERSION, finMs, app, totalMs, arrastradoMs, videos, motivo.name, cielos, seguir, limpiar(recuerdas)
    ).joinToString(SEPARADOR)

    companion object {
        private const val VERSION = "1"
        private const val SEPARADOR = "\t"

        private fun limpiar(s: String) = s.replace('\t', ' ').replace('\n', ' ')

        /** null si la linea esta rota o es de otra version: se salta, no rompe el historial. */
        fun deLinea(linea: String): RegistroSesion? {
            val p = linea.split(SEPARADOR)
            if (p.size < 9 || p[0] != VERSION) return null
            return runCatching {
                RegistroSesion(
                    finMs = p[1].toLong(),
                    app = p[2],
                    totalMs = p[3].toLong(),
                    arrastradoMs = p[4].toLong(),
                    videos = p[5].toInt(),
                    motivo = Motivo.valueOf(p[6]),
                    cielos = p[7].toInt(),
                    seguir = p[8].toInt(),
                    recuerdas = p.getOrElse(9) { "" }
                )
            }.getOrNull()
        }
    }
}

/**
 * Las ultimas visitas cerradas, para Inicio. El resumen sigue leyendo la
 * semana de AcumuladoSemanal; esto guarda el detalle de cada una.
 *
 * Va en SharedPreferences como texto, una linea por visita y la mas nueva
 * primero. Son unos cientos de lineas: no hace falta una base de datos.
 */
class HistorialSesiones(context: Context, archivo: String = ARCHIVO) {

    private val prefs = context.getSharedPreferences(archivo, Context.MODE_PRIVATE)

    private var texto: String
        get() = prefs.getString(CLAVE, "") ?: ""
        set(valor) = prefs.edit().putString(CLAVE, valor).apply()

    fun agregar(r: RegistroSesion) {
        texto = conAgregada(texto, r)
    }

    /** La mas nueva primero. */
    fun ultimas(): List<RegistroSesion> = leer(texto)

    /** Anota lo que respondio en "recuerdas" la visita que se cerro en finMs. */
    fun responder(finMs: Long, recuerdas: String) {
        texto = conRespuesta(texto, finMs, recuerdas)
    }

    fun borrar() = prefs.edit().clear().apply()

    companion object {
        const val ARCHIVO = "historial_sesiones"

        /** Lo medido con el reloj acelerado va aparte, igual que en AcumuladoSemanal. */
        const val ARCHIVO_DEV = "historial_sesiones_dev"

        /** Cuantas visitas se guardan: unas semanas de uso. */
        const val MAX = 300

        private const val CLAVE = "lineas"

        // Lo que se hace con el texto, sin Android: se prueba solo.

        fun leer(texto: String): List<RegistroSesion> =
            lineas(texto).mapNotNull { RegistroSesion.deLinea(it) }.toList()

        fun conAgregada(texto: String, r: RegistroSesion): String =
            (sequenceOf(r.aLinea()) + lineas(texto)).take(MAX).joinToString("\n")

        fun conRespuesta(texto: String, finMs: Long, recuerdas: String): String =
            lineas(texto).joinToString("\n") { linea ->
                val r = RegistroSesion.deLinea(linea)
                if (r != null && r.finMs == finMs) r.copy(recuerdas = recuerdas).aLinea() else linea
            }

        private fun lineas(texto: String) = texto.lineSequence().filter { it.isNotBlank() }
    }
}
