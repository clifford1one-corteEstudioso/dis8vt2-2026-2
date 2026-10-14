# sonda PROBE2 — primera lectura del árbol de Instagram

Prueba del 14-10-2026 con `love-yourself-sonda` (copia de la fase-06 con la sonda PROBE2).
Pregunta: **¿el árbol de accesibilidad de Instagram distingue contenido elegido de contenido empujado?**

## Qué se hizo

- Vuelco del árbol de la ventana activa (`rootInActiveWindow`) cuando el paquete es Instagram, cada 1,5 s como máximo, con tope de 300 nodos y 25 niveles.
- La primera versión (sin tope, en el hilo principal) apagó el teléfono. La v2 corre en su propio hilo y con tope, y no tuvo problemas.
- Recorrido: perfil de una cuenta seguida (`elhonestomike`) → un reel desde su grilla → inicio de Instagram → pestaña Reels → salida al inicio del teléfono.

## Lo que el árbol sí muestra

| Señal | Dónde aparece | Qué dice |
| --- | --- | --- |
| **Si sigues la cuenta** | perfil, `profile_header_follow_button` | `text=Siguiendo` · `desc=Sigues a El Honesto Mike` |
| **Desde dónde se abrió** | contenedores | perfil: `profile_viewpager`, `clips_grid_recyclerview`. Pestaña Reels: `root_clips_layout`, `clips_viewer_view_pager`, título `clips_viewer_action_bar_title text=Reels` |
| **Autor del reel** | `clips_media_component` | `desc=Reel de andiefowler.wav. Toca dos veces…` |
| **Recomendación explícita** | inicio, sobre el feed | `text=Sugerencias para ti` |
| **Grilla del perfil** | `preview_clip_thumbnail` | `desc=Reel de elhonestomike. Número de visualizaciones 22.1 mil` |
| **Pestaña activa** | barra inferior | `feed_tab`, `clips_tab`, `search_tab`, `profile_tab` |

## Lo que todavía no sabemos

- **Falta el volcado del reel elegido.** Entre 16:53:44 (se abre el reel desde la grilla) y 16:54:10 no hubo volcado: mientras se mira un reel sin deslizar no llegan eventos y la sonda no se dispara. Hay que repetir esa parte deslizando un poco dentro del reel.
- **En el reel de la pestaña Reels no aparece el botón Seguir.** `clips_media_info_component` y `clips_ufi_component` salen sin hijos: el nombre, el botón y los "me gusta" no se exponen como nodos sueltos (o quedan bajo el tope). Solo el autor, dentro del `desc`.
- No sabemos si en ese reel el autor era alguien que sigues o no: hay que anotarlo durante la prueba.

## Hipótesis de trabajo

El árbol no dice "esto es algoritmo", pero sí deja ver **el camino**: un reel que vive dentro del perfil (`profile_viewpager`) llegó por elección; uno que vive bajo `root_clips_layout` con título "Reels", o bajo "Sugerencias para ti", llegó empujado. **Distinguir por contexto de navegación parece posible; distinguir por relación con el autor, todavía no.**

## Próxima prueba

1. Perfil seguido → abrir un reel → **deslizar apenas** dentro del reel → copiar ese volcado.
2. Pestaña Reels → un reel de cuenta que **no** sigues (anotar el usuario) → copiar el volcado.
3. Comparar los contenedores y el título de la barra superior en los dos.

## Ojo con publicar esto

Los volcados traen nombres de usuario de terceros (quiénes siguen la cuenta) y el widget de tiempo de uso del teléfono. Antes de subirlos a GitHub, anonimizar o dejar solo los fragmentos de `volcados-clave.txt`.

## Archivos

- `volcados-clave.txt`: las líneas que sostienen cada fila de las tablas, con su hora.
- `volcado-completo.txt`: pegar aquí el log entero de Logcat (no lo incluyo: es tuyo y trae datos personales).
