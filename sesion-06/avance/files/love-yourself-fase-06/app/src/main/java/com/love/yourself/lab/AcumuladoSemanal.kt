package com.love.yourself.lab

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Tiempo en arrastre por dia, para el "Esta semana" del resumen.
 *
 * El costo del uso no se percibe por episodio sino por agregado: sin esto el
 * resumen solo podria hablar de la sesion que acaba de terminar.
 *
 * Va en SharedPreferences y no en Room a proposito: son un par de numeros por
 * dia. Cuando haga falta historial detallado (fase 07), esto se reemplaza sin
 * tocar el resumen, que solo pide dos cosas: sumar y leer la semana.
 */
class AcumuladoSemanal(context: Context, archivo: String = ARCHIVO) {

    private val prefs = context.getSharedPreferences(archivo, Context.MODE_PRIVATE)
    private val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun sumar(arrastradoMs: Long, ahora: Long) {
        if (arrastradoMs <= 0L) return
        val clave = formato.format(Date(ahora))
        prefs.edit()
            .putLong(clave, prefs.getLong(clave, 0L) + arrastradoMs)
            .apply()
        olvidarViejos(ahora)
    }

    /** De lunes a hoy. */
    fun estaSemana(ahora: Long): Long {
        val dia = Calendar.getInstance().apply { timeInMillis = ahora }
        val diasDesdeLunes = (dia.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        var total = 0L
        repeat(diasDesdeLunes + 1) {
            total += prefs.getLong(formato.format(dia.time), 0L)
            dia.add(Calendar.DAY_OF_MONTH, -1)
        }
        return total
    }

    /** Mas de 60 dias no sirven para la semana y solo ocupan espacio. */
    private fun olvidarViejos(ahora: Long) {
        val limite = Calendar.getInstance().apply {
            timeInMillis = ahora
            add(Calendar.DAY_OF_MONTH, -60)
        }
        val corte = formato.format(limite.time)
        val viejas = prefs.all.keys.filter { it < corte }
        if (viejas.isEmpty()) return
        prefs.edit().apply { viejas.forEach { remove(it) } }.apply()
    }

    companion object {
        const val ARCHIVO = "acumulado_arrastre"

        /**
         * Con el reloj acelerado los minutos no son reales: van aparte, para
         * que probar no ensucie la semana de verdad.
         */
        const val ARCHIVO_DEV = "acumulado_arrastre_dev"
    }
}
