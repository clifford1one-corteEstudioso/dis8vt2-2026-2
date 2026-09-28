package com.love.yourself.lab

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Pone el resumen (VistaBrief) encima de todo.
 *
 * Aparece cuando el usuario abandona la app, no durante el uso. Se cierra con
 * "Cerrar" o con el boton atras. No se cierra solo: es la unica pantalla del
 * sistema que pide ser leida.
 */
class OverlayBrief(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var raiz: View? = null

    fun visible(): Boolean = raiz != null

    fun mostrar(datos: DatosBrief) {
        ocultar()
        val vista = VistaBrief(context).crear(datos, alCerrar = { ocultar() })

        val contenedor = object : FrameLayout(context) {
            override fun dispatchKeyEvent(evento: KeyEvent): Boolean {
                if (evento.keyCode == KeyEvent.KEYCODE_BACK && evento.action == KeyEvent.ACTION_UP) {
                    ocultar()
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

    fun ocultar() {
        raiz?.let { runCatching { wm.removeView(it) } }
        raiz = null
    }
}
