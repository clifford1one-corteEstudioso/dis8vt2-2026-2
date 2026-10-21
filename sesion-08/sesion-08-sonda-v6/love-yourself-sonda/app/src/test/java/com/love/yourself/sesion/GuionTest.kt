package com.love.yourself.sesion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las dos reglas de cuando habla la mascota. */
class GuionTest {

    private enum class M { A, B, C }
    private val g = Guion<M>(pausaMs = 120_000L)

    @Test
    fun cadaMomentoUnaSolaVez() {
        assertTrue(g.pedir(M.A, 0L))
        assertFalse(g.pedir(M.A, 500_000L))
    }

    @Test
    fun hayPausaEntreMomentos() {
        assertTrue(g.pedir(M.A, 0L))
        assertFalse("muy seguido", g.pedir(M.B, 60_000L))
        assertTrue(g.pedir(M.B, 121_000L))
    }

    @Test
    fun loQueNoSalioPorLaPausaPuedeSalirDespues() {
        assertTrue(g.pedir(M.A, 0L))
        assertFalse(g.pedir(M.B, 10_000L))
        assertTrue("B no quedo marcado como dicho", g.pedir(M.B, 130_000L))
    }

    @Test
    fun unaRespuestaInmediataSeSaltaLaPausa() {
        assertTrue(g.pedir(M.A, 0L))
        assertTrue(g.pedir(M.C, 5_000L, sinPausa = true))
        assertFalse("y cuenta como ultimo", g.pedir(M.B, 100_000L))
    }
}
