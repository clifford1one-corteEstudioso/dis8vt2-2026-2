# Love Yourself — v6

Distingue arrastre de uso con intención, tiene mascota y resumen al salir, y trae modo dev para probar sin esperar.

Se instala al lado de la fase 05 (`com.love.yourself.fase06`, se llama **Love Yourself · v6**).

## qué cambia

| | fase 05 | v6 |
| --- | --- | --- |
| Dónde mide | cualquier scroll, cualquier app | solo apps marcadas, solo scroll a **pantalla completa** |
| Qué cuenta el reloj | tiempo desde el primer scroll | tiempo **arrastrado**. Mensajes y búsqueda no suman |
| Mascota | — | lee; levanta la vista con el primer swipe; deja el libro al minuto de arrastre |
| Al salir | nada | resumen (WF3): arrastrado vs buscado + semana |
| Pausas | contaban | bloquear o ir a otra app no suma |
| Modo dev | — | reloj acelerado y forzar pantallas |

Apps marcadas de fábrica: Instagram, TikTok, YouTube, Facebook, X. Se editan en la app.

## capas

| Cuándo | Qué |
| --- | --- |
| Abres una app marcada | Mascota leyendo |
| Primer swipe a pantalla completa | Levanta la vista. Aparece la caja |
| 1 min de arrastre seguido | Deja el libro. No se pone triste: solo no puede seguir |
| 5 min de arrastre | Caja grande. El cielo se va asomando |
| 15 min de arrastre | Decisión: seguir o salir |
| Inicio / recientes / bloqueo > 3 min / "Salir" | Resumen (si hubo ≥ 1 min de arrastre) |
| Otra app > 30 s | Cierra en silencio |

## "pantalla completa"

Accesibilidad dice **qué zona se deslizó y cuánto mide**, no qué muestra. Si ocupa ≥ 85 % del alto → arrastre. El feed tiene barra arriba y abajo, así que puede quedar bajo el 85 %.

**Hay que calibrarlo.** Con modo dev activo, la caja muestra `scroll 0.xx` = lo que midió el último scroll. Anotar el número en reels, feed, mensajes y búsqueda, y ajustar `FRACCION_PANTALLA_COMPLETA` en `Config.kt`.

También queda en Logcat, filtro `LoveYourself` (formato; los valores reales están por verse):

```
scroll com.instagram.android fraccion=0.xx clase=<tipo de vista> id=<nombre interno>
```

## modo dev

Casilla en la pantalla de la app, antes del modo investigación.

- **Reloj acelerado** x1 / x10 / x60. Acelera los contadores, no a ti: mirar un reel, responder un mensaje o bloquear se siguen midiendo en segundos reales. A x10 la decisión llega a los 90 s. Lo medido acelerado va a una semana aparte (`dev ×10` en el resumen).
- **Forzar** decisión y resumen, sin sesión.
- **Mascota** en sus tres estados.
- La caja aparece desde que abres la app marcada, con: velocidad, estado de la mascota, arrastrado, buscado y fracción del último scroll.

## probado

22 pruebas JUnit con los casos de uso (`RegistroVisitasTest`, `FormatoBriefTest`). Todo `lab/` y `MainActivity` compilan contra Android 16. **No se ha probado en el celular.**

## archivos

```
lab/
├── RegistroVisitas.kt   reglas de sesión y mascota, sin Android (se prueban solas)
├── SesionService.kt     accesibilidad: une todo
├── Reloj.kt             reloj acelerable (modo dev)
├── Ajustes.kt           apps marcadas y modo dev
├── OverlayFriccion.kt   ventanas: cielo → caja → mascota → decisión
├── OverlayBrief.kt      ventana del resumen
├── AcumuladoSemanal.kt  arrastre por día
│
│  idénticos en la app de diseño (sesion-06/love-yourself-ui):
├── Config.kt  VistasFriccion.kt  CieloView.kt  MascotaView.kt
├── VistaBrief.kt  BarraOrigen.kt  ColoresBrief.kt  FormatoBrief.kt
│
└── CapturaContinuaService.kt, DetectorCortes.kt, RegistroCsv.kt   modo investigación (igual que fase 05)
```

La pantalla de la app ya no usa Compose: vistas simples, igual que las capas.

## abierto

- Qué hace "Salir" (hoy: manda al inicio).
- "Oscuridad" en el resumen sigue en `—`.
- Dónde va la mascota: arriba a la izquierda es provisorio (`VistasFriccion.kt`).
