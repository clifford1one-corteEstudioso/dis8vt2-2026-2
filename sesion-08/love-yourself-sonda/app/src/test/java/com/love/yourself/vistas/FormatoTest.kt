package com.love.yourself.vistas

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class FormatoTest {

    @Test
    fun minutos() {
        assertEquals("Estuviste menos de un minuto", Formato.estuviste(45_000))
        assertEquals("Estuviste 1 minuto", Formato.estuviste(90_000))
        assertEquals("Estuviste 22 minutos", Formato.estuviste(22 * 60_000L + 30_000))
        assertEquals("de ese minuto", Formato.deEsos(70_000))
        assertEquals("de esos 22 minutos", Formato.deEsos(22 * 60_000L))
    }

    @Test
    fun relojComoEnElWireframe() {
        assertEquals("19:45", Formato.reloj((19 * 60 + 45) * 1000L))
        assertEquals("1:15", Formato.reloj(75_000))
        assertEquals("0:05", Formato.reloj(5_000))
        assertEquals("1:05:30", Formato.reloj((3600 + 5 * 60 + 30) * 1000L))
    }

    @Test
    fun semanaComoEnElWireframe() {
        assertEquals("14h 20", Formato.semana((14 * 60 + 20) * 60_000L))
        assertEquals("3h 40", Formato.semana((3 * 60 + 40) * 60_000L))
        assertEquals("0h 05", Formato.semana(5 * 60_000L))
    }

    @Test
    fun minutosParaListas() {
        assertEquals("<1 min", Formato.minutos(40_000))
        assertEquals("22 min", Formato.minutos(22 * 60_000L + 59_000))
        assertEquals("1 h 05", Formato.minutos(65 * 60_000L))
    }

    @Test
    fun segundosPorVideo() {
        assertEquals("≈ 16 s c/u", Formato.porVideo((19 * 60 + 45) * 1000L, 71))
        assertEquals("≈ 2 min c/u", Formato.porVideo(10 * 60_000L, 5))
        assertEquals("", Formato.porVideo(60_000L, 0))
    }

    @Test
    fun comoTermino() {
        assertEquals("saliste desde el cielo", Formato.comoTermino("SALIR", 0, ""))
        assertEquals("te fuiste a otra app · seguiste 1 vez", Formato.comoTermino("OTRA_APP", 1, ""))
        assertEquals(
            "saliste al inicio · seguiste 2 veces · recordabas 1–3",
            Formato.comoTermino("INICIO", 2, "1–3")
        )
    }

    @Test
    fun cuandoEsHoyAyerOOtroDia() {
        // Jueves 8 de octubre de 2026, 20:00.
        val ahora = instante(2026, Calendar.OCTOBER, 8, 20, 0)
        assertEquals("hoy 18:42", Formato.cuando(instante(2026, Calendar.OCTOBER, 8, 18, 42), ahora))
        assertEquals("ayer 23:59", Formato.cuando(instante(2026, Calendar.OCTOBER, 7, 23, 59), ahora))
        assertEquals("lun 5 · 14:02", Formato.cuando(instante(2026, Calendar.OCTOBER, 5, 14, 2), ahora))
    }

    @Test
    fun sesionesYVideos() {
        assertEquals("sin sesiones de arrastre", Formato.sesionesYVideos(0, 0))
        assertEquals("1 sesión · 1 video", Formato.sesionesYVideos(1, 1))
        assertEquals("3 sesiones · 151 videos", Formato.sesionesYVideos(3, 151))
        assertEquals("solo buscado", Formato.detalleSesion(0L, 0))
        assertEquals("19:45 arrastrado · 71 videos", Formato.detalleSesion((19 * 60 + 45) * 1000L, 71))
    }

    private fun instante(anio: Int, mes: Int, dia: Int, hora: Int, minuto: Int) = Calendar.getInstance().apply {
        clear()
        set(anio, mes, dia, hora, minuto)
    }.timeInMillis
}
