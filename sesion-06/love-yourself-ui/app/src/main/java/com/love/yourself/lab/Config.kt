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

    /** Minutos hasta que el overlay pasa de espejo discreto a presencia. */
    const val MIN_PRESENCIA = 5

    /** Minutos hasta el momento de decision a pantalla completa. */
    const val MIN_DECISION = 15

    /**
     * Si el usuario elige seguir, cuantos minutos hasta volver a preguntar.
     * Preguntar muy seguido convierte la friccion en hostigamiento y termina
     * en desinstalar la app.
     */
    const val MIN_REPREGUNTA = 10

    /**
     * Opacidad maxima del cielo, a la que llega a los MIN_DECISION minutos.
     * No puede pasar de 0.8: desde Android 12, una capa de otra app mas opaca
     * que eso bloquea los toques, e Instagram dejaria de responder.
     */
    const val OPACIDAD_MAX_CIELO = 0.7f

    /** Ventana movil para el ritmo de swipes, en segundos. */
    const val VENTANA_RITMO_S = 60

    /**
     * Volver antes de esto no abre una sesion nueva: la anterior sigue.
     * Sin esta tolerancia, responder un WhatsApp y volver partiria una sesion
     * de 40 minutos en pedazos y el contador nunca llegaria a la decision.
     */
    const val TOLERANCIA_REGRESO_S = 30

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
