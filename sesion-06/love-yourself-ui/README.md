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
| **tiempo de sesión** | De 0 a 20 min. Lo muestra la burbuja |
| **▶** | El tiempo corre solo, un segundo por segundo. ⏸ lo detiene |
| **swipes por minuto** | El número que muestra la caja |
| **0:00 / 10 min** | Saltos a los momentos donde cambia algo |
| **Cielo** | Abre el cielo de los 10 minutos (también sale solo al llegar a 10 min) |
| **Resumen** | Abre el resumen de salida con el tiempo del deslizador (en 0, los números del wireframe) |
| **burbuja** | Un botón por momento: agranda la burbuja con la mascota |
| **⚙** | Esconde el panel, para ver la pantalla limpia o sacar una captura |

Detrás hay un reel de mentira con las posiciones del wireframe WF2, para ver
la caja y el cielo sobre algo parecido a Instagram.

## Pasar un diseño al celular

Estos archivos son **idénticos** en las dos apps:

```
app/src/main/java/com/love/yourself/lab/
├── VistasFriccion.kt   la caja del modo dev
├── CieloView.kt        el cielo dibujado (degradado, sol, nubes)
├── VistaCielo.kt       la pantalla del cielo: mensaje, clima, Seguir / Salir
├── BurbujaView.kt      la burbuja de arriba, su animación y los momentos
├── VistaBrief.kt       el resumen de salida
├── BarraOrigen.kt      la barra arrastrado / buscado
├── ColoresBrief.kt     los grises del resumen
├── FormatoBrief.kt     los textos del resumen
└── Config.kt           tiempos y opacidad máxima del cielo
```

Lo que ajustes acá, lo copias **entero** a:

```
sesion-06/love-yourself-fase-06/app/src/main/java/com/love/yourself/lab/
```

Y las imágenes de `res/drawable-nodpi/` (cerebros y nube).

Siempre el archivo completo, nunca pedazos: así no hay que mezclar nada.

`MainActivity.kt` y `FondoReel.kt` son solo del simulador. No se copian.

## Lo que no está

- Cualquier medición real: esto es un maniquí.
