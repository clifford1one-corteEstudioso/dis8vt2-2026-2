package com.love.yourself.servicio

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.love.yourself.vistas.DatosBrief
import com.love.yourself.vistas.Pantalla
import com.love.yourself.vistas.VistaBrief

/**
 * Pone el resumen (VistaBrief) encima de todo.
 *
 * Aparece cuando el usuario abandona la app, no durante el uso. Se cierra con
 * "Cerrar" o con el boton atras, siempre con su animacion de salida. No se
 * cierra solo: es la unica pantalla del sistema que pide ser leida.
 */
class OverlayBrief(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var raiz: View? = null

    fun visible(): Boolean = raiz != null

    /**
     * @param alVerDetalle que hace "ver detalle". Sin esto, se ve apagado.
     * @param alResponder donde se guarda lo que contesto en "recuerdas".
     */
    fun mostrar(datos: DatosBrief, alVerDetalle: (() -> Unit)? = null, alResponder: ((String) -> Unit)? = null) {
        ocultar()
        lateinit var contenedor: FrameLayout
        // Se quita solo si sigue siendo este: si mientras se iba aparecio otro
        // resumen, no hay que llevarselo.
        val vista: Pantalla = VistaBrief(context).crear(
            datos,
            alCerrar = { if (raiz === contenedor) ocultar() },
            alVerDetalle = alVerDetalle,
            alResponder = alResponder
        )

        contenedor = object : FrameLayout(context) {
            override fun dispatchKeyEvent(evento: KeyEvent): Boolean {
                if (evento.keyCode == KeyEvent.KEYCODE_BACK && evento.action == KeyEvent.ACTION_UP) {
                    vista.salir { if (raiz === this) ocultar() }
                    return true
                }
                return super.dispatchKeyEvent(evento)
            }
        }.apply { isFocusableInTouchMode = true }
        contenedor.addView(vista)

        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            tipo,
            // Sin NOT_FOCUSABLE: tiene que recibir el boton atras.
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(contenedor, params)
        contenedor.requestFocus()
        raiz = contenedor
    }

    /** Lo saca al tiro, sin animacion. */
    fun ocultar() {
        raiz?.let { runCatching { wm.removeView(it) } }
        raiz = null
    }
}
