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
