# avance pre sesion-04

## APK

### plan

| Paso | Qué haces | Qué confirmas | Si falla |
| --- | --- | --- | --- |
| **0** | Proyecto vacío en Android Studio → instalarlo en tu celu (modo desarrollador + depuración USB) | Que la cadena build → APK → celu funciona | Drivers, firma o versiones. No avances sin esto |
| **1a** | App desechable: MediaProjection captura un frame y lo guarda | Que puedes obtener píxeles con consentimiento por sesión | Revisar foreground service y notificación persistente |
| **1b** | App desechable: AccessibilityService loguea eventos con Instagram abierto | **Si Instagram entrega un árbol de nodos legible** | Tu métrica de origen no existe → replantearla ahora |
| **1c** | App desechable: dibujar un cuadrado sobre otra app | Que el overlay se renderiza sin bloquear | Permiso SYSTEM_ALERT_WINDOW |
| **2** | Andamiaje por tandas: Gradle+Manifest → servicios → analizador+rastreador → resto | Estructura correcta antes que lógica completa | — |
| **3** | Video con cortes contados a mano vs. lo que mide la app | Validez de la medición, no solo que corra | Ajustar umbral; ver cuánto ensucia el movimiento de cámara |
| **4** | Room y overlay definitivos | Persistencia y sello visual | Lo más mecánico, bajo riesgo |
| **5** | APK release firmado | Solo si alguien más lo instala | El APK de debug ya te sirve para trabajar |

## fase 02 — árbol de accesibilidad

Paso **1b**. La 1a quedó cerrada: MediaProjection sí entrega píxeles de un reel, Instagram no marca `FLAG_SECURE`.

### la complicación

`TYPE_VIEW_SCROLLED` se dispara decenas de veces por segundo y cada volcado son cientos de nodos. Logcat es buffer circular: se llena y bota lo viejo — justo los primeros segundos, donde está la transición de feed a perfil.

Segundo costo: recorrer el árbol completo en cada evento consume CPU del sistema y puede enlentecer a Instagram, el comportamiento que después quiero medir limpio.

Si el log se desborda concluyo *"Instagram no expone señal estable"* cuando la verdad es *"no pude leerlo"*. Falso negativo disfrazado de hallazgo. Mismo error que evité con el botón de control en la 1a. Por eso se decide antes y no después.

### opciones

| Opción | Cómo | Ventaja | Desventaja | Falso negativo |
| --- | --- | --- | --- | --- |
| **1** Estrangular Logcat | Un volcado cada ~800 ms, descarta el resto | Simple. Feedback en vivo. Poca CPU | Pierdo eventos a propósito; la señal puede estar en uno descartado | Medio |
| **2** Volcar a archivo | `.txt` junto a los PNG, sale por `adb pull` | No se pierde nada. Se lee con Ctrl+F en el PC. Sirve de anexo | A ciegas hasta el pull. Si el service falla me entero tarde | Bajo |
| **3** Las dos | Logcat estrangulado + archivo completo | Veo si corre **y** conservo todo | Más código. Hay que borrar los `.txt` viejos | Bajo |

### decisión

**3.** Cada salida responde algo distinto:

- Logcat → *¿esto está corriendo?* Falla más probable: service mal declarado en el manifest, nunca se activa y no avisa.
- Archivo → *¿hay señal estable que distinga feed / perfil / búsqueda / DM?* Se contesta en el PC comparando volcados, no mirando el celu.

Solo la 1: riesgo de descartar la métrica de origen por un evento estrangulado. Solo la 2: depurar a ciegas un service que quizá ni arrancó.

## investigar

- <https://youtu.be/xT8oP0wy-A0>