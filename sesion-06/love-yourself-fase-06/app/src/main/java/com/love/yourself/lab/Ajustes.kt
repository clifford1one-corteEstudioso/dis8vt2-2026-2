package com.love.yourself.lab

import android.content.Context

/**
 * Lo que la persona elige en la pantalla de configuracion. Lo lee el servicio
 * en cada evento, asi que un cambio vale al tiro, sin reiniciar nada.
 */
class Ajustes(context: Context) {

    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Apps donde se mide. Parte con la lista sugerida. */
    var appsVigiladas: Set<String>
        get() = prefs.getStringSet(CLAVE_APPS, null)?.toSet() ?: Config.APPS_SUGERIDAS
        set(valor) = prefs.edit().putStringSet(CLAVE_APPS, valor).apply()

    fun vigilar(paquete: String, si: Boolean) {
        appsVigiladas = if (si) appsVigiladas + paquete else appsVigiladas - paquete
    }

    /**
     * Modo dev: muestra la caja con datos de depuracion desde que se abre una
     * app vigilada, y la pantalla de configuracion muestra el reloj acelerado y
     * los botones para forzar pantallas.
     */
    var modoDev: Boolean
        get() = prefs.getBoolean(CLAVE_DEV, false)
        set(valor) = prefs.edit().putBoolean(CLAVE_DEV, valor).apply()

    /** Velocidad del reloj elegida en modo dev (1, 10 o 60). Se guarda para no perderla al reinstalar. */
    var factorReloj: Int
        get() = prefs.getInt(CLAVE_FACTOR, 1)
        set(valor) = prefs.edit().putInt(CLAVE_FACTOR, valor).apply()

    private companion object {
        const val CLAVE_APPS = "apps_vigiladas"
        const val CLAVE_DEV = "modo_dev"
        const val CLAVE_FACTOR = "factor_reloj"
    }
}
