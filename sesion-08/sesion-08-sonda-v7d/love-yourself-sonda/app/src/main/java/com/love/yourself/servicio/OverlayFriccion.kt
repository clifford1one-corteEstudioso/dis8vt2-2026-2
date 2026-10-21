package com.love.yourself.servicio

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.love.yourself.vistas.AvisoNotch
import com.love.yourself.vistas.BurbujaView
import com.love.yourself.vistas.CajaDev
import com.love.yourself.vistas.MascotaAsomada
import com.love.yourself.vistas.Momento
import com.love.yourself.vistas.VistaCielo

/**
 * Pone la friccion encima de las otras apps. Solo maneja ventanas: como se ve
 * cada cosa esta en vistas/ (BurbujaView, VistaCielo, CajaDev).
 *
 *   CAJA       solo en modo dev: datos para calibrar
 *   BURBUJA    arriba, siempre: tiempo y ritmo. Se agranda con la mascota en ciertos momentos
 *   AVISO      al tercer swipe de arrastre: deslizar = salir, quedarse 5 s = seguir
 *   ASOMO      la mascota grande asomandose por un costado. Recibe toques (solo ella)
 *   CIELO      a los MIN_CIELO minutos, pantalla completa: seguir o salir
 *
 * Solo el cielo recibe toques; lo demas deja usar la app de abajo.
 */
class OverlayFriccion(private val context: Context) {

    /**
     * El orden de las capas, de abajo hacia arriba. Android pone encima la
     * ultima ventana agregada, asi que agregar una capa obliga a volver a subir
     * las que van sobre ella. Si no, la caja dev taparia la burbuja.
     */
    private enum class Capa { CAJA, BURBUJA, AVISO, ASOMO, CIELO }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val densidad = context.resources.displayMetrics.density
    private val capas = sortedMapOf<Capa, Pair<View, WindowManager.LayoutParams>>()

    private val tipoVentana = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    /** Ventana que deja pasar los toques: la app de abajo se sigue usando igual. */
    private fun paramsAtravesables() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
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

    // ---- caja de datos (modo dev) ----

    fun mostrarCaja(texto: String) {
        if (Capa.CAJA !in capas) {
            val p = paramsAtravesables().apply {
                gravity = CajaDev.GRAVEDAD
                y = (CajaDev.MARGEN_SUPERIOR_DP * densidad).toInt()
            }
            agregar(Capa.CAJA, CajaDev.crear(context), p)
        }
        val v = capas.getValue(Capa.CAJA).first as TextView
        if (v.text.toString() != texto) v.text = texto
    }

    fun quitarCaja() = quitar(Capa.CAJA)

    // ---- burbuja ----

    private fun burbuja(): BurbujaView {
        if (Capa.BURBUJA !in capas) {
            val p = paramsAtravesables().apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
            agregar(Capa.BURBUJA, BurbujaView(context), p)
        }
        return capas.getValue(Capa.BURBUJA).first as BurbujaView
    }

    fun quitarBurbuja() = quitar(Capa.BURBUJA)

    fun mostrarBurbuja(tiempo: String, ritmo: String, cara: Int) = burbuja().mostrarDatos(tiempo, ritmo, cara)

    fun expandirBurbuja(momento: Momento) = burbuja().expandir(momento)

    /** Si la burbuja esta agrandada, hablando. */
    fun burbujaExpandida(): Boolean = (capas[Capa.BURBUJA]?.first as? BurbujaView)?.expandida() == true

    // ---- aviso ----

    fun avisoVisible(): Boolean = Capa.AVISO in capas

    /** Muestra el aviso o actualiza su cuenta regresiva. Deja pasar los toques. */
    fun mostrarAviso(segundos: Int) {
        if (Capa.AVISO !in capas) {
            val p = paramsAtravesables().apply {
                gravity = AvisoNotch.GRAVEDAD
                y = (AvisoNotch.MARGEN_SUPERIOR_DP * densidad).toInt()
            }
            agregar(Capa.AVISO, AvisoNotch.crear(context), p)
        }
        val v = capas.getValue(Capa.AVISO).first as TextView
        v.text = AvisoNotch.texto(segundos)
    }

    fun quitarAviso() = quitar(Capa.AVISO)

    // ---- la mascota asomandose ----

    fun mascotaAsomada(): Boolean = Capa.ASOMO in capas

    /**
     * La mascota grande por un costado, a altura yFraccion del alto de la
     * pantalla. La ventana mide solo lo que se ve de ella: el resto de la
     * pantalla sigue siendo Instagram.
     */
    fun asomarMascota(
        cara: Int,
        caraFinal: Int?,
        derecha: Boolean,
        yFraccion: Float,
        entradaMs: Long,
        quedaMs: Long,
        alTocar: () -> Unit
    ) {
        quitar(Capa.ASOMO)
        val vista = MascotaAsomada(context, cara, caraFinal, derecha).apply { this.alTocar = alTocar }
        val params = WindowManager.LayoutParams(
            vista.anchoVentana,
            vista.altoVentana,
            tipoVentana,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or if (derecha) Gravity.END else Gravity.START
            y = (context.resources.displayMetrics.heightPixels * yFraccion).toInt() - vista.altoVentana / 2
        }
        agregar(Capa.ASOMO, vista, params)
        vista.post {
            vista.asomar(entradaMs, quedaMs) {
                // Solo si sigue siendo esta: si mientras tanto aparecio otra, no se la lleva.
                if (capas[Capa.ASOMO]?.first === vista) quitar(Capa.ASOMO)
            }
        }
    }

    fun quitarMascota() = quitar(Capa.ASOMO)

    // ---- cielo ----

    fun cieloVisible(): Boolean = Capa.CIELO in capas

    /**
     * El cielo a pantalla completa. Recibe toques: hay que poder elegir.
     * Sigue en pantalla mientras se va (al elegir "Seguir" sube antes de
     * desaparecer); los avisos llegan cuando ya se fue.
     */
    fun mostrarCielo(minutos: Long, cara: Int, alSeguir: () -> Unit, alSalir: () -> Unit) {
        if (cieloVisible()) return
        val vista = VistaCielo(context).crear(
            minutos,
            cara,
            alSeguir = {
                quitar(Capa.CIELO)
                alSeguir()
            },
            alSalir = {
                quitar(Capa.CIELO)
                alSalir()
            }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            tipoVentana,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        agregar(Capa.CIELO, vista, params)
    }

    fun ocultarTodo() {
        Capa.values().forEach { quitar(it) }
    }
}
