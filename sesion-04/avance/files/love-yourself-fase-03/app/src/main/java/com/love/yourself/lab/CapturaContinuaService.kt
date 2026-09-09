package com.love.yourself.lab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

const val TAG = "LYSFase03"

/**
 * Fase 03 — captura sostenida.
 *
 * La fase 01 probo que se puede sacar UN frame. Esta prueba la pregunta que de
 * verdad decide el proyecto: si se puede muestrear la pantalla de forma continua,
 * con la app en segundo plano, durante una sesion real de veinte minutos.
 *
 * Formas conocidas de fallar, y por eso lo que se registra:
 *  - que Android mate el servicio a los pocos minutos  -> se registra el tiempo vivo
 *  - que el ritmo de muestreo no se sostenga           -> se registran muestras perdidas
 *  - que el telefono se caliente o se coma la bateria  -> se mide antes y despues, a mano
 */
class CapturaContinuaService : Service() {

    private var proyeccion: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var hilo: HandlerThread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val detector = DetectorCortes()
    private var csv: RegistroCsv? = null

    private var inicioMs = 0L
    private var ultimaMuestraMs = 0L
    private var muestras = 0
    private var perdidas = 0
    private var cortes = 0
    private var ultimoResumenMs = 0L

    private val callbackProyeccion = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.w(TAG, "MediaProjection detenida por el sistema tras ${(System.currentTimeMillis() - inicioMs) / 1000}s")
            detener()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_DETENER) {
            detener()
            return START_NOT_STICKY
        }

        irAForeground()

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (data == null) {
            Log.e(TAG, "Falta el Intent de consentimiento")
            detener()
            return START_NOT_STICKY
        }

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proyeccion = manager.getMediaProjection(resultCode, data)
        if (proyeccion == null) {
            Log.e(TAG, "getMediaProjection devolvio null")
            detener()
            return START_NOT_STICKY
        }
        this.proyeccion = proyeccion

        hilo = HandlerThread("captura-continua").also { it.start() }
        val handler = Handler(hilo!!.looper)
        proyeccion.registerCallback(callbackProyeccion, handler)

        csv = RegistroCsv(this)
        inicioMs = System.currentTimeMillis()
        ultimoResumenMs = inicioMs

        val (ancho, alto, densidad) = medidasDePantalla()
        Log.i(TAG, "pantalla ${ancho}x${alto} @ ${densidad}dpi, muestreando cada $INTERVALO_MS ms")
        Log.i(TAG, "CSV: ${csv?.archivo?.absolutePath}")

        val reader = ImageReader.newInstance(ancho, alto, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        reader.setOnImageAvailableListener({ lector ->
            // Hay que cerrar TODA imagen que se saque, incluso las descartadas,
            // o el reader se atasca y el muestreo se muere en silencio.
            val image = lector.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val ahora = System.currentTimeMillis()
                if (ahora - ultimaMuestraMs < INTERVALO_MS) {
                    perdidas++
                    return@setOnImageAvailableListener
                }
                ultimaMuestraMs = ahora
                muestras++
                if (muestras == 1) {
                    // Sin esta linea habria que esperar al primer resumen para
                    // saber si el muestreo arranco. Media prueba a ciegas.
                    Log.i(TAG, "primera muestra recibida; resumen cada ${RESUMEN_MS / 1000}s")
                }

                val diferencia = detector.diferencia(image)
                if (diferencia >= 0) {
                    val sobreUmbral = diferencia > UMBRAL_PROVISORIO
                    if (sobreUmbral) cortes++
                    csv?.fila(ahora - inicioMs, diferencia, sobreUmbral, perdidas)
                }

                if (ahora - ultimoResumenMs >= RESUMEN_MS) {
                    resumir(ahora)
                    ultimoResumenMs = ahora
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando frame", e)
            } finally {
                image.close()
            }
        }, handler)

        virtualDisplay = proyeccion.createVirtualDisplay(
            "lys-fase03",
            ancho,
            alto,
            densidad,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )

        return START_NOT_STICKY
    }

    /** Cada 30 s deja constancia de que sigue vivo y a que ritmo va. */
    private fun resumir(ahora: Long) {
        val segundos = (ahora - inicioMs) / 1000.0
        val ritmo = muestras / segundos
        Log.i(
            TAG,
            "vivo ${"%.0f".format(segundos)}s  muestras=$muestras  " +
                "ritmo=${"%.1f".format(ritmo)}/s  perdidas=$perdidas  " +
                "sobre_umbral=$cortes"
        )
        csv?.sincronizar()
        actualizarNotificacion(segundos, cortes)
    }

    private fun irAForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CANAL, "Captura continua", NotificationManager.IMPORTANCE_LOW)
            )
        }
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notificacion("Iniciando"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
        )
    }

    private fun notificacion(texto: String): Notification {
        val detener = Intent(this, CapturaContinuaService::class.java).setAction(ACCION_DETENER)
        val pendiente = android.app.PendingIntent.getService(
            this, 0, detener,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CANAL)
            .setContentTitle("LYS Fase 03")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", pendiente)
            .build()
    }

    private fun actualizarNotificacion(segundos: Double, cortes: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notificacion("${"%.0f".format(segundos)}s · $cortes sobre umbral"))
    }

    private fun medidasDePantalla(): Triple<Int, Int, Int> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val densidad = resources.displayMetrics.densityDpi
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Triple(bounds.width(), bounds.height(), densidad)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            Triple(metrics.widthPixels, metrics.heightPixels, densidad)
        }
    }

    private fun detener() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { detener() }
            return
        }
        if (inicioMs > 0L) {
            val segundos = (System.currentTimeMillis() - inicioMs) / 1000.0
            Log.i(TAG, "FIN. vivo ${"%.0f".format(segundos)}s  muestras=$muestras  perdidas=$perdidas")
            Log.i(TAG, "adb pull \"${csv?.archivo?.absolutePath}\"")
        }
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close(); imageReader = null
        proyeccion?.unregisterCallback(callbackProyeccion)
        proyeccion?.stop(); proyeccion = null
        csv?.cerrar(); csv = null
        hilo?.quitSafely(); hilo = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        csv?.cerrar()
        hilo?.quitSafely()
    }

    companion object {
        private const val CANAL = "lys_captura_continua"
        private const val NOTIF_ID = 3
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        const val ACCION_DETENER = "com.love.yourself.DETENER"

        /** 4 muestras por segundo. Suficiente para cortes de reel, barato para 20 min. */
        private const val INTERVALO_MS = 250L
        private const val RESUMEN_MS = 10_000L

        /** Provisorio. El valor real sale de comparar el CSV con un video contado a mano. */
        private const val UMBRAL_PROVISORIO = 18.0
    }
}
