# mapa-2-visual — Diagrama de Flujo Funcional (versión imprimible)

Recreación en HTML/CSS del **mapa 2** de [../README.md](../README.md): la lógica de navegación desde
la perspectiva del usuario. Mismo sistema visual que [../mapa-1-visual/](../mapa-1-visual/) —
escuadras de detección, color por clase, mono para lo técnico.

## Archivo

`index.html` — autocontenido, se abre con doble clic. Sin PDF: exporta tú cuando lo des por bueno.

## Cómo se lee

La hoja está partida en dos zonas, y esa partición **es** el argumento del mapa:

- **A · Puesta en marcha** — vertical, pasa una sola vez. Dos rombos de permiso, cada uno con su
  bucle de corrección en rojo. Son bloqueantes: no dejan avanzar hasta resolverse.
- **B · Bucle de análisis** — dibujado literalmente como un circuito cerrado, porque eso es: una
  vuelta por cada pantalla que aparece. El «No» del rombo es un atajo que cruza el centro del anillo
  y devuelve al usuario a scrollear sin enterarse de nada.

## Qué cambié respecto del mermaid original

- Los nodos `Sí` / `No` del mermaid (`n4`, `n5`, `n8`, `n9`, `n17`, `n18`) **pasaron a ser etiquetas
  sobre los conectores**. En el lenguaje de Garrett una rama de decisión se rotula en la flecha, no
  se convierte en una caja propia; el diagrama queda con 14 nodos en vez de 20 y se lee mucho mejor.
- El paréntesis de `n16` («que entre dentro de los parámetros predeterminados») salió del rombo y
  quedó como pie técnico en mono al lado. La pregunta corta va dentro, el detalle va fuera.
- El resto del texto es literal.

## Tamaño de hoja

**480 × 350 mm**, no A3. El diagrama no cabía entero en A3 y agrandar la hoja era más sano que
apretar el diagrama. Para llevarlo a A3 real, imprime el PDF con **«ajustar a página»**: baja a 87 %
y sigue leyéndose bien, porque el original va un 14 % sobredimensionado.

## Imprimir

1. Abre `index.html` en Chrome o Edge.
2. Botón *Imprimir / exportar PDF* — o `Ctrl+P`.
3. Márgenes **ninguno** y activa ✅ **Gráficos de fondo** (`Background graphics`), o se pierden
   colores y cajas. El tamaño de papel lo fija la propia hoja vía `@page`.

Por línea de comandos:

```powershell
& "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --headless=new `
  --no-pdf-header-footer --print-to-pdf="mapa-2.pdf" `
  "file:///D:/GitHub/dis8vt2-2026-2/sesion-02/entrega/mapa-2-visual/index.html"
```

Tema claro (mucha menos tinta): botón en pantalla o `index.html?theme=light`.

## Cómo mover cosas

El diagrama se dibuja desde dos listas al final del archivo, en **milímetros**. El lienzo nativo
mide 392 × 215 mm y el `viewBox` del SVG usa las mismas unidades, así que **1 unidad = 1 mm de
lienzo nativo**. Ese lienzo entero se amplía después con la variable `--k` de `.canvas` (ahora
`1.14`): sube o baja ese número y crece o encoge todo junto —cajas, textos y cables— sin tocar una
sola coordenada.

> **Ojo si tocas el layout:** `.canvas` lleva `flex:none` y `.page` también. Sin eso, el navegador
> comprime en silencio la caja cuando el contenido no cabe; el SVG se encoge con ella pero los nodos
> HTML no, y el diagrama sale descuadrado y recortado por abajo. Fue exactamente el bug que tenía
> esta lámina.

```js
{id:'apk', k:'box', c:'--c-proc', x:44, y:124, w:76, h:18, t:'Ejecutar APK'}
```

- `x`, `y` son el **centro** del nodo. Cámbialos y las flechas se recalculan solas.
- `k`: `box` (rectángulo), `dec` (rombo), `terminal` (píldora).
- `c`: color de clase, de las variables `--c-*` del `:root`.

```js
{f:'d1', fs:'r', t:'fix1', ts:'l', lab:'No', lp:[86,78], k:'fix'}
```

- `fs` / `ts`: por qué lado sale y entra la flecha (`t` arriba, `b` abajo, `l` izquierda,
  `r` derecha). En los rombos son los vértices.
- `pts`: codos intermedios, si el conector tiene que rodear algo.
- `lab` / `lp`: texto de la etiqueta y dónde va.
- `k`: `fix` (rojo, ruta de corrección), `loop` (punteado, retorno) o nada (flujo normal).

Las esquinas se redondean solas y la punta de flecha se orienta sola. Si mueves un nodo y una línea
queda cruzando una caja, añade o corre un codo en `pts`.
