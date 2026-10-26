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
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
import com.love.yourself.vistas.GraficoSesiones
import com.love.yourself.vistas.Momento
import com.love.yourself.vistas.SesionDelDia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
 *  - en arrastre: la burbuja con el tiempo y los videos vistos
 *  - primer swipe: la burbuja se agranda y aparece la mascota
 *  - al tercer swipe de arrastre: el aviso arriba con un boton "Salir"; seguir
 *    deslizando lo quita
 *  - a los 10 min en la app, si se esta en arrastre: el cielo a pantalla completa
 *  - al salir: el resumen, y la sesion queda en el historial de Inicio
 *
 * Los primeros DIAS_LINEA_BASE dias del estudio no hay friccion: solo se registra.
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

    // ---- el aviso: cuantos swipes de arrastre lleva y si esta en pantalla ----
    private var avisoDesdeMs = 0L
    private var swipesArrastre = 0
    private var avisoDisponible = true
    private var visitaDelAviso: Visita? = null
    private var ultimaFase: Boolean? = null
    // ---- la mascota asomandose: por que lado fue la ultima vez y hasta cuando la cara chica sale de la burbuja ----
    private var asomoDerecha = false
    private var caraFueraHasta = 0L

    // ---- arrastre segun el clasificador, por visita ----
    private var visitaContada: Visita? = null
    private var arrastreClasificadoMs = 0L
    private var ultimoConteo = 0L

    /** Desde cuando no se esta en arrastre (uptime). 0 = en arrastre. */
    private var fueraDeArrastreDesde = 0L

    // ---- las preguntas al cerrar ----
    private var preguntas: OverlayPreguntas? = null
    private class Pendiente(
        val app: String,
        val motivo: Motivo,
        val cierre: String,
        val arrastradoS: Long,
        val creadaReal: Long,
        /** Cuanto duro la sesion entera. */
        val totalS: Long,
        /** "regular" (pasaron MIN_ENTRE_PREGUNTAS) o "sesion_larga" (aparecio solo por durar mucho). */
        val porQue: String
    )
    private var pendiente: Pendiente? = null

    /** Cuando aparecio el resumen o las preguntas (uptime): los eventos justo despues son del mismo cierre. */
    private var pantallaCierreDesdeMs = 0L

    /** Cuando se anoto el ultimo latido (hora real). */
    private var ultimoLatido = 0L

    /** El ultimo swipe que conto el aviso: un gesto manda varios eventos seguidos. */
    private var ultimoDeslizarMs = 0L

    private val tic = object : Runnable {
        override fun run() {
            // Si algo falla adentro, el reloj igual sigue: sin tic no se cierra
            // ninguna sesion ni se actualiza nada.
            try {
                val ahora = ahora()
                // Sin eventos (mirar un reel quieto, tocar en vez de deslizar) la
                // clasificacion quedaria vieja: mientras se esta en Instagram, se
                // lee igual cada pocos segundos.
                val enInstagram = registro.actual?.let { it.app == PROBE2_PAQUETE && !it.pausada && !it.afuera } == true
                if (PROBE2_ENABLED && enInstagram) sonda()
                latido()
                contarArrastre(ahora)
                revisarAdelante(ahora)
                atender(registro.tic(ahora))
                refrescar(ahora)
                revisarAviso()
                revisarFase()
                revisarPregunta()
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
            // Para leer los huecos del registro: un hueco que empieza con la
            // pantalla apagada es el telefono durmiendo; uno sin eso, el servicio caido.
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> RegistroEstudio.anotar("pantalla", "apagada")
                Intent.ACTION_SCREEN_ON -> RegistroEstudio.anotar("pantalla", "encendida")
            }
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
        preguntas = OverlayPreguntas(this)
        launchers = paquetesDeInicio()
        RegistroEstudio.iniciar(this)
        // Si el sistema apaga el servicio y lo vuelve a prender, aca se ve el hueco.
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
        RegistroEstudio.anotar("servicio_inicio", version, Build.MODEL, Build.VERSION.SDK_INT, sinAhorroDeBateria())
        if (ajustes.inicioEstudioMs == 0L) {
            ajustes.inicioEstudioMs = System.currentTimeMillis()
            RegistroEstudio.anotar("estudio_inicio", Config.DIAS_LINEA_BASE)
        }

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
        // Tambien fuera de Instagram mientras haya un segmento abierto: asi se cierra al salir.
        if (PROBE2_ENABLED && paquete == PROBE2_PAQUETE) sonda()
        else if (PROBE2_ENABLED && segmentoAbierto) sonda(forzar = true)
        val ahora = ahora()

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Al volver al inicio, la app que se cierra a veces avisa una vez
                // mas. No es que haya vuelto: abriria una visita fantasma.
                if (paquete == appQueSeFue && SystemClock.uptimeMillis() - llegoAlInicioMs < 1500L) return
                // El resumen y las preguntas nunca quedan encima de otra app: si la
                // persona abre otra cosa sin cerrarlos, se van.
                if (paquete != packageName && paquete !in Config.PAQUETES_DE_SISTEMA && !esInicio(paquete) &&
                    SystemClock.uptimeMillis() - pantallaCierreDesdeMs > 2_000L
                ) {
                    ocultarPantallasDeCierre(paquete)
                }
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
                if (completa && paquete == PROBE2_PAQUETE) alDeslizar()
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
            val arrastre = arrastreDe(v)
            acumulado.sumar(arrastre, real)
            // Las visitas sin arrastre tambien quedan, si fueron mas que mirar
            // una notificacion: son el uso con intencion.
            if (v.sesionIniciada || v.totalMs >= Config.MIN_VISITA_HISTORIAL_S * 1000L) {
                historial.agregar(
                    RegistroSesion(
                        finMs = real,
                        app = v.app,
                        totalMs = v.totalMs,
                        arrastradoMs = arrastre,
                        videos = v.videos,
                        motivo = c.motivo,
                        cielos = v.cielos,
                        seguir = v.seguir
                    )
                )
            }
            // El resumen aparece con mas de un minuto de ARRASTRE segun el clasificador
            // (no el calculo antiguo por tamaño del scroll). Nunca al irse a otra app
            // (seria encima de ella) ni tras un bloqueo (la sesion se cierra al
            // desbloquear, quizas horas despues): la misma regla que las preguntas.
            val muestraBrief = if (PROBE2_ENABLED) {
                c.motivo != Motivo.OTRA_APP && c.motivo != Motivo.BLOQUEO &&
                    arrastre >= Config.MIN_ARRASTRE_BRIEF_S * 1000L
            } else {
                c.muestraBrief
            }
            DiarioDev.anotar(
                "cierra ${v.app} por ${c.motivo} · x${Reloj.factor} · arrastre ${arrastre / 1000}s · " +
                    "iniciada ${v.sesionIniciada} · resumen $muestraBrief"
            )
            RegistroEstudio.anotar(
                "visita", v.app, c.motivo, v.totalMs / 1000, v.arrastradoMs / 1000, v.cielos, v.seguir, fase(),
                arrastre / 1000
            )
            if (v.sesionIniciada) ultimoCierre[v.app] = c.cierreMs
            // Las preguntas van en las dos fases, solo para Instagram: como mucho una
            // cada MIN_ENTRE_PREGUNTAS, salvo que la sesion haya sido larga.
            val pasoElTiempo = real - ajustes.ultimaPreguntaMs >= Config.MIN_ENTRE_PREGUNTAS * 60_000L
            val larga = v.totalMs >= Config.SESION_LARGA_MIN * 60_000L
            val tocaPregunta = v.app == PROBE2_PAQUETE && v.totalMs >= Config.MIN_VISITA_PREGUNTA_S * 1000L &&
                (pasoElTiempo || larga)
            // Nunca tras un bloqueo: la sesion se cierra al desbloquear, quizas
            // horas despues, y la respuesta ya no seria del momento.
            if (tocaPregunta && c.motivo == Motivo.BLOQUEO) {
                RegistroEstudio.anotar("pregunta_descartada", v.app, c.motivo, "bloqueo", v.totalMs / 1000, arrastre / 1000)
            } else if (tocaPregunta) {
                // Formato propio: el de la sonda vive en otro hilo.
                val cierre = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(real))
                pendiente = Pendiente(
                    v.app, c.motivo, cierre, arrastre / 1000, real,
                    totalS = v.totalMs / 1000,
                    porQue = if (pasoElTiempo) "regular" else "sesion_larga"
                )
            }
            if (ajustes.modoDev) avisoDev(c, arrastre, muestraBrief)
            if (muestraBrief && friccionActiva()) {
                friccion?.ocultarTodo()
                // Aliviado si salio antes del cielo; frito si llego a verlo.
                val frito = vioElCielo(c)
                // Hoy: las sesiones desde medianoche (esta incluida: ya se guardo arriba).
                val medianoche = java.util.Calendar.getInstance().apply {
                    timeInMillis = real
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val deHoy = historial.ultimas().filter { it.finMs >= medianoche }
                val datos = DatosBrief(
                    cierreMs = real,
                    totalMs = v.totalMs,
                    arrastreMs = arrastre,
                    // La mas nueva primero en el historial; el grafico va de izquierda a derecha.
                    sesionesHoy = deHoy.take(GraficoSesiones.COLUMNAS).reversed()
                        .map { SesionDelDia(it.finMs - it.totalMs / Reloj.factor, it.totalMs) },
                    totalHoyMs = deHoy.sumOf { it.totalMs },
                    etiqueta = if (dev) "dev ×${Reloj.factor}" else "",
                    mascota = if (frito) R.drawable.brain_f else R.drawable.brain_relief,
                    fraseMascota = (if (frito) FrasesCierre.FRITO else FrasesCierre.ALIVIO).random()
                )
                runCatching {
                    brief?.mostrar(datos, alVerDetalle = { abrirInicio() })
                    pantallaCierreDesdeMs = SystemClock.uptimeMillis()
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
    private fun avisoDev(c: Cierre, arrastre: Long, muestraBrief: Boolean) {
        val porQueNo = when {
            muestraBrief -> "resumen: sí"
            c.motivo == Motivo.OTRA_APP -> "sin resumen: saliste a otra app (inicio reconocido: $launchers)"
            c.motivo == Motivo.BLOQUEO -> "sin resumen: se cerró por bloqueo"
            else -> "sin resumen: menos de ${Config.MIN_ARRASTRE_BRIEF_S} s de arrastre"
        }
        Toast.makeText(
            this,
            "cierra por ${c.motivo} · arrastre ${Formato.reloj(arrastre)}\n$porQueNo",
            Toast.LENGTH_LONG
        ).show()
    }

    /** Un tic por segundo: quedarse quieto mirando un reel tambien es estar ahi. */
    private fun refrescar(ahora: Long) {
        val ov = friccion ?: return
        // Linea base: la app solo registra.
        if (!friccionActiva()) {
            ov.ocultarTodo()
            return
        }
        val v = registro.actual
        if (v == null || v.pausada || v.afuera || brief?.visible() == true) {
            ov.ocultarTodo()
            return
        }
        if (ov.cieloVisible()) return

        // La burbuja y la mascota solo en arrastre: en un DM o un perfil no hay
        // nada encima. Se espera un poco antes de quitarla, para que un cambio
        // de pantalla a medias no la haga parpadear.
        if (PROBE2_ENABLED && claseActual != Clase.ARRASTRE) {
            val ahoraMs = SystemClock.uptimeMillis()
            if (fueraDeArrastreDesde == 0L) fueraDeArrastreDesde = ahoraMs
            if (ahoraMs - fueraDeArrastreDesde >= Config.ESPERA_OCULTAR_BURBUJA_MS) {
                ov.quitarBurbuja()
                ov.quitarMascota()
            }
            if (ajustes.modoDev) ov.mostrarCaja(lineaDev(v)) else ov.quitarCaja()
            return
        }
        fueraDeArrastreDesde = 0L

        // Tiempo en la app y videos vistos. El arrastrado queda para el resumen.
        // Mientras la mascota se asoma, la cara chica no esta en la burbuja: salio por ahi.
        val caraFuera = ov.mascotaAsomada() || SystemClock.uptimeMillis() < caraFueraHasta
        ov.mostrarBurbuja(
            Formato.reloj(v.totalMs),
            v.videos.toString(),
            if (caraFuera) 0 else CaraProgresiva.para(v.sesionIniciada, v.totalMs)
        )

        // En modo dev, debajo, una caja con lo que mide cada scroll.
        if (ajustes.modoDev) ov.mostrarCaja(lineaDev(v)) else ov.quitarCaja()

        // El cielo cuenta tiempo en la app, pero solo si hubo arrastre.
        // Y solo en un segmento de ARRASTRE: no interrumpe un DM ni un perfil.
        val tocaCielo = v.sesionIniciada && v.totalMs >= v.proximoCieloMs &&
            (!PROBE2_ENABLED || claseActual == Clase.ARRASTRE)
        if (!tocaCielo) {
            hablaLaMascota(v, ahora)
            return
        }
        // Si la mascota esta hablando o asomada, el cielo espera a que termine.
        // Mientras tanto no empieza ningun momento nuevo.
        if (ov.burbujaExpandida() || ov.mascotaAsomada()) return

        val minutos = v.totalMs / 60_000
        v.cielos++
        DiarioDev.anotar("cielo a los $minutos min")
        RegistroEstudio.anotar("cielo_mostrado", v.app, minutos)
        cancelarAviso("cielo")
        // Despues del cielo el aviso vuelve a estar disponible.
        avisoDisponible = true
        swipesArrastre = 0
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
                RegistroEstudio.anotar("cielo_seguir", v.app, minutos)
            },
            alSalir = {
                DiarioDev.anotar("eligió salir a los $minutos min")
                RegistroEstudio.anotar("cielo_salir", v.app, minutos)
                salirAlInicio()
            }
        )
    }

    /**
     * Cierra la visita y lleva al inicio del telefono, donde queda el resumen
     * esperando. No se puede cerrar Instagram por el usuario.
     */
    private fun salirAlInicio() {
        atender(registro.cerrarActual(Motivo.SALIR, ahora()))
        // La accion "inicio" de accesibilidad es como apretar el boton: no depende
        // de que Android deje abrir pantallas desde segundo plano.
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) {
            runCatching {
                startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }.onFailure { DiarioDev.anotar("no se pudo ir al inicio: ${it.message}") }
        }
    }

    // ---- arrastre segun el clasificador ----

    /**
     * Cada tic suma el tiempo de la visita actual que paso en ARRASTRE. Es lo
     * que muestran el resumen e Inicio: asi dicen lo mismo que el registro
     * del estudio, y no el calculo antiguo por tamaño del scroll.
     */
    private fun contarArrastre(ahora: Long) {
        val v = registro.actual
        val delta = (ahora - ultimoConteo).coerceIn(0L, 5_000L * Reloj.factor)
        ultimoConteo = ahora
        if (v == null || v.pausada || v.afuera) return
        if (v !== visitaContada) {
            visitaContada = v
            arrastreClasificadoMs = 0L
            return
        }
        if (claseActual == Clase.ARRASTRE) arrastreClasificadoMs += delta
    }

    /** El arrastre de una visita que se cierra: el del clasificador si esta encendido. */
    private fun arrastreDe(v: Visita): Long = when {
        !PROBE2_ENABLED -> v.arrastradoMs
        v === visitaContada -> arrastreClasificadoMs.coerceAtMost(v.totalMs)
        else -> 0L
    }

    /** Cada MIN_LATIDO minutos, una linea que dice que el servicio sigue vivo y en que estaba. */
    private fun latido() {
        val real = System.currentTimeMillis()
        if (real - ultimoLatido < Config.MIN_LATIDO * 60_000L) return
        ultimoLatido = real
        RegistroEstudio.anotar("latido", fase(), registro.actual?.app ?: "", claseActual, sinAhorroDeBateria())
    }

    /** Si Android le dejo de aplicar el ahorro de bateria (en Vivo y otras marcas puede decir no aunque este apagado). */
    private fun sinAhorroDeBateria(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
    }

    // ---- fase del estudio ----

    private fun friccionActiva() = ajustes.friccionActiva(System.currentTimeMillis())

    private fun fase() = if (friccionActiva()) "intervencion" else "linea_base"

    /** Anota cuando cambia de linea base a intervencion (o al activar el modo dev). */
    private fun revisarFase() {
        val activa = friccionActiva()
        if (activa == ultimaFase) return
        ultimaFase = activa
        RegistroEstudio.anotar("fase", fase())
        DiarioDev.anotar("fase: ${fase()}")
    }

    // ---- las preguntas ----

    /**
     * Cada tic: si hay una pregunta pendiente, la muestra solo en la pantalla de
     * inicio del telefono (nunca encima de otra app) y cuando no hay otra
     * pantalla nuestra encima (el resumen va primero). Si la persona vuelve a
     * Instagram antes, o no llega al inicio a tiempo, se descarta.
     */
    private fun revisarPregunta() {
        val p = pendiente ?: return
        val ov = preguntas ?: return
        val real = System.currentTimeMillis()
        if (real - p.creadaReal > Config.VENCE_PREGUNTA_MIN * 60_000L) {
            pendiente = null
            RegistroEstudio.anotar("pregunta_vencida", p.app, p.motivo, p.cierre, "", p.porQue, p.totalS, p.arrastradoS)
            return
        }
        if (ov.visible() || brief?.visible() == true) return
        val actual = registro.actual
        if (actual != null && actual.app == p.app && !actual.afuera) {
            // Volvio a Instagram: es otra sesion, y esta pregunta ya no le corresponde.
            pendiente = null
            RegistroEstudio.anotar("pregunta_descartada", p.app, p.motivo, "volvio_a_instagram", p.totalS, p.arrastradoS)
            return
        }
        // Nunca encima de otra app: espera a que este en la pantalla de inicio.
        val adelante = ultimoAdelante ?: return
        if (!esInicio(adelante)) return
        pendiente = null
        ajustes.ultimaPreguntaMs = real
        val fase = fase()
        RegistroEstudio.anotar("pregunta_mostrada", p.app, p.motivo, p.cierre, fase, p.porQue, p.totalS, p.arrastradoS)
        pantallaCierreDesdeMs = SystemClock.uptimeMillis()
        runCatching {
            ov.mostrar(
                alResponder = { motivacion, sentido, control ->
                    RegistroEstudio.anotar(
                        "respuesta", p.app, p.motivo, p.cierre, p.arrastradoS, fase, motivacion, sentido, control,
                        p.porQue, p.totalS
                    )
                    DiarioDev.anotar("respuesta: $motivacion · sentido $sentido · control $control")
                },
                alSaltar = {
                    RegistroEstudio.anotar("pregunta_saltada", p.app, p.motivo, p.cierre, fase, p.porQue, p.totalS, p.arrastradoS)
                    DiarioDev.anotar("pregunta saltada")
                }
            )
        }.onFailure { DiarioDev.anotar("preguntas FALLÓ: ${it.message}") }
    }

    /** Quita el resumen y las preguntas si quedaron abiertos y la persona se fue a otra app. */
    private fun ocultarPantallasDeCierre(otraApp: String) {
        if (preguntas?.visible() == true) {
            preguntas?.ocultar()
            // Sin el nombre de la otra app: el registro no guarda nada de otras apps.
            RegistroEstudio.anotar("pregunta_abandonada", "otra_app")
            DiarioDev.anotar("preguntas: se fue a $otraApp sin responder")
        }
        if (brief?.visible() == true) brief?.ocultar()
    }

    // ---- el aviso ----

    /**
     * Cada swipe a un reel nuevo en Instagram. Con el aviso en pantalla, seguir
     * deslizando (al siguiente o al anterior) lo quita: la persona eligio seguir.
     * Sin aviso, cuenta los swipes de arrastre hasta SWIPES_AVISO.
     */
    private fun alDeslizar() {
        if (!Config.AVISO_ACTIVO || !friccionActiva()) return
        val ahoraMs = SystemClock.uptimeMillis()
        if (ahoraMs - ultimoDeslizarMs < Config.DEBOUNCE_SWIPE_MS) return
        ultimoDeslizarMs = ahoraMs
        val v = registro.actual ?: return
        val ov = friccion ?: return
        if (v !== visitaDelAviso) {
            visitaDelAviso = v
            swipesArrastre = 0
            avisoDisponible = true
        }
        if (avisoDesdeMs > 0) {
            // Los eventos que siguen al swipe que lo hizo aparecer son del mismo gesto.
            if (SystemClock.uptimeMillis() - avisoDesdeMs < Config.GRACIA_AVISO_MS) return
            RegistroEstudio.anotar("aviso_siguio", v.app)
            DiarioDev.anotar("aviso: siguió deslizando")
            terminarAviso()
            return
        }
        if (claseActual != Clase.ARRASTRE || !avisoDisponible) return
        if (ov.cieloVisible() || brief?.visible() == true) return
        swipesArrastre++
        if (swipesArrastre < Config.SWIPES_AVISO) return
        avisoDesdeMs = SystemClock.uptimeMillis()
        avisoDisponible = false
        ov.mostrarAviso {
            // Tocar "Salir": la unica forma en que el aviso saca de la app.
            if (avisoDesdeMs == 0L) return@mostrarAviso
            RegistroEstudio.anotar("aviso_salio", registro.actual?.app)
            DiarioDev.anotar("aviso: tocó Salir")
            terminarAviso()
            salirAlInicio()
        }
        RegistroEstudio.anotar("aviso_mostrado", v.app, swipesArrastre)
        DiarioDev.anotar("aviso al swipe $swipesArrastre")
    }

    /** Cada tic: se quita si ya no corresponde o si paso su tiempo sin que se tocara. */
    private fun revisarAviso() {
        if (avisoDesdeMs == 0L) return
        val ov = friccion ?: return
        when {
            // Otra capa lo saco (salio de la app, resumen, bloqueo).
            !ov.avisoVisible() -> cancelarAviso("oculto")
            // Ir al feed, a un DM o a un perfil lo quita sin sacar de Instagram.
            // Dar like o abrir comentarios no: se sigue en el reel.
            claseActual != Clase.ARRASTRE -> cancelarAviso("salió de reels")
            SystemClock.uptimeMillis() - avisoDesdeMs >= Config.SEGUNDOS_AVISO * 1000L -> {
                RegistroEstudio.anotar("aviso_expiro", registro.actual?.app)
                DiarioDev.anotar("aviso: pasó su tiempo, sigue")
                terminarAviso()
            }
        }
    }

    private fun cancelarAviso(motivo: String) {
        if (avisoDesdeMs == 0L) return
        RegistroEstudio.anotar("aviso_cancelado", registro.actual?.app, motivo)
        DiarioDev.anotar("aviso cancelado: $motivo")
        terminarAviso()
    }

    private fun terminarAviso() {
        avisoDesdeMs = 0L
        friccion?.quitarAviso()
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
        if (!friccionActiva()) return
        // La mascota habla solo en arrastre, igual que la burbuja, y no encima del aviso.
        if (PROBE2_ENABLED && claseActual != Clase.ARRASTRE) return
        if (avisoDesdeMs > 0) return
        if (!guionDe(v, ahora).pedir(m, ahora, sinPausa)) return
        DiarioDev.anotar("mascota: ${m.name.lowercase()}")
        // La mayoria de las veces crece la burbuja; a veces se asoma por un costado.
        if (kotlin.random.Random.nextFloat() < Config.PROBABILIDAD_ASOMO) {
            asomar(m)
        } else {
            friccion?.expandirBurbuja(m)
            RegistroEstudio.anotar("mascota_burbuja", m.name.lowercase())
        }
    }

    /**
     * La cara chica sale de la burbuja y, cuando ya se fue, la mascota grande se
     * asoma por un costado. Cambia de lado cada vez; la altura y la velocidad
     * varian, para que no se vuelva paisaje.
     */
    private fun asomar(m: Momento) {
        val ov = friccion ?: return
        val azar = kotlin.random.Random
        asomoDerecha = !asomoDerecha
        val derecha = asomoDerecha
        val y = Config.ASOMO_Y_MIN + azar.nextFloat() * (Config.ASOMO_Y_MAX - Config.ASOMO_Y_MIN)
        val entrada = azar.nextLong(Config.ASOMO_ENTRADA_MIN_MS, Config.ASOMO_ENTRADA_MAX_MS + 1)
        val queda = azar.nextLong(Config.ASOMO_QUEDA_MIN_MS, Config.ASOMO_QUEDA_MAX_MS + 1)
        caraFueraHasta = SystemClock.uptimeMillis() + Config.ASOMO_ESPERA_MS + 200L
        RegistroEstudio.anotar("mascota_asoma", m.name.lowercase(), if (derecha) "derecha" else "izquierda")
        handler.postDelayed({
            // Si mientras salia la cara se dejo el arrastre, no se asoma.
            if (claseActual != Clase.ARRASTRE || !friccionActiva()) return@postDelayed
            runCatching {
                ov.asomarMascota(m.desde, m.hasta, derecha, y, entrada, queda) {
                    RegistroEstudio.anotar("mascota_tocada", m.name.lowercase())
                    DiarioDev.anotar("tocó la mascota")
                }
            }.onFailure { DiarioDev.anotar("mascota FALLÓ: ${it.message}") }
        }, Config.ASOMO_ESPERA_MS)
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

    // ---- PROBE2: sonda de diagnostico, no es parte de la intervencion ----
    //
    // Clasificador de origen (tag CLASIF). Regla, segun la definicion de arrastre:
    //  - pestana Reels y "Sugerencias" (incluidos anuncios) -> ARRASTRE
    //  - reel enviado por mensaje (el primero) y reels de un perfil -> DIRIGIDO
    //  - Explorar y busqueda: el primer reel DIRIGIDO, al deslizar ARRASTRE
    //  - visor de reels sin senales conocidas -> DESCONOCIDO (para revisar en el piloto)
    // Solo lee ids y textos de nodos conocidos; nada se guarda fuera de Logcat.

    private var ultimaSondaMs = 0L

    /** El volcado corre en su propio hilo: el hilo principal sigue libre para la burbuja. */
    private val hiloSonda by lazy { HandlerThread(PROBE2_TAG).apply { start() } }
    private val manejadorSonda by lazy { Handler(hiloSonda.looper) }
    @Volatile private var sondaCorriendo = false

    private enum class Clase { ARRASTRE, DIRIGIDO, DESCONOCIDO, FUERA }

    /** Lo que una lectura del arbol encontro. */
    private class Lectura {
        var visorReels = false
        var raizReels = false
        var crearReel = false
        var barraPestanas = false
        var tituloReels = false
        var volver = false
        var comentarios = false
        var remitente = false
        var sugerencias = false
        var anuncio = false
        var autor: String? = null
        var patrocinador: String? = null
        var botonSeguir = false
    }

    // Estado del segmento: solo se toca desde hiloSonda (segmentoAbierto se lee tambien afuera).
    @Volatile private var segmentoAbierto = false
    /** La clase del segmento actual. La escribe hiloSonda y la lee el aviso y el cielo. */
    @Volatile private var claseActual = Clase.FUERA
    private var segClase = Clase.FUERA
    private var segContexto = ""
    private var segInicio = 0L
    private var segUltima = 0L
    private var segReels = 0
    private var segAnuncios = 0
    private var segNoSeguidas = 0
    private var ultimoReel: String? = null
    private val hora = SimpleDateFormat("HH:mm:ss", Locale.US)

    // Explorar y busqueda: el primer reel lo toca la persona (DIRIGIDO); al
    // deslizar al siguiente, lo pone el algoritmo (ARRASTRE).
    private var explorarPrimerReel: String? = null
    private var explorarDeslizado = false
    /** El visor de Explorar se vuelca una vez por arranque, para revisar sus señales. */
    private var explorarVolcado = false

    /**
     * Lee el arbol de la ventana activa como mucho cada PROBE2_THROTTLE_MS y
     * nunca dos a la vez. Con tope de nodos y de profundidad: el arbol de
     * Instagram completo satura el telefono.
     */
    private fun sonda(forzar: Boolean = false) {
        val ahora = SystemClock.uptimeMillis()
        if (sondaCorriendo || (!forzar && ahora - ultimaSondaMs < PROBE2_THROTTLE_MS)) return
        ultimaSondaMs = ahora
        sondaCorriendo = true
        manejadorSonda.post {
            try {
                val raiz = runCatching { rootInActiveWindow }.getOrNull() ?: return@post
                val lectura = Lectura()
                // A veces la ventana activa es la nuestra (el resumen) u otra app: cuenta como fuera.
                if (raiz.packageName?.toString() == PROBE2_PAQUETE) {
                    if (PROBE2_VOLCAR) volcar(raiz, 0, intArrayOf(0))
                    leer(raiz, 0, intArrayOf(0), lectura)
                }
                val nuevoDesconocido = registrar(lectura, System.currentTimeMillis())
                // Contexto sin senales conocidas (o Explorar, la primera vez): se vuelca para revisarlo.
                if (nuevoDesconocido && !PROBE2_VOLCAR) {
                    Log.d(CLASIF_TAG, "visor para revisar: volcado en tag $PROBE2_TAG")
                    runCatching { rootInActiveWindow }.getOrNull()?.let { volcar(it, 0, intArrayOf(0)) }
                    Log.d(PROBE2_TAG, "----------------------------------------")
                }
            } catch (e: Exception) {
                Log.d(CLASIF_TAG, "error: $e")
            } finally {
                sondaCorriendo = false
            }
        }
    }

    private fun leer(nodo: AccessibilityNodeInfo, profundidad: Int, nodos: IntArray, l: Lectura) {
        // Instagram deja pantallas anteriores (la pestana Reels) ocultas en el arbol: solo cuenta lo visible.
        if (profundidad > 0 && !nodo.isVisibleToUser) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                nodo.recycle()
            }
            return
        }
        nodos[0]++
        val id = nodo.viewIdResourceName?.substringAfter(":id/")
        val texto = nodo.text?.toString()
        val desc = nodo.contentDescription?.toString()
        when (id) {
            "root_clips_layout" -> {
                l.visorReels = true
                l.raizReels = true
            }
            "clips_viewer_view_pager", "clips_media_component" -> l.visorReels = true
            "tab_bar" -> l.barraPestanas = true
            "clips_viewer_action_bar_title" -> if (texto == "Reels") l.tituloReels = true
            "comment_composer_text_view" -> l.comentarios = true
            "sender_username_or_fullname" -> l.remitente = true
            "suggested_title" -> l.sugerencias = true
            "clips_single_image_ads_media_content" -> l.anuncio = true
            // Los reels precargados traen el bloque del autor vacio: el primero con texto es el actual.
            "clips_author_username" -> if (l.autor == null) l.autor = texto
            "inline_follow_button" -> l.botonSeguir = true
        }
        if (desc == "Crear un reel") l.crearReel = true
        if (desc == "Volver") l.volver = true
        if (texto == "Publicidad") l.anuncio = true
        if (desc != null && desc.startsWith("Reel patrocinado de ")) {
            l.anuncio = true
            if (l.patrocinador == null) l.patrocinador = desc.removePrefix("Reel patrocinado de ").substringBefore('.')
        }
        if (profundidad < PROBE2_MAX_PROFUNDIDAD) {
            for (i in 0 until nodo.childCount) {
                if (nodos[0] >= PROBE2_MAX_NODOS) break
                val hijo = nodo.getChild(i) ?: continue
                leer(hijo, profundidad + 1, nodos, l)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            nodo.recycle()
        }
    }

    private fun clasificar(l: Lectura): Pair<Clase, String> = when {
        !l.visorReels -> Clase.FUERA to ""
        l.sugerencias -> Clase.ARRASTRE to "sugerencias"
        l.remitente -> Clase.DIRIGIDO to "enviado por mensaje"
        // El visor de un perfil tiene el titulo "Reels"; el de Explorar y la busqueda no.
        l.volver && l.comentarios && l.tituloReels -> Clase.DIRIGIDO to "perfil"
        l.volver && l.comentarios -> Clase.DIRIGIDO to CONTEXTO_EXPLORAR
        // La pestana Reels nunca tiene "Volver": si aparece, es un visor encima.
        // Algunos anuncios esconden el titulo, pero la barra de pestanas sigue visible.
        !l.volver && (l.crearReel || (l.barraPestanas && (l.tituloReels || l.raizReels))) ->
            Clase.ARRASTRE to "pestaña reels"
        else -> Clase.DESCONOCIDO to "visor sin señales claras"
    }

    /** Devuelve true si conviene volcar el arbol: un visor desconocido, o Explorar la primera vez. */
    private fun registrar(l: Lectura, ahora: Long): Boolean {
        var (clase, contexto) = clasificar(l)
        if (contexto == CONTEXTO_EXPLORAR) {
            val autor = l.autor ?: l.patrocinador
            if (explorarPrimerReel == null) {
                explorarPrimerReel = autor
            } else if (autor != null && autor != explorarPrimerReel) {
                explorarDeslizado = true
            }
            if (explorarDeslizado) {
                clase = Clase.ARRASTRE
                contexto = "explorar deslizando"
            }
        } else {
            explorarPrimerReel = null
            explorarDeslizado = false
        }
        val cambia = clase != segClase || contexto != segContexto
        if (cambia) {
            cerrarSegmento()
            segClase = clase
            segContexto = contexto
            segInicio = ahora
            segReels = 0
            segAnuncios = 0
            segNoSeguidas = 0
            ultimoReel = null
            segmentoAbierto = clase != Clase.FUERA
            claseActual = clase
        }
        var nuevoDesconocido = cambia && clase == Clase.DESCONOCIDO
        if (cambia && contexto == CONTEXTO_EXPLORAR && !explorarVolcado) {
            explorarVolcado = true
            nuevoDesconocido = true
        }
        if (clase == Clase.FUERA) return false
        segUltima = ahora
        // Un reel nuevo = cambia el autor (dos seguidos de la misma cuenta cuentan como uno).
        val reel = l.autor ?: l.patrocinador ?: return nuevoDesconocido
        if (reel == ultimoReel) return nuevoDesconocido
        ultimoReel = reel
        segReels++
        val relacion = when {
            l.anuncio -> "anuncio"
            " y " in reel -> "colab"
            l.botonSeguir -> "no seguida"
            else -> "seguida"
        }
        if (l.anuncio) segAnuncios++
        if (relacion == "no seguida") segNoSeguidas++
        Log.d(CLASIF_TAG, "reel ${hora.format(Date(ahora))} · $clase · $contexto · $reel · $relacion")
        RegistroEstudio.anotar("reel", clase, contexto, if (Config.REGISTRAR_AUTORES) reel.trim() else "", relacion)
        return nuevoDesconocido
    }

    private fun cerrarSegmento() {
        if (segClase == Clase.FUERA) return
        val segundos = (segUltima - segInicio) / 1000
        Log.d(
            CLASIF_TAG,
            "SEGMENTO $segClase · $segContexto · ${hora.format(Date(segInicio))}–${hora.format(Date(segUltima))}" +
                " · ${segundos / 60}m ${segundos % 60}s · $segReels reels ($segAnuncios anuncios, $segNoSeguidas no seguidas)"
        )
        RegistroEstudio.anotar(
            "segmento", segClase, segContexto, hora.format(Date(segInicio)), hora.format(Date(segUltima)),
            segundos, segReels, segAnuncios, segNoSeguidas
        )
    }

    private fun volcar(nodo: AccessibilityNodeInfo, profundidad: Int, nodos: IntArray) {
        if (profundidad > 0 && !nodo.isVisibleToUser) return
        nodos[0]++
        Log.d(
            PROBE2_TAG,
            "  ".repeat(profundidad) +
                "${nodo.className} id=${nodo.viewIdResourceName} text=${nodo.text} desc=${nodo.contentDescription}"
        )
        if (profundidad < PROBE2_MAX_PROFUNDIDAD) {
            for (i in 0 until nodo.childCount) {
                if (nodos[0] >= PROBE2_MAX_NODOS) break
                val hijo = nodo.getChild(i) ?: continue
                volcar(hijo, profundidad + 1, nodos)
            }
        }
        // La raiz la reutiliza leer(): no se recicla aqui.
        if (profundidad > 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            nodo.recycle()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tic)
        runCatching { unregisterReceiver(receptorPantalla) }
        friccion?.ocultarTodo()
        brief?.ocultar()
        preguntas?.ocultar()
        RegistroEstudio.anotar("servicio_fin")
        friccion = null
        brief = null
        // El segmento abierto se anota antes de cerrar el hilo (quitSafely deja terminar lo pendiente).
        if (PROBE2_ENABLED) runCatching {
            manejadorSonda.post { cerrarSegmento() }
            hiloSonda.quitSafely()
        }
    }

    private companion object {
        /** false = la sonda no corre. */
        const val PROBE2_ENABLED = true
        const val PROBE2_TAG = "PROBE2"
        const val CLASIF_TAG = "CLASIF"
        /** true = ademas vuelca el arbol entero (tag PROBE2), como en la v3. */
        const val PROBE2_VOLCAR = false
        const val PROBE2_PAQUETE = "com.instagram.android"
        const val CONTEXTO_EXPLORAR = "explorar"
        const val PROBE2_THROTTLE_MS = 1500L
        /** Topes del volcado: alcanzan para ver botones y textos sin saturar el telefono. */
        const val PROBE2_MAX_NODOS = 400
        const val PROBE2_MAX_PROFUNDIDAD = 40
    }
}
