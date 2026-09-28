# love-yourself-ui

App solo de diseño. Muestra las pantallas de la v6 dentro de una pantalla
normal, sin permisos, sin accesibilidad y sin Instagram. Sirve para ajustar el
diseño en el emulador sin pasar por el celular.

No mide nada. El tiempo de arrastre y los swipes se mueven a mano.

## Abrirla

1. Android Studio → *Open* → esta carpeta (`sesion-06/love-yourself-ui`).
2. *Device Manager* → crear un emulador (cualquier teléfono, Android 14 o más).
3. *Run*.

## Controles

| | |
|---|---|
| **tiempo de arrastre** | De 0 a 20 min. En 0 no hay sesión: solo la mascota leyendo |
| **swipes por minuto** | El número que muestra la caja |
| **0:00 / 5 min / 15 min** | Saltos a los momentos donde cambia algo |
| **Decisión** | Abre la pantalla de los 15 minutos |
| **Resumen** | Abre el resumen de salida con el tiempo del deslizador (en 0, los números del wireframe) |
| **mascota** | *auto* la deja seguir al deslizador; los otros la fijan en un estado |
| **⚙** | Esconde el panel, para ver la pantalla limpia o sacar una captura |

Detrás hay un reel de mentira con las posiciones del wireframe WF2, para ver
la caja y el cielo sobre algo parecido a Instagram.

## Pasar un diseño al celular

Estos archivos son **idénticos** en las dos apps:

```
app/src/main/java/com/love/yourself/lab/
├── VistasFriccion.kt   caja, decisión, y dónde van caja y mascota
├── CieloView.kt        el cielo
├── MascotaView.kt      la mascota (colores arriba del archivo)
├── VistaBrief.kt       el resumen de salida
├── BarraOrigen.kt      la barra arrastrado / buscado
├── ColoresBrief.kt     los grises del resumen
├── FormatoBrief.kt     los textos del resumen
└── Config.kt           tiempos y opacidad máxima del cielo
```

Lo que ajustes acá, lo copias **entero** a:

```
sesion-04/avance/files/love-yourself-fase-06/app/src/main/java/com/love/yourself/lab/
```

Siempre el archivo completo, nunca pedazos: así no hay que mezclar nada.

`MainActivity.kt` y `FondoReel.kt` son solo del simulador. No se copian.

## Lo que no está

- Cualquier medición real: esto es un maniquí.
