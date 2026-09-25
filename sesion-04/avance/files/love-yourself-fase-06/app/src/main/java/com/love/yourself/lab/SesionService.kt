package com.love.yourself.lab

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * El corazon de la app. Vive encendido una vez activado en Ajustes.
 *
 * Mide sin grabar pantalla: tiempo, swipes y como se sale. El conteo de cortes
 * exige un dialogo de consentimiento en cada medicion y nunca podria arrancar
 * solo; una intervencion que hay que encender a mano no sirve para quien se
 * pierde cuarenta minutos sin darse cuenta.
 *
 * Tres momentos de friccion:
 *  - durante: la caja (espejo, y presencia a los 5 min)
 *  - a los 15 min: la pantalla de decision
 *  - al salir: el resumen
 */
class SesionService : AccessibilityService() {

    private lateinit var registro: RegistroVisitas
    private lateinit var acumulado: AcumuladoSemanal
    private var friccion: OverlayFriccion? = null
    private var brief: OverlayBrief? = null
    private var launchers: Set<String> = emptySet()
    private val reloj = Handler(Looper.getMainLooper())

    private val tic = object : Runnable {
        override fun run() {
            val ahora = System.currentTimeMillis()
            atender(registro.tic(ahora))
            refrescar(ahora)
            reloj.postDelayed(this, 1000L)
        }
    }

    /**
     * Bloquear el celular pausa la sesion. Si la pausa dura mas de 3 minutos,
     * al volver se cierra y aparece el resumen.
     */
    private val receptorPantalla = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val ahora = System.currentTimeMillis()
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        registro = RegistroVisitas(packageName)
        acumulado = AcumuladoSemanal(this)
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

        reloj.post(tic)
        Log.i(TAG, "activo. Pantalla de inicio: $launchers")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val e = evento ?: return
        val paquete = e.packageName?.toString() ?: return
        val ahora = System.currentTimeMillis()

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                atender(registro.enPrimerPlano(paquete, paquete in launchers, ahora))

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (paquete in launchers) return
                // Un scroll implica que esa app esta adelante, aunque no haya
                // llegado el evento de ventana.
                if (registro.actual?.app != paquete) {
                    atender(registro.enPrimerPlano(paquete, false, ahora))
                }
                if (registro.scroll(paquete, ahora)) {
                    Log.i(TAG, "sesion inicia en $paquete")
                }
            }
        }
    }

    /** Guarda cada visita cerrada y, si corresponde, muestra el resumen. */
    private fun atender(cierres: List<Cierre>) {
        for (c in cierres) {
            val v = c.visita
            acumulado.sumar(v.arrastradoMs, c.cierreMs)
            Log.i(
                TAG,
                "cierra ${v.app} por ${c.motivo}: total ${v.totalMs / 1000}s, " +
                    "arrastre ${v.arrastradoMs / 1000}s, ${v.videos} videos"
            )
            if (c.muestraBrief) {
                friccion?.ocultarTodo()
                brief?.mostrar(
                    DatosBrief(
                        cierreMs = c.cierreMs,
                        totalMs = v.totalMs,
                        arrastradoMs = v.arrastradoMs,
                        videos = v.videos,
                        semanaArrastreMs = acumulado.estaSemana(c.cierreMs)
                    )
                )
            }
        }
    }

    /** Un tic por segundo: quedarse quieto mirando un reel tambien es estar ahi. */
    private fun refrescar(ahora: Long) {
        val ov = friccion ?: return
        val v = registro.actual
        if (v == null || !v.sesionIniciada || v.pausada || v.afuera || brief?.visible() == true) {
            ov.mostrarCaja(OverlayFriccion.Etapa.OCULTO, 0, 0.0)
            return
        }

        val segundos = v.totalMs / 1000
        val minutos = segundos / 60

        if (v.totalMs >= v.proximaDecisionMs && !ov.decisionVisible()) {
            ov.mostrarCaja(OverlayFriccion.Etapa.OCULTO, 0, 0.0)
            ov.mostrarDecision(
                minutos = minutos,
                alSeguir = {
                    // No castiga la eleccion: solo aplaza. Preguntar de nuevo al
                    // tiro convertiria la friccion en hostigamiento.
                    v.proximaDecisionMs = v.totalMs + Config.MIN_REPREGUNTA * 60_000L
                    Log.i(TAG, "eligio seguir a los $minutos min")
                },
                alSalir = {
                    Log.i(TAG, "eligio salir a los $minutos min")
                    atender(registro.cerrarActual(Motivo.SALIR, System.currentTimeMillis()))
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
        if (ov.decisionVisible()) return

        val etapa = if (minutos >= Config.MIN_PRESENCIA) {
            OverlayFriccion.Etapa.PRESENCIA
        } else {
            OverlayFriccion.Etapa.ESPEJO
        }
        ov.mostrarCaja(etapa, segundos, registro.ritmoPorMinuto(ahora))
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
        reloj.removeCallbacks(tic)
        runCatching { unregisterReceiver(receptorPantalla) }
        friccion?.ocultarTodo()
        brief?.ocultar()
        friccion = null
        brief = null
    }
}
