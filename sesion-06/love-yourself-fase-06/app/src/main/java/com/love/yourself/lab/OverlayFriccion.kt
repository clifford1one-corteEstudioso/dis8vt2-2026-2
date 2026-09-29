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
 * cada cosa esta en BurbujaView.kt, VistasFriccion.kt y CieloView.kt.
 *
 *   BURBUJA    arriba, siempre: tiempo y ritmo. Se agranda con la mascota en ciertos momentos
 *   CIELO      se va haciendo visible a medida que pasa el tiempo
 *   DECISION   pantalla completa, obliga a elegir
 *   CAJA       solo en modo dev: datos para calibrar
 *
 * Ninguna bloquea la app de abajo.
 */
class OverlayFriccion(private val context: Context) {

    /**
     * El orden de las capas, de abajo hacia arriba. Android pone encima la
     * ultima ventana agregada, asi que agregar una capa obliga a volver a subir
     * las que van sobre ella. Si no, el cielo taparia la burbuja.
     */
    private enum class Capa { CIELO, CAJA, BURBUJA, DECISION }

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

    // ---- caja de datos (modo dev) ----

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
                // Debajo de la burbuja expandida.
                y = dp(BurbujaView.MARGEN_SUPERIOR_DP.toInt() + 96)
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

    // ---- burbuja ----

    private fun burbuja(): BurbujaView {
        if (Capa.BURBUJA !in capas) {
            val p = paramsAtravesables(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
            agregar(Capa.BURBUJA, BurbujaView(context), p)
        }
        return capas.getValue(Capa.BURBUJA).first as BurbujaView
    }

    fun mostrarBurbuja(tiempo: String, ritmo: String) = burbuja().mostrarDatos(tiempo, ritmo)

    fun expandirBurbuja(momento: Momento) = burbuja().expandir(momento)

    fun quitarBurbuja() = quitar(Capa.BURBUJA)

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
