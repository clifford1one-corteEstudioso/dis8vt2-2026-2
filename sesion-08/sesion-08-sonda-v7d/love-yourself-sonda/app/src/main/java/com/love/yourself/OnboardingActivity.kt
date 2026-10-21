package com.love.yourself

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.love.yourself.config.Ajustes
import com.love.yourself.config.Config
import com.love.yourself.servicio.SesionService
import com.love.yourself.vistas.ChecklistPermisos
import com.love.yourself.vistas.Colores
import com.love.yourself.vistas.PuntosPasos
import com.love.yourself.vistas.VistasOnboarding

/**
 * La bienvenida, la primera vez que se abre la app:
 *
 *   1. los permisos, como lista: accesibilidad y mostrar encima
 *   2. ¿a qué quieres que le pongamos ojo?          (elegir apps; no en la version de estudio)
 *   3. ¿qué te gustaría hacer más y no alcanzas?    (WF1b)
 *
 * Los permisos se dan en los ajustes del telefono, fuera de la app. Al
 * volver, onResume marca en la lista los que ya estan. Cada paso nuevo entra
 * deslizandose y los puntos de arriba avanzan con el.
 */
class OnboardingActivity : Activity() {

    private enum class Paso { PERMISOS, APPS, ACTIVIDAD }

    /** Los pasos que se ven: en la version de estudio no se eligen apps. */
    private val pasos = Paso.values().filter { it != Paso.APPS || !Config.VERSION_ESTUDIO }

    private lateinit var ajustes: Ajustes
    private lateinit var vistas: VistasOnboarding
    private lateinit var contenedor: FrameLayout
    private lateinit var puntos: PuntosPasos

    private var pasoMostrado: Paso? = null
    private var vistaActual: View? = null
    private var checklist: ChecklistPermisos? = null
    private var permisosListos = false

    /** Los pasos que no son permisos avanzan con los botones, no solos. */
    /** En la version de estudio no se eligen apps: es solo Instagram. */
    private var appsListas = Config.VERSION_ESTUDIO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ajustes = Ajustes(this)
        vistas = VistasOnboarding(this)
        contenedor = FrameLayout(this)
        puntos = vistas.puntos(pasos.size)
        // fitsSystemWindows: nada queda debajo de las barras del sistema ni del
        // teclado (la actividad se escribe abajo).
        setContentView(FrameLayout(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Colores.PAGINA)
            addView(contenedor, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(puntos, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = (28 * resources.displayMetrics.density).toInt() })
        })
    }

    override fun onResume() {
        super.onResume()
        mostrar(pasoActual())
        // Al volver de los ajustes del telefono, marca lo que ya se activo.
        checklist?.actualizar(servicioActivo(this), Settings.canDrawOverlays(this))
    }

    private fun pasoActual(): Paso = when {
        !permisosListos || !Settings.canDrawOverlays(this) || !servicioActivo(this) -> Paso.PERMISOS
        !appsListas -> Paso.APPS
        else -> Paso.ACTIVIDAD
    }

    private fun mostrar(paso: Paso) {
        // Volver de los ajustes sin haber dado el permiso: el mismo paso sigue
        // ahi, tal como estaba.
        if (paso == pasoMostrado) return
        val adelante = (pasoMostrado?.ordinal ?: -1) < paso.ordinal
        val nueva = when (paso) {
            Paso.PERMISOS -> vistas.permisos(
                porQue = "Accesibilidad: reconoce cuándo estás en Reels y de dónde viene lo que ves " +
                    "(pestaña Reels, un mensaje, un perfil). No guarda imágenes, mensajes ni nombres " +
                    "de cuentas. En la lista de Accesibilidad busca \"${getString(R.string.servicio_label)}\".\n\n" +
                    "Mostrar encima de otras apps: para que la burbuja, el aviso y el cielo aparezcan " +
                    "encima de Instagram. Sin este permiso, Android no deja mostrarlos.",
                alAccesibilidad = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                alEncima = {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                },
                alSalir = { finish() },
                alContinuar = {
                    permisosListos = true
                    mostrar(pasoActual())
                }
            ).also { checklist = it }.vista
            Paso.APPS -> vistas.apps(
                instaladas = Ajustes.appsInstaladas(this, ajustes.appsVigiladas),
                marcadas = ajustes.appsVigiladas,
                alCambiar = { paquete, si -> ajustes.vigilar(paquete, si) },
                alContinuar = {
                    appsListas = true
                    mostrar(pasoActual())
                }
            )
            Paso.ACTIVIDAD -> vistas.declaracion(
                actual = ajustes.actividad,
                alOmitir = { terminar("") },
                alContinuar = { terminar(it) }
            )
        }
        vistas.cambiar(contenedor, vistaActual, nueva, adelante)
        vistaActual = nueva
        pasoMostrado = paso
        if (paso != Paso.PERMISOS) checklist = null
        puntos.marcar(pasos.indexOf(paso))
    }

    private fun terminar(actividad: String) {
        ajustes.actividad = actividad
        ajustes.onboardingHecho = true
        startActivity(Intent(this, InicioActivity::class.java))
        finish()
    }

    companion object {
        /**
         * No hay API para preguntarle al sistema si un service propio esta
         * activo, asi que se lee el ajuste donde Android guarda la lista. Se
         * compara como componente: Android a veces lo anota en forma corta
         * (".servicio.SesionService").
         */
        fun servicioActivo(activity: Activity): Boolean {
            val activos = Settings.Secure.getString(
                activity.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val propio = ComponentName(activity, SesionService::class.java)
            return activos.split(':').any { ComponentName.unflattenFromString(it) == propio }
        }
    }
}
