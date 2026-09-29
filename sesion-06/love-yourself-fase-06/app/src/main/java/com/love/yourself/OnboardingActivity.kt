package com.love.yourself

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.love.yourself.lab.Ajustes
import com.love.yourself.lab.SesionService
import com.love.yourself.lab.VistasOnboarding

/**
 * La bienvenida, la primera vez que se abre la app:
 *
 *   1. permiso para dibujar encima de otras apps   (WF1a)
 *   2. permiso de accesibilidad                    (WF1a)
 *   3. ¿a qué quieres que le pongamos ojo?          (elegir apps)
 *   4. ¿qué te gustaría hacer más y no alcanzas?    (WF1b)
 *
 * Los permisos se dan en Ajustes, fuera de la app. Al volver, onResume mira
 * cual falta y muestra ese paso; si ya estaban dados, se los salta.
 */
class OnboardingActivity : Activity() {

    private enum class Paso { DIBUJAR, ACCESIBILIDAD, APPS, ACTIVIDAD }

    private lateinit var ajustes: Ajustes
    private lateinit var vistas: VistasOnboarding

    /** Los pasos que no son permisos avanzan con los botones, no solos. */
    private var appsListas = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ajustes = Ajustes(this)
        vistas = VistasOnboarding(this)
    }

    override fun onResume() {
        super.onResume()
        mostrar(pasoActual())
    }

    private fun pasoActual(): Paso = when {
        !Settings.canDrawOverlays(this) -> Paso.DIBUJAR
        !servicioActivo(this) -> Paso.ACCESIBILIDAD
        !appsListas -> Paso.APPS
        else -> Paso.ACTIVIDAD
    }

    private fun mostrar(paso: Paso) {
        val vista = when (paso) {
            Paso.DIBUJAR -> vistas.permiso(
                texto = "Para mostrarte la burbuja encima de Instagram, Love Yourself necesita dibujar sobre otras apps.",
                porQue = "La burbuja y el cielo aparecen encima de la app que estás usando. Sin este permiso, Android no deja mostrarlos.",
                alSalir = { finish() },
                alActivar = {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            )
            Paso.ACCESIBILIDAD -> vistas.permiso(
                texto = "Para saber cuándo empiezas a scrollear, Love Yourself necesita el permiso de accesibilidad. " +
                    "Busca \"Love Yourself · v6\" y actívalo.",
                porQue = "Mira qué app tienes adelante y cuánto de la pantalla ocupa lo que deslizas. " +
                    "No lee lo que ves ni guarda nada fuera de tu teléfono.",
                alSalir = { finish() },
                alActivar = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
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
        setContentView(vista)
    }

    private fun terminar(actividad: String) {
        ajustes.actividad = actividad
        ajustes.onboardingHecho = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        /**
         * No hay API para preguntarle al sistema si un service propio esta
         * activo, asi que se lee el ajuste donde Android guarda la lista.
         */
        fun servicioActivo(activity: Activity): Boolean {
            val activos = Settings.Secure.getString(
                activity.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val propio = "${activity.packageName}/${SesionService::class.java.name}"
            return activos.split(':').any { it.equals(propio, ignoreCase = true) }
        }
    }
}
