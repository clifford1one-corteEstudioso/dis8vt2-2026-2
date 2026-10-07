package com.love.yourself.sesion

import android.content.Context
import java.util.Calendar

/**
 * Tiempo en arrastre por dia, para el "Esta semana" del resumen y las barras
 * de Inicio.
 *
 * El costo del uso no se percibe por episodio sino por agregado: sin esto el
 * resumen solo podria hablar de la sesion que acaba de terminar.
 *
 * Va en SharedPreferences y no en una base de datos a proposito: son un par
 * de numeros por dia. El detalle de cada sesion esta en HistorialSesiones.
 */
class AcumuladoSemanal(context: Context, archivo: String = ARCHIVO) {

    private val prefs = context.getSharedPreferences(archivo, Context.MODE_PRIVATE)

    fun sumar(arrastradoMs: Long, ahora: Long) {
        if (arrastradoMs <= 0L) return
        val clave = Semana.fecha(ahora)
        prefs.edit()
            .putLong(clave, prefs.getLong(clave, 0L) + arrastradoMs)
            .apply()
        olvidarViejos(ahora)
    }

    /** El arrastre del dia de ahora. */
    fun dia(ahora: Long): Long = prefs.getLong(Semana.fecha(ahora), 0L)

    /** Cada dia de esta semana, de lunes a domingo. Los que vienen, en 0. */
    fun semanaPorDia(ahora: Long): List<Long> = Semana.dias(ahora).map { prefs.getLong(it, 0L) }

    /** De lunes a hoy. */
    fun estaSemana(ahora: Long): Long = semanaPorDia(ahora).take(Semana.indice(ahora) + 1).sum()

    /** La semana pasada, del lunes al mismo dia que hoy: para comparar a la misma altura. */
    fun semanaPasadaHastaHoy(ahora: Long): Long =
        Semana.mismosDiasSemanaPasada(ahora).sumOf { prefs.getLong(it, 0L) }

    fun borrar() = prefs.edit().clear().apply()

    /** Mas de 60 dias no sirven para la semana y solo ocupan espacio. */
    private fun olvidarViejos(ahora: Long) {
        val limite = Calendar.getInstance().apply {
            timeInMillis = ahora
            add(Calendar.DAY_OF_MONTH, -60)
        }
        val corte = Semana.fecha(limite.timeInMillis)
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
