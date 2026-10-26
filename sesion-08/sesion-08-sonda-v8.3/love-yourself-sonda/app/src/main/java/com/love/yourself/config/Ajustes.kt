package com.love.yourself.config

import android.content.Context
import android.content.Intent
import com.love.yourself.vistas.AppInstalada

/**
 * Lo que la persona elige en la bienvenida y en Ajustes. Lo lee el servicio
 * en cada evento, asi que un cambio vale al tiro, sin reiniciar nada.
 */
class Ajustes(context: Context) {

    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Apps donde se mide. Parte con la lista sugerida. En la version de estudio, solo Instagram. */
    var appsVigiladas: Set<String>
        get() = if (Config.VERSION_ESTUDIO) setOf(Config.PAQUETE_INSTAGRAM)
        else prefs.getStringSet(CLAVE_APPS, null)?.toSet() ?: Config.APPS_SUGERIDAS
        set(valor) = prefs.edit().putStringSet(CLAVE_APPS, valor).apply()

    fun vigilar(paquete: String, si: Boolean) {
        appsVigiladas = if (si) appsVigiladas + paquete else appsVigiladas - paquete
    }

    /**
     * Modo dev: muestra la caja con datos de depuracion desde que se abre una
     * app vigilada, y en Ajustes, el reloj acelerado y los botones para forzar
     * pantallas.
     */
    var modoDev: Boolean
        get() = prefs.getBoolean(CLAVE_DEV, false)
        set(valor) = prefs.edit().putBoolean(CLAVE_DEV, valor).apply()

    /** Velocidad del reloj elegida en modo dev (1, 10 o 60). Se guarda para no perderla al reinstalar. */
    var factorReloj: Int
        get() = prefs.getInt(CLAVE_FACTOR, 1)
        set(valor) = prefs.edit().putInt(CLAVE_FACTOR, valor).apply()

    /** Si ya se paso por la bienvenida (permisos, apps, actividad). */
    var onboardingHecho: Boolean
        get() = prefs.getBoolean(CLAVE_ONBOARDING, false)
        set(valor) = prefs.edit().putBoolean(CLAVE_ONBOARDING, valor).apply()

    /** "¿Que te gustaria hacer mas y no alcanzas?". Vacio si la omitio. */
    var actividad: String
        get() = prefs.getString(CLAVE_ACTIVIDAD, "") ?: ""
        set(valor) = prefs.edit().putString(CLAVE_ACTIVIDAD, valor).apply()

    /** Cuando empezo el estudio (hora real). 0 si todavia no. Lo fija el servicio la primera vez. */
    var inicioEstudioMs: Long
        get() = prefs.getLong(CLAVE_INICIO_ESTUDIO, 0L)
        set(valor) = prefs.edit().putLong(CLAVE_INICIO_ESTUDIO, valor).apply()

    /** El codigo que une el consentimiento con el registro (P1, P2...). Vacio si no se ha dado. */
    var codigoParticipante: String
        get() = prefs.getString(CLAVE_CODIGO, "") ?: ""
        set(valor) = prefs.edit().putString(CLAVE_CODIGO, valor).apply()

    /** Cuando aparecieron las ultimas preguntas (hora real). Sobrevive a que el sistema reinicie el servicio. */
    var ultimaPreguntaMs: Long
        get() = prefs.getLong(CLAVE_ULTIMA_PREGUNTA, 0L)
        set(valor) = prefs.edit().putLong(CLAVE_ULTIMA_PREGUNTA, valor).apply()

    /** Linea base o intervencion, segun los dias desde el inicio del estudio. */
    fun friccionActiva(ahoraReal: Long): Boolean =
        modoDev || (inicioEstudioMs > 0 && ahoraReal - inicioEstudioMs >= Config.DIAS_LINEA_BASE * 86_400_000L)

    companion object {
        /**
         * Las apps con icono en el telefono: primero las sugeridas y las ya
         * marcadas, despues el resto por nombre.
         */
        fun appsInstaladas(context: Context, marcadas: Set<String>): List<AppInstalada> {
            val pm = context.packageManager
            val todas = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { AppInstalada(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .filter { it.paquete != context.packageName }
                .distinctBy { it.paquete }
                .sortedBy { it.nombre.lowercase() }
            val (destacadas, otras) = todas.partition { it.paquete in Config.APPS_SUGERIDAS || it.paquete in marcadas }
            return destacadas + otras
        }

        private const val CLAVE_ONBOARDING = "onboarding_hecho"
        private const val CLAVE_ACTIVIDAD = "actividad"
        private const val CLAVE_APPS = "apps_vigiladas"
        private const val CLAVE_DEV = "modo_dev"
        private const val CLAVE_FACTOR = "factor_reloj"
        private const val CLAVE_INICIO_ESTUDIO = "inicio_estudio"
        private const val CLAVE_CODIGO = "codigo_participante"
        private const val CLAVE_ULTIMA_PREGUNTA = "ultima_pregunta"
    }
}
