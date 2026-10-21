package com.love.yourself.servicio

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.love.yourself.config.Config
import com.love.yourself.vistas.Colores

/**
 * Las preguntas al cerrar una sesion de Instagram (muestreo de experiencias,
 * como Lukoff et al. 2018): por que entro, que tan significativo fue y cuanto
 * control sintio. Se pueden saltar con "Saltar" o el boton atras.
 *
 * Wireframe: grises de Colores, sin animacion. Los textos estan en Config.
 */
class OverlayPreguntas(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val densidad = context.resources.displayMetrics.density
    private var raiz: View? = null

    fun visible(): Boolean = raiz != null

    /**
     * @param alResponder motivacion (instrumental / habitual / no_se), sentido y control (1 a 7).
     */
    fun mostrar(alResponder: (String, Int, Int) -> Unit, alSaltar: () -> Unit) {
        ocultar()
        var motivacion: String? = null
        var sentido = 0
        var control = 0
        lateinit var listo: TextView

        fun revisar() {
            val completo = motivacion != null && sentido > 0 && control > 0
            listo.isEnabled = completo
            listo.alpha = if (completo) 1f else 0.4f
        }

        val tarjeta = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Colores.FONDO)
            }
        }

        tarjeta.addView(titulo(Config.PREGUNTA_MOTIVACION))
        tarjeta.addView(grupo(Config.OPCIONES_MOTIVACION) { i ->
            motivacion = CODIGOS_MOTIVACION[i]
            revisar()
        }, separado(10))

        tarjeta.addView(titulo(Config.PREGUNTA_SENTIDO), separado(22))
        tarjeta.addView(grupo((1..7).map { it.toString() }) { i ->
            sentido = i + 1
            revisar()
        }, separado(10))
        tarjeta.addView(extremos(), separado(4))

        tarjeta.addView(titulo(Config.PREGUNTA_CONTROL), separado(22))
        tarjeta.addView(grupo((1..7).map { it.toString() }) { i ->
            control = i + 1
            revisar()
        }, separado(10))
        tarjeta.addView(extremos(), separado(4))

        lateinit var contenedor: FrameLayout
        val saltar = boton("Saltar") { ocultar(); alSaltar() }
        listo = boton("Listo") {
            val m = motivacion ?: return@boton
            ocultar()
            alResponder(m, sentido, control)
        }
        val acciones = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(saltar, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(listo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
            })
        }
        tarjeta.addView(acciones, separado(24))
        revisar()

        contenedor = object : FrameLayout(context) {
            override fun dispatchKeyEvent(evento: KeyEvent): Boolean {
                if (evento.keyCode == KeyEvent.KEYCODE_BACK && evento.action == KeyEvent.ACTION_UP) {
                    ocultar()
                    alSaltar()
                    return true
                }
                return super.dispatchKeyEvent(evento)
            }
        }.apply {
            isFocusableInTouchMode = true
            isClickable = true
            setBackgroundColor(Colores.VELO)
        }
        contenedor.addView(tarjeta, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply { setMargins(dp(20), 0, dp(20), 0) })

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

    private fun titulo(contenido: String) = TextView(context).apply {
        text = contenido
        setTextColor(Colores.TEXTO)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
    }

    /** Una fila de opciones; queda marcada la ultima que se toco. */
    private fun grupo(etiquetas: List<String>, alElegir: (Int) -> Unit) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        val opciones = etiquetas.mapIndexed { i, etiqueta ->
            TextView(context).apply {
                text = etiqueta
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (etiquetas.size > 3) 15f else 13f)
                setPadding(dp(4), dp(10), dp(4), dp(10))
                marcar(this, false)
            }
        }
        opciones.forEachIndexed { i, opcion ->
            opcion.setOnClickListener {
                opciones.forEach { marcar(it, it === opcion) }
                alElegir(i)
            }
            addView(opcion, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i > 0) leftMargin = dp(6)
            })
        }
    }

    private fun marcar(opcion: TextView, elegida: Boolean) {
        opcion.setTextColor(if (elegida) Colores.PAGINA else Colores.TEXTO)
        opcion.background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (elegida) Colores.ARRASTRADO else Colores.TARJETA)
        }
    }

    /** "Nada" a la izquierda, "Harto" a la derecha. */
    private fun extremos() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        listOf(Config.ESCALA_MIN to Gravity.START, Config.ESCALA_MAX to Gravity.END).forEach { (etiqueta, lado) ->
            addView(TextView(context).apply {
                text = etiqueta
                gravity = lado
                setTextColor(Colores.TEXTO_SUAVE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun boton(etiqueta: String, alTocar: () -> Unit) = TextView(context).apply {
        text = etiqueta
        gravity = Gravity.CENTER
        setTextColor(Colores.TEXTO)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(dp(8), dp(12), dp(8), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(Colores.SECCION)
        }
        setOnClickListener { alTocar() }
    }

    private fun separado(margen: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(margen) }

    private fun dp(valor: Int) = (valor * densidad).toInt()

    private companion object {
        /** Lo que queda en el registro, en el orden de OPCIONES_MOTIVACION (Lukoff 2018). */
        val CODIGOS_MOTIVACION = listOf("instrumental", "habitual", "no_se")
    }
}
