package com.love.yourself.config

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
     * en Ajustes; las que no estan en su lista se ignoran.
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

    // ---- cuando habla la mascota ----

    /**
     * Minimo entre dos expansiones de la burbuja, en segundos. Si salta seguido
     * deja de verse, como las notificaciones. Con el reloj dev acelerado
     * tambien se acorta (a x60, 2 s).
     */
    const val PAUSA_ENTRE_MOMENTOS_S = 120

    /** Swipes por minuto que cuentan como ritmo alto... */
    const val RITMO_ALTO = 20

    /** ...sostenido por esta cantidad de segundos reales. */
    const val RITMO_ALTO_S = 30

    /** Minutos en la app para el momento "llevamos 5 min". */
    const val MIN_SUENO = 5

    /** Volver a abrir la app antes de esto (minutos) es "¿otra vez?". */
    const val MIN_VOLVISTE = 5

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

    /**
     * Lo que dice el cielo. "(en alguna parte)" es para el estudio: el clima
     * es fijo, y si le sale un dia de lluvia, que sea chiste y no error.
     */
    fun textoCielo(minutos: Long) = "¡Llevas $minutos minutos aquí! Hay un hermoso día afuera (en alguna parte)"
    const val CLIMA_TEMPERATURA = "26°C"
    const val CLIMA_LUGAR = "Santiago"

    /** Ventana movil para el ritmo de swipes, en segundos. */
    const val VENTANA_RITMO_S = 60

    // ---- la mascota asomandose ----

    /**
     * Cuando la mascota tiene algo que decir, casi siempre crece la burbuja con
     * la frase; con esta probabilidad, en cambio, la cara chica sale de la
     * burbuja y la mascota grande se asoma por un costado, sin dialogo.
     * 0 = nunca se asoma; 1 = siempre.
     */
    const val PROBABILIDAD_ASOMO = 0.22f

    /** Entre que alturas se asoma, en fraccion del alto de la pantalla: donde esta la info del reel. */
    const val ASOMO_Y_MIN = 0.50f
    const val ASOMO_Y_MAX = 0.72f

    /** Cuanto tarda en entrar: varia cada vez. Sale un poco mas rapido. */
    const val ASOMO_ENTRADA_MIN_MS = 300L
    const val ASOMO_ENTRADA_MAX_MS = 950L

    /** Cuanto se queda asomada. */
    const val ASOMO_QUEDA_MIN_MS = 2500L
    const val ASOMO_QUEDA_MAX_MS = 4500L

    /** Espera para que la cara chica termine de salir de la burbuja antes de asomarse. */
    const val ASOMO_ESPERA_MS = 550L

    // ---- el aviso (notch) ----

    /**
     * El aviso al tercer swipe de arrastre: pregunta si salir y ofrece un boton.
     * Salir es una eleccion explicita (en one sec, la opcion de desistir fue lo
     * que mas funciono); seguir deslizando lo quita. Nunca saca a nadie de la
     * app sin que lo pida. false: sin aviso.
     */
    const val AVISO_ACTIVO = true

    /** Swipes dentro de un segmento de ARRASTRE hasta que aparece el aviso. */
    const val SWIPES_AVISO = 3

    /** Cuanto se queda el aviso si no se toca ni se desliza. */
    const val SEGUNDOS_AVISO = 7

    /**
     * Un swipe dispara varios eventos mientras termina de moverse el reel. Los
     * que llegan justo despues de mostrar el aviso son del mismo gesto: no
     * cuentan como seguir deslizando.
     */
    const val GRACIA_AVISO_MS = 900L

    /** Cuanto fuera de arrastre hasta quitar la burbuja: evita parpadeos al cambiar de pantalla. */
    const val ESPERA_OCULTAR_BURBUJA_MS = 1500L

    const val TEXTO_AVISO = "¿Salir del arrastre?"
    const val TEXTO_AVISO_PIE = "o sigue deslizando"
    const val TEXTO_BOTON_AVISO = "Salir"

    // ---- las preguntas al cerrar (muestreo de experiencias) ----

    /**
     * Basadas en Lukoff et al. (2018) para motivacion y sentido, y en Lukoff et
     * al. (2021) para control. Aparecen en las dos fases del estudio.
     */
    const val PREGUNTA_MOTIVACION = "¿A qué entraste?"
    /** En este orden se anotan como instrumental, habitual y no_se. */
    val OPCIONES_MOTIVACION = listOf("A algo puntual", "A pasar el rato", "No sé")
    const val PREGUNTA_SENTIDO = "¿Qué tan significativo fue este rato para ti?"
    const val PREGUNTA_CONTROL = "¿Sentiste que tú llevabas el control de este rato?"
    const val ESCALA_MIN = "Nada"
    const val ESCALA_MAX = "Mucho"

    /** Lo que aparece la primera vez que se toca "Saltar". La segunda, se salta. */
    const val TEXTO_INSISTIR = "Por favor responde, es importante"

    /** Solo despues de visitas a Instagram de al menos esto: menos es mirar una notificacion. */
    const val MIN_VISITA_PREGUNTA_S = 15

    /**
     * Minimo entre dos preguntas, para no hostigar ni volver a la persona
     * demasiado consciente de su uso (Lukoff 2018 usaba 30).
     */
    const val MIN_ENTRE_PREGUNTAS = 90

    /**
     * Una sesion de al menos esto se pregunta siempre, aunque no hayan pasado
     * MIN_ENTRE_PREGUNTAS: son las que mas interesa entender. 23 min: lo que
     * tarda en promedio volver a una tarea interrumpida (Gloria Mark); una sesion
     * asi equivale a una interrupcion completa de lo que se estaba haciendo.
     * Cada pregunta anota por que aparecio ("regular" o "sesion_larga") y cuanto
     * duro la sesion, para poder separarlas en el analisis.
     */
    const val SESION_LARGA_MIN = 23

    /**
     * Las preguntas nunca aparecen encima de otra app: esperan a que la persona
     * vuelva a la pantalla de inicio del telefono. Si no vuelve en este tiempo
     * desde que cerro Instagram, se descartan: la respuesta ya no seria "del
     * momento" (Lukoff 2018 preguntaba justo al cerrar).
     */
    const val VENCE_PREGUNTA_MIN = 10

    // ---- el estudio ----

    /**
     * Version para participantes: solo Instagram (donde funciona el
     * clasificador), sin el modo investigacion, y el modo dev escondido
     * (se muestra manteniendo apretada la version al pie de Ajustes).
     */
    const val VERSION_ESTUDIO = true

    const val PAQUETE_INSTAGRAM = "com.instagram.android"

    /**
     * El formulario de consentimiento (Google Forms). Pegar aca el enlace "para
     * enviar" que da el script. La bienvenida lo abre antes de pedir permisos.
     */
    const val URL_CONSENTIMIENTO =
        "https://docs.google.com/forms/d/e/1FAIpQLSeVxIsb-pG3rCWtXQh43Susr6dAfy0-BWoQ2Ql8p4Esx88wsA/viewform"

    /**
     * Si el registro anota el autor de cada reel. Para participantes, no: son
     * datos de terceros y el analisis solo necesita la relacion (seguida, no
     * seguida, anuncio).
     */
    const val REGISTRAR_AUTORES = false

    /**
     * Dias de linea base desde la primera vez que se activa el servicio: la app
     * solo registra, sin burbuja, aviso, cielo ni resumen. Despues empieza la
     * intervencion. En modo dev la friccion esta siempre activa, para probar.
     */
    const val DIAS_LINEA_BASE = 7

    /** Dias de intervencion despues de la linea base. Solo se usa para decir "dia N de 21". */
    const val DIAS_INTERVENCION = 14

    /** Cada cuanto el servicio anota que sigue vivo: los huecos dicen cuando el telefono lo apago. */
    const val MIN_LATIDO = 30

    // ---- cuando termina una sesion ----

    /**
     * Volver antes de esto no abre una sesion nueva: la anterior sigue.
     * Sin esta tolerancia, responder un WhatsApp y volver partiria una sesion
     * de 40 minutos en pedazos y el contador nunca llegaria al cielo.
     */
    const val TOLERANCIA_REGRESO_S = 30

    /** Bloqueo mas largo que esto cierra la sesion y muestra el resumen. */
    const val PAUSA_BLOQUEO_MIN = 3

    /** Arrastre minimo para mostrar el resumen al salir. */
    const val MIN_ARRASTRE_BRIEF_S = 60

    /**
     * Las visitas sin arrastre (mensajes, buscar algo) tambien quedan en el
     * historial de Inicio si duran al menos esto: muestran el uso con
     * intencion, que es justo lo que el proyecto no quiere tocar. Las mas
     * cortas son mirar una notificacion y no dicen nada.
     */
    const val MIN_VISITA_HISTORIAL_S = 60

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
