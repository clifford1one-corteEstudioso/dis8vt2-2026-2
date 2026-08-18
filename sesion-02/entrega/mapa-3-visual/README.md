# mapa-3-visual — Diagrama de Flujo Técnico (versión imprimible)

Recreación en HTML/CSS del **mapa 3** de [../README.md](../README.md): arquitectura del sistema,
permisos y gestión de excepciones. Mismo sistema visual que [../mapa-1-visual/](../mapa-1-visual/) y
[../mapa-2-visual/](../mapa-2-visual/).

## Archivo

`index.html` — autocontenido, se abre con doble clic. Sin PDF: exporta tú cuando lo des por bueno.

## Cómo se lee

Misma partición que el mapa 2, para que las dos láminas se lean con la misma lógica:

- **A · Arranque y permisos** — vertical, bloqueante. Dos guardas (`canDrawOverlays()`,
  `onServiceConnected()`), cada una con su cadena de recuperación en rojo a la izquierda: guardar lo
  que sirva, avisar al usuario, mandarlo a ajustes, reintentar.
- **B · Ciclo de análisis** — circuito cerrado, una vuelta por pantalla. Dos puntos de fallo dentro:
  la inferencia que no llega a tiempo y el permiso de superposición revocado a mitad de sesión.

Todo lo rojo es excepción. La lectura rápida de la hoja es cuánto rojo hay y dónde: casi la mitad
del diagrama es gestión de fallos, que es justo lo que pedía el enunciado.

### El conector Ⓐ

La rama «permiso revocado a mitad de sesión» (`n11b → n0c` en el mermaid) va desde abajo a la
derecha hasta arriba a la izquierda, cruzando la hoja entera. Dibujarla habría metido una línea
larga por encima de medio diagrama, así que está resuelta con un **conector de página**: el círculo
Ⓐ junto a «descartar el resultado» continúa en el círculo Ⓐ que entra al aviso de permiso. Es
notación estándar de diagramas de flujo y está en la leyenda. Si prefieres la línea explícita,
borra los nodos `cA` y `cB` y añade una arista `q2 → r1` con codos en `pts`.

## Qué cambié respecto del mermaid original

- Las preguntas de los rombos se acortaron y **el nombre de la API salió fuera**, como pie técnico
  en mono al lado del rombo (`canDrawOverlays()`, `onServiceConnected()`). Nada se pierde y el rombo
  deja de ir apretado de texto.
- Igual con los procesos: `extrae el contenido` + `AccessibilityNodeInfo`, `envía a VLM local` +
  `Gemma 3n`, `redirigir a ajustes de superposición` + `ACTION_MANAGE_OVERLAY_PERMISSION`. La línea
  mono es la que lleva el dato técnico.
- Los mensajes al usuario van entrecomillados y literales, salvo un recorte en el de accesibilidad
  («Para continuar, enciéndalas» en vez de «Para continuar con el análisis, por favor enciéndalas»).
- La etiqueta larga `timeout / error de inferencia` quedó como `timeout / error`, y
  `no, revocado a mitad de sesión` como `no · revocado`.

## Tamaño de hoja

**480 × 350 mm**, no A3. El diagrama no cabía entero en A3 y agrandar la hoja era más sano que
apretar el diagrama. Para llevarlo a A3 real, imprime el PDF con **«ajustar a página»**: baja a 87 %
y sigue leyéndose bien, porque el original va un 14 % sobredimensionado.

## Imprimir

1. Abre `index.html` en Chrome o Edge.
2. Botón *Imprimir / exportar PDF* — o `Ctrl+P`.
3. Márgenes **ninguno** y activa ✅ **Gráficos de fondo** (`Background graphics`), o se pierden
   colores y cajas. El tamaño de papel lo fija la propia hoja vía `@page`.

```powershell
& "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --headless=new `
  --no-pdf-header-footer --print-to-pdf="mapa-3.pdf" `
  "file:///D:/GitHub/dis8vt2-2026-2/sesion-02/entrega/mapa-3-visual/index.html"
```

Tema claro: botón en pantalla o `index.html?theme=light`. En este mapa se nota más, porque el rojo
de las excepciones sobre fondo oscuro come bastante tinta.

## Cómo mover cosas

Igual que el mapa 2. Dos listas al final del archivo, en **milímetros**: el lienzo nativo mide
392 × 221 mm y el `viewBox` del SVG usa las mismas unidades, así que **1 unidad = 1 mm de lienzo
nativo**. Ese lienzo entero se amplía después con la variable `--k` de `.canvas` (ahora `1.14`):
sube o baja ese número y crece o encoge todo junto —cajas, textos y cables— sin tocar una sola
coordenada.

> **Ojo si tocas el layout:** `.canvas` lleva `flex:none` y `.page` también. Sin eso, el navegador
> comprime en silencio la caja cuando el contenido no cabe; el SVG se encoge con ella pero los nodos
> HTML no, y el diagrama sale descuadrado y recortado por abajo. Fue exactamente el bug que tenía
> esta lámina.

```js
{id:'p2', k:'box', c:'--c-proc', x:288, y:24, w:64, h:26,
 t:'extrae el contenido', code:'AccessibilityNodeInfo'}
```

- `x`, `y` son el **centro**. `k`: `box`, `dec` (rombo), `terminal`, `conn` (círculo conector).
- `code`: la línea mono dentro de la caja. `cap`: el pie técnico fuera del rombo
  (`capside:'l'` lo pone a la izquierda si a la derecha estorba).

```js
{f:'p4', fs:'l', t:'q1', ts:'r', lab:'timeout / error', lp:[336,80], k:'fix'}
```

- `fs` / `ts`: lado de salida y de entrada (`t`/`b`/`l`/`r`; en los rombos, los vértices).
- `pts`: codos intermedios. `lab` / `lp`: etiqueta y posición.
- `k`: `fix` (rojo), `loop` (punteado) o nada.

Regla que se repite en todo el mapa y conviene respetar si añades algo: en un rombo, **entra por
arriba, «sí» sale por donde sigue el flujo, «no» sale hacia la columna de recuperación, y el
reintento vuelve a entrar por el lado libre**. Es lo que evita que las líneas se crucen.
