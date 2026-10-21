package com.love.yourself.vistas

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.love.yourself.config.Config

/**
 * El aviso tipo notch: al tercer swipe de arrastre, una pildora negra justo
 * debajo de la burbuja, con su mismo estilo (fondo negro, letra blanca,
 * esquinas de 16 dp). Pregunta si salir y ofrece un boton: salir es una
 * eleccion explicita, como la opcion de desistir de one sec. Si se sigue
 * deslizando, desaparece. Solo arma la vista.
 */
object AvisoNotch {

    const val GRAVEDAD = Gravity.TOP or Gravity.CENTER_HORIZONTAL

    /** Justo debajo de la burbuja recogida, como si colgara de ella. */
    const val MARGEN_SUPERIOR_DP = BurbujaView.MARGEN_SUPERIOR_DP + 32f + 8f

    /** @param alSalir se llama al tocar "Salir". */
    fun crear(context: Context, alSalir: () -> Unit): View {
        val d = context.resources.displayMetrics.density
        fun dp(v: Float) = (v * d).toInt()

        val textos = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = Config.TEXTO_AVISO
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = Config.TEXTO_AVISO_PIE
                setTextColor(Colores.TEXTO_SUAVE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })
        }
        val salir = TextView(context).apply {
            text = Config.TEXTO_BOTON_AVISO
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(Color.WHITE)
            }
            Animacion.presionable(this)
            setOnClickListener { alSalir() }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18f), dp(10f), dp(10f), dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = dp(16f).toFloat()
                setColor(Color.BLACK)
            }
            addView(textos)
            addView(salir, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(16f) })
        }
    }
}
