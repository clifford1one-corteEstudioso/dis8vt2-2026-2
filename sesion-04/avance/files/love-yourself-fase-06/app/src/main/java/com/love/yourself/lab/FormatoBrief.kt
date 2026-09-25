package com.love.yourself.lab

/** Los textos del resumen. Sin Android adentro, para poder probarlos. */
object FormatoBrief {

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
}
