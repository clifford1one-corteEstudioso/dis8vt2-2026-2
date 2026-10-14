package com.love.yourself.vistas

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.love.yourself.config.Config

/**
 * El cielo a pantalla completa (Figma: "sky"). Aparece a los MIN_CIELO
 * minutos y pregunta sin decidir por la persona: "Seguir" y "Salir" pesan lo
 * mismo. Solo arma la vista; la ventana la pone OverlayFriccion.
 *
 * Llega bajando desde arriba, entero y sin transparencias: el cielo no se
 * insinua, aparece. Despues entran el mensaje y el clima, y al final los
 * botones. Los botones no responden hasta que se terminan de ver: si la
 * persona venia deslizando, ese dedo no tiene que elegir por ella.
 *
 * Todo se ubica en proporciones del lienzo de Figma (1440 x 3120) sobre el
 * tamano real de la ventana, igual que el cielo dibujado (CieloView).
 * Identico en la app real y en la de diseno: se copia entero.
 */
class VistaCielo(private val context: Context) {

    private val anchoPantalla = context.resources.displayMetrics.widthPixels.toFloat()

    /** px de Figma a px de pantalla, para tamanos de letra y bordes. Las posiciones las calcula el lienzo. */
    private fun x(v: Float) = v / 1440f * anchoPantalla

    /**
     * @param cara el cerebro que se sienta en la nube (un drawable). 0 = sin cerebro.
     * @param alSeguir se llama cuando el cielo termino de irse.
     * @param alSalir se llama al tiro: la persona ya eligio irse.
     */
    fun crear(minutos: Long, cara: Int, alSeguir: () -> Unit, alSalir: () -> Unit): Pantalla {
        val pantalla = Pantalla(context).apply { isClickable = true }
        pantalla.addView(CieloView(context).apply { this.cara = cara }, completo())

        // Los botones esperan a verse enteros antes de responder.
        var aparecioMs = Long.MAX_VALUE
        var decidido = false
        pantalla.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                aparecioMs = SystemClock.uptimeMillis()
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
        val esperaMs = if (Animacion.activas()) BOTONES_DESDE_MS + BOTONES_DURACION_MS else ESPERA_SIN_ANIMACION_MS
        fun puedeElegir() = !decidido && SystemClock.uptimeMillis() - aparecioMs >= esperaMs

        val mensaje = texto(Config.TEXTO_CIELO, 70f, negrita = false)
        val clima = tarjetaClima()
        val botones = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(boton("Seguir") {
                if (puedeElegir()) {
                    decidido = true
                    // El cielo vuelve a subir y deja ver el reel de nuevo.
                    pantalla.salir(alSeguir)
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(boton("Salir") {
                if (puedeElegir()) {
                    decidido = true
                    alSalir()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = x(40f).toInt()
            })
        }

        pantalla.addView(LienzoFigma(context).apply {
            // Mensaje arriba a la izquierda, clima a la derecha, botones abajo.
            addView(mensaje, LienzoFigma.Lugar(123f, 341f, 620f))
            addView(clima, LienzoFigma.Lugar(835f, 1396f, 504f))
            addView(botones, LienzoFigma.Lugar(123f, 380f, 1440f - 2 * 123f, desdeAbajo = true))
        }, completo())

        pantalla.entrada = {
            pantalla.translationY = -pantalla.height.toFloat()
            pantalla.animate().translationY(0f)
                .setStartDelay(0L).setDuration(BAJA_MS).setInterpolator(Animacion.FRENA)
                .start()
            Animacion.entrar(mensaje, demoraMs = 380L, desdeDp = -14f, duracionMs = 600L)
            clima.alpha = 0f
            clima.translationX = x(80f)
            clima.animate().alpha(1f).translationX(0f)
                .setStartDelay(560L).setDuration(600L).setInterpolator(Animacion.FRENA)
                .start()
            Animacion.entrar(botones, demoraMs = BOTONES_DESDE_MS, desdeDp = 28f, duracionMs = BOTONES_DURACION_MS)
        }
        pantalla.salida = { fin ->
            pantalla.animate().translationY(-pantalla.height.toFloat())
                .setStartDelay(0L).setDuration(SUBE_MS).setInterpolator(Animacion.ACELERA)
                .withEndAction(fin)
                .start()
        }
        return pantalla
    }

    private fun tarjetaClima() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            cornerRadius = x(45f)
            setColor(Color.argb(79, 255, 255, 255))
        }
        val margen = x(25f).toInt()
        setPadding(x(50f).toInt(), margen, x(50f).toInt(), margen)
        addView(View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FFF6CC"))
            }
        }, LinearLayout.LayoutParams(x(144f).toInt(), x(144f).toInt()))
        addView(
            texto(Config.CLIMA_TEMPERATURA, 50f, negrita = true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = x(12f).toInt()
            }
        )
        addView(texto(Config.CLIMA_LUGAR, 35f, negrita = false).apply { gravity = Gravity.CENTER })
    }

    private fun texto(contenido: String, pxFigma: Float, negrita: Boolean) = TextView(context).apply {
        text = contenido
        setTextColor(TINTA)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, x(pxFigma))
        if (negrita) setTypeface(typeface, Typeface.BOLD)
    }

    private fun boton(texto: String, alTocar: () -> Unit) = TextView(context).apply {
        text = texto
        gravity = Gravity.CENTER
        setTextColor(TINTA)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, x(50f))
        val alto = x(40f).toInt()
        setPadding(0, alto, 0, alto)
        val forma = GradientDrawable().apply {
            cornerRadius = x(45f)
            setColor(Color.argb(79, 255, 255, 255))
            setStroke(x(4f).toInt(), Color.argb(160, 1, 1, 1))
        }
        background = RippleDrawable(ColorStateList.valueOf(Color.argb(70, 255, 255, 255)), forma, null)
        Animacion.presionable(this)
        setOnClickListener { alTocar() }
    }

    private fun completo() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private companion object {
        val TINTA = Color.parseColor("#010101")

        const val BAJA_MS = 720L
        const val SUBE_MS = 420L
        const val BOTONES_DESDE_MS = 1000L
        const val BOTONES_DURACION_MS = 450L

        /** Aunque no haya animaciones, un respiro antes de poder elegir. */
        const val ESPERA_SIN_ANIMACION_MS = 700L
    }
}

