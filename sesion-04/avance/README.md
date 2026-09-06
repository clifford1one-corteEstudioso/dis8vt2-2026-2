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

Corresponde al paso **1b** del plan. La fase 01 ya cerró el paso 1a: MediaProjection
sí entrega píxeles reales de un reel, Instagram no marca esa ventana con
`FLAG_SECURE`. Con eso confirmado, toca la otra métrica: si se puede inferir
**por qué ruta** llegué a un reel — feed, perfil, búsqueda, DM.

### la complicación

La prueba no se juega en si Instagram expone datos. Se juega en si puedo
**leerlos**.

`TYPE_VIEW_SCROLLED` se dispara decenas de veces por segundo mientras scrolleo
Reels, y cada volcado del árbol son cientos de nodos. Logcat no es un archivo:
es un buffer circular de tamaño fijo (unos cientos de KB), así que cuando se
llena empieza a botar las líneas más viejas. Los primeros segundos de la sesión
—justo donde está la transición de feed a perfil que quiero observar—
desaparecen antes de que alcance a mirarlos.

Hay un segundo costo: un `AccessibilityService` que recorre el árbol completo en
cada evento consume CPU en el hilo del sistema. Puede poner lento al propio
Instagram, que es exactamente el comportamiento que más adelante voy a querer
medir sin contaminar.

**Por qué decidirlo ahora y no después:** si el log se desborda, voy a ver poco
o nada y voy a concluir *"Instagram no expone señal estable"*. Pero la
conclusión real habría sido *"no pude leer lo que expuso"*. Es el mismo error
que evité en la fase 01 con el botón de control: un problema de infraestructura
disfrazado de hallazgo. Solo que aquí el falso negativo me haría abandonar la
métrica de origen sin motivo.

### opciones de salida del log

| | **1. Estrangular Logcat** | **2. Volcar a archivo** | **3. Las dos** |
| --- | --- | --- | --- |
| **Cómo funciona** | Máximo un volcado cada ~800 ms; los eventos intermedios se descartan | Cada volcado se escribe a un `.txt` junto a los PNG; se saca con `adb pull` | Logcat estrangulado para ver en vivo + archivo completo para analizar |
| **Ventajas** | Lo más simple. Feedback inmediato en pantalla. Bajo costo de CPU | No se pierde nada. Se lee con calma en el PC, con Ctrl+F. Permite comparar rutas lado a lado y adjuntarlo a la memoria | Se ve en vivo que algo llega *y* se conserva todo. El log en vivo avisa si el service ni siquiera se activó |
| **Desventajas** | Pierdo eventos a propósito: si la señal que distingue las rutas aparece en un evento descartado, no la veo | A ciegas mientras pruebo — no sé si funciona hasta hacer `adb pull`. Si el service falla, me entero tarde. Archivos que crecen rápido | Un poco más de código. Hay que acordarse de borrar los `.txt` viejos |
| **Riesgo de falso negativo** | Medio | Bajo | Bajo |
| **Cuándo conviene** | Si solo quiero confirmar rápido que el árbol no viene vacío | Si ya sé que llega señal y toca analizarla en serio | Cuando no sé ninguna de las dos cosas — o sea, ahora |

### decisión

La **3**, y no por completista.

Las dos salidas responden preguntas distintas. El Logcat en vivo responde
*"¿esto está corriendo?"* — que es la falla más probable, porque un
`AccessibilityService` mal declarado en el manifest simplemente nunca se activa
y no avisa. El archivo responde *"¿hay señal estable que distinga feed de perfil
de búsqueda?"*, y esa se contesta sentado en el PC comparando cuatro volcados,
no mirando el celular.

Con solo la 1, me arriesgo a descartar la métrica de origen por un evento que
estrangulé. Con solo la 2, pierdo una tarde depurando a ciegas un service que
quizá ni arrancó.

## investigar

- <https://youtu.be/xT8oP0wy-A0>