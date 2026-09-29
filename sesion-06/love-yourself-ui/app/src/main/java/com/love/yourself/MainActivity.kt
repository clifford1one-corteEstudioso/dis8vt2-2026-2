package com.love.yourself

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.love.yourself.lab.Config
import com.love.yourself.lab.DatosBrief
import com.love.yourself.lab.BurbujaView
import com.love.yourself.lab.FormatoBrief
import com.love.yourself.lab.Momento
import com.love.yourself.lab.VistaBrief
import com.love.yourself.lab.VistaCielo

/**
 * App solo de diseno. Muestra las mismas vistas que la app real, pero dentro
 * de una pantalla normal: sin permisos, sin accesibilidad, sin Instagram.
 *
 * Se mueve el tiempo de arrastre con un deslizador y se ve como reaccionan la
 * burbuja y el cielo. Lo que se ajuste en los archivos de lab/ se
 * copia entero a la app real.
 *
 * Capas, de abajo hacia arriba, en el mismo orden que en el celular:
 *   reel de mentira -> burbuja -> cielo / resumen -> panel
 */
class MainActivity : Activity() {

    private lateinit var burbuja: BurbujaView
    private lateinit var raiz: FrameLayout
    private lateinit var panel: LinearLayout
    private lateinit var estado: TextView

    private var segundos = 0L
    private var swipesPorMinuto = 12.0
    private var cieloPantalla: View? = null
    private var cieloYaSalio = false
    private var resumen: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dibujarDetrasDeLaBarraDeEstado()

        raiz = FrameLayout(this)
        raiz.addView(FondoReel(this), completo())

        // Misma posicion que en el celular: arriba al centro.
        burbuja = BurbujaView(this)
        raiz.addView(
            burbuja,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
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
        // Debajo de la burbuja expandida, para no taparla.
        raiz.addView(botonPanel(), FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(120)
            marginEnd = dp(12)
        })

        setContentView(raiz)
        actualizar()
    }

    /** Aplica el estado actual a la burbuja y al cielo, con las mismas reglas del servicio. */
    private fun actualizar() {
        burbuja.mostrarDatos(FormatoBrief.reloj(segundos * 1000), "%.0f".format(swipesPorMinuto))
        // Igual que en el celular: el cielo aparece solo al llegar a MIN_CIELO.
        val limite = Config.MIN_CIELO * 60L
        if (segundos >= limite && !cieloYaSalio) {
            cieloYaSalio = true
            mostrarCielo()
        }
        if (segundos < limite) cieloYaSalio = false
        estado.text = if (segundos < limite) "cielo en %d:%02d".format((limite - segundos) / 60, (limite - segundos) % 60) else "después del cielo"
    }

    /** El resumen de salida, con los numeros del deslizador (o los del wireframe, en 0). */
    private fun mostrarResumen() {
        if (resumen != null) return
        val ahora = System.currentTimeMillis()
        val datos = if (segundos == 0L) {
            DatosBrief.ejemplo(ahora)
        } else {
            DatosBrief(
                cierreMs = ahora,
                totalMs = (segundos + BUSCADO_DE_MENTIRA_S) * 1000L,
                arrastradoMs = segundos * 1000L,
                videos = (segundos / 15).toInt(),
                semanaArrastreMs = (14 * 60 + 20) * 60_000L,
                etiqueta = "diseño"
            )
        }
        val vista = VistaBrief(this).crear(datos, alCerrar = {
            resumen?.let { raiz.removeView(it) }
            resumen = null
        })
        raiz.addView(vista, raiz.indexOfChild(panel), completo())
        resumen = vista
    }

    private fun mostrarCielo() {
        if (cieloPantalla != null) return
        val vista = VistaCielo(this).crear(
            segundos / 60,
            alSeguir = { quitarCielo() },
            alSalir = {
                // En el celular, salir cierra la sesion. Aca vuelve a cero.
                quitarCielo()
                deslizadorTiempo?.progress = 0
            }
        )
        // Se agrega debajo del panel, igual que en el celular queda sobre todo lo demas.
        raiz.addView(vista, raiz.indexOfChild(panel), completo())
        cieloPantalla = vista
    }

    private fun quitarCielo() {
        cieloPantalla?.let { raiz.removeView(it) }
        cieloPantalla = null
    }

    // ---- panel de control ----

    private var deslizadorTiempo: SeekBar? = null

    /** ▶ hace correr el tiempo solo, un segundo por segundo, como en el celular. */
    private val reloj = Handler(Looper.getMainLooper())
    private var corriendo = false
    private val tic = object : Runnable {
        override fun run() {
            val d = deslizadorTiempo ?: return
            if (d.progress < d.max) d.progress = d.progress + 1
            reloj.postDelayed(this, 1000L)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reloj.removeCallbacks(tic)
    }

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
            addView(boton("▶") {
                corriendo = !corriendo
                (this@apply.getChildAt(0) as Button).text = if (corriendo) "⏸" else "▶"
                reloj.removeCallbacks(tic)
                if (corriendo) reloj.postDelayed(tic, 1000L)
            }, peso())
            addView(boton("0:00") { tiempo.progress = 0 }, peso())
            addView(boton("${Config.MIN_CIELO} min") { tiempo.progress = Config.MIN_CIELO * 60 }, peso())
            addView(boton("Cielo") { mostrarCielo() }, peso())
            addView(boton("Resumen") { mostrarResumen() }, peso())
        }

        // Cada momento que agranda la burbuja.
        val momentos = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            for (m in Momento.values()) {
                addView(boton(m.name.lowercase().replace('_', ' ')) { burbuja.expandir(m) }, peso())
            }
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
            addView(texto("burbuja", 13f), conMargen(dp(8)))
            addView(momentos)
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
     * En el celular la burbuja se mide desde el borde de arriba de la pantalla.
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
        /** Un poco mas que el cielo, para ver que pasa despues. */
        const val TIEMPO_MAX_S = 20 * 60

        /** El resumen necesita algo de tiempo buscado; en el celular sale de la sesion real. */
        const val BUSCADO_DE_MENTIRA_S = 150L
    }
}
