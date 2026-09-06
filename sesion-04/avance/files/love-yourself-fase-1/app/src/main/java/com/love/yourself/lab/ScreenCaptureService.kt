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
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val TAG = "LYSFase1"

/**
 * Prueba 1: obtener UN frame de pantalla y escribirlo a disco.
 *
 * Sin analisis, sin overlay, sin persistencia. La unica pregunta que responde
 * es si MediaProjection entrega pixeles reales de Instagram o un rectangulo
 * negro por FLAG_SECURE.
 */
class ScreenCaptureService : Service() {

    private var proyeccion: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var hilo: HandlerThread? = null
    private var yaGuardo = false
    private var retrasoMs = 0L
    private val mainHandler = Handler(android.os.Looper.getMainLooper())

    private val callbackProyeccion = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection detenida por el sistema o el usuario")
            detener()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        retrasoMs = intent?.getLongExtra(EXTRA_RETRASO_MS, 0L) ?: 0L

        // Paso 1: estar en foreground ANTES de pedir la proyeccion.
        // Desde API 34 invertir este orden lanza SecurityException.
        irAForeground()

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (data == null) {
            Log.e(TAG, "Falta el Intent de consentimiento; nada que capturar")
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

        hilo = HandlerThread("captura").also { it.start() }
        val handler = Handler(hilo!!.looper)

        // Desde API 34 hay que registrar el callback antes de createVirtualDisplay.
        proyeccion.registerCallback(callbackProyeccion, handler)

        val (ancho, alto, densidad) = medidasDePantalla()
        Log.i(TAG, "pantalla ${ancho}x${alto} @ ${densidad}dpi")

        val reader = ImageReader.newInstance(ancho, alto, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        reader.setOnImageAvailableListener({ lector ->
            val image = lector.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (!yaGuardo) {
                    yaGuardo = true
                    val archivo = destinoPng()
                    FrameWriter.guardar(image, archivo)
                    Log.i(TAG, "PNG guardado en: ${archivo.absolutePath}")
                    Log.i(TAG, "adb pull \"${archivo.absolutePath}\"")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando el frame", e)
            } finally {
                image.close()
                if (yaGuardo) detener()
            }
        }, handler)

        // El token de consentimiento se consume ya; solo se difiere el momento
        // de empezar a espejar la pantalla, que es lo que define QUE se captura.
        val montarDisplay = Runnable {
            Log.i(TAG, "montando VirtualDisplay ahora")
            virtualDisplay = proyeccion.createVirtualDisplay(
                "lys-fase1",
                ancho,
                alto,
                densidad,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )
        }

        if (retrasoMs > 0L) {
            Log.i(TAG, "modo diferido: capturando en $retrasoMs ms. Cambia de app AHORA.")
            handler.postDelayed(montarDisplay, retrasoMs)
        } else {
            Log.i(TAG, "modo inmediato (control): capturando esta misma app")
            montarDisplay.run()
        }

        return START_NOT_STICKY
    }

    private fun irAForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CANAL, "Captura de pantalla", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notificacion: Notification = androidx.core.app.NotificationCompat.Builder(this, CANAL)
            .setContentTitle("LYS Fase 1")
            .setContentText(
                if (retrasoMs > 0L) {
                    "Abre Instagram: captura en ${retrasoMs / 1000} s"
                } else {
                    "Capturando un frame de pantalla"
                }
            )
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notificacion,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
        )
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

    private fun destinoPng(): File {
        val carpeta = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        carpeta.mkdirs()
        val sello = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val modo = if (retrasoMs > 0L) "diferido" else "control"
        return File(carpeta, "frame-$modo-$sello.png")
    }

    private fun detener() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mainHandler.post { detener() }
            return
        }
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        proyeccion?.unregisterCallback(callbackProyeccion)
        proyeccion?.stop()
        proyeccion = null
        hilo?.quitSafely()
        hilo = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        hilo?.quitSafely()
    }

    companion object {
        private const val CANAL = "lys_captura"
        private const val NOTIF_ID = 1
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        const val EXTRA_RETRASO_MS = "retrasoMs"
    }
}
