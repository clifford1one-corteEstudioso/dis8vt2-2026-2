# mapa-1-visual — Viaje del Usuario (versión imprimible)

Recreación en HTML/CSS del **mapa 1** (User Journey Map) de [../README.md](../README.md), con estética
de *pattern recognition* / detección de objetos: cajas delimitadoras con escuadras, etiquetas de
clase y código de color por capa de lectura.

## Archivos

| archivo | qué es |
| - | - |
| `index-v2.html` | **versión de lámina.** Síntesis: una línea por celda, la curva emocional como columna vertebral. Para colgar e imprimir. |
| `index-v1.html` | versión extendida: la tabla completa, 5 capas × 6 fases con el texto íntegro. Para leer de cerca o como respaldo del contenido. |
| `mapa-1-a3.pdf` | export A3 horizontal de la **v1** (1191 × 842 pt, 1 página). |

## Qué cambia de v1 a v2

La v1 pone en la hoja todo lo que hay en la tabla. La v2 baja la densidad por cuatro vías:

- **Una etiqueta de clase por fila**, en el eje izquierdo, en vez de un chip por cada celda.
- **Fuera los índices, códigos y valores `conf`** — el ornamento de detección quedó reducido a las
  escuadras de las cajas.
- **La curva emocional absorbe la fila "emoción"**: la palabra de cada fase cuelga de su nodo, así
  que una fila entera de la tabla desaparece sin perder el dato.
- **Textos a una línea**, tipografía más grande (3.7 mm ≈ 10.5 pt) y más aire entre cajas.

Quedan 4 filas de texto en vez de 5, y ~1/3 de las palabras. El texto largo sigue existiendo en la
v1 y en la tabla original.

## Imprimir

**Opción rápida:** manda `mapa-1-a3.pdf` a imprimir — ya viene a medida (420 × 297 mm), sin márgenes.
Ojo: ese PDF es de la **v1**. Para la v2 hay que exportarla.

**Desde el navegador:**

1. Abre el `.html` en Chrome o Edge.
2. Botón *Imprimir A3 horizontal* — o `Ctrl+P`.
3. En el diálogo:
   - Papel: **A3** · Orientación: **horizontal** · Márgenes: **ninguno**
   - ✅ **Gráficos de fondo** (`Background graphics`) — sin esto se pierden los colores y las cajas.
4. *Guardar como PDF* o imprimir directo.

Por línea de comandos:

```powershell
& "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --headless=new `
  --no-pdf-header-footer --print-to-pdf="mapa-1-v2-a3.pdf" `
  "file:///D:/GitHub/dis8vt2-2026-2/sesion-02/entrega/mapa-1-visual/index-v2.html"
```

## Dos temas

- **Oscuro** (por defecto): el look de overlay de detección. Cubre casi toda la hoja de tinta —
  pide impresión láser o papel de buen gramaje.
- **Claro**: botón *Tema oscuro / claro*, o abre el archivo con `?theme=light`. Mismo lenguaje
  gráfico, mucho menos tinta, más legible en fotocopia.

## Cómo está construido

- Una sola grilla CSS: `16mm` para el eje de clases + 6 columnas de fase; filas en `fr`, así que
  todo se reparte solo dentro de los 297 mm de alto. Si alargas un texto, la caja no se desborda.
- Las **escuadras** de cada caja son 8 `linear-gradient` de fondo (`.det`), no imágenes.
- Color por clase en las variables `--c-*` del `:root`. Cambia ahí y cambia todo el mapa.
- La **curva de valencia emocional** es un SVG estirado con `preserveAspectRatio="none"`; los nodos
  y sus etiquetas son elementos HTML posicionados en `%` (`--x` / `--y`), para que no se deformen.
  Para mover un punto, edita ese `--y`: 50% es neutro, más abajo es más negativo.

## Sobre el contenido

Todo sale de la tabla de `mapa-1` en [../README.md](../README.md). En la v2 las frases están
condensadas —misma idea, menos palabras—; si alguna te suena mal dicha para presentar, cámbiala
directo en el HTML, son literales de texto plano.

En la v1, los índices (`ACC·01`), códigos (`CLS.02`) y valores `conf 0.9x` son **recurso gráfico**,
no métricas reales, y así está declarado en el pie de esa lámina. La v2 ya no los usa.
