# Love Yourself — v6

Distingue arrastre de uso con intención, tiene mascota, cielo y resumen al salir, guarda tu semana en Inicio y trae modo dev para probar sin esperar.

Se instala al lado de la fase 05 (`com.love.yourself.fase06`, se llama **Love Yourself · v6**). Versión actual: **6.1** (se ve al final de Ajustes).

## al actualizar desde 6.0

- **Hay que volver a activar la accesibilidad.** El servicio se movió de carpeta (`lab/` → `servicio/`) y Android lo trata como otro servicio. La app lo detecta y abre la bienvenida en el paso del permiso.
- El ícono de la app ahora abre **Inicio** (antes abría la configuración, que pasó a **Ajustes**, detrás del ⚙). Si lo tenías en la pantalla de inicio del teléfono, puede que haya que volver a ponerlo.
- La semana y los ajustes se conservan. El historial de sesiones parte vacío: se llena desde la primera sesión con 6.1.

## qué cambia

| | fase 05 | v6 |
| --- | --- | --- |
| Dónde mide | cualquier scroll, cualquier app | solo apps marcadas, solo scroll a **pantalla completa** |
| Qué cuenta el reloj | tiempo desde el primer scroll | tiempo **arrastrado**. Mensajes y búsqueda no suman |
| Arriba | caja de texto | burbuja con 2 datos; se agranda con la mascota en ciertos momentos |
| Al salir | nada | resumen (WF3): arrastrado vs buscado + semana |
| Al abrir la app | configuración | **Inicio**: tu día, la semana y las últimas sesiones (6.1) |
| Pausas | contaban | bloquear o ir a otra app no suma |
| Modo dev | — | reloj acelerado y forzar pantallas |

Apps marcadas de fábrica: Instagram, TikTok, YouTube, Facebook, X. Se editan en Ajustes.

## 6.1: el pulido

- **Inicio**, lo que se ve al abrir la app y a donde lleva "ver detalle" del resumen (ver abajo).
- **Historial de sesiones** (`HistorialSesiones.kt`): cada sesión con su duración, arrastre, videos, cómo terminó y lo que respondiste en "recuerdas". También las visitas sin arrastre de más de 1 min: son el uso con intención.
- **Resumen más completo**: "ver detalle" funciona, "≈ 16 s c/u" bajo los videos, la semana pasada a la misma altura, y el "?" de "recuerdas" se puede contestar.
- **Animaciones** en todo: burbuja, cielo, resumen, bienvenida e Inicio (ver cada sección). Si el teléfono tiene las animaciones apagadas (Accesibilidad → Quitar animaciones), todo aparece directo.
- **Archivos ordenados** por lo que hacen (ver *archivos*), sin Compose ni código muerto.
- **Tema oscuro** en toda la app, como los wireframes: sin destello blanco al abrir.

## bienvenida

La primera vez que se abre (o si falta un permiso):

1. Permiso para dibujar encima (WF1a).
2. Permiso de accesibilidad (WF1a). "¿Por qué requerimos este permiso?" despliega la explicación.
3. ¿A qué quieres que le pongamos ojo? Elegir apps.
4. ¿Qué te gustaría hacer más y no alcanzas? (WF1b). Aparece en el resumen y en Inicio como **"? en [actividad]"**: la app no puede medir ese tiempo, así que va como pregunta.

Los permisos se saltan si ya están dados. Cada paso entra deslizándose y unos puntos arriba dicen en cuál vas. "Volver a la bienvenida", en Ajustes, la repite.

## capas

| Cuándo | Qué |
| --- | --- |
| Abres una app marcada | Burbuja: baja desde arriba, con tiempo en la app · swipes por minuto |
| Primer swipe a pantalla completa | La burbuja salta, se agranda, entra el cerebro y escribe lo que dice. A los 4 s se recoge |
| 10 min en la app (si hubo arrastre) | El cielo (Figma *sky*) a pantalla completa: mensaje, clima fijo, el cerebro sentado en una nube, Seguir / Salir. Seguir lo aplaza 10 min. Si la mascota está hablando, el cielo espera a que termine |
| Inicio / recientes / bloqueo > 3 min / "Salir" | Resumen (si hubo ≥ 1 min de arrastre) |
| Otra app > 30 s | Cierra en silencio |

## Inicio

Lo que se ve al abrir la app. El agregado que el resumen solo insinúa:

- **Tu cerebro hoy**: su cara según todo el arrastre del día (`CaraDelDia`, en `Mascota.kt`), con lo que dice. Flota despacio.
- **Hoy**: minutos en arrastre, en cuántas sesiones y cuántos videos, y la barra arrastrado / buscado.
- **Esta semana**: una barra por día (lunes a domingo, hoy en blanco), el total, cómo iba la semana pasada a esta altura, y "? en [actividad]".
- **Últimas sesiones**: app, hora, duración, arrastre y videos, una barra chica, y cómo terminó ("saliste desde el cielo · seguiste 1 vez · recordabas 1–3"). Las visitas sin arrastre dicen "solo buscado".