/**
 * Ubica a cada hijo en coordenadas del lienzo de Figma (1440 x 3120) segun el
 * tamano que de verdad le toca. Asi el mensaje, el clima y los botones calzan
 * con el cielo dibujado aunque la ventana no mida lo mismo que la pantalla
 * (barra de estado, barra de navegacion).
 */
private class LienzoFigma(context: Context) : ViewGroup(context) {

    /** x, y y ancho en px de Figma. desdeAbajo: y se mide desde el borde de abajo. */
    class Lugar(val x: Float, val y: Float, val ancho: Float, val desdeAbajo: Boolean = false) :
        ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)

    override fun onMeasure(anchoSpec: Int, altoSpec: Int) {
        val w = MeasureSpec.getSize(anchoSpec)
        val h = MeasureSpec.getSize(altoSpec)
        for (i in 0 until childCount) {
            val hijo = getChildAt(i)
            val lugar = hijo.layoutParams as Lugar
            hijo.measure(
                MeasureSpec.makeMeasureSpec((lugar.ancho / 1440f * w).toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST)
            )
        }
        setMeasuredDimension(w, h)
    }

    override fun onLayout(cambio: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        val h = b - t
        for (i in 0 until childCount) {
            val hijo = getChildAt(i)
            val lugar = hijo.layoutParams as Lugar
            val izq = (lugar.x / 1440f * w).toInt()
            val dy = (lugar.y / 3120f * h).toInt()
            val arriba = if (lugar.desdeAbajo) h - dy - hijo.measuredHeight else dy
            hijo.layout(izq, arriba, izq + hijo.measuredWidth, arriba + hijo.measuredHeight)
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?) = p is Lugar

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams = Lugar(0f, 0f, 1440f)

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams = Lugar(0f, 0f, 1440f)

    override fun shouldDelayChildPressedState() = false
}
