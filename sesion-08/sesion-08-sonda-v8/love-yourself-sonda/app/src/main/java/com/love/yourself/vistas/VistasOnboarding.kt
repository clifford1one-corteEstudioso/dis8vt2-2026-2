package com.love.yourself.vistas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.love.yourself.vistas.Animacion.frena
import com.love.yourself.vistas.Animacion.lerp
import com.love.yourself.vistas.Animacion.tramo
import kotlin.math.abs

/** Una app que se puede vigilar: paquete y nombre visible. */
class AppInstalada(val paquete: String, val nombre: String)

/** La lista de permisos y como marcar los que ya estan dados al volver de los ajustes. */
class ChecklistPermisos(
    val vista: View,
    private val marcar: (accesibilidad: Boolean, encima: Boolean, bateria: Boolean) -> Unit
) {
    fun actualizar(accesibilidad: Boolean, encima: Boolean, bateria: Boolean) = marcar(accesibilidad, encima, bateria)
}

/**
 * Las pantallas de bienvenida (Figma: WF1a, WF1b y "¿A qué quieres que le
 * pongamos ojo?"). Solo arman vistas: que pasa al tocar lo decide quien las
 * usa (OnboardingActivity en la app real, un simulador en la de diseno).
 *
 * Identico en las dos apps: se copia entero.
 */
class VistasOnboarding(private val context: Context) {

    private val d = context.resources.displayMetrics.density
    private fun dp(v: Float) = (v * d).toInt()

    // ---- colores de los wireframes: cambiar aca ----
    private val fondo = Colores.PAGINA
    private val modal = Color.parseColor("#3A3A3A")
    private val linea = Color.parseColor("#5A5A5A")
    private val campo = Color.parseColor("#2E2E2E")
    private val bordeCampo = Color.parseColor("#8C8C8C")
    private val ayuda = Color.parseColor("#A6A6A6")
    private val panel = Color.parseColor("#4D4D4D")
    private val bordePanel = Color.parseColor("#737373")

    /**
     * WF1a: el modal de un permiso. El enlace "¿Por qué...?" despliega la
     * explicacion dentro del mismo modal, sin salir.
     */
    fun permiso(
        texto: String,
        porQue: String,
        alSalir: () -> Unit,
        alActivar: () -> Unit
    ): View {
        val explicacion = texto(porQue, 14f, ayuda).apply { visibility = View.GONE }
        val enlace = texto("¿Por qué requerimos este permiso?", 14f, Color.WHITE).apply {
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                explicacion.visibility = if (explicacion.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        val cuerpo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto(texto, 17f, Color.WHITE))
            addView(enlace, margenArriba(dp(18f)))
            addView(explicacion, margenArriba(dp(10f)))
            // La explicacion se despliega estirando el modal, no de golpe.
            layoutTransition = android.animation.LayoutTransition()
        }
        return pantalla(tarjeta(cuerpo, "Salir" to alSalir, "Activar" to alActivar))
    }