No hay metas, rachas ni puntajes: muestra, igual que la mascota. Con el reloj dev acelerado, Inicio muestra los datos de las pruebas (y lo dice arriba).

| Arrastre en el día | Cara | Dice |
| --- | --- | --- |
| < 1 min | relief | hoy estoy entero |
| < 15 | sus | por ahora, bien |
| < 30 | pfff | un poco cansado |
| < 45 | serio | ya fue bastante |
| < 60 | drowzy | me está dando sueño |
| < 90 | sad | hoy me cuesta |
| 90 o más | f | hoy quedé frito |

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

Casilla en Ajustes, antes del modo investigación.

- **Reloj acelerado** x1 / x10 / x60. Acelera los contadores, no a ti: mirar un reel, responder un mensaje o bloquear se siguen midiendo en segundos reales. A x10 el cielo llega al minuto. Lo medido acelerado va a una semana y un historial aparte (`dev ×10` en el resumen y en Inicio).
- **Forzar** cielo y resumen, sin sesión. El resumen forzado también abre Inicio con "ver detalle".
- **Burbuja**: un botón por momento, para ver la animación sin esperar.
- **Diario**: lo que el servicio fue viendo, para diagnosticar sin Logcat.
- **Borrar los datos dev**: la semana y el historial de las pruebas.
- Debajo de la burbuja, una caja con: velocidad, arrastrado, buscado y fracción del último scroll.

## la mascota

Es tu cerebro: no te reta, muestra lo que el arrastre le hace. Habla agrandando la burbuja.

| Momento | Cuándo | Cara |
| --- | --- | --- |
| primer swipe | empieza el arrastre | sus → sad |
| arrastre seguido | 1 min sin parar | pfff |
| ritmo alto | ≥ 20 swipes/min durante 30 s | pfff → f |
| 5 min | 5 min en la app | drowzy |
| volviste | reabrir antes de 5 min de haber cerrado | sus |
| antes del cielo | 1 min antes | serio → f |
| tras seguir | eligió "Seguir" en el cielo | sad |
| resumen | no vio el cielo / lo vio | relief / f |

Además, **la píldora lleva siempre un cerebro chico que se va gastando** (`CaraProgresiva`). Cuando cambia, el nuevo entra con un saltito:

| Tiempo en la app | Cara |
| --- | --- |
| antes del primer swipe | relief |
| 0–1 min | sus |
| 1–3 | pfff |
| 3–5 | serio |
| 5–7 | drowzy |
| 7–10 | sad |
| cielo en adelante | f |

Reglas (`Guion.kt`): cada momento una vez por sesión, y al menos 2 min entre dos (salvo "tras seguir", que responde al tiro). La pausa se acelera con el reloj dev: a x60 son 2 s, para ver todos en una prueba corta. Cada momento tiene 3 frases y se elige una al azar: un estímulo fijo se vuelve invisible.

Frases y caras: `vistas/Mascota.kt` (`Momento`, `CaraProgresiva`, `CaraDelDia`, `FrasesCierre`). Tiempos: `Config.kt`.

## burbuja

Sale del frame *animacion burbuja oficial* de Figma. Los milisegundos son los del prototipo; en 6.1 tienen curvas (en Figma son lineales):

| ms | qué pasa | curva |
| --- | --- | --- |
| 190–475 | se apagan los datos | suave (la del prototipo) |
| 595–800 | saltito de 17 dp | sube frenando, cae acelerando |
| 800–1000 | crece en alto (32 → 88) | se pasa un poco y vuelve |
| 1000–1190 | crece en ancho (153 → 361) | se pasa un poco y vuelve |
| 1190–1570 | entra el cerebro | crece desde nada |
| 1300– | escribe lo que dice, letra por letra | 32 ms por letra |
| +4 s | se recoge: lo mismo al revés, sin salto | suave |

Mientras escribe, el cerebro da saltitos (habla); después respira despacio. El cambio de cara (sus → sad) es un fundido con un apretón, como una reacción. Al abrir una app marcada la píldora baja desde arriba.

Alto expandido 88 como en el frame con los cerebros (el prototipo llega a 60).

**Cerebros:** los de `sesion-06/avance/brains-ilust`, copiados a `res/drawable-nodpi/` de las dos apps como `brain_f`, `brain_pfff`, `brain_sad`, `brain_serio`, `brain_sus`, `brain_drowzy`, `brain_relief` (Android no acepta `-` en el nombre). Se cargan a la escala justa (`Imagenes.kt`): las capas viven encima de Instagram y no conviene ocupar memoria de más.

