# Love Yourself — v6

Distingue arrastre de uso con intención, tiene mascota y resumen al salir, y trae modo dev para probar sin esperar.

Se instala al lado de la fase 05 (`com.love.yourself.fase06`, se llama **Love Yourself · v6**).

## qué cambia

| | fase 05 | v6 |
| --- | --- | --- |
| Dónde mide | cualquier scroll, cualquier app | solo apps marcadas, solo scroll a **pantalla completa** |
| Qué cuenta el reloj | tiempo desde el primer scroll | tiempo **arrastrado**. Mensajes y búsqueda no suman |
| Arriba | caja de texto | burbuja con 2 datos; se agranda con la mascota en ciertos momentos |
| Al salir | nada | resumen (WF3): arrastrado vs buscado + semana |
| Pausas | contaban | bloquear o ir a otra app no suma |
| Modo dev | — | reloj acelerado y forzar pantallas |

Apps marcadas de fábrica: Instagram, TikTok, YouTube, Facebook, X. Se editan en la app.

## capas

| Cuándo | Qué |
| --- | --- |
| Abres una app marcada | Burbuja: tiempo en la app · swipes por minuto |
| Primer swipe a pantalla completa | La burbuja salta, se agranda, y el cerebro pasa de *sus* a *sad* con un diálogo. A los 4 s se recoge |
| Con el arrastre | El cielo se va asomando |
| 15 min de arrastre | Decisión: seguir o salir |
| Inicio / recientes / bloqueo > 3 min / "Salir" | Resumen (si hubo ≥ 1 min de arrastre) |
| Otra app > 30 s | Cierra en silencio |

## "pantalla completa"

Accesibilidad dice **qué zona se deslizó y cuánto mide**, no qué muestra. Si ocupa ≥ 85 % del alto → arrastre. El feed tiene barra arriba y abajo, así que puede quedar bajo el 85 %.

Segunda señal: si el salto del scroll es de más de media pantalla (`FRACCION_SALTO_PAGINA`), también cuenta. Pasar de un reel al siguiente mueve una página entera. Y si la vista que avisa es chica, se revisan los contenedores deslizables que la envuelven.

La caja del modo dev muestra: `scroll` (fracción), `salto`, la clase de la vista y `ev` (eventos de scroll recibidos). Si `ev` se queda en 0, Instagram no está avisando los scrolls.

**Hay que calibrarlo.** Con modo dev activo, la caja muestra `scroll 0.xx` = lo que midió el último scroll. Anotar el número en reels, feed, mensajes y búsqueda, y ajustar `FRACCION_PANTALLA_COMPLETA` en `Config.kt`.

También queda en Logcat, filtro `LoveYourself` (formato; los valores reales están por verse):

```
scroll com.instagram.android fraccion=0.xx clase=<tipo de vista> id=<nombre interno>
```

## salir

La app reconoce la pantalla de inicio por lo que declara el sistema y, por si acaso, por nombre (`launcher`, `.home`). Si no la reconoce, el inicio cuenta como "otra app" y la sesión se cierra 30 s después sin resumen.

En modo dev, cada cierre muestra un aviso: por qué se cerró, cuánto arrastre hubo y si hubo resumen (y si no, por qué).

## modo dev

Casilla en la pantalla de la app, antes del modo investigación.

- **Reloj acelerado** x1 / x10 / x60. Acelera los contadores, no a ti: mirar un reel, responder un mensaje o bloquear se siguen midiendo en segundos reales. A x10 la decisión llega a los 90 s. Lo medido acelerado va a una semana aparte (`dev ×10` en el resumen).
- **Forzar** decisión y resumen, sin sesión.
- **Burbuja**: un botón por momento, para ver la animación sin esperar.
- Debajo de la burbuja, una caja con: velocidad, arrastrado, buscado y fracción del último scroll.

## burbuja

Sale del frame *animacion burbuja oficial* de Figma. Tiempos copiados del prototipo:

| ms | qué pasa |
| --- | --- |
| 190–475 | se apagan los datos |
| 595–800 | saltito de 17 dp |
| 800–1000 | crece en alto (32 → 88) |
| 1000–1190 | crece en ancho (153 → 361) |
| 1190–1400 | aparece la mascota y el diálogo |
| +4 s | se recoge: lo mismo al revés, sin salto |

Alto expandido 88 como en el frame con los cerebros (el prototipo llega a 60).

**Cerebros:** los de `sesion-06/avance/brains-ilust`, copiados a `res/drawable-nodpi/` de las dos apps como `brain_f`, `brain_pfff`, `brain_sad`, `brain_serio`, `brain_sus` (Android no acepta `-` en el nombre). Para usar otro en un momento, cámbialo en `enum Momento`.

**Datos de la píldora:** tiempo en la app (corre siempre desde que la abres) y swipes por minuto. El tiempo arrastrado sigue mandando en el cielo, la decisión y el resumen.

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
├── Config.kt  VistasFriccion.kt  CieloView.kt  BurbujaView.kt
├── VistaBrief.kt  BarraOrigen.kt  ColoresBrief.kt  FormatoBrief.kt
│
└── CapturaContinuaService.kt, DetectorCortes.kt, RegistroCsv.kt   modo investigación (igual que fase 05)
```

La pantalla de la app ya no usa Compose: vistas simples, igual que las capas.

## abierto

- Qué hace "Salir" (hoy: manda al inicio).
- "Oscuridad" en el resumen sigue en `—`.
- Qué otros momentos agrandan la burbuja. Se agregan en `Momento`, al inicio de `BurbujaView.kt`.
- El cerebro *sad* es provisorio: contradice que la mascota no juzga.
- La burbuja a 21 dp del borde puede quedar bajo la barra de estado de Android (`MARGEN_SUPERIOR_DP`).
