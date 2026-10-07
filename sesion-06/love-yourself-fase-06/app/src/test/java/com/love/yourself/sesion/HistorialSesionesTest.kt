package com.love.yourself.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorialSesionesTest {

    private fun registro(finMs: Long, videos: Int = 71, recuerdas: String = "") = RegistroSesion(
        finMs = finMs,
        app = "com.instagram.android",
        totalMs = 22 * 60_000L,
        arrastradoMs = (19 * 60 + 45) * 1000L,
        videos = videos,
        motivo = Motivo.SALIR,
        cielos = 1,
        seguir = 0,
        recuerdas = recuerdas
    )

    @Test
    fun seGuardaYSeLeeIgual() {
        val r = registro(1_000L, recuerdas = "1–3")
        assertEquals(r, RegistroSesion.deLinea(r.aLinea()))
    }

    @Test
    fun loQueRespondeNoRompeLaLinea() {
        val r = registro(1_000L, recuerdas = "uno\tdos\ntres")
        assertEquals("uno dos tres", RegistroSesion.deLinea(r.aLinea())!!.recuerdas)
    }

    @Test
    fun unaLineaRotaSeSaltaSinRomperElResto() {
        assertNull(RegistroSesion.deLinea("basura"))
        assertNull(RegistroSesion.deLinea("9\t1\tapp\t1\t1\t1\tSALIR\t0\t0\t"))      // otra version
        assertNull(RegistroSesion.deLinea("1\tx\tapp\t1\t1\t1\tSALIR\t0\t0\t"))      // numero roto
        val texto = registro(2_000L).aLinea() + "\nbasura\n" + registro(1_000L).aLinea()
        assertEquals(listOf(2_000L, 1_000L), HistorialSesiones.leer(texto).map { it.finMs })
    }

    @Test
    fun laMasNuevaVaPrimero() {
        var texto = ""
        texto = HistorialSesiones.conAgregada(texto, registro(1_000L))
        texto = HistorialSesiones.conAgregada(texto, registro(2_000L))
        assertEquals(listOf(2_000L, 1_000L), HistorialSesiones.leer(texto).map { it.finMs })
    }

    @Test
    fun seGuardaUnMaximo() {
        var texto = ""
        repeat(HistorialSesiones.MAX + 5) { texto = HistorialSesiones.conAgregada(texto, registro(it.toLong())) }
        val leidas = HistorialSesiones.leer(texto)
        assertEquals(HistorialSesiones.MAX, leidas.size)
        assertEquals((HistorialSesiones.MAX + 4).toLong(), leidas.first().finMs)
    }

    @Test
    fun responderSoloCambiaEsaSesion() {
        var texto = HistorialSesiones.conAgregada("", registro(1_000L))
        texto = HistorialSesiones.conAgregada(texto, registro(2_000L))
        texto = HistorialSesiones.conRespuesta(texto, 1_000L, "0")
        val leidas = HistorialSesiones.leer(texto)
        assertEquals("", leidas[0].recuerdas)
        assertEquals("0", leidas[1].recuerdas)
    }

    @Test
    fun sinArrastreEsUsoConIntencion() {
        assertTrue(registro(1_000L, videos = 3).fueSesion)
        assertFalse(registro(1_000L, videos = 0).fueSesion)
    }
}
