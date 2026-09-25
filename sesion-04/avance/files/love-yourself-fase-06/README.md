# Love Yourself — fase 06

El resumen de cierre de sesión (WF3) y los bordes de sesión de la sesión 05.
Es la fase 05 más el momento de salida.

## Cuándo aparece el resumen

| Cómo se sale | Qué pasa |
|---|---|
| Botón de inicio | Resumen |
| Apps recientes | Resumen (en casi todos los teléfonos viven en la misma app de inicio) |
| Bloquear el celular | Pausa. Si dura más de 3 min, resumen al desbloquear |
| "Salir" en la pantalla de decisión | Resumen, sobre la pantalla de inicio |
| Irse a otra app | Si vuelve en 30 s sigue la misma sesión. Si no, se cierra **en silencio** |

Irse a otra app no muestra el resumen a propósito: aparecería encima de
WhatsApp medio minuto después, interrumpiendo justo lo que no hay que
interrumpir.

Solo aparece si hubo al menos 1 minuto de arrastre. Así bajar por un chat no
termina en un resumen.

## De dónde sale cada dato

| En el resumen | Cómo se calcula | Estado |
|---|---|---|
| Estuviste 22 minutos | Tiempo en la app desde que se abrió, sin contar pausas | Medido |
| videos | Swipes, agrupando los eventos de un mismo gesto | Medido |
| recuerdas | Es una pregunta, no un dato. Se muestra `?` | Fijo |
| oscuridad | Sin definición todavía. Se muestra `—` | **Pendiente** |
| arrastrado | Tiempo dentro de los 90 s siguientes a un swipe | Medido |
| buscado | El resto: antes del primer swipe, o sin swipes por más de 90 s | Medido |
| Esta semana | Suma del arrastre de lunes a hoy | Medido |
| 3h 40 en [actividad] | — | **Fuera** |
| ver detalle | Se ve apagado | **Fuera** |

La línea de la actividad declarada quedó fuera porque la app no tiene cómo
saber cuánto tiempo se dedicó a esa actividad. Es una pregunta de diseño, no
de código.

## Probarlo sin esperar una sesión

En la app, **Ver ejemplo del resumen** abre el resumen con los números del
wireframe. Sirve para revisar cómo se ve.

## Lo que cambió en el código

- `RegistroVisitas.kt` reemplaza a `EstadoSesion.kt`. El tiempo se acumula por
  segundo en vez de restar horas, así las pausas no cuentan.
- Un swipe dispara varios eventos de scroll. Ahora se agrupan: antes, "videos"
  habría contado cinco o diez por reel.
- `OverlayBrief.kt`, `BarraOrigen.kt`, `ColoresBrief.kt`, `FormatoBrief.kt`,
  `AcumuladoSemanal.kt`: el resumen.
- `SesionService.kt` detecta la pantalla de inicio y el bloqueo.

## Pruebas

`app/src/test/.../lab/`: 12 pruebas con los casos de uso de la sesión 05
(inicio, bloqueo corto y largo, volver de WhatsApp, barra de notificaciones) y
los formatos del resumen. Corren en Android Studio con clic derecho →
*Run tests*.

## Números nuevos en `Config.kt`

```kotlin
VENTANA_ARRASTRE_S = 90     // después de un swipe, cuánto sigue siendo arrastre
DEBOUNCE_SWIPE_MS = 350     // eventos más juntos que esto son el mismo swipe
PAUSA_BLOQUEO_MIN = 3       // bloqueo más largo cierra la sesión
MIN_ARRASTRE_BRIEF_S = 60   // arrastre mínimo para mostrar el resumen
```

Ninguno está validado.

## Instalar

Desactiva primero el servicio de la fase 05 en Ajustes → Accesibilidad. Si los
dos están activos, vas a ver dos cajas encima de Instagram.
