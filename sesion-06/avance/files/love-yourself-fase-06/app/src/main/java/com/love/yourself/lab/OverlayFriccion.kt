package com.love.yourself.lab

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Pone la friccion encima de las otras apps. Solo maneja ventanas: como se ve
 * cada cosa esta en VistasFriccion.kt, CieloView.kt y MascotaView.kt.
 *
 *   MASCOTA    lee mientras la app se usa con intencion; deja el libro con el arrastre
 *   ESPEJO     caja discreta, informa
 *   PRESENCIA  caja mas grande, cuesta ignorarla
 *   CIELO      se va haciendo visible a medida que pasa el tiempo
 *   DECISION   pantalla completa, obliga a elegir
 *
 * Ninguna bloquea la app de abajo.
 */
class OverlayFriccion(private val context: Context) {

    /**
     * El orden de las capas, de abajo hacia arriba. Android pone encima la
     * ultima ventana agregada, asi que agregar una capa obliga a volver a subir
     * las que van sobre ella. Si no, el cielo taparia a la mascota.
     */
    private enum class Capa { CIELO, CAJA, MASCOTA, DECISION }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val densidad = context.resources.displayMetrics.density
    private val capas = sortedMapOf<Capa, Pair<View, WindowManager.LayoutParams>>()

    private var etapaActual = EtapaCaja.OCULTO

    private val tipoVentana = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    /** Ventana que deja pasar los toques: la app de abajo se sigue usando igual. */
    private fun paramsAtravesables(ancho: Int, alto: Int) = WindowManager.LayoutParams(
        ancho,
        alto,
        tipoVentana,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    private fun agregar(capa: Capa, vista: View, params: WindowManager.LayoutParams) {
        val encima = capas.tailMap(capa).filterKeys { it != capa }
        encima.values.forEach { (v, _) -> runCatching { wm.removeView(v) } }
        wm.addView(vista, params)
        capas[capa] = vista to params
        encima.values.forEach { (v, p) -> wm.addView(v, p) }
    }

    private fun quitar(capa: Capa) {
        val (v, _) = capas.remove(capa) ?: return
        runCatching { wm.removeView(v) }
    }

    private fun dp(valor: Int) = (valor * densidad).toInt()

    // ---- cielo ----

    fun mostrarCielo(opacidad: Float) {
        if (Capa.CIELO !in capas) {
            val p = paramsAtravesables(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            ).apply { alpha = 0f }
            agregar(Capa.CIELO, CieloView(context), p)
        }
        val (v, p) = capas.getValue(Capa.CIELO)
        val nueva = opacidad.coerceIn(0f, Config.OPACIDAD_MAX_CIELO)
        if (nueva == p.alpha) return
        p.alpha = nueva
        wm.updateViewLayout(v, p)
    }

    fun quitarCielo() = quitar(Capa.CIELO)

    // ---- caja de datos ----

    fun mostrarCaja(etapa: EtapaCaja, texto: String) {
        if (etapa == EtapaCaja.OCULTO) {
            quitarCaja()
            return
        }
        if (Capa.CAJA !in capas) {
            val p = paramsAtravesables(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = VistasFriccion.GRAVEDAD_CAJA
                y = VistasFriccion.MARGEN_SUPERIOR_CAJA_PX
            }
            agregar(Capa.CAJA, VistasFriccion.caja(context), p)
            etapaActual = EtapaCaja.ESPEJO
        }
        val v = capas.getValue(Capa.CAJA).first as TextView
        if (etapa != etapaActual) {
            VistasFriccion.aplicarEtapa(v, etapa)
            etapaActual = etapa
        }
        if (v.text.toString() != texto) v.text = texto
    }

    private fun quitarCaja() {
        quitar(Capa.CAJA)
        etapaActual = EtapaCaja.OCULTO
    }

    // ---- mascota ----

    fun mostrarMascota(estado: EstadoMascota) {
        if (estado == EstadoMascota.OCULTA) {
            quitar(Capa.MASCOTA)
            return
        }
        if (Capa.MASCOTA !in capas) {
            val lado = dp(VistasFriccion.MASCOTA_LADO_DP)
            val p = paramsAtravesables(lado, lado).apply {
                gravity = VistasFriccion.GRAVEDAD_MASCOTA
                x = dp(VistasFriccion.MASCOTA_MARGEN_X_DP)
                y = dp(VistasFriccion.MASCOTA_MARGEN_Y_DP)
            }
            agregar(Capa.MASCOTA, MascotaView(context), p)
        }
        (capas.getValue(Capa.MASCOTA).first as MascotaView).estado = estado
    }

    // ---- momento de decision ----

    fun decisionVisible(): Boolean = Capa.DECISION in capas

    fun mostrarDecision(minutos: Long, alSeguir: () -> Unit, alSalir: () -> Unit) {
        if (decisionVisible()) return
        val vista = VistasFriccion.decision(
            context,
            minutos,
            alSeguir = {
                quitar(Capa.DECISION)
                alSeguir()
            },
            alSalir = {
                quitar(Capa.DECISION)
                alSalir()
            }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            tipoVentana,
            // Esta si recibe toques: hay que poder elegir.
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        agregar(Capa.DECISION, vista, params)
    }

    fun ocultarTodo() {
        Capa.values().forEach { quitar(it) }
        etapaActual = EtapaCaja.OCULTO
    }
}
