package com.love.yourself.vistas

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Inicio durante la linea base: solo dice en que dia va el estudio. Sin
 * tiempos ni sesiones, porque verlos ya cambiaria como se usa Instagram y la
 * linea base dejaria de ser "sin intervencion".
 */
class VistaEnCurso(private val context: Context) {

    private val densidad = context.resources.displayMetrics.density

    fun crear(dia: Int, total: Int, alAjustes: () -> Unit): View {
        val columna = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)
            addView(texto("Estudio en curso", 20f, Colores.TEXTO_SUAVE))
            addView(texto("Día $dia de $total", 44f, Colores.TEXTO).apply {
                setTypeface(typeface, Typeface.BOLD)
            }, margenArriba(dp(10)))
            addView(texto(
                "Por ahora la app solo registra. Usa Instagram como siempre: no tienes que hacer nada más.",
                17f, Colores.TEXTO_SUAVE
            ), margenArriba(dp(22)))
        }
        val ajustes = texto("Ajustes", 17f, Colores.TEXTO).apply {
            setPadding(dp(24), dp(16), dp(24), dp(16))
            Animacion.presionable(this)
            setOnClickListener { alAjustes() }
        }
        return FrameLayout(context).apply {
            setBackgroundColor(Colores.PAGINA)
            addView(columna, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            ))
            addView(ajustes, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = dp(24) })
        }
    }

    private fun texto(contenido: String, sp: Float, color: Int) = TextView(context).apply {
        text = contenido
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        gravity = Gravity.CENTER
    }

    private fun margenArriba(m: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = m }

    private fun dp(v: Int) = (v * densidad).toInt()
}
