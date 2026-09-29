package com.love.yourself.lab

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * El corazon de la app. Vive encendido una vez activado en Ajustes.
 *
 * Mide sin grabar pantalla: lee la estructura de la pantalla (que zona se
 * desliza y cuanto ocupa), no su contenido. Asi distingue el arrastre (reels,
 * feed a pantalla completa) del uso con intencion (mensajes, busqueda).
 *
 * Capas de friccion, de menos a mas:
 *  - siempre en una app vigilada: la burbuja con tiempo y ritmo
 *  - primer swipe: la burbuja se agranda y aparece la mascota. Empieza el cielo
 *  - a los 15 min de arrastre: la pantalla de decision
 *  - al salir: el resumen
 */
class SesionService : AccessibilityService() {

    private lateinit var registro: RegistroVisitas
    private lateinit var ajustes: Ajustes
    private var friccion: OverlayFriccion? = null
    private var brief: OverlayBrief? = null
    private var launchers: Set<String> = emptySet()
    private val handler = Handler(Looper.getMainLooper())
    private val bordes = Rect()

    /** Lo ultimo que midio un scroll, para la caja del modo dev. */
    private var ultimaFraccion = -1f

    private val tic = object : Runnable {
        override fun run() {
            val ahora = ahora()
            atender(registro.tic(ahora))
            refrescar(ahora)
            handler.postDelayed(this, 1000L)
        }
    }

