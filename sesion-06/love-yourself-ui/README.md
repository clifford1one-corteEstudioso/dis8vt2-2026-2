# love-yourself-ui

App solo de diseño. Muestra las pantallas de la fase 05 dentro de una pantalla
normal, sin permisos, sin accesibilidad y sin Instagram. Sirve para ajustar el
diseño en el emulador sin pasar por el celular.

No mide nada. El tiempo y los swipes se mueven a mano.

## Abrirla

1. Android Studio → *Open* → esta carpeta (`sesion-06/love-yourself-ui`).
2. *Device Manager* → crear un emulador (cualquier teléfono, Android 14 o más).
3. *Run*.

## Controles

| | |
|---|---|
| **tiempo de sesión** | De 0 a 20 min. En 0 no hay sesión: no se ve nada |
| **swipes por minuto** | El número que muestra la caja |
| **0:00 / 5 min / 15 min** | Saltos a los momentos donde cambia algo |
| **Decisión** | Abre la pantalla de los 15 minutos |
| **⚙** | Esconde el panel, para ver la pantalla limpia o sacar una captura |

Detrás hay un reel de mentira con las posiciones del wireframe WF2, para ver
la caja y el cielo sobre algo parecido a Instagram.

## Pasar un diseño al celular

Estos tres archivos son **idénticos** en las dos apps:

```
app/src/main/java/com/love/yourself/lab/
├── VistasFriccion.kt   cómo se ven la caja y la pantalla de decisión
├── CieloView.kt        el cielo
└── Config.kt           tiempos y opacidad máxima del cielo
```

Lo que ajustes acá, lo copias **entero** a:

```
sesion-04/avance/files/love-yourself-fase-05/app/src/main/java/com/love/yourself/lab/
```

Siempre el archivo completo, nunca pedazos: así no hay que mezclar nada.

`MainActivity.kt` y `FondoReel.kt` son solo del simulador. No se copian.

## Lo que no está

- El resumen al salir (fase 06), porque todavía no está en `main`.
- Cualquier medición real: esto es un maniquí.
