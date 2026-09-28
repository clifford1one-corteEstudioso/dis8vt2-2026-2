package com.love.yourself

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.love.yourself.lab.CieloView
import com.love.yourself.lab.Config
import com.love.yourself.lab.EtapaCaja
import com.love.yourself.lab.VistasFriccion

/**
 * App solo de diseno. Muestra las mismas vistas que la app real, pero dentro
 * de una pantalla normal: sin permisos, sin accesibilidad, sin Instagram.
 *
 * Se mueve el tiempo de sesion con un deslizador y se ve como reaccionan la
 * caja y el cielo. Lo que se ajuste en VistasFriccion.kt o CieloView.kt se
 * copia entero a la app real.
 *
 * Capas, de abajo hacia arriba, en el mismo orden que en el celular:
 *   reel de mentira -> cielo -> caja -> decision -> panel de control
 */
class MainActivity : Activity() {

    private lateinit var cielo: CieloView
    private lateinit var caja: TextView
    private lateinit var raiz: FrameLayout
    private lateinit var panel: LinearLayout
    private lateinit var estado: TextView

    private var segundos = 0L
    private var swipesPorMinuto = 12.0
    private var etapaActual = EtapaCaja.OCULTO
    private var decision: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dibujarDetrasDeLaBarraDeEstado()

        raiz = FrameLayout(this)
        raiz.addView(FondoReel(this), completo())

        cielo = CieloView(this).apply { alpha = 0f }
        raiz.addView(cielo, completo())

        // Misma posicion que en el celular: arriba al centro, a 120 px del borde.
        caja = VistasFriccion.caja(this)
        raiz.addView(
            caja,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = VistasFriccion.MARGEN_SUPERIOR_CAJA_PX }
        )

        panel = panelDeControl()
        raiz.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )
        raiz.addView(botonPanel(), FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(36)
            marginEnd = dp(12)
        })

        setContentView(raiz)
        actualizar()
    }

    /** Aplica el estado actual a la caja y al cielo, con las mismas reglas del servicio. */
    private fun actualizar() {
        val minutos = segundos / 60
        val etapa = when {
            segundos == 0L -> EtapaCaja.OCULTO
            minutos >= Config.MIN_PRESENCIA -> EtapaCaja.PRESENCIA
            else -> EtapaCaja.ESPEJO
        }

        if (etapa == EtapaCaja.OCULTO) {
            caja.visibility = View.GONE
            cielo.alpha = 0f
        } else {
            caja.visibility = View.VISIBLE
            if (etapa != etapaActual) VistasFriccion.aplicarEtapa(caja, etapa)
            caja.text = VistasFriccion.texto(segundos, swipesPorMinuto)
            val progreso = segundos / (Config.MIN_DECISION * 60f)
            cielo.alpha = (progreso * Config.OPACIDAD_MAX_CIELO).coerceIn(0f, Config.OPACIDAD_MAX_CIELO)
        }
        etapaActual = etapa

        estado.text = when (etapa) {
            EtapaCaja.OCULTO -> "antes del primer swipe"
            EtapaCaja.ESPEJO -> "espejo · cielo %.2f".format(cielo.alpha)
            EtapaCaja.PRESENCIA -> "presencia · cielo %.2f".format(cielo.alpha)
        }
    }

    private fun mostrarDecision() {
        if (decision != null) return
        val vista = VistasFriccion.decision(
            this,
            segundos / 60,
            alSeguir = { quitarDecision() },
            alSalir = {
                // En el celular, salir cierra la sesion. Aca vuelve a cero.
                quitarDecision()
                deslizadorTiempo?.progress = 0
            }
        )
        // Se agrega debajo del panel, igual que en el celular queda sobre todo lo demas.
        raiz.addView(vista, raiz.indexOfChild(panel), completo())
        decision = vista
    }

    private fun quitarDecision() {
        decision?.let { raiz.removeView(it) }
        decision = null
    }

    // ---- panel de control ----

    private var deslizadorTiempo: SeekBar? = null

    private fun panelDeControl(): LinearLayout {
        estado = texto("", 12f).apply { alpha = 0.7f }

        val etiquetaTiempo = texto("tiempo de sesión", 13f)
        val tiempo = deslizador(TIEMPO_MAX_S) { valor ->
            segundos = valor.toLong()
            etiquetaTiempo.text = "tiempo de sesión · %d:%02d".format(segundos / 60, segundos % 60)
            actualizar()
        }
        deslizadorTiempo = tiempo

        val etiquetaRitmo = texto("", 13f)
        val ritmo = deslizador(60) { valor ->
            swipesPorMinuto = valor.toDouble()
            etiquetaRitmo.text = "swipes por minuto · $valor"
            actualizar()
        }
        ritmo.progress = swipesPorMinuto.toInt()
        etiquetaRitmo.text = "swipes por minuto · ${swipesPorMinuto.toInt()}"

        val atajos = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(boton("0:00") { tiempo.progress = 0 }, peso())
            addView(boton("${Config.MIN_PRESENCIA} min") { tiempo.progress = Config.MIN_PRESENCIA * 60 }, peso())
            addView(boton("${Config.MIN_DECISION} min") { tiempo.progress = Config.MIN_DECISION * 60 }, peso())
            addView(boton("Decisión") { mostrarDecision() }, peso())
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(225, 12, 12, 14))
            setPadding(dp(16), dp(12), dp(16), dp(28))
            addView(estado)
            addView(etiquetaTiempo, conMargen(dp(8)))
            addView(tiempo)
            addView(etiquetaRitmo, conMargen(dp(4)))
            addView(ritmo)
            addView(atajos, conMargen(dp(8)))
        }
    }

    /** Esconde el panel para ver la pantalla limpia, o sacarle una captura. */
    private fun botonPanel() = TextView(this).apply {
        text = "⚙"
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(120, 0, 0, 0))
        }
        setOnClickListener {
            panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun deslizador(maximo: Int, alCambiar: (Int) -> Unit) = SeekBar(this).apply {
        max = maximo
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(barra: SeekBar, valor: Int, delUsuario: Boolean) = alCambiar(valor)
            override fun onStartTrackingTouch(barra: SeekBar) {}
            override fun onStopTrackingTouch(barra: SeekBar) {}
        })
    }

    private fun boton(texto: String, alTocar: () -> Unit) = Button(this).apply {
        text = texto
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setOnClickListener { alTocar() }
    }

    private fun texto(contenido: String, sp: Float) = TextView(this).apply {
        text = contenido
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    private fun completo() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun conMargen(arriba: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = arriba }

    private fun peso() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    /**
     * En el celular la caja se mide desde el borde de arriba de la pantalla.
     * Para que caiga en el mismo lugar, esta pantalla tambien se dibuja detras
     * de la barra de estado.
     */
    @Suppress("DEPRECATION")
    private fun dibujarDetrasDeLaBarraDeEstado() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        window.statusBarColor = Color.TRANSPARENT
    }

    private companion object {
        /** Un poco mas que la decision, para ver que pasa despues. */
        const val TIEMPO_MAX_S = 20 * 60
    }
}
