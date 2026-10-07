package com.love.yourself.sesion

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SemanaTest {

    private fun dia(anio: Int, mes: Int, dia: Int, hora: Int = 12) = Calendar.getInstance().apply {
        clear()
        set(anio, mes, dia, hora, 0)
    }.timeInMillis

    @Test
    fun laSemanaParteElLunes() {
        assertEquals(0, Semana.indice(dia(2026, Calendar.OCTOBER, 5)))   // lunes
        assertEquals(3, Semana.indice(dia(2026, Calendar.OCTOBER, 8)))   // jueves
        assertEquals(6, Semana.indice(dia(2026, Calendar.OCTOBER, 4)))   // domingo
    }

    @Test
    fun losSieteDiasDeLaSemana() {
        assertEquals(
            listOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09", "2026-10-10", "2026-10-11"),
            Semana.dias(dia(2026, Calendar.OCTOBER, 8))
        )
        // El domingo todavia es de la semana que partio el lunes anterior.
        assertEquals("2026-09-28", Semana.dias(dia(2026, Calendar.OCTOBER, 4)).first())
    }

    @Test
    fun cruzaElAnio() {
        assertEquals(
            listOf("2026-12-28", "2026-12-29", "2026-12-30", "2026-12-31", "2027-01-01", "2027-01-02", "2027-01-03"),
            Semana.dias(dia(2027, Calendar.JANUARY, 1))
        )
    }

    @Test
    fun laSemanaPasadaSeComparaALaMismaAltura() {
        // Un jueves contra lunes a jueves de la semana anterior.
        assertEquals(
            listOf("2026-09-28", "2026-09-29", "2026-09-30", "2026-10-01"),
            Semana.mismosDiasSemanaPasada(dia(2026, Calendar.OCTOBER, 8))
        )
        // Un lunes, solo contra el lunes anterior.
        assertEquals(listOf("2026-09-28"), Semana.mismosDiasSemanaPasada(dia(2026, Calendar.OCTOBER, 5)))
    }

    @Test
    fun laFechaEsLaDelDiaLocal() {
        assertEquals("2026-10-08", Semana.fecha(dia(2026, Calendar.OCTOBER, 8, hora = 0)))
        assertEquals("2026-10-08", Semana.fecha(dia(2026, Calendar.OCTOBER, 8, hora = 23)))
    }
}
