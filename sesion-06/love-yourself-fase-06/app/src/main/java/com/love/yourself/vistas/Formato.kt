package com.love.yourself.vistas

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Los textos con numeros: tiempos, semanas, fechas. Sin Android adentro, para poder probarlos. */
object Formato {

    fun estuviste(totalMs: Long): String {
        val min = totalMs / 60_000
        return when (min) {
            0L -> "Estuviste menos de un minuto"
            1L -> "Estuviste 1 minuto"
            else -> "Estuviste $min minutos"
        }
    }

    fun deEsos(totalMs: Long): String {
        val min = totalMs / 60_000
        return if (min <= 1L) "de ese minuto" else "de esos $min minutos"
    }

    /** 19:45, o 1:05:30 pasada la hora. */
    fun reloj(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val seg = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, seg) else "%d:%02d".format(m, seg)
    }

    /** 14h 20 */
    fun semana(ms: Long): String {
        val min = ms / 60_000
        return "%dh %02d".format(min / 60, min % 60)
    }

    /** 22 min, o 1 h 05. Para listas y etiquetas, donde el reloj es demasiado preciso. */
    fun minutos(ms: Long): String {
        val min = ms / 60_000
        return when {
            min < 1 -> "<1 min"
            min < 60 -> "$min min"
            else -> "%d h %02d".format(min / 60, min % 60)
        }
    }

    /** "≈ 16 s c/u": cuanto se quedo, en promedio, en cada video. Vacio si no hubo. */
    fun porVideo(arrastradoMs: Long, videos: Int): String {
        if (videos <= 0) return ""
        val s = arrastradoMs / 1000 / videos
        return if (s < 60) "≈ $s s c/u" else "≈ ${s / 60} min c/u"
    }

    fun semanaPasada(ms: Long) = "la semana pasada a esta altura: ${semana(ms)}"

    /** "3 sesiones · 151 videos". */
    fun sesionesYVideos(sesiones: Int, videos: Int): String {
        if (sesiones == 0) return "sin sesiones de arrastre"
        return "${cuenta(sesiones, "sesión", "sesiones")} · ${cuenta(videos, "video", "videos")}"
    }

    /** Lo que se hizo en una visita: "19:45 arrastrado · 71 videos", o "solo buscado" si no hubo arrastre. */
    fun detalleSesion(arrastradoMs: Long, videos: Int): String =
        if (videos == 0) "solo buscado" else "${reloj(arrastradoMs)} arrastrado · ${cuenta(videos, "video", "videos")}"

    private fun cuenta(n: Int, uno: String, varios: String) = if (n == 1) "1 $uno" else "$n $varios"

    /**
     * Como termino una sesion, para la lista de Inicio. motivo es el nombre
     * de Motivo (INICIO, BLOQUEO, SALIR, OTRA_APP).
     */
    fun comoTermino(motivo: String, seguir: Int, recuerdas: String): String {
        val partes = mutableListOf<String>()
        when (motivo) {
            "SALIR" -> partes += "saliste desde el cielo"
            "INICIO" -> partes += "saliste al inicio"
            "BLOQUEO" -> partes += "bloqueaste el teléfono"
            "OTRA_APP" -> partes += "te fuiste a otra app"
        }
        if (seguir == 1) partes += "seguiste 1 vez"
        if (seguir > 1) partes += "seguiste $seguir veces"
        if (recuerdas.isNotEmpty()) partes += "recordabas $recuerdas"
        return partes.joinToString(" · ")
    }

    /** "hoy 18:42", "ayer 21:10", "lun 29 · 14:02". */
    fun cuando(ms: Long, ahora: Long): String {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
        return when (diasEntre(ms, ahora)) {
            0 -> "hoy $hora"
            1 -> "ayer $hora"
            else -> {
                val c = Calendar.getInstance().apply { timeInMillis = ms }
                val dia = DIAS[(c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7]
                "$dia ${c.get(Calendar.DAY_OF_MONTH)} · $hora"
            }
        }
    }

    private val DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

    /** Dias de calendario entre dos instantes (no de 24 horas: ayer a las 23:59 es "ayer"). */
    private fun diasEntre(antes: Long, despues: Long): Int {
        // Las 12 horas de mas absorben el cambio de horario de verano.
        return ((medianoche(despues) - medianoche(antes) + 12 * 3_600_000L) / 86_400_000L).toInt()
    }

    private fun medianoche(ms: Long) = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
