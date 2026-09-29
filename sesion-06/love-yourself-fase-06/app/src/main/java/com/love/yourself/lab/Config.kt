package com.love.yourself.lab

const val TAG = "LoveYourself"

/**
 * Todo lo ajustable, en un solo lugar.
 *
 * Estos numeros NO estan validados. Salen de intuicion, no de datos. Cuando
 * tengas registros de tus propias sesiones vas a querer moverlos, y la idea es
 * que eso sea cambiar una linea y no rehacer nada.
 */
object Config {

    // ---- que cuenta como arrastre ----

    /**
     * Apps marcadas de fabrica. La persona puede quitar o agregar cualquiera
     * en la pantalla de configuracion; las que no estan en su lista se ignoran.
     */
    val APPS_SUGERIDAS = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",   // TikTok
        "com.ss.android.ugc.trill",   // TikTok, algunas regiones
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.twitter.android"         // X
    )

    /**
     * Que parte del alto de la pantalla tiene que ocupar la zona que se desliza
     * para contar como "contenido a pantalla completa" (reels, feed).
     * Mensajes y busqueda ocupan menos, porque tienen cabecera o teclado.
     *
     * Provisorio: calibrarlo mirando Logcat, donde cada scroll anota la
     * fraccion que midio.
     */
    const val FRACCION_PANTALLA_COMPLETA = 0.85f

    /**
     * Un scroll que mueve mas que esto (en fraccion del alto de pantalla)
     * tambien cuenta como pantalla completa: pasar de un reel al siguiente
     * mueve una pagina entera, y en mensajes los saltos son chicos.
     */
    const val FRACCION_SALTO_PAGINA = 0.5f

    /** Un swipe dispara varios eventos seguidos; mas juntos que esto son el mismo gesto. */
    const val DEBOUNCE_SWIPE_MS = 350L

    /**
     * Despues de un swipe, cuanto tiempo se sigue contando como arrastre aunque
     * no haya otro. Cubre el rato que se mira un reel antes de pasar al
     * siguiente; pasado eso, el tiempo cuenta como buscado.
     */
    const val VENTANA_ARRASTRE_S = 90

    // ---- la mascota ----

    /** Segundos de arrastre seguido hasta que la mascota deja su actividad. */
    const val LIMITE_ARRASTRE_S = 60

    // ---- el cielo ----

    /**
     * Minutos en la app hasta que aparece el cielo a pantalla completa, con
     * "seguir" o "salir". Cuenta desde que se abre la app, pero solo si hubo
     * arrastre: diez minutos respondiendo mensajes no lo muestran.
     */
    const val MIN_CIELO = 10

    /**
     * Si el usuario elige seguir, cuantos minutos hasta volver a mostrarlo.
     * Preguntar muy seguido convierte la friccion en hostigamiento y termina
     * en desinstalar la app.
     */
    const val MIN_REPREGUNTA = 10

    /** Lo que dice el cielo. Provisorio, igual que el clima: todavia es fijo. */
    const val TEXTO_CIELO = "Hay un hermoso día afuera!"
    const val CLIMA_TEMPERATURA = "26°C"
    const val CLIMA_LUGAR = "Santiago"

    /** Ventana movil para el ritmo de swipes, en segundos. */
    const val VENTANA_RITMO_S = 60

    // ---- cuando termina una sesion ----

    /**
     * Volver antes de esto no abre una sesion nueva: la anterior sigue.
     * Sin esta tolerancia, responder un WhatsApp y volver partiria una sesion
     * de 40 minutos en pedazos y el contador nunca llegaria a la decision.
     */
    const val TOLERANCIA_REGRESO_S = 30

    /** Bloqueo mas largo que esto cierra la sesion y muestra el resumen. */
    const val PAUSA_BLOQUEO_MIN = 3

    /** Arrastre minimo para mostrar el resumen al salir. */
    const val MIN_ARRASTRE_BRIEF_S = 60

    /**
     * Superficies del sistema que se dibujan encima sin que el usuario salga
     * de la app. No cuentan como abandonar la sesion.
     */
    val PAQUETES_DE_SISTEMA = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.touchtype.swiftkey"
    )
}
