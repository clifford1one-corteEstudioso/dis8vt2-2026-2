# love-yourself-fase-1

App desechable. Responde **una** pregunta:

> ¿MediaProjection entrega píxeles reales de Instagram, o un rectángulo negro
> porque la ventana está marcada `FLAG_SECURE`?

Sin análisis de cortes, sin overlay, sin Room. Si la respuesta es "negro", la
vía de captura de pantalla no sirve y hay que replantear la métrica antes de
construir nada encima.

## Los dos botones

La app tiene dos botones que recorren **el mismo código**; lo único que cambia
es cuándo se monta el `VirtualDisplay`.

| Botón | Retraso | Qué captura | Para qué |
|---|---|---|---|
| Control: capturar ahora | 0 s | Esta misma app | Verifica que la cadena permiso → VirtualDisplay → PNG está sana |
| Instagram: capturar en 5 s | 5 s | Lo que esté en pantalla a los 5 s | Responde la pregunta de `FLAG_SECURE` |

Esa es toda la lógica del experimento: si el **control** sale con color y el
**diferido** sale negro, la diferencia no puede ser un bug del código —
es Instagram bloqueando la captura.

Si los dos salen negros, el problema es el código o el dispositivo, no Instagram.
Por eso el control existe.

## Cómo correr la prueba

1. Abrir esta carpeta como proyecto en Android Studio (es un proyecto Gradle
   independiente del de `../`).
2. Instalar en el celular con depuración USB.
3. Abrir Logcat y filtrar por el tag `LYSFase1`.
4. **Control**: tocar *Capturar ahora* → aceptar el diálogo → leer el veredicto.
5. **Diferido**: tocar *Capturar en 5 s* → aceptar el diálogo → salir a
   Instagram y llegar a un reel antes de que se acaben los 5 segundos.
6. Comparar los dos PNG: `frame-control-*.png` y `frame-diferido-*.png`.

El diálogo de consentimiento sale **en cada captura**. No es un bug: desde
Android 14 el permiso de MediaProjection es de un solo uso, no queda concedido.

## Qué leer en el log

```
LYSFase1  pantalla 1080x2400 @ 420dpi
LYSFase1  rowStride=4352 pixelStride=4 padding=32
LYSFase1  VEREDICTO: 376 de 400 pixeles muestreados tienen color -> hay captura real
LYSFase1  PNG guardado en: /storage/emulated/0/Android/data/com.love.yourself.fase1/files/Pictures/frame-....png
LYSFase1  adb pull "/storage/emulated/0/Android/data/..."
```

El PNG queda en la carpeta privada de la app (sin permisos de almacenamiento).
Se saca con `adb pull` o con Device Explorer de Android Studio. No aparece en la
galería.

## Ajustar el retraso

Son 5 segundos. Si te queda corto para llegar al reel, cambia
`RETRASO_DIFERIDO_MS` en `MainActivity.kt`.

Nota de implementación: el `MediaProjection` se obtiene de inmediato y solo se
difiere el `createVirtualDisplay`. El token de consentimiento se consume al
tiro; lo que se retrasa es el momento de empezar a espejar la pantalla, que es
lo que define *qué* se captura.

## Detalles que rompen esto en silencio

- **Orden en API 34+**: el service tiene que estar en foreground *antes* de
  `getMediaProjection()`. Al revés lanza `SecurityException`. Por eso el Intent
  de consentimiento viaja como extra hacia el service.
- **Callback obligatorio en API 34+**: hay que llamar `registerCallback()`
  antes de `createVirtualDisplay()`.
- **Row padding**: el buffer del `ImageReader` suele ser más ancho que la
  pantalla. Ignorarlo produce una imagen con una diagonal de basura que parece
  captura fallida cuando los píxeles llegaron bien. Ver `FrameWriter.kt`.

## Archivos

| Archivo | Rol |
|---|---|
| `MainActivity.kt` | El botón y el diálogo de consentimiento |
| `lab/ScreenCaptureService.kt` | Foreground service, VirtualDisplay + ImageReader |
| `lab/FrameWriter.kt` | Image → Bitmap → PNG, y el veredicto de negro |

---

# fase 02 — límites de sesión

Regla acordada:

> **Sesión** = desde el primer scroll sobre contenido a pantalla completa,
> hasta que sales de la app.

Esta fase comprueba si esos dos bordes se pueden detectar. Ya **no** vuelca el
árbol de nodos ni intenta inferir por qué ruta se llegó a un reel: esa métrica
quedó descartada. Solo usa lo que viene en el evento mismo — tipo, paquete,
clase — con `canRetrieveWindowContent="false"`.

La condición de "pantalla completa" no se resuelve acá; sale de los píxeles de
la fase 01.

## Dos servicios, misma lógica

Ambos comparten `RegistroSesion.kt`, así que describen exactamente lo mismo y
solo cambia dónde escriben.

| | **Fase 02a** | **Fase 02b** |
|---|---|---|
| Salida | `.txt` en `files/registros/` | Logcat, tag `LYSFase1` |
| Responde | ¿Hay señal para marcar los bordes? | ¿Esto está corriendo? |
| Pierde datos | No | Sí, a propósito (1 tanda cada 800 ms) |
| Necesita el PC | Solo al final, para el `adb pull` | Sí, conectado todo el rato |

La 02b existe porque la falla más probable es que el service ni siquiera se
active — y en ese caso la 02a te deja un archivo vacío sin explicación.

La 02a existe porque el scroll automático no se observa con el celular
enchufado al escritorio.

## Cómo correr

1. Instalar. En la app, botón **Fase 02: abrir Accesibilidad**.
2. Activar `LYS Fase 02a`, `LYS Fase 02b`, o las dos.
3. Usar Instagram normal. Entrar a reels, scrollear, salir, volver.
4. Sacar el registro:
   ```
   adb pull /sdcard/Android/data/com.love.yourself.fase1/files/registros/
   ```

## Qué leer

```
14:22:07.881  APP -> com.instagram.android
14:22:07.902  ventana  com.instagram.mainactivity.MainActivity
14:22:11.043  === SESION INICIA  com.instagram.android  (primer scroll)
14:22:12.610  scroll #2  androidx.recyclerview.widget.RecyclerView
14:22:14.229  scroll #3  androidx.recyclerview.widget.RecyclerView
14:24:38.117  === SESION TERMINA  com.instagram.android  scrolls=61  duracion=147.1s
14:24:38.117  APP -> com.whatsapp
```

Lo que hay que verificar: que `SESION INICIA` caiga en el primer scroll real y
no antes, y que `SESION TERMINA` caiga al salir de la app.

## Casos pendientes

- **Salir y volver.** Responder un WhatsApp y regresar cuenta como dos sesiones.
  Se arregla con una tolerancia de N segundos; es un número, no arquitectura.
- **Bloquear pantalla.** No cambia el paquete en primer plano, así que hoy no
  cierra la sesión.
- **Galería a pantalla completa.** También dispara scroll. Filtrar por app o
  exigir que el contenido sea video.

No están resueltos a propósito: primero hay que ver en los datos si importan.
