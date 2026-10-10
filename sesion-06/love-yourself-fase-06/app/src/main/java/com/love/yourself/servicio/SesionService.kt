package com.love.yourself.servicio

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
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.love.yourself.InicioActivity
import com.love.yourself.R
import com.love.yourself.config.Ajustes
import com.love.yourself.config.Config
import com.love.yourself.config.TAG
import com.love.yourself.sesion.AcumuladoSemanal
import com.love.yourself.sesion.Cierre
import com.love.yourself.sesion.EstadoMascota
import com.love.yourself.sesion.Guion
import com.love.yourself.sesion.HistorialSesiones
import com.love.yourself.sesion.Motivo
import com.love.yourself.sesion.RegistroSesion
import com.love.yourself.sesion.RegistroVisitas
import com.love.yourself.sesion.Reloj
import com.love.yourself.sesion.Visita
import com.love.yourself.vistas.CaraProgresiva
import com.love.yourself.vistas.DatosBrief
import com.love.yourself.vistas.Formato
import com.love.yourself.vistas.FrasesCierre
import com.love.yourself.vistas.Momento
import kotlin.math.abs

/**
 * El corazon de la app. Vive encendido una vez activado en la accesibilidad
 * del telefono.
 *
 * Mide sin grabar pantalla: lee la estructura de la pantalla (que zona se
 * desliza y cuanto ocupa), no su contenido. Asi distingue el arrastre (reels,
 * feed a pantalla completa) del uso con intencion (mensajes, busqueda).
 *
 * Capas de friccion, de menos a mas:
 *  - siempre en una app vigilada: la burbuja con tiempo y ritmo
 *  - primer swipe: la burbuja se agranda y aparece la mascota
 *  - a los 10 min en la app: el cielo a pantalla completa, seguir o salir
 *  - al salir: el resumen, y la sesion queda en el historial de Inicio
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
    private var ultimoSalto = -1f
    private var ultimaClase = "—"
    private var eventosScroll = 0
    private var llegoAlInicioMs = 0L
    private var appQueSeFue: String? = null

    // ---- la mascota: que momentos ya dijo en esta visita ----
    private var guion = Guion<Momento>(0)
    private var visitaDelGuion: Visita? = null
    private val ultimoCierre = mutableMapOf<String, Long>()
    private var ritmoAltoDesde = -1L
    private var eligioSeguir = false
    private var ultimoAdelante: String? = null
    private val indices = mutableMapOf<String, Int>()
    private var ultimoIndice = -1

    private val tic = object : Runnable {
        override fun run() {
            // Si algo falla adentro, el reloj igual sigue: sin tic no se cierra
            // ninguna sesion ni se actualiza nada.
            try {
                val ahora = ahora()
                revisarAdelante(ahora)
                atender(registro.tic(ahora))
                refrescar(ahora)
            } catch (e: Exception) {
                DiarioDev.anotar("error en el tic: $e")
            } finally {
                handler.postDelayed(this, 1000L)
            }
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
        // La velocidad del reloj sobrevive a reinstalar la app (modo dev).
        Reloj.cambiarFactor(if (ajustes.modoDev) ajustes.factorReloj else 1)
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
        DiarioDev.anotar("servicio activo · inicio = $launchers")
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val e = evento ?: return
        val paquete = e.packageName?.toString() ?: return
        val ahora = ahora()

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Al volver al inicio, la app que se cierra a veces avisa una vez
                // mas. No es que haya vuelto: abriria una visita fantasma.
                if (paquete == appQueSeFue && SystemClock.uptimeMillis() - llegoAlInicioMs < 1500L) return
                if (paquete != ultimoAdelante && paquete != packageName) {
                    DiarioDev.anotar("ventana: $paquete" + if (esInicio(paquete)) " (inicio)" else "")
                }
                if (esInicio(paquete)) alLlegarAlInicio()
                if (paquete != packageName && paquete !in Config.PAQUETES_DE_SISTEMA) ultimoAdelante = paquete
                if (registro.actual?.app != paquete) indices.clear()
                atender(registro.enPrimerPlano(paquete, esInicio(paquete), paquete in ajustes.appsVigiladas, ahora))
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (esInicio(paquete)) return
                val vigilada = paquete in ajustes.appsVigiladas
                if (!vigilada) return
                // Un scroll implica que esa app esta adelante, aunque no haya
                // llegado el evento de ventana.
                if (registro.actual?.app != paquete) {
                    // Al volver al inicio, la app que se cierra a veces manda un
                    // ultimo scroll. No es que haya vuelto: se ignora.
                    if (SystemClock.uptimeMillis() - llegoAlInicioMs < 1500L) return
                    atender(registro.enPrimerPlano(paquete, false, true, ahora))
                }
                val completa = esPantallaCompleta(e, paquete) && cambioDeElemento(e)
                if (registro.scroll(paquete, completa, ahora)) {
                    DiarioDev.anotar("sesión inicia en $paquete · reloj x${Reloj.factor}")
                    registro.actual?.let { momento(it, Momento.PRIMER_SWIPE, ahora) }
                }
            }
        }
    }

    /**
     * Si el swipe fue sobre contenido a pantalla completa: reels, feed.
     * Mensajes y busqueda ocupan menos (cabecera, teclado).
     *
     * Solo mira tamanos, no lo que se muestra. Dos senales, basta una:
     *  - la zona que se desliza (o un contenedor deslizable que la envuelve)
     *    ocupa casi todo el alto. Instagram a veces avisa desde una vista
     *    interna chica, por eso se revisan los contenedores.
     *  - el salto del scroll es de mas de media pantalla: pasar de un reel al
     *    siguiente mueve una pagina entera.
     *
     * Cada medicion queda en Logcat (filtro "LoveYourself") y en la caja del
     * modo dev, para calibrar FRACCION_PANTALLA_COMPLETA.
     */
    private fun esPantallaCompleta(e: AccessibilityEvent, paquete: String): Boolean {
        eventosScroll++
        val alto = altoPantalla()
        ultimoSalto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) abs(e.scrollDeltaY) / alto else -1f
        val porSalto = ultimoSalto >= Config.FRACCION_SALTO_PAGINA

        val nodo = e.source
        if (nodo == null) {
            ultimaFraccion = -1f
            ultimaClase = "sin nodo"
            Log.d(TAG, "scroll $paquete sin nodo, salto=%.2f".format(ultimoSalto))
            return porSalto
        }
        nodo.getBoundsInScreen(bordes)
        var fraccion = bordes.height() / alto
        ultimaClase = nodo.className?.toString()?.substringAfterLast('.') ?: "?"
        val id = nodo.viewIdResourceName

        // Subir por los contenedores deslizables, hasta 6 niveles.
        var actual = nodo.parent
        var niveles = 0
        while (actual != null && niveles < 6) {
            if (actual.isScrollable) {
                actual.getBoundsInScreen(bordes)
                fraccion = maxOf(fraccion, bordes.height() / alto)
            }
            val siguiente = actual.parent
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                actual.recycle()
            }
            actual = siguiente
            niveles++
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            nodo.recycle()
        }

        ultimaFraccion = fraccion
        Log.d(
            TAG,
            "scroll $paquete fraccion=%.2f salto=%.2f clase=%s id=%s".format(fraccion, ultimoSalto, ultimaClase, id)
        )
        return fraccion >= Config.FRACCION_PANTALLA_COMPLETA || porSalto
    }

    /**
     * Un swipe es pasar a otro reel (u otro post), no cualquier movimiento: dentro
     * de un reel tambien se deslizan textos y listas. Si Android dice que
     * elemento quedo arriba (fromIndex), se cuenta solo cuando cambia. Si no lo
     * dice, se cuenta como antes: cada gesto.
     */
    private fun cambioDeElemento(e: AccessibilityEvent): Boolean {
        val indice = e.fromIndex
        ultimoIndice = indice
        if (indice < 0) return true
        val clave = e.className?.toString() ?: "?"
        val anterior = indices.put(clave, indice)
        // Un salto de pagina entera tambien es un swipe, cambie o no el indice.
        if (ultimoSalto >= Config.FRACCION_SALTO_PAGINA) return true
        // El primer aviso de una lista solo dice donde esta: todavia no hubo swipe.
        return anterior != null && anterior != indice
    }

    private fun alLlegarAlInicio() {
        llegoAlInicioMs = SystemClock.uptimeMillis()
        registro.actual?.app?.let { appQueSeFue = it }
    }

    /**
     * Respaldo por si Android no avisa el cambio de app (pasa con el gesto de
     * inicio en algunos telefonos): cada segundo se mira que app esta adelante.
     */
    private fun revisarAdelante(ahora: Long) {
        val raiz = runCatching { rootInActiveWindow }.getOrNull() ?: return
        val paquete = raiz.packageName?.toString()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            raiz.recycle()
        }
        if (paquete == null || paquete == ultimoAdelante || paquete == packageName) return
        ultimoAdelante = paquete
        if (paquete in Config.PAQUETES_DE_SISTEMA) return
        if (esInicio(paquete)) alLlegarAlInicio()
        if (registro.actual?.app == paquete) return
        DiarioDev.anotar("adelante (revisión): $paquete" + if (esInicio(paquete)) " (inicio)" else "")
        atender(registro.enPrimerPlano(paquete, esInicio(paquete), paquete in ajustes.appsVigiladas, ahora))
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

    /**
     * Guarda cada visita cerrada (en la semana y en el historial de Inicio)
     * y, si corresponde, muestra el resumen.
     */
    private fun atender(cierres: List<Cierre>) {
        for (c in cierres) {
            val v = c.visita
            val dev = Reloj.factor != 1
            val acumulado = AcumuladoSemanal(
                this,
                if (dev) AcumuladoSemanal.ARCHIVO_DEV else AcumuladoSemanal.ARCHIVO
            )
            val historial = HistorialSesiones(
                this,
                if (dev) HistorialSesiones.ARCHIVO_DEV else HistorialSesiones.ARCHIVO
            )
            // Las fechas van en hora real aunque el reloj este acelerado.
            val real = System.currentTimeMillis()
            acumulado.sumar(v.arrastradoMs, real)
            // Las visitas sin arrastre tambien quedan, si fueron mas que mirar
            // una notificacion: son el uso con intencion.
            if (v.sesionIniciada || v.totalMs >= Config.MIN_VISITA_HISTORIAL_S * 1000L) {
                historial.agregar(
                    RegistroSesion(
                        finMs = real,
                        app = v.app,
                        totalMs = v.totalMs,
                        arrastradoMs = v.arrastradoMs,
                        videos = v.videos,
                        motivo = c.motivo,
                        cielos = v.cielos,
                        seguir = v.seguir
                    )
                )
            }
            DiarioDev.anotar(
                "cierra ${v.app} por ${c.motivo} · x${Reloj.factor} · arrastre ${v.arrastradoMs / 1000}s · " +
                    "iniciada ${v.sesionIniciada} · resumen ${c.muestraBrief}"
            )
            if (v.sesionIniciada) ultimoCierre[v.app] = c.cierreMs
            if (ajustes.modoDev) avisoDev(c)
            if (c.muestraBrief) {
                friccion?.ocultarTodo()
                // Aliviado si salio antes del cielo; frito si llego a verlo.
                val frito = vioElCielo(c)
                val semanaPasada = acumulado.semanaPasadaHastaHoy(real)
                val datos = DatosBrief(
                    cierreMs = real,
                    totalMs = v.totalMs,
                    arrastradoMs = v.arrastradoMs,
                    videos = v.videos,
                    semanaArrastreMs = acumulado.estaSemana(real),
                    semanaPasadaMs = if (semanaPasada > 0) semanaPasada else -1L,
                    etiqueta = if (dev) "dev ×${Reloj.factor}" else "",
                    actividad = ajustes.actividad,
                    mascota = if (frito) R.drawable.brain_f else R.drawable.brain_relief,
                    fraseMascota = (if (frito) FrasesCierre.FRITO else FrasesCierre.ALIVIO).random()
                )
                runCatching {
                    brief?.mostrar(
                        datos,
                        alVerDetalle = { abrirInicio() },
                        alResponder = { respuesta ->
                            historial.responder(real, respuesta)
                            DiarioDev.anotar("recuerdas: $respuesta")
                        }
                    )
                }.onFailure {
                    Log.e(TAG, "no se pudo mostrar el resumen", it)
                    DiarioDev.anotar("resumen FALLÓ: ${it.message}")
                    if (ajustes.modoDev) Toast.makeText(this, "resumen falló: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Modo dev: cada vez que se cierra una visita, dice por que y si hubo
     * resumen. Asi se diagnostica en el celular, sin Logcat.
     */
    private fun avisoDev(c: Cierre) {
        val v = c.visita
        val porQueNo = when {
            c.muestraBrief -> "resumen: sí"
            c.motivo == Motivo.OTRA_APP -> "sin resumen: saliste a otra app (inicio reconocido: $launchers)"
            !v.sesionIniciada -> "sin resumen: no hubo swipe a pantalla completa"
            else -> "sin resumen: menos de ${Config.MIN_ARRASTRE_BRIEF_S} s de arrastre"
        }
        Toast.makeText(
            this,
            "cierra por ${c.motivo} · arrastre ${Formato.reloj(v.arrastradoMs)}\n$porQueNo",
            Toast.LENGTH_LONG
        ).show()
    }

    /** Un tic por segundo: quedarse quieto mirando un reel tambien es estar ahi. */
    private fun refrescar(ahora: Long) {
        val ov = friccion ?: return
        val v = registro.actual
        if (v == null || v.pausada || v.afuera || brief?.visible() == true) {
            ov.ocultarTodo()
            return
        }
        if (ov.cieloVisible()) return

        // La burbuja esta siempre que haya una app vigilada adelante: tiempo en
        // la app y ritmo. El arrastrado queda para el resumen.
        ov.mostrarBurbuja(
            Formato.reloj(v.totalMs),
            "%.0f".format(registro.ritmoPorMinuto(ahora)),
            CaraProgresiva.para(v.sesionIniciada, v.totalMs)
        )

        // En modo dev, debajo, una caja con lo que mide cada scroll.
        if (ajustes.modoDev) ov.mostrarCaja(lineaDev(v)) else ov.quitarCaja()

        // El cielo cuenta tiempo en la app, pero solo si hubo arrastre.
        val tocaCielo = v.sesionIniciada && v.totalMs >= v.proximoCieloMs
        if (!tocaCielo) {
            hablaLaMascota(v, ahora)
            return
        }
        // Si la mascota esta hablando, el cielo espera a que termine: no la
        // tapa a mitad de frase. Mientras tanto no empieza ningun momento nuevo.
        if (ov.burbujaExpandida()) return

        val minutos = v.totalMs / 60_000
        v.cielos++
        DiarioDev.anotar("cielo a los $minutos min")
        ov.mostrarCielo(
            minutos = minutos,
            cara = CaraProgresiva.para(true, v.totalMs),
            alSeguir = {
                // No castiga la eleccion: solo aplaza. Preguntar de nuevo al
                // tiro convertiria la friccion en hostigamiento.
                v.proximoCieloMs = v.totalMs + Config.MIN_REPREGUNTA * 60_000L
                v.seguir++
                eligioSeguir = true
                DiarioDev.anotar("eligió seguir a los $minutos min")
            },
            alSalir = {
                DiarioDev.anotar("eligió salir a los $minutos min")
                atender(registro.cerrarActual(Motivo.SALIR, ahora()))
                // No se puede cerrar Instagram por el usuario; se lo lleva al
                // inicio, donde queda el resumen esperandolo.
                startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        )
    }

    // ---- la mascota ----

    /** El guion de esta visita. Una visita nueva empieza uno nuevo. */
    private fun guionDe(v: Visita, ahora: Long): Guion<Momento> {
        if (v === visitaDelGuion) return guion
        visitaDelGuion = v
        // La pausa va en tiempo del reloj: con el reloj acelerado tambien se
        // acorta, para poder ver todos los momentos en una prueba corta.
        guion = Guion(Config.PAUSA_ENTRE_MOMENTOS_S * 1000L)
        ritmoAltoDesde = -1L
        eligioSeguir = false
        // Volver a abrir la app al poco rato de haberla cerrado.
        val antes = ultimoCierre[v.app]
        if (antes != null && ahora - antes < Config.MIN_VOLVISTE * 60_000L * Reloj.factor) {
            momento(v, Momento.VOLVISTE, ahora)
        }
        return guion
    }

    private fun momento(v: Visita, m: Momento, ahora: Long, sinPausa: Boolean = false) {
        if (!guionDe(v, ahora).pedir(m, ahora, sinPausa)) return
        friccion?.expandirBurbuja(m)
        DiarioDev.anotar("mascota: ${m.name.lowercase()}")
    }

    /**
     * Cada tic, si corresponde algun momento. El guion se encarga de que cada
     * uno salga una sola vez y con pausa entre ellos.
     */
    private fun hablaLaMascota(v: Visita, ahora: Long) {
        guionDe(v, ahora)
        if (eligioSeguir) {
            eligioSeguir = false
            momento(v, Momento.TRAS_SEGUIR, ahora, sinPausa = true)
            return
        }
        if (!v.sesionIniciada) return

        // Ritmo alto sostenido: cuenta desde que empezo a estar alto.
        if (registro.ritmoPorMinuto(ahora) >= Config.RITMO_ALTO) {
            if (ritmoAltoDesde < 0) ritmoAltoDesde = ahora
        } else {
            ritmoAltoDesde = -1L
        }
        val ritmoAlto = ritmoAltoDesde >= 0 && ahora - ritmoAltoDesde >= Config.RITMO_ALTO_S * 1000L * Reloj.factor
        val primerCielo = v.proximoCieloMs == Config.MIN_CIELO * 60_000L

        when {
            primerCielo && v.totalMs >= v.proximoCieloMs - 60_000L -> momento(v, Momento.ANTES_DEL_CIELO, ahora)
            ritmoAlto -> momento(v, Momento.RITMO_ALTO, ahora)
            registro.mascota() == EstadoMascota.DETENIDA -> momento(v, Momento.ARRASTRE_SEGUIDO, ahora)
            v.totalMs >= Config.MIN_SUENO * 60_000L -> momento(v, Momento.CINCO_MIN, ahora)
        }
    }

    /** Si la visita llego a ver el cielo, eligiera lo que eligiera. */
    private fun vioElCielo(c: Cierre) = c.motivo == Motivo.SALIR || c.visita.cielos > 0

    /**
     * "ver detalle" en el resumen: abre Inicio. Un servicio de accesibilidad
     * puede abrir pantallas aunque la app no este adelante.
     */
    private fun abrirInicio() {
        runCatching {
            startActivity(
                Intent(this, InicioActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }.onFailure { DiarioDev.anotar("no se pudo abrir Inicio: ${it.message}") }
    }

    private fun lineaDev(v: Visita): String {
        val fraccion = if (ultimaFraccion < 0f) "—" else "%.2f".format(ultimaFraccion)
        val salto = if (ultimoSalto < 0f) "—" else "%.2f".format(ultimoSalto)
        return "x${Reloj.factor} · arr ${Formato.reloj(v.arrastradoMs)} · " +
            "bus ${Formato.reloj(v.buscadoMs)} · ${v.videos} videos\n" +
            "scroll $fraccion · salto $salto · idx $ultimoIndice · $ultimaClase · ev $eventosScroll"
    }

    /**
     * La app de inicio del telefono. Entrar ahi (boton de inicio o apps
     * recientes, que en la mayoria de los telefonos vive en el mismo launcher)
     * cierra la sesion.
     *
     * Todas las que declaran ser inicio, menos Ajustes: suele declarar una de
     * respaldo, y abrir Ajustes no es salir.
     */
    private fun paquetesDeInicio(): Set<String> {
        val inicio = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val porDefecto = packageManager
            .resolveActivity(inicio, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        val todas = packageManager.queryIntentActivities(inicio, 0).map { it.activityInfo.packageName }
        return (todas + listOfNotNull(porDefecto))
            .filter { it != "android" && it != "com.android.settings" }
            .toSet()
    }

    /**
     * Si el paquete es la pantalla de inicio. Ademas de lo que declara el
     * sistema, por nombre: algunos telefonos no dejan preguntarlo, y confundir
     * el inicio con otra app hace que la sesion se cierre sin resumen.
     */
    private fun esInicio(paquete: String) =
        paquete in launchers || "launcher" in paquete || paquete.endsWith(".home")

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
