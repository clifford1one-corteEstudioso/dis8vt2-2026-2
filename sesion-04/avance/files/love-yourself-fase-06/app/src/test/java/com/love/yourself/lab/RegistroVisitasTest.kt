package com.love.yourself.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los casos de uso de la sesion 05, escritos como pruebas. Si alguien mueve
 * los numeros de Config.kt, estas pruebas dicen que regla se rompio.
 */
class RegistroVisitasTest {

    private val IG = "com.instagram.android"
    private val WA = "com.whatsapp"
    private val INICIO = "com.sec.android.app.launcher"

    private var t = 1_000_000L
    private val r = RegistroVisitas("com.love.yourself.fase06")

    /** Avanza el reloj de a un segundo, como el tic del servicio. */
    private fun esperar(segundos: Int): List<Cierre> {
        val cierres = mutableListOf<Cierre>()
        repeat(segundos) {
            t += 1000
            cierres += r.tic(t)
        }
        return cierres
    }

    private fun swipe() {
        // Un swipe real dispara varios eventos seguidos mientras se asienta.
        repeat(6) {
            r.scroll(IG, t)
            t += 16
        }
    }

    @Test
    fun entrarSinScrollearNoIniciaSesion() {
        r.enPrimerPlano(IG, false, t)
        esperar(30)
        assertFalse(r.actual!!.sesionIniciada)
        val cierres = r.enPrimerPlano(INICIO, true, t)
        assertEquals(1, cierres.size)
        assertFalse("sin swipes no hay resumen", cierres[0].muestraBrief)
    }

    @Test
    fun variosEventosDeUnMismoSwipeCuentanUnVideo() {
        r.enPrimerPlano(IG, false, t)
        swipe()
        assertEquals(1, r.actual!!.videos)
    }

    @Test
    fun sesionLargaAlInicioMuestraResumen() {
        r.enPrimerPlano(IG, false, t)
        esperar(30) // mira lo que le mandaron: buscado
        repeat(30) {
            swipe()
            esperar(20)
        }
        val cierres = r.enPrimerPlano(INICIO, true, t)
        val c = cierres.single()
        assertEquals(Motivo.INICIO, c.motivo)
        assertTrue(c.muestraBrief)
        assertEquals(30, c.visita.videos)
        // ~30 s buscado antes del primer swipe; el resto arrastrado.
        assertTrue("buscado ${c.visita.buscadoMs}", c.visita.buscadoMs in 29_000L..33_000L)
        assertTrue("arrastrado ${c.visita.arrastradoMs}", c.visita.arrastradoMs in 598_000L..604_000L)
    }

    @Test
    fun pocoArrastreNoMuestraResumen() {
        r.enPrimerPlano(IG, false, t)
        swipe()
        esperar(20)
        val c = r.enPrimerPlano(INICIO, true, t).single()
        assertFalse(c.muestraBrief)
    }

    @Test
    fun bloqueoCortoPausaYSigue() {
        r.enPrimerPlano(IG, false, t)
        swipe()
        esperar(10)
        val antes = r.actual!!.totalMs
        r.pantallaApagada(t)
        t += 2 * 60_000
        assertTrue(r.desbloqueo(t).isEmpty())
        assertEquals("el bloqueo no suma tiempo", antes, r.actual!!.totalMs)
    }

    @Test
    fun bloqueoLargoCierraConResumen() {
        r.enPrimerPlano(IG, false, t)
        repeat(5) {
            swipe()
            esperar(20)
        }
        r.pantallaApagada(t)
        t += 4 * 60_000
        val c = r.desbloqueo(t).single()
        assertEquals(Motivo.BLOQUEO, c.motivo)
        assertTrue(c.muestraBrief)
    }

    @Test
    fun responderWhatsappYVolverEsLaMismaSesion() {
        r.enPrimerPlano(IG, false, t)
        swipe()
        val visita = r.actual
        r.enPrimerPlano(WA, false, t)
        esperar(20)
        r.enPrimerPlano(IG, false, t)
        assertTrue(r.actual === visita)
    }

    @Test
    fun irseAOtraAppMasDeLaToleranciaCierraEnSilencio() {
        r.enPrimerPlano(IG, false, t)
        repeat(5) {
            swipe()
            esperar(20)
        }
        r.enPrimerPlano(WA, false, t)
        val cierres = esperar(Config.TOLERANCIA_REGRESO_S + 2)
        val c = cierres.single { it.visita.app == IG }
        assertEquals(Motivo.OTRA_APP, c.motivo)
        assertFalse("irse a otra app no interrumpe con el resumen", c.muestraBrief)
    }

    @Test
    fun laBarraDeNotificacionesNoCortaLaSesion() {
        r.enPrimerPlano(IG, false, t)
        swipe()
        val visita = r.actual
        r.enPrimerPlano("com.android.systemui", false, t)
        esperar(5)
        assertTrue(r.actual === visita)
        assertFalse(visita!!.afuera)
    }
}
