# love-yourself-fase-02

Comprueba si se pueden detectar los dos bordes de una sesión.

> **Sesión** = desde el primer scroll sobre contenido a pantalla completa,
> hasta que sales de la app.

Ya **no** vuelca el árbol de nodos ni intenta inferir por qué ruta se llegó a un
reel: esa métrica quedó descartada. Solo usa lo que trae el evento — tipo,
paquete y clase — con `canRetrieveWindowContent="false"`.

La condición de "pantalla completa" no se resuelve acá; sale de los píxeles de
la fase 01.

## Dos versiones

Son dos proyectos separados con el mismo `RegistroSesion.kt`, así que describen
exactamente lo mismo y solo cambia dónde escriben. Distinto `applicationId`, se
instalan las dos a la vez.

| | **version-a** | **version-b** |
|---|---|---|
| Salida | `.txt` en `files/registros/` | Logcat, tag `LYSFase02b` |
| Responde | ¿Hay señal para marcar los bordes? | ¿Esto está corriendo? |
| Pierde datos | No | Sí, a propósito (1 tanda cada 800 ms) |
| Necesita el PC | Solo al final, para el `adb pull` | Sí, conectado todo el rato |
| `applicationId` | `com.love.yourself.fase02a` | `com.love.yourself.fase02b` |

La **b** existe porque la falla más probable es que el service ni siquiera se
active — y en ese caso la **a** te deja un archivo vacío sin explicación.

La **a** existe porque el scroll automático no se observa con el celular
enchufado al escritorio.

## Cómo correr

1. Abrir `version-a` (o `version-b`) como proyecto en Android Studio. Instalar.
2. En la app, botón **Abrir Ajustes de Accesibilidad**.
3. Activar el servicio de la lista y volver.
4. Usar Instagram normal: entrar a reels, scrollear, salir, volver.
5. Version a — sacar el registro:
   ```
   adb pull /sdcard/Android/data/com.love.yourself.fase02a/files/registros/
   ```
   Version b — mirar en vivo:
   ```
   adb logcat -s LYSFase02b
   ```

## Qué leer

```
14:22:07.881  APP -> com.instagram.android
14:22:07.902  ventana  com.instagram.mainactivity.MainActivity
14:22:11.043  === SESION INICIA  com.instagram.android  (primer scroll)
14:22:12.610  scroll #2  androidx.recyclerview.widget.RecyclerView
14:24:38.117  === SESION TERMINA  com.instagram.android  scrolls=61  duracion=147.1s
14:24:38.117  APP -> com.whatsapp
```

Verificar: que `SESION INICIA` caiga en el primer scroll real y no antes, y que
`SESION TERMINA` caiga al salir de la app.

## Casos pendientes

- **Salir y volver.** Responder un WhatsApp y regresar cuenta como dos sesiones.
  Se arregla con una tolerancia de N segundos; es un número, no arquitectura.
- **Bloquear pantalla.** No cambia el paquete en primer plano, así que hoy no
  cierra la sesión.
- **Galería a pantalla completa.** También dispara scroll. Filtrar por app o
  exigir que el contenido sea video.

No están resueltos a propósito: primero hay que ver en los datos si importan.
