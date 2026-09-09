# love-yourself-fase-04

Medir y **mostrar**. Es la fase 03 más el sello visual encima de Instagram.

```
        ┌──────────────────────┐
        │  03:47  ·  0.8 cortes/s │
        └──────────────────────┘
```

Una caja chica arriba de la pantalla, sobre cualquier app.

## Qué muestra

| | |
|---|---|
| Izquierda | Tiempo desde que empezó la medición |
| Derecha | Cortes por segundo de los **últimos 15 segundos** |

El ritmo es de ventana móvil, no acumulado. El promedio de una sesión de veinte
minutos se aplana y deja de decir nada; lo que importa es a qué velocidad te
está estimulando *ahora*.

## Dos decisiones de diseño

**El overlay atraviesa los toques** (`FLAG_NOT_TOUCHABLE`). Instagram se sigue
usando exactamente igual. Esta fase es un espejo: muestra, no interrumpe. La
fricción que estorba de verdad viene después, y sobre datos validados.

**Sin base de datos.** Room es invisible en una demostración y agrega andamiaje
sobre una métrica que todavía no está validada. El CSV de la fase 03 sigue
siendo el registro duro.

## Cómo correr

1. Instalar. Anota el porcentaje de batería.
2. **Permitir dibujar sobre otras apps** — abre Ajustes, activas, vuelves. La
   pantalla se actualiza sola al volver.
3. **Empezar a medir** → aceptar el permiso de grabar pantalla.
4. Salir a Instagram. La caja aparece arriba.
5. **Detener** desde la notificación.
6. Sacar el CSV:
   ```
   adb pull /sdcard/Android/data/com.love.yourself.fase04/files/Documents/registros/
   ```

Sin el permiso de dibujar, el botón dice *Empezar sin overlay*: la medición
corre y el CSV se escribe igual, solo que no ves nada encima.

## El umbral

`UMBRAL_PROVISORIO = 18.0`, en `CapturaContinuaService.kt`. Una sola constante.

Sigue **sin validar**. La prueba de 83 s de la fase 03 mostró que la señal se
separa en dos poblaciones con un hueco vacío entre 17 y 25, así que 18 cae en
zona segura — pero eso no prueba que lo que cuenta sean cortes.

Cuando hagas la prueba de 20 minutos y la comparación contra un video con los
cortes contados a mano, **cambias ese número y nada más**. El overlay muestra lo
que le entregue el analizador.

## Comprobado

Prueba de 56 s, 9 sep 2026:

- El overlay se dibuja (`ty=2038`, TextView de 540×77) y se quita limpio al detener.
- Muestreo estable entre 3.0 y 3.6/s con la app en segundo plano.
- Notificación actualizándose cada 10 s.

Sigue sin probarse la sesión de 20 minutos.

## Pendiente

- Prueba de 20 minutos.
- Video con cortes contados a mano para fijar el umbral.
- Separar corte de plano de swipe al siguiente reel. Hoy los dos suben la misma
  columna, y miden cosas distintas: uno cuánto te estimula el contenido, otro
  cuánto te mueves tú.
- Posición del overlay: está fijo arriba al centro. Si tapa algo de Instagram
  que importe, se mueve en `VentanaOverlay.kt`.
