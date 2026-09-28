package com.love.yourself.lab

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoBriefTest {

    @Test
    fun minutos() {
        assertEquals("Estuviste menos de un minuto", FormatoBrief.estuviste(45_000))
        assertEquals("Estuviste 1 minuto", FormatoBrief.estuviste(90_000))
        assertEquals("Estuviste 22 minutos", FormatoBrief.estuviste(22 * 60_000L + 30_000))
        assertEquals("de ese minuto", FormatoBrief.deEsos(70_000))
        assertEquals("de esos 22 minutos", FormatoBrief.deEsos(22 * 60_000L))
    }

    @Test
    fun relojComoEnElWireframe() {
        assertEquals("19:45", FormatoBrief.reloj((19 * 60 + 45) * 1000L))
        assertEquals("1:15", FormatoBrief.reloj(75_000))
        assertEquals("0:05", FormatoBrief.reloj(5_000))
        assertEquals("1:05:30", FormatoBrief.reloj((3600 + 5 * 60 + 30) * 1000L))
    }

    @Test
    fun semanaComoEnElWireframe() {
        assertEquals("14h 20", FormatoBrief.semana((14 * 60 + 20) * 60_000L))
        assertEquals("3h 40", FormatoBrief.semana((3 * 60 + 40) * 60_000L))
        assertEquals("0h 05", FormatoBrief.semana(5 * 60_000L))
    }
}
