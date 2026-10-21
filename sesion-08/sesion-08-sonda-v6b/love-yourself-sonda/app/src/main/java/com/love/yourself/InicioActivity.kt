package com.love.yourself

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import com.love.yourself.config.Ajustes
import com.love.yourself.sesion.AcumuladoSemanal
import com.love.yourself.sesion.HistorialSesiones
import com.love.yourself.sesion.Reloj
import com.love.yourself.sesion.Semana
import com.love.yourself.vistas.DatosInicio
import com.love.yourself.vistas.SesionInicio
import com.love.yourself.vistas.VistaInicio

/**
 * Lo que se ve al abrir la app: tu semana (vistas/VistaInicio.kt). Tambien
 * es a donde lleva "ver detalle" en el resumen.
 *
 * La app trabaja sola y no hace falta volver aca: quien abre esta pantalla
 * ya esta mirando su uso con algo de distancia, y eso es lo que muestra.
 * Lo tecnico (permisos, apps, modo dev) esta en Ajustes, detras del engranaje.
 */
class InicioActivity : Activity() {

    private lateinit var ajustes: Ajustes
    private lateinit var raiz: FrameLayout

    /** La entrada animada, solo la primera vez: al volver de Ajustes ya no. */
    private var animar = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ajustes = Ajustes(this)
        Reloj.cambiarFactor(if (ajustes.modoDev) ajustes.factorReloj else 1)
        raiz = FrameLayout(this).apply { fitsSystemWindows = true }
        setContentView(raiz)
        animar = savedInstanceState == null
    }

    /** Desde el resumen ("ver detalle"), con la pantalla ya abierta: se arma de nuevo, con los datos nuevos. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        animar = true
    }

    override fun onResume() {
        super.onResume()
        // La primera vez, o si se quitaron los permisos: la bienvenida.
        val sinPermisos = !Settings.canDrawOverlays(this) || !OnboardingActivity.servicioActivo(this)
        if (!ajustes.onboardingHecho || sinPermisos) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        // Se arma de nuevo cada vez (los datos cambian), pero sin perder hasta
        // donde se habia bajado.
        val bajado = if (animar) 0 else raiz.getChildAt(0)?.scrollY ?: 0
        raiz.removeAllViews()
        val vista = VistaInicio(this).crear(datos(), animar, alAjustes = {
            startActivity(Intent(this, AjustesActivity::class.java))
        })
        raiz.addView(vista)
        if (bajado > 0) vista.post { vista.scrollTo(0, bajado) }
        animar = false
    }

    /** Junta lo que muestra Inicio. Con el reloj dev acelerado, muestra las pruebas. */
    private fun datos(): DatosInicio {
        val dev = ajustes.modoDev && Reloj.factor != 1
        val acumulado = AcumuladoSemanal(this, if (dev) AcumuladoSemanal.ARCHIVO_DEV else AcumuladoSemanal.ARCHIVO)
        val historial = HistorialSesiones(this, if (dev) HistorialSesiones.ARCHIVO_DEV else HistorialSesiones.ARCHIVO)
        val ahora = System.currentTimeMillis()
        val registros = historial.ultimas()
        val hoy = Semana.fecha(ahora)
        val deHoy = registros.filter { Semana.fecha(it.finMs) == hoy }
        val nombres = mutableMapOf<String, String>()
        fun nombre(paquete: String) = nombres.getOrPut(paquete) { nombreDeApp(paquete) }
        val semanaPasada = acumulado.semanaPasadaHastaHoy(ahora)
        return DatosInicio(
            ahora = ahora,
            hoyArrastreMs = acumulado.dia(ahora),
            hoyBuscadoMs = deHoy.sumOf { it.buscadoMs },
            hoySesiones = deHoy.count { it.fueSesion },
            hoyVideos = deHoy.sumOf { it.videos },
            semana = acumulado.semanaPorDia(ahora),
            hoy = Semana.indice(ahora),
            semanaPasadaMs = if (semanaPasada > 0) semanaPasada else -1L,
            actividad = ajustes.actividad,
            sesiones = registros.take(10).map {
                SesionInicio(
                    finMs = it.finMs,
                    app = nombre(it.app),
                    totalMs = it.totalMs,
                    arrastradoMs = it.arrastradoMs,
                    videos = it.videos,
                    motivo = it.motivo.name,
                    seguir = it.seguir,
                    recuerdas = it.recuerdas
                )
            },
            apps = ajustes.appsVigiladas.map { nombre(it) }.filter { !it.contains('.') }.sorted(),
            etiqueta = if (dev) "datos dev ×${Reloj.factor}" else ""
        )
    }

    /** "Instagram" en vez de "com.instagram.android". Si no esta instalada, el paquete. */
    private fun nombreDeApp(paquete: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(paquete, 0)).toString()
    }.getOrDefault(paquete)
}
