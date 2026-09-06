# love-yourself-fase-1

App desechable. Responde **una** pregunta:

> ¿MediaProjection entrega píxeles reales de Instagram, o un rectángulo negro
> porque la ventana está marcada `FLAG_SECURE`?

Sin análisis de cortes, sin overlay, sin Room. Si la respuesta es "negro", la
vía de captura de pantalla no sirve y hay que replantear la métrica antes de
construir nada encima.

## Cómo correr la prueba

1. Abrir esta carpeta como proyecto en Android Studio (es un proyecto Gradle
   independiente del de `../`).
2. Instalar en el celular con depuración USB.
3. Abrir Logcat y filtrar por el tag `LYSFase1`.
4. En el celular: tocar **Capturar un frame** → aceptar el diálogo del sistema.
5. Leer el log.

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

## Limitación conocida de esta versión

La captura es **inmediata** al aceptar el permiso, así que el frame es de esta
app, no de Instagram. Para apuntar a un reel hay que darse tiempo de cambiar de
app: en `ScreenCaptureService.kt`, envolver el `createVirtualDisplay` en un
`postDelayed` de ~5000 ms.

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
