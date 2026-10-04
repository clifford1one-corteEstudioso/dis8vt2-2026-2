package com.love.yourself.vistas

import com.love.yourself.R
import com.love.yourself.config.Config
import org.junit.Assert.assertEquals
import org.junit.Test

class MascotaTest {

    private val min = 60_000L

    @Test
    fun laPildoraSeGastaConElTiempoEnLaApp() {
        assertEquals(R.drawable.brain_relief, CaraProgresiva.para(false, 30 * min))
        assertEquals(R.drawable.brain_sus, CaraProgresiva.para(true, 0))
        assertEquals(R.drawable.brain_drowzy, CaraProgresiva.para(true, 6 * min))
        assertEquals(R.drawable.brain_f, CaraProgresiva.para(true, Config.MIN_CIELO * min))
    }

    @Test
    fun laCaraDelDiaSumaTodoElArrastre() {
        assertEquals(R.drawable.brain_relief, CaraDelDia.para(0))
        assertEquals("hoy estoy entero", CaraDelDia.frase(0))
        assertEquals(R.drawable.brain_sus, CaraDelDia.para(14 * min))
        assertEquals(R.drawable.brain_pfff, CaraDelDia.para(15 * min))
        assertEquals(R.drawable.brain_f, CaraDelDia.para(3 * 60 * min))
        assertEquals("hoy quedé frito", CaraDelDia.frase(3 * 60 * min))
    }

    @Test
    fun cadaMomentoTieneVariasFrases() {
        for (m in Momento.values()) assertEquals("${m.name} con 3 frases", 3, m.frases.size)
    }
}