**Datos de la píldora:** tiempo en la app (corre siempre desde que la abres) y swipes por minuto. El tiempo arrastrado sigue mandando en el cielo y en el resumen.

## cielo

Llega bajando desde arriba, entero y sin transparencias: no se insinúa, aparece. Después entran el mensaje, el clima y al final los botones. Las nubes derivan, el sol respira y el cerebro flota con su nube, todo despacio: lo contrario del ritmo de los reels.

Los botones **no responden hasta que se terminan de ver** (≈ 1,5 s): si venías deslizando, ese dedo no elige por ti. "Seguir" hace subir el cielo y vuelves al reel; "Salir" va directo.

## resumen

Entra con un velo y la tarjeta subiendo; cada parte aparece un poco después de la anterior. Los números se cuentan desde 0 y la barra arrastrado / buscado se llena: el agregado se ve juntarse.

- Bajo los videos: **≈ N s c/u**, cuánto duró cada uno en promedio.
- **recuerdas ?**: el "?" da un saltito para decir que se toca. Al tocarlo pregunta "¿cuántos de esos videos recuerdas?" (0 · 1–3 · 4–10 · 10+). La respuesta queda en el historial. Si no contestas, el signo sigue siendo la respuesta.
- **Esta semana**: además, la semana pasada a esta altura (lunes a hoy contra lunes al mismo día).
- **ver detalle** abre Inicio. **Cerrar** y el botón atrás lo cierran con su animación.

## probado

47 pruebas JUnit con los casos de uso (`RegistroVisitasTest`, `GuionTest`, `HistorialSesionesTest`, `SemanaTest`, `FormatoTest`, `MascotaTest`). Todo el código (menos la notificación del modo investigación, que usa androidx) y la app de diseño compilan contra Android 16 sin avisos. **No se ha probado en el celular**: las animaciones hay que verlas en la app de diseño o en el teléfono.

## archivos

```
com/love/yourself/
├── InicioActivity.kt       lo que se ve al abrir la app
├── AjustesActivity.kt      permisos, apps, modo dev, modo investigación
├── OnboardingActivity.kt   la bienvenida
│
├── config/
│   ├── Config.kt           todos los números ajustables (igual en la app de diseño)
│   └── Ajustes.kt          lo que elige la persona: apps, actividad, modo dev
│
├── sesion/                 las reglas, sin pantallas (se prueban solas)
│   ├── RegistroVisitas.kt  cuándo abre, inicia y cierra una sesión
│   ├── Guion.kt            cuándo puede hablar la mascota
│   ├── Reloj.kt            reloj acelerable (modo dev)
│   ├── AcumuladoSemanal.kt arrastre por día
│   ├── HistorialSesiones.kt cada sesión cerrada, para Inicio
│   └── Semana.kt           fechas de lunes a domingo
│
├── servicio/               lo que corre encima de las otras apps
│   ├── SesionService.kt    accesibilidad: une todo
│   ├── OverlayFriccion.kt  ventanas: caja dev → burbuja → cielo
│   ├── OverlayBrief.kt     ventana del resumen
│   └── DiarioDev.kt        lo que el servicio va viendo (modo dev)
│
├── vistas/                 cómo se ve todo. IDÉNTICA en la app de diseño
│   ├── BurbujaView.kt      la burbuja y su animación
│   ├── Mascota.kt          momentos, caras y frases
│   ├── CieloView.kt  VistaCielo.kt        el cielo
│   ├── VistaBrief.kt  BarraOrigen.kt      el resumen
│   ├── VistaInicio.kt  BarrasSemana.kt    Inicio
│   ├── VistasOnboarding.kt                la bienvenida
│   ├── CajaDev.kt          la caja del modo dev
│   ├── Animacion.kt        curvas y piezas de animación
│   ├── Colores.kt  Formato.kt  Imagenes.kt
│
└── investigacion/          medir cortes de plano (igual que fase 05)
    └── CapturaContinuaService.kt, DetectorCortes.kt, RegistroCsv.kt
```

Para pasar un diseño de la app de diseño a esta: copiar **`vistas/` entera** y `config/Config.kt`, siempre archivos completos.

## abierto

- Qué hace "Salir" (hoy: manda al inicio).
- "Oscuridad" en el resumen sigue en `—`.
- Qué otros momentos agrandan la burbuja. Se agregan en `Momento`, en `Mascota.kt`.
- El cerebro *sad* es provisorio: contradice que la mascota no juzga.
- "recuerdas" ahora se puede contestar: ¿suma reflexión o le quita fuerza al "?" como respuesta?
- Los minutos de la cara del día son provisorios, como todos los de `Config.kt`.
- La burbuja a 21 dp del borde puede quedar bajo la barra de estado de Android (`MARGEN_SUPERIOR_DP`).
