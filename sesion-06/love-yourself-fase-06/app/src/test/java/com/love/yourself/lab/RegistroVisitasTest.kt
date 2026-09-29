package com.love.yourself.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los casos de uso de las sesiones 05 y 06, escritos como pruebas. Si alguien
 * mueve los numeros de Config.kt, estas pruebas dicen que regla se rompio.
 */
class RegistroVisitasTest {

    private val IG = "com.instagram.android"
    private val WA = "com.whatsapp"
    private val INICIO = "com.sec.android.app.launcher"

    private var t = 1_000_000L
    private val r = RegistroVisitas("com.love.yourself.fase06")

    private fun abrir(app: String, vigilada: Boolean = true) = r.enPrimerPlano(app, false, vigilada, t)
    private fun alInicio() = r.enPrimerPlano(INICIO, true, false, t)

    /** Avanza el reloj de a un segundo, como el tic del servicio. */
    private fun esperar(segundos: Int): List<Cierre> {
        val cierres = mutableListOf<Cierre>()
        repeat(segundos) {
            t += 1000
            cierres += r.tic(t)
        }
        return cierres
    }

    /** Un swipe real dispara varios eventos seguidos mientras se asienta. */
    private fun swipe(pantallaCompleta: Boolean = true, app: String = IG) {
        repeat(6) {
            r.scroll(app, pantallaCompleta, t)
            t += 16
        }
    }

    // ---- que cuenta ----

    @Test
    fun entrarSinScrollearNoIniciaSesion() {
        abrir(IG)
        esperar(30)
        assertFalse(r.actual!!.sesionIniciada)
        val c = alInicio().single()
        assertFalse("sin swipes no hay resumen", c.muestraBrief)
    }

    @Test
    fun scrollQueNoEsPantallaCompletaNoCuenta() {
        abrir(IG)
        repeat(10) {
            swipe(pantallaCompleta = false) // mensajes, busqueda
            esperar(10)
        }
        assertFalse(r.actual!!.sesionIniciada)
        assertEquals(0, r.actual!!.videos)
    }

    @Test
    fun appNoVigiladaNoAbreVisita() {
        abrir(WA, vigilada = false)
        swipe(app = WA)
        assertNull(r.actual)
    }

    @Test
    fun variosEventosDeUnMismoSwipeCuentanUnVideo() {
        abrir(IG)
        swipe()
        assertEquals(1, r.actual!!.videos)
    }

    @Test
    fun relojAceleradoNoMultiplicaLosSwipes() {
        r.factorReloj = 60
        abrir(IG)
        // Mismo gesto, pero cada evento "dura" 60 veces mas en el reloj acelerado.
        repeat(6) {
            r.scroll(IG, true, t)
            t += 16 * 60
        }
        assertEquals(1, r.actual!!.videos)
    }

    @Test
    fun relojAceleradoMideLasPausasEnSegundosReales() {
        r.factorReloj = 10
        abrir(IG)
        swipe()
        // 60 s reales mirando el mismo reel = 600 s en el reloj: sigue siendo arrastre.
        esperar(600)
        assertTrue(r.actual!!.arrastradoMs >= 595_000L)
        assertEquals(EstadoMascota.DETENIDA, r.mascota())
        // Pasados los 90 s reales sin otro swipe, ya no.
        esperar(400)
        assertEquals(EstadoMascota.TRANQUILA, r.mascota())
    }

    // ---- la mascota ----

    @Test
    fun mascotaOcultaFueraDeAppsVigiladas() {
        assertEquals(EstadoMascota.OCULTA, r.mascota())
        abrir(WA, vigilada = false)
        assertEquals(EstadoMascota.OCULTA, r.mascota())
    }

    @Test
    fun mascotaTranquilaAlEntrarSinScroll() {
        abrir(IG)
        esperar(20)
        assertEquals(EstadoMascota.TRANQUILA, r.mascota())
    }

    @Test
    fun mascotaAlertaConElPrimerSwipe() {
        abrir(IG)
        swipe()
        assertEquals(EstadoMascota.ALERTA, r.mascota())
    }

    @Test
    fun mascotaSeDetieneTrasUnMinutoDeArrastre() {
        abrir(IG)
        repeat(4) {
            swipe()
            esperar(10)
        }
        assertEquals("40 s: todavia alerta", EstadoMascota.ALERTA, r.mascota())
        repeat(3) {
            swipe()
            esperar(10)
        }
        assertEquals("70 s: se detuvo", EstadoMascota.DETENIDA, r.mascota())
    }

    @Test
    fun mascotaVuelveALoSuyoSiElArrastreSeCorta() {
        abrir(IG)
        swipe()
        esperar(Config.VENTANA_ARRASTRE_S + 2)
        assertEquals(EstadoMascota.TRANQUILA, r.mascota())
    }

    @Test
    fun buscarEnMensajesNoDespiertaALaMascota() {
        abrir(IG)
        repeat(5) {
            swipe(pantallaCompleta = false)
            esperar(15)
        }
        assertEquals(EstadoMascota.TRANQUILA, r.mascota())
    }

    // ---- cierres ----

    @Test
    fun sesionLargaAlInicioMuestraResumen() {
        abrir(IG)
        esperar(30) // mira lo que le mandaron: buscado
        repeat(30) {
            swipe()
            esperar(20)
        }
        val c = alInicio().single()
        assertEquals(Motivo.INICIO, c.motivo)
        assertTrue(c.muestraBrief)
        assertEquals(30, c.visita.videos)
        assertTrue("buscado ${c.visita.buscadoMs}", c.visita.buscadoMs in 29_000L..33_000L)
        assertTrue("arrastrado ${c.visita.arrastradoMs}", c.visita.arrastradoMs in 598_000L..604_000L)
    }

    @Test
    fun pocoArrastreNoMuestraResumen() {
        abrir(IG)
        swipe()
        esperar(20)
        assertFalse(alInicio().single().muestraBrief)
    }

    @Test
    fun bloqueoCortoPausaYSigue() {
        abrir(IG)
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
        abrir(IG)
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
        abrir(IG)
        swipe()
        val visita = r.actual
        abrir(WA, vigilada = false)
        esperar(20)
        abrir(IG)
        assertTrue(r.actual === visita)
    }

    @Test
    fun otraAppQueAvisaVariasVecesNoCortaLaTolerancia() {
        abrir(IG)
        swipe()
        esperar(10)
        abrir(WA, vigilada = false)
        esperar(5)
        // WhatsApp abre un chat: otro aviso de la misma app.
        assertTrue(abrir(WA, vigilada = false).isEmpty())
        esperar(5)
        assertTrue(abrir(IG).isEmpty())
        assertTrue(r.actual!!.sesionIniciada)
    }

    @Test
    fun irseAOtraAppMasDeLaToleranciaCierraEnSilencio() {
        abrir(IG)
        repeat(5) {
            swipe()
            esperar(20)
        }
        abrir(WA, vigilada = false)
        val c = esperar(Config.TOLERANCIA_REGRESO_S + 2).single { it.visita.app == IG }
        assertEquals(Motivo.OTRA_APP, c.motivo)
        assertFalse("irse a otra app no interrumpe con el resumen", c.muestraBrief)
    }

    @Test
    fun laBarraDeNotificacionesNoCortaLaSesion() {
        abrir(IG)
        swipe()
        val visita = r.actual
        r.enPrimerPlano("com.android.systemui", false, false, t)
        esperar(5)
        assertTrue(r.actual === visita)
        assertFalse(visita!!.afuera)
    }
}
