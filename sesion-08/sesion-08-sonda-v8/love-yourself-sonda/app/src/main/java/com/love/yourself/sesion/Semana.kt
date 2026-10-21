package com.love.yourself.sesion

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Las fechas de la semana, en el formato con que se guardan los dias
 * ("2026-10-04"). Sin Android adentro, para probarlo solo.
 *
 * La semana parte el lunes, igual que "Esta semana" en el resumen.
 */
object Semana {

    private val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** La fecha de un instante, como se guarda. */
    fun fecha(ms: Long): String = synchronized(formato) { formato.format(Date(ms)) }

    /** Que dia de la semana es: 0 = lunes, 6 = domingo. */
    fun indice(ms: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    }

    /** Las 7 fechas de la semana de ms, de lunes a domingo. */
    fun dias(ms: Long): List<String> = desdeElLunes(ms, semanasAtras = 0)

    /**
     * La semana pasada, del lunes al mismo dia de la semana que ms. Para
     * comparar a la misma altura: un martes contra el lunes y martes anteriores,
     * no contra la semana entera.
     */
    fun mismosDiasSemanaPasada(ms: Long): List<String> =
        desdeElLunes(ms, semanasAtras = 1).take(indice(ms) + 1)

    private fun desdeElLunes(ms: Long, semanasAtras: Int): List<String> {
        val c = Calendar.getInstance().apply {
            timeInMillis = ms
            add(Calendar.DAY_OF_MONTH, -indice(ms) - 7 * semanasAtras)
        }
        return List(7) {
            fecha(c.timeInMillis).also { c.add(Calendar.DAY_OF_MONTH, 1) }
        }
    }
}
