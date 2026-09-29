package com.love.yourself

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.love.yourself.lab.AcumuladoSemanal
import com.love.yourself.lab.Ajustes
import com.love.yourself.lab.CapturaContinuaService
import com.love.yourself.lab.Config
import com.love.yourself.lab.DiarioDev
import com.love.yourself.lab.DatosBrief
import com.love.yourself.lab.BurbujaView
import com.love.yourself.lab.Momento
import com.love.yourself.lab.OverlayBrief
import com.love.yourself.lab.OverlayFriccion
import com.love.yourself.lab.Reloj
import com.love.yourself.lab.SesionService

/**
 * Pantalla de configuracion. Una vez dados los dos permisos, la app trabaja
 * sola y no hay que volver aca.
 *
 * Es a proposito: una intervencion que hay que encender a mano no sirve para
 * alguien que se pierde cuarenta minutos sin darse cuenta. Quien se acuerda de
 * abrir esta pantalla ya esta siendo consciente.
 *
 * Hecha con vistas simples y no con Compose, igual que las capas: asi todo el
 * codigo de la app se escribe de una sola forma.
 */
class MainActivity : Activity() {

    private lateinit var ajustes: Ajustes
    private lateinit var raiz: LinearLayout
    private var mostrarTodasLasApps = false

    // Para forzar pantallas desde el modo dev, sin esperar una sesion.
    private var friccionDePrueba: OverlayFriccion? = null
    private var briefDePrueba: OverlayBrief? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ajustes = Ajustes(this)
        Reloj.cambiarFactor(if (ajustes.modoDev) ajustes.factorReloj else 1)
        raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(48))
        }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(raiz)
        })
    }

    /**
     * Los permisos se conceden en Ajustes, fuera de la app, y al volver no
     * llega ningun resultado. Por eso la pantalla se arma de nuevo cada vez.
     */
    override fun onResume() {
        super.onResume()
        dibujar()
    }

    override fun onDestroy() {
        super.onDestroy()
        friccionDePrueba?.ocultarTodo()
        briefDePrueba?.ocultar()
    }

    private fun dibujar() {
        raiz.removeAllViews()
        val puedeDibujar = Settings.canDrawOverlays(this)
        val servicioActivo = servicioDeAccesibilidadActivo()

        titulo("Love Yourself · v6", 24f)
        parrafo(
            if (puedeDibujar && servicioActivo) {
                "Listo. No tienes que volver aquí: la app se enciende sola cuando " +
                    "empiezas a deslizar contenido a pantalla completa."
            } else {
                "Faltan permisos para que la app pueda trabajar sola."
            }
        )

        // ---- permisos ----
        separador()
        titulo(if (puedeDibujar) "✓  1. Dibujar sobre otras apps" else "1. Dibujar sobre otras apps")
        parrafo("Para mostrar la burbuja y el tiempo encima de la app.")
        if (!puedeDibujar) boton("Conceder") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        titulo(if (servicioActivo) "✓  2. Accesibilidad" else "2. Accesibilidad")
        parrafo(
            "Para saber qué app tienes adelante y si lo que deslizas ocupa toda la " +
                "pantalla. Mira la forma de la pantalla, no lo que muestra, y no guarda " +
                "nada fuera de tu teléfono."
        )
        if (!servicioActivo) boton("Activar \"Love Yourself · v6\"") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // ---- apps ----
        separador()
        titulo("Dónde mide")
        parrafo(
            "Solo cuenta el deslizamiento a pantalla completa en estas apps. " +
                "Mensajes y búsqueda no cuentan."
        )
        listaDeApps()

        // ---- comportamiento ----
        separador()
        titulo("Cómo se comporta")
        parrafo(
            "· En una app marcada: arriba, una burbuja con el tiempo y el ritmo.\n" +
                "· Primer deslizamiento a pantalla completa: la burbuja se agranda y aparece la mascota.\n" +
                "· Con el arrastre, el cielo se va asomando.\n" +
                "· A los ${Config.MIN_DECISION} min de arrastre: pantalla completa, seguir o salir.\n" +
                "· Al salir al inicio: el resumen.\n" +
                "Nunca bloquea. Solo devuelve la decisión."
        )

        // ---- modo dev ----
        separador()
        casilla("Modo dev", ajustes.modoDev) { si ->
            ajustes.modoDev = si
            Reloj.cambiarFactor(if (si) ajustes.factorReloj else 1)
            dibujar()
        }
        if (ajustes.modoDev) modoDev(puedeDibujar)

        // ---- investigacion ----
        separador()
        titulo("Modo investigación")
        parrafo(
            "Cuenta cortes de plano leyendo la pantalla. Android exige aceptar un " +
                "diálogo cada vez, así que esto no puede encenderse solo: es para " +
                "medir, no parte de la app."
        )
        boton("Medir cortes") { pedirCaptura() }
        boton("Detener medición") {
            startService(
                Intent(this, CapturaContinuaService::class.java).setAction(CapturaContinuaService.ACCION_DETENER)
            )
            Toast.makeText(this, "Medición detenida.", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- lista de apps ----

    private fun listaDeApps() {
        val lanzables = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
        val vigiladas = ajustes.appsVigiladas

        // Primero las marcadas y las sugeridas instaladas; el resto, a pedido.
        val destacadas = lanzables
            .filter { it.first in vigiladas || it.first in Config.APPS_SUGERIDAS }
            .sortedBy { it.second.lowercase() }
        val otras = lanzables.filter { it !in destacadas }.sortedBy { it.second.lowercase() }

        for ((paquete, nombre) in destacadas) casillaApp(paquete, nombre, paquete in vigiladas)
        if (destacadas.isEmpty()) parrafo("(Ninguna de las apps sugeridas está instalada.)")

        if (mostrarTodasLasApps) {
            for ((paquete, nombre) in otras) casillaApp(paquete, nombre, false)
        } else if (otras.isNotEmpty()) {
            botonSecundario("Agregar otra app (${otras.size})") {
                mostrarTodasLasApps = true
                dibujar()
            }
        }
    }

    private fun casillaApp(paquete: String, nombre: String, marcada: Boolean) {
        casilla(nombre, marcada) { si -> ajustes.vigilar(paquete, si) }
    }

    // ---- modo dev ----

    private fun modoDev(puedeDibujar: Boolean) {
        titulo("Reloj acelerado")
        parrafo(
            "Acelera los contadores, no a ti: tus gestos y pausas (mirar un reel, " +
                "responder un mensaje, bloquear) se siguen midiendo en segundos reales. " +
                "A x10 la decisión llega a los 90 s de arrastre. Lo que midas así va a " +
                "una semana aparte y el resumen dice \"dev\"."
        )
        fila(listOf(1, 10, 60).map { f ->
            (if (Reloj.factor == f) "● x$f" else "x$f") to {
                Reloj.cambiarFactor(f)
                ajustes.factorReloj = f
                dibujar()
            }
        })

        titulo("Forzar pantallas")
        val sinPermiso = { Toast.makeText(this, "Falta el permiso 1.", Toast.LENGTH_SHORT).show() }
        fila(listOf(
            "Decisión" to {
                if (!puedeDibujar) sinPermiso() else {
                    val f = friccionDePrueba ?: OverlayFriccion(this).also { friccionDePrueba = it }
                    f.mostrarDecision(Config.MIN_DECISION.toLong(), alSeguir = {}, alSalir = {})
                }
            },
            "Resumen" to {
                if (!puedeDibujar) sinPermiso() else {
                    val b = briefDePrueba ?: OverlayBrief(this).also { briefDePrueba = it }
                    val ejemplo = DatosBrief.ejemplo(System.currentTimeMillis())
                    val semanaDev = AcumuladoSemanal(this, AcumuladoSemanal.ARCHIVO_DEV)
                        .estaSemana(System.currentTimeMillis())
                    // Si ya probaste sesiones aceleradas, la semana es la de esas pruebas.
                    b.mostrar(if (semanaDev > 0) ejemplo.copy(semanaArrastreMs = semanaDev) else ejemplo)
                }
            }
        ))

        titulo("Burbuja")
        val burbuja = BurbujaView(this).apply { mostrarDatos("2:23", "18") }
        raiz.addView(FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(186, 186, 186))
            addView(burbuja, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL
            ))
        }, margenArriba(dp(8)))
        fila(Momento.values().map { m -> m.name.lowercase().replace('_', ' ') to { burbuja.expandir(m) } })

        titulo("Diario")
        parrafo("Lo que el servicio vio, lo más nuevo arriba. Prueba en Instagram, vuelve acá y saca una captura.")
        raiz.addView(TextView(this).apply {
            text = DiarioDev.texto()
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextIsSelectable(true)
        }, margenArriba(dp(6)))
        fila(listOf(
            "Actualizar" to { dibujar() },
            "Borrar diario" to { DiarioDev.borrar(); dibujar() }
        ))

        botonSecundario("Borrar la semana dev") {
            getSharedPreferences(AcumuladoSemanal.ARCHIVO_DEV, Context.MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(this, "Semana dev borrada.", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- captura (modo investigacion) ----

    private fun pedirCaptura() {
        val faltaNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (faltaNotif) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), PEDIDO_NOTIFICACIONES)
            return
        }
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(manager.createScreenCaptureIntent(), PEDIDO_CAPTURA)
    }

    override fun onRequestPermissionsResult(codigo: Int, permisos: Array<out String>, resultados: IntArray) {
        super.onRequestPermissionsResult(codigo, permisos, resultados)
        // Con o sin notificaciones, se sigue: la medicion funciona igual.
        if (codigo == PEDIDO_NOTIFICACIONES) {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            @Suppress("DEPRECATION")
            startActivityForResult(manager.createScreenCaptureIntent(), PEDIDO_CAPTURA)
        }
    }

    @Deprecated("startActivityForResult basta para una pantalla de configuracion")
    override fun onActivityResult(codigo: Int, resultado: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(codigo, resultado, data)
        if (codigo != PEDIDO_CAPTURA || resultado != RESULT_OK || data == null) return
        val intent = Intent(this, CapturaContinuaService::class.java).apply {
            putExtra(CapturaContinuaService.EXTRA_RESULT_CODE, resultado)
            putExtra(CapturaContinuaService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Midiendo cortes. El CSV queda en files/Documents/registros.", Toast.LENGTH_LONG).show()
    }

    /**
     * No hay API para preguntarle al sistema si un service propio esta activo,
     * asi que se lee el ajuste donde Android guarda la lista.
     */
    private fun servicioDeAccesibilidadActivo(): Boolean {
        val activos = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val propio = "$packageName/${SesionService::class.java.name}"
        return activos.split(':').any { it.equals(propio, ignoreCase = true) }
    }

    // ---- piezas de la pantalla ----

    private fun titulo(texto: String, sp: Float = 17f) {
        raiz.addView(TextView(this).apply {
            text = texto
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            setTypeface(typeface, Typeface.BOLD)
        }, margenArriba(dp(12)))
    }

    private fun parrafo(texto: String) {
        raiz.addView(TextView(this).apply {
            text = texto
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            alpha = 0.8f
        }, margenArriba(dp(4)))
    }

    private fun boton(texto: String, alTocar: () -> Unit) {
        raiz.addView(Button(this).apply {
            text = texto
            isAllCaps = false
            setOnClickListener { alTocar() }
        }, margenArriba(dp(8)))
    }

    private fun botonSecundario(texto: String, alTocar: () -> Unit) {
        raiz.addView(Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            text = texto
            isAllCaps = false
            setOnClickListener { alTocar() }
        }, margenArriba(dp(4)))
    }

    private fun fila(botones: List<Pair<String, () -> Unit>>) {
        val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((texto, alTocar) in botones) {
            f.addView(Button(this).apply {
                text = texto
                isAllCaps = false
                setOnClickListener { alTocar() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        raiz.addView(f, margenArriba(dp(8)))
    }

    private fun casilla(texto: String, marcada: Boolean, alCambiar: (Boolean) -> Unit) {
        raiz.addView(CheckBox(this).apply {
            text = texto
            isChecked = marcada
            setOnCheckedChangeListener { _, si -> alCambiar(si) }
        }, margenArriba(0))
    }

    private fun separador() {
        raiz.addView(View(this).apply { setBackgroundColor(Color.argb(40, 0, 0, 0)) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(20)
                bottomMargin = dp(4)
            })
    }

    private fun margenArriba(margen: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = margen }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    private companion object {
        const val PEDIDO_CAPTURA = 1
        const val PEDIDO_NOTIFICACIONES = 2
    }
}
