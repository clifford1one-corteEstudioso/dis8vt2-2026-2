package com.love.yourself.lab

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Pone la friccion encima de las otras apps. Solo maneja ventanas: como se ve
 * cada cosa esta en VistasFriccion.kt y CieloView.kt.
 *
 *   ESPEJO     caja discreta, informa
 *   PRESENCIA  caja mas grande, cuesta ignorarla
 *   CIELO      se va haciendo visible a medida que pasa el tiempo
 *   DECISION   pantalla completa, obliga a elegir
 *
 * Ninguna bloquea Instagram.
 */
class OverlayFriccion(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var caja: TextView? = null
    private var etapaActual = EtapaCaja.OCULTO
    private var pantallaDecision: View? = null
    private var cielo: CieloView? = null
    private var paramsCielo: WindowManager.LayoutParams? = null

    private val tipoVentana = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    /** Ventana que deja pasar los toques: Instagram se sigue usando igual. */
    private fun paramsAtravesables(ancho: Int, alto: Int) = WindowManager.LayoutParams(
        ancho,
        alto,
        tipoVentana,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    // ---- cielo ----

    /**
     * Hay que llamarla antes que mostrarCaja: la ventana que se agrega despues
     * queda encima, y el cielo taparia la caja.
     */
    fun mostrarCielo(opacidad: Float) {
        val p = paramsCielo ?: paramsAtravesables(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        ).also { nuevos ->
            nuevos.alpha = 0f
            val v = CieloView(context)
            wm.addView(v, nuevos)
            cielo = v
            paramsCielo = nuevos
        }
        val v = cielo ?: return
        p.alpha = opacidad.coerceIn(0f, Config.OPACIDAD_MAX_CIELO)
        wm.updateViewLayout(v, p)
    }

    private fun quitarCielo() {
        cielo?.let { runCatching { wm.removeView(it) } }
        cielo = null
        paramsCielo = null
    }

    // ---- caja de datos ----

    fun mostrarCaja(etapa: EtapaCaja, segundos: Long, swipesPorMinuto: Double) {
        if (etapa == EtapaCaja.OCULTO) {
            quitarCielo()
            quitarCaja()
            return
        }
        val v = caja ?: crearCaja().also { caja = it }
        if (etapa != etapaActual) {
            VistasFriccion.aplicarEtapa(v, etapa)
            etapaActual = etapa
        }
        v.text = VistasFriccion.texto(segundos, swipesPorMinuto)
    }

    private fun crearCaja(): TextView {
        val v = VistasFriccion.caja(context)
        val params = paramsAtravesables(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = VistasFriccion.MARGEN_SUPERIOR_CAJA_PX
        }
        wm.addView(v, params)
        etapaActual = EtapaCaja.ESPEJO
        return v
    }

    private fun quitarCaja() {
        caja?.let { runCatching { wm.removeView(it) } }
        caja = null
        etapaActual = EtapaCaja.OCULTO
    }

    // ---- momento de decision ----

    fun decisionVisible(): Boolean = pantallaDecision != null

    fun mostrarDecision(minutos: Long, alSeguir: () -> Unit, alSalir: () -> Unit) {
        if (pantallaDecision != null) return
        val vista = VistasFriccion.decision(
            context,
            minutos,
            alSeguir = {
                quitarDecision()
                alSeguir()
            },
            alSalir = {
                quitarDecision()
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
        wm.addView(vista, params)
        pantallaDecision = vista
    }

    private fun quitarDecision() {
        pantallaDecision?.let { runCatching { wm.removeView(it) } }
        pantallaDecision = null
    }

    fun ocultarTodo() {
        quitarCaja()
        quitarCielo()
        quitarDecision()
    }
}
