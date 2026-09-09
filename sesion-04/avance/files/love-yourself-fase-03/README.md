# love-yourself-fase-03

Captura sostenida y señal de cortes.

La fase 01 probó que se puede sacar **un** frame. Esta prueba la pregunta que de
verdad decide el proyecto:

> ¿Se puede muestrear la pantalla de forma continua, con la app en segundo plano,
> durante una sesión real de veinte minutos — y sirve esa señal para contar cortes?

Si la respuesta es no, la métrica central de la tesis no es medible y hay que
replantearla. Es el último riesgo grande que queda abierto.

## Lo que NO hace: decidir qué es un corte

El detector devuelve un **número crudo** de cuánto cambió la pantalla entre una
muestra y la anterior. No dictamina "hubo corte".

Hay un `UMBRAL_PROVISORIO = 18.0` en el código, pero es solo para que la
notificación muestre algo mientras corre. **El umbral real sale de los datos:**
grabas un video con los cortes contados a mano, lo comparas contra el CSV, y
eliges el valor donde calzan. Un umbral inventado ahora sería un supuesto
disfrazado de medición.

Eso corresponde al paso 3 de tu plan original.

## Cómo correr la prueba

1. Instalar. **Anota el porcentaje de batería.**
2. Botón **Empezar a medir** → aceptar el permiso.
3. Salir a Instagram y usarlo normal. Veinte minutos, sin forzar nada.
4. Tocar **Detener** en la notificación. Para verla mientras corre hay que
   **bajar la barra de notificaciones**: el canal es de importancia baja a
   propósito, así que se actualiza en silencio y nunca salta sobre la pantalla.
5. Anotar batería final y si el teléfono se calentó.
6. Sacar el CSV:
   ```
   adb pull /sdcard/Android/data/com.love.yourself.fase03/files/Documents/registros/
   ```

Con el celular conectado puedes mirar en vivo:

```
adb logcat -s LYSFase03
```

## Qué mide

Muestrea 4 veces por segundo. Cada muestra lee 32×32 puntos sueltos del buffer
—1024 lecturas en vez de 2.5 millones— y compara esa huella con la anterior. Por
eso puede sostener veinte minutos sin fundir la batería.

### El CSV

| Columna | |
|---|---|
| `ms_desde_inicio` | Tiempo desde que empezó |
| `diferencia` | Cuánto cambió la pantalla, 0 a 255. **Esta es la señal** |
| `sobre_umbral` | 1 si pasó el umbral provisorio. Ignorable por ahora |
| `muestras_perdidas` | Acumulado de frames descartados |

Ábrelo en una planilla y grafica `diferencia` contra `ms_desde_inicio`. Los
cortes deberían verse como picos.

### El resumen en Logcat, cada 10 s

```
LYSFase03  pantalla 1080x2340 @ 450dpi, muestreando cada 250 ms
LYSFase03  CSV: /storage/emulated/0/Android/data/...
LYSFase03  primera muestra recibida; resumen cada 10s
LYSFase03  vivo 300s  muestras=1187  ritmo=4.0/s  perdidas=8421  sobre_umbral=143
```

Las tres primeras salen en el primer segundo. Si aparecen, el muestreo arrancó
y no hay que esperar a ciegas para saberlo.

Lo importante de esa línea:

- **`vivo`** — si deja de crecer y el servicio muere antes de los 20 minutos,
  Android lo mató. Ese es el fracaso que hay que detectar.
- **`ritmo`** — debe mantenerse cerca de 4.0. Si baja, el muestreo no se sostiene.
- **`perdidas`** — número alto es **normal y esperado**: la pantalla emite frames
  a 60 fps o más y solo se procesan 4 por segundo. Es descarte deliberado, no falla.

## Los tres resultados posibles

| | Qué significa |
|---|---|
| Aguanta 20 min, ritmo estable, picos claros en el CSV | La métrica central es viable. Sigue el plan |
| Aguanta, pero el CSV es ruido plano | Se captura bien, pero contar cortes así no sirve. Cambiar de método, no de proyecto |
| El servicio muere antes | Medir en segundo plano no es posible así. Replantear la métrica |

## Pendiente

La batería y la temperatura se anotan a mano. Si la prueba pasa, valdrá la pena
registrarlas dentro de la app; ahora sería construir sobre algo sin validar.