    /**
     * Antes de todo, en la version de estudio: abrir el formulario de
     * consentimiento y escribir el codigo de participante que une la firma con
     * el registro. "Continuar" se enciende cuando se abrio el formulario y hay
     * codigo (la app no puede saber si se envio: eso se revisa en Forms).
     */
    fun consentimiento(
        codigoActual: String,
        /** Abre el formulario; devuelve false si no se pudo (por ejemplo, falta el enlace). */
        alAbrirFormulario: () -> Boolean,
        alSalir: () -> Unit,
        alContinuar: (codigo: String) -> Unit
    ): View {
        var abrioFormulario = codigoActual.isNotEmpty()
        lateinit var continuar: View
        val codigo = EditText(context).apply {
            setText(codigoActual)
            hint = "Código de participante (ej. P3)"
            setHintTextColor(Color.parseColor("#CCCCCC"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            isSingleLine = true
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = dp(4f).toFloat()
                setColor(campo)
                setStroke(dp(1f), bordeCampo)
            }
        }
        fun revisar() {
            continuar.alpha = if (abrioFormulario && codigo.text.isNotBlank()) 1f else 0.35f
        }
        val abrir = texto("Abrir formulario", 16f, Color.WHITE).apply {
            setPadding(0, dp(14f), 0, dp(14f))
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(panel)
                setStroke(dp(1f), bordePanel)
            }
            Animacion.presionable(this)
            setOnClickListener {
                if (alAbrirFormulario()) abrioFormulario = true
                revisar()
            }
        }
        val cuerpo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto("Antes de empezar", 19f, Color.WHITE))
            addView(texto(
                "Love Yourself es parte de un estudio. Lee el consentimiento informado y, " +
                    "si estás de acuerdo, complétalo. Después escribe aquí tu código de participante.",
                14f, ayuda
            ), margenArriba(dp(12f)))
            addView(abrir, margenArriba(dp(20f)))
            addView(codigo, margenArriba(dp(14f)))
        }
        val vista = pantalla(tarjeta(
            cuerpo,
            "Salir" to alSalir,
            "Continuar" to {
                val c = codigo.text.toString().trim().uppercase()
                if (abrioFormulario && c.isNotEmpty()) alContinuar(c)
            },
            alCrearDerecho = { continuar = it }
        ))
        codigo.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = revisar()
        })
        revisar()
        return vista
    }

    /**
     * Los permisos juntos, como lista: se ve que son cosas distintas y cuales
     * ya estan, asi no parece que pide dos veces lo mismo. Tocar una fila
     * pendiente abre el ajuste del telefono. El "?" de arriba a la derecha
     * despliega para que sirve cada permiso.
     */
    fun permisos(
        porQue: String,
        alAccesibilidad: () -> Unit,
        alEncima: () -> Unit,
        alBateria: () -> Unit,
        alSalir: () -> Unit,
        alContinuar: () -> Unit
    ): ChecklistPermisos {
        val explicacion = texto(porQue, 14f, ayuda).apply {
            gravity = Gravity.START
            visibility = View.GONE
        }
        val signo = texto("?", 16f, Color.WHITE).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(panel)
                setStroke(dp(1f), bordePanel)
            }
            Animacion.presionable(this)
            setOnClickListener {
                explicacion.visibility = if (explicacion.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        val cabecera = FrameLayout(context).apply {
            addView(texto("¡Bienvenide! Para continuar debes seguir estos pasos:", 17f, Color.WHITE).apply {
                gravity = Gravity.START
                setPadding(0, 0, dp(40f), 0)
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(signo, FrameLayout.LayoutParams(dp(30f), dp(30f), Gravity.TOP or Gravity.END))
        }

        var accesibilidad = false
        var encima = false
        var bateria = false
        lateinit var continuar: View
        val (filaInstalar, _) = filaPaso("Instala la app") {}
        val (filaAccesibilidad, marcaAccesibilidad) = filaPaso("Activa Accesibilidad") { if (!accesibilidad) alAccesibilidad() }
        val (filaEncima, marcaEncima) = filaPaso("Activa \"Mostrar encima de otras apps\"") { if (!encima) alEncima() }
        val (filaBateria, marcaBateria) = filaPaso("Quita la app del ahorro de batería") { if (!bateria) alBateria() }

        val cuerpo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(cabecera)
            addView(explicacion, margenArriba(dp(12f)))
            addView(filaInstalar, margenArriba(dp(20f)))
            addView(filaAccesibilidad, margenArriba(dp(10f)))
            addView(filaEncima, margenArriba(dp(10f)))
            addView(filaBateria, margenArriba(dp(10f)))
            layoutTransition = android.animation.LayoutTransition()
        }
        val vista = pantalla(tarjeta(
            cuerpo,
            "Salir" to alSalir,
            "Continuar" to { if (accesibilidad && encima && bateria) alContinuar() },
            alCrearDerecho = { continuar = it }
        ))
        fun aplicar(a: Boolean, e: Boolean, b: Boolean) {
            accesibilidad = a
            encima = e
            bateria = b
            ponerMarca(marcaAccesibilidad, a)
            ponerMarca(marcaEncima, e)
            ponerMarca(marcaBateria, b)
            continuar.alpha = if (a && e && b) 1f else 0.35f
        }
        aplicar(false, false, false)
        return ChecklistPermisos(vista) { a, e, b -> aplicar(a, e, b) }
    }

    /** Una fila de la lista: el paso a la izquierda y su casilla a la derecha. */
    private fun filaPaso(etiqueta: String, alTocar: () -> Unit): Pair<View, TextView> {
        val marca = texto("", 18f, Color.WHITE).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(4f).toFloat()
                setColor(campo)
                setStroke(dp(1f), bordeCampo)
            }
        }
        ponerMarca(marca, true)
        val fila = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(14f), dp(14f), dp(14f))
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(panel)
            }
            addView(texto(etiqueta, 16f, Color.WHITE).apply { gravity = Gravity.START },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(marca, LinearLayout.LayoutParams(dp(30f), dp(30f)).apply { leftMargin = dp(12f) })
            Animacion.presionable(this)
            setOnClickListener { alTocar() }
        }
        return fila to marca
    }

    private fun ponerMarca(marca: TextView, hecho: Boolean) {
        marca.text = if (hecho) "✓" else ""
    }

    /** "¿A qué quieres que le pongamos ojo?": elegir las apps donde se mide. */
    fun apps(
        instaladas: List<AppInstalada>,
        marcadas: Set<String>,
        alCambiar: (paquete: String, si: Boolean) -> Unit,
        alContinuar: () -> Unit
    ): View {
        val lista = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            for (app in instaladas) {
                addView(CheckBox(context).apply {
                    text = app.nombre
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    isChecked = app.paquete in marcadas
                    setOnCheckedChangeListener { _, si -> alCambiar(app.paquete, si) }
                })
            }
        }
        val caja = ScrollView(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(30f).toFloat()
                setColor(panel)
                setStroke(dp(2f), bordePanel)
            }
            addView(lista)
        }
        val columna = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(35f), dp(90f), dp(35f), dp(40f))
            addView(texto("¿A qué quieres que le pongamos ojo?", 18f, Color.WHITE))
            addView(texto("Solo cuenta el scroll a pantalla completa en estas apps.", 13f, ayuda), margenArriba(dp(6f)))
            addView(caja, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(16f) })
            addView(boton("Continuar", alContinuar).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(30f).toFloat()
                    setColor(modal)
                }
            }, margenArriba(dp(16f)))
        }
        return FrameLayout(context).apply {
            setBackgroundColor(fondo)
            addView(columna, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    /**
     * WF1b: lo que la persona quiere hacer mas. Una grilla de intereses de a
     * tres (se pueden elegir varios) y, abajo, un campo para otra cosa. Se
     * guarda como texto ("Música, Leer, tejer") y aparece en el resumen.
     */
    fun declaracion(
        actual: String,
        alOmitir: () -> Unit,
        alContinuar: (actividad: String) -> Unit
    ): View {
        val previos = actual.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val elegidos = Intereses.TODOS.filter { it.nombre in previos }.map { it.nombre }.toMutableSet()
        val otra = previos.filter { p -> Intereses.TODOS.none { it.nombre == p } }.joinToString(", ")
        val grilla = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        Intereses.TODOS.chunked(3).forEachIndexed { i, tres ->
            val fila = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            tres.forEachIndexed { j, interes ->
                fila.addView(celdaInteres(interes, interes.nombre in elegidos) { si ->
                    if (si) elegidos += interes.nombre else elegidos -= interes.nombre
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (j > 0) leftMargin = dp(8f)
                })
            }
            grilla.addView(fila, margenArriba(if (i > 0) dp(8f) else 0))
        }
        val entrada = EditText(context).apply {
            setText(otra)
            hint = "otra cosa…"
            setHintTextColor(Color.parseColor("#CCCCCC"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = true
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = dp(4f).toFloat()
                setColor(campo)
                setStroke(dp(1f), bordeCampo)
            }
        }
        val cuerpo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto("¿Qué te gustaría hacer más y no alcanzas?", 17f, Color.WHITE))
            addView(grilla, margenArriba(dp(20f)))
            addView(entrada, margenArriba(dp(14f)))
            addView(texto("Aparecerá en tu resumen semanal como punto de comparación.", 11f, ayuda).apply {
                gravity = Gravity.START
            }, margenArriba(dp(8f)))
        }
        return pantalla(tarjeta(cuerpo, "Omitir" to alOmitir, "Continuar" to {
            // En el orden de la grilla, y lo escrito al final.
            val partes = Intereses.TODOS.map { it.nombre }.filter { it in elegidos } +
                listOfNotNull(entrada.text.toString().trim().takeIf { it.isNotEmpty() })
            alContinuar(partes.joinToString(", "))
        }))
    }

    /** Un interes de la grilla: icono y nombre. Tocarlo lo marca o lo desmarca. */
    private fun celdaInteres(interes: Interes, marcado: Boolean, alCambiar: (Boolean) -> Unit): View {
        var si = marcado
        val icono = ImageView(context).apply { setImageResource(interes.icono) }
        val nombre = texto(interes.nombre, 13f, Color.WHITE)
        val celda = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4f), dp(12f), dp(4f), dp(10f))
            addView(icono, LinearLayout.LayoutParams(dp(32f), dp(32f)))
            addView(nombre, margenArriba(dp(6f)))
        }
        fun pintar() {
            val color = if (si) Colores.PAGINA else Color.WHITE
            icono.setColorFilter(color)
            nombre.setTextColor(color)
            celda.background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(if (si) Color.WHITE else panel)
            }
        }
        pintar()
        Animacion.presionable(celda)
        celda.setOnClickListener {
            si = !si
            pintar()
            alCambiar(si)
        }
        return celda
    }

    // ---- moverse entre pasos ----

    /**
     * Pone la pantalla nueva en el contenedor: la anterior se va hacia un lado
     * y la nueva entra por el otro. Hacia atras (adelante = false), al reves.
     */
    fun cambiar(contenedor: ViewGroup, anterior: View?, nueva: View, adelante: Boolean = true) {
        contenedor.addView(nueva, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (!Animacion.activas()) {
            anterior?.let { contenedor.removeView(it) }
            return
        }
        val dx = (if (contenedor.width > 0) contenedor.width * 0.25f else dp(90f).toFloat()) * if (adelante) 1f else -1f
        nueva.alpha = 0f
        nueva.translationX = dx
        nueva.animate().alpha(1f).translationX(0f)
            .setStartDelay(if (anterior == null) 0L else 90L).setDuration(420L).setInterpolator(Animacion.FRENA)
            .start()
        anterior?.animate()?.alpha(0f)?.translationX(-dx)
            ?.setStartDelay(0L)?.setDuration(240L)?.setInterpolator(Animacion.ACELERA)
            ?.withEndAction { contenedor.removeView(anterior) }
            ?.start()
    }

    /** Los puntos de arriba: en que paso vas, de cuantos. */
    fun puntos(total: Int) = PuntosPasos(context, total)

    // ---- piezas ----

    /** El modal: contenido arriba y dos botones abajo, separados por lineas. */
    private fun tarjeta(
        cuerpo: View,
        izq: Pair<String, () -> Unit>,
        der: Pair<String, () -> Unit>,
        alCrearDerecho: ((View) -> Unit)? = null
    ): View {
        val derecho = boton(der.first, der.second).also { alCrearDerecho?.invoke(it) }
        val botones = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(boton(izq.first, izq.second), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(View(context).apply { setBackgroundColor(linea) }, LinearLayout.LayoutParams(dp(1f), ViewGroup.LayoutParams.MATCH_PARENT))
            addView(derecho, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(22f).toFloat()
                setColor(modal)
            }
            addView(cuerpo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(26f), dp(40f), dp(26f), dp(30f))
            })
            addView(View(context).apply { setBackgroundColor(linea) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f)).apply {
                leftMargin = dp(18f)
                rightMargin = dp(18f)
            })
            addView(botones)
        }
    }

    /** Fondo oscuro con el modal centrado. */
    private fun pantalla(contenido: View) = FrameLayout(context).apply {
        setBackgroundColor(fondo)
        addView(contenido, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            leftMargin = dp(20f)
            rightMargin = dp(20f)
        })
    }

    private fun texto(contenido: String, sp: Float, color: Int) = TextView(context).apply {
        text = contenido
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        gravity = Gravity.CENTER
    }

    private fun boton(texto: String, alTocar: () -> Unit) = texto(texto, 16f, Color.WHITE).apply {
        setPadding(0, dp(18f), 0, dp(18f))
        Animacion.presionable(this)
        setOnClickListener { alTocar() }
    }

    private fun margenArriba(m: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = m }
}

