# love-yourself-ui

App solo de diseño. Muestra las pantallas de la v6 dentro de una pantalla
normal, sin permisos, sin accesibilidad y sin Instagram. Sirve para ajustar el
diseño (y ver las animaciones) en el emulador sin pasar por el celular.

No mide nada. El tiempo de arrastre y los swipes se mueven a mano.

## Abrirla

1. Android Studio → *Open* → esta carpeta (`sesion-06/love-yourself-ui`).
2. *Device Manager* → crear un emulador (cualquier teléfono, Android 14 o más).
3. *Run*.

## Controles

| | |
|---|---|
| **tiempo de sesión** | De 0 a 20 min. Lo muestra la burbuja, y su cerebro chico se va gastando |
| **▶** | El tiempo corre solo, un segundo por segundo. ⏸ lo detiene |
| **swipes por minuto** | El número que muestra la burbuja |
| **0:00 / 10 min** | Saltos a los momentos donde cambia algo |
| **Cielo** | Abre el cielo de los 10 minutos (también sale solo al llegar a 10 min) |
| **Resumen** | Abre el resumen de salida con el tiempo del deslizador (en 0, los números del wireframe). "ver detalle" abre Inicio y el "?" de recuerdas se puede contestar |
| **Inicio** | La pantalla de la app, con una semana inventada. El ⚙ la cierra |
| **Bienvenida** | Las cuatro pantallas del onboarding, en orden. Los botones avanzan |
| **burbuja** | Un botón por momento: agranda la burbuja con la mascota |
| **⚙** (arriba a la derecha) | Esconde el panel, para ver la pantalla limpia o sacar una captura |

Detrás hay un reel de mentira con las posiciones del wireframe WF2, para ver
la burbuja y el cielo sobre algo parecido a Instagram.

## Pasar un diseño al celular

Estos archivos son **idénticos** en las dos apps:

```
app/src/main/java/com/love/yourself/
├── config/Config.kt         tiempos y textos ajustables
└── vistas/
    ├── BurbujaView.kt       la burbuja de arriba y su animación
    ├── Mascota.kt           momentos, caras y frases del cerebro
    ├── CieloView.kt         el cielo dibujado (degradado, sol, nubes, cerebro)
    ├── VistaCielo.kt        la pantalla del cielo: mensaje, clima, Seguir / Salir
    ├── VistaBrief.kt        el resumen de salida
    ├── BarraOrigen.kt       la barra arrastrado / buscado
    ├── VistaInicio.kt       Inicio: el día, la semana, las últimas sesiones
    ├── BarrasSemana.kt      las barras de la semana
    ├── VistasOnboarding.kt  la bienvenida: permisos, apps, actividad
    ├── CajaDev.kt           la caja del modo dev
    ├── Animacion.kt         curvas y piezas de animación
    ├── Colores.kt           los grises de los wireframes
    ├── Formato.kt           los textos con números
    └── Imagenes.kt          cargar los cerebros a la escala justa
```

Lo que ajustes acá, lo copias **entero** (la carpeta `vistas/` completa y
`config/Config.kt`) a:

```
sesion-06/love-yourself-fase-06/app/src/main/java/com/love/yourself/
```

Y las imágenes de `res/drawable-nodpi/` (cerebros y nube).

Siempre archivos completos, nunca pedazos: así no hay que mezclar nada.

`MainActivity.kt` y `FondoReel.kt` son solo del simulador. No se copian.

## Lo que no está

- Cualquier medición real: esto es un maniquí.