    /**
     * Bloquear el celular pausa la sesion. Si la pausa dura mas de 3 minutos,
     * al volver se cierra y aparece el resumen.
     */
    private val receptorPantalla = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val ahora = ahora()
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> registro.pantallaApagada(ahora)
                Intent.ACTION_USER_PRESENT -> atender(registro.desbloqueo(ahora))
                Intent.ACTION_SCREEN_ON -> {
                    // Sin pantalla de bloqueo no llega USER_PRESENT: el
                    // desbloqueo es encender la pantalla.
                    val kg = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (!kg.isKeyguardLocked) atender(registro.desbloqueo(ahora))
                }
            }
        }
    }

    /** El reloj de la sesion. En modo dev puede ir acelerado. */
    private fun ahora(): Long {
        registro.factorReloj = Reloj.factor
        return Reloj.ahora()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        registro = RegistroVisitas(packageName)
        ajustes = Ajustes(this)
        friccion = OverlayFriccion(this)
        brief = OverlayBrief(this)
        launchers = paquetesDeInicio()

        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receptorPantalla, filtro, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receptorPantalla, filtro)
        }

        handler.post(tic)
        Log.i(TAG, "activo. Inicio: $launchers. Vigiladas: ${ajustes.appsVigiladas}")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val e = evento ?: return
        val paquete = e.packageName?.toString() ?: return
        val ahora = ahora()

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> atender(
                registro.enPrimerPlano(paquete, paquete in launchers, paquete in ajustes.appsVigiladas, ahora)
            )

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (paquete in launchers) return
                val vigilada = paquete in ajustes.appsVigiladas
                if (!vigilada) return
                // Un scroll implica que esa app esta adelante, aunque no haya
                // llegado el evento de ventana.
                if (registro.actual?.app != paquete) {
                    atender(registro.enPrimerPlano(paquete, false, true, ahora))
                }
                val completa = esPantallaCompleta(e, paquete)
                if (registro.scroll(paquete, completa, ahora)) {
                    Log.i(TAG, "sesion inicia en $paquete")
                    friccion?.expandirBurbuja(Momento.PRIMER_SWIPE)
                }
            }
        }
    }

    /**
     * Si lo que se deslizo ocupa casi todo el alto de la pantalla: reels, feed.
     * Mensajes y busqueda ocupan menos (cabecera, teclado).
     *
     * Solo mira el tamano de la zona, no lo que muestra. Cada medicion queda en
     * Logcat (filtro "LoveYourself") para calibrar FRACCION_PANTALLA_COMPLETA.
     */
    private fun esPantallaCompleta(e: AccessibilityEvent, paquete: String): Boolean {
        val nodo = e.source
        if (nodo == null) {
            Log.d(TAG, "scroll $paquete sin nodo: no cuenta")
            ultimaFraccion = -1f
            return false
        }
        nodo.getBoundsInScreen(bordes)
        val fraccion = bordes.height().toFloat() / altoPantalla()
        Log.d(
            TAG,
            "scroll $paquete fraccion=%.2f clase=%s id=%s".format(
                fraccion, nodo.className, nodo.viewIdResourceName
            )
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            nodo.recycle()
        }
        ultimaFraccion = fraccion
        return fraccion >= Config.FRACCION_PANTALLA_COMPLETA
    }

    private fun altoPantalla(): Float {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val alto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.height()
        } else {
            resources.displayMetrics.heightPixels
        }
        return alto.coerceAtLeast(1).toFloat()
    }

    /** Guarda cada visita cerrada y, si corresponde, muestra el resumen. */
    private fun atender(cierres: List<Cierre>) {
        for (c in cierres) {
            val v = c.visita
            val dev = Reloj.factor != 1
            val acumulado = AcumuladoSemanal(
                this,
                if (dev) AcumuladoSemanal.ARCHIVO_DEV else AcumuladoSemanal.ARCHIVO
            )
            // Las fechas van en hora real aunque el reloj este acelerado.
            val real = System.currentTimeMillis()
            acumulado.sumar(v.arrastradoMs, real)
            Log.i(
                TAG,
                "cierra ${v.app} por ${c.motivo}: total ${v.totalMs / 1000}s, " +
                    "arrastre ${v.arrastradoMs / 1000}s, ${v.videos} videos"
            )
            if (c.muestraBrief) {
                friccion?.ocultarTodo()
                brief?.mostrar(
                    DatosBrief(
                        cierreMs = real,
                        totalMs = v.totalMs,
                        arrastradoMs = v.arrastradoMs,
                        videos = v.videos,
                        semanaArrastreMs = acumulado.estaSemana(real),
                        etiqueta = if (dev) "dev ×${Reloj.factor}" else ""
                    )
                )
            }
        }
    }

    /** Un tic por segundo: quedarse quieto mirando un reel tambien es estar ahi. */
    private fun refrescar(ahora: Long) {
        val ov = friccion ?: return
        val v = registro.actual
        if (v == null || v.pausada || v.afuera || brief?.visible() == true) {
            ov.ocultarTodo()
            return
        }
        if (ov.decisionVisible()) return

        // Todo responde al tiempo arrastrado, no al tiempo en la app.
        val segundos = v.arrastradoMs / 1000
        val minutos = segundos / 60

        // La burbuja esta siempre que haya una app vigilada adelante.
        ov.mostrarBurbuja(FormatoBrief.reloj(v.arrastradoMs), "%.0f".format(registro.ritmoPorMinuto(ahora)))

        // En modo dev, debajo, una caja con lo que mide cada scroll.
        val dev = ajustes.modoDev
        if (dev) ov.mostrarCaja(EtapaCaja.ESPEJO, lineaDev(v)) else ov.mostrarCaja(EtapaCaja.OCULTO, "")

        if (!v.sesionIniciada) {
            ov.quitarCielo()
            return
        }

        if (v.arrastradoMs >= v.proximaDecisionMs) {
            ov.mostrarDecision(
                minutos = minutos,
                alSeguir = {
                    // No castiga la eleccion: solo aplaza. Preguntar de nuevo al
                    // tiro convertiria la friccion en hostigamiento.
                    v.proximaDecisionMs = v.arrastradoMs + Config.MIN_REPREGUNTA * 60_000L
                    Log.i(TAG, "eligio seguir a los $minutos min")
                },
                alSalir = {
                    Log.i(TAG, "eligio salir a los $minutos min")
                    atender(registro.cerrarActual(Motivo.SALIR, ahora()))
                    // No se puede cerrar Instagram por el usuario; se lo lleva
                    // al inicio, donde queda el resumen esperandolo.
                    startActivity(
                        Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            )
            return
        }

        // El cielo llega a su maximo justo cuando aparece la decision.
        val progreso = v.arrastradoMs / (Config.MIN_DECISION * 60_000f)
        ov.mostrarCielo(progreso * Config.OPACIDAD_MAX_CIELO)
    }

    private fun lineaDev(v: Visita): String {
        val fraccion = if (ultimaFraccion < 0f) "—" else "%.2f".format(ultimaFraccion)
        return "x${Reloj.factor} · ${registro.mascota().name.lowercase()} · " +
            "arr ${FormatoBrief.reloj(v.arrastradoMs)} · bus ${FormatoBrief.reloj(v.buscadoMs)} · " +
            "scroll $fraccion"
    }

    /**
     * La app de inicio del telefono. Entrar ahi (boton de inicio o apps
     * recientes, que en la mayoria de los telefonos vive en el mismo launcher)
     * cierra la sesion.
     *
     * Se usa la de por defecto y no todas las que declaran ser inicio: Ajustes
     * suele declarar una de respaldo, y abrir Ajustes no es salir.
     */
    private fun paquetesDeInicio(): Set<String> {
        val inicio = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val porDefecto = packageManager
            .resolveActivity(inicio, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        if (porDefecto != null && porDefecto != "android") return setOf(porDefecto)
        return packageManager.queryIntentActivities(inicio, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .filter { it != "com.android.settings" }
            .toSet()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tic)
        runCatching { unregisterReceiver(receptorPantalla) }
        friccion?.ocultarTodo()
        brief?.ocultar()
        friccion = null
        brief = null
    }
}