/**
 * Puntos que dicen en que paso de la bienvenida vas. El del paso actual es
 * una pildora, y se desliza hasta el siguiente cuando avanzas.
 */
class PuntosPasos(context: Context, private val total: Int) : View(context) {

    private val d = resources.displayMetrics.density
    private val apagado = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5A5A5A") }
    private val encendido = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val forma = RectF()
    private var desde = 0f
    private var hasta = 0f
    private var cambioMs = -1L

    fun marcar(paso: Int) {
        val ahora = SystemClock.uptimeMillis()
        desde = posicion(ahora)
        hasta = paso.toFloat()
        cambioMs = if (Animacion.activas() && isAttachedToWindow) ahora else -1L
        invalidate()
    }

    private fun posicion(ahora: Long): Float {
        if (cambioMs < 0) return hasta
        return lerp(desde, hasta, frena(tramo((ahora - cambioMs).toFloat(), 0f, 420f)))
    }

    override fun onMeasure(anchoSpec: Int, altoSpec: Int) {
        setMeasuredDimension(resolveSize((total * 18 * d).toInt(), anchoSpec), resolveSize((8 * d).toInt(), altoSpec))
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val ahora = SystemClock.uptimeMillis()
        val paso = 18 * d
        val radio = 3 * d
        val inicio = (width - total * paso) / 2f + paso / 2f
        val cy = height / 2f
        for (i in 0 until total) c.drawCircle(inicio + i * paso, cy, radio, apagado)
        // La pildora va de un punto al otro, estirandose en el camino.
        val p = posicion(ahora)
        val estiramiento = 4 * d * abs(p - hasta).coerceAtMost(1f)
        val cx = inicio + p * paso
        val medio = 7 * d + estiramiento
        forma.set(cx - medio, cy - radio, cx + medio, cy + radio)
        c.drawRoundRect(forma, radio, radio, encendido)
        if (cambioMs >= 0 && ahora - cambioMs < 420L) postInvalidateOnAnimation()
    }
}
