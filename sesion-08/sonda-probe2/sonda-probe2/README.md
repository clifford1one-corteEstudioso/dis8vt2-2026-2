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

## Prueba 2 (17:09): reel elegido, colaborativo de dos cuentas seguidas

Recorrido: inicio → publicación de `piruetas.xyz` (colaboración con `montoyamoraga`) → perfil de piruetas.xyz (`Siguiendo`) → reel desde su grilla → deslizar apenas.

Comparado con el reel de la pestaña Reels de la prueba 1:

| | Reel **elegido** (desde perfil) | Reel de la **pestaña Reels** |
| --- | --- | --- |
| Botón arriba a la izquierda | `desc=Volver` | `desc=Crear un reel` |
| Título | `Reels` | `Reels` |
| Barra inferior de pestañas (`tab_bar`) | **no está** | está, con `clips_tab` |
| Caja de comentario abajo | `Agrega un comentario…` | no está |
| Autor en el árbol | `Reel de montoyamoraga` y `Reel de piruetas.xyz` | `Reel de andiefowler.wav` |
| Botón Seguir dentro del reel | no aparece | no aparece |

**Lo nuevo:** la señal más limpia es **"Volver" + sin barra de pestañas**. Un reel abierto desde otra pantalla (perfil, publicación) se apila encima y trae "Volver"; el de la pestaña Reels es la raíz y muestra la barra de pestañas. Eso distingue **cómo llegaste**, no a quién sigues.

**Cuidado:**
- El visor carga el reel actual y el siguiente a la vez: aparecen dos `clips_media_component`. Todavía no sabemos cuál está en pantalla (¿el primero?).
- En un colaborativo aparecen los dos autores, porque el reel de al lado era del otro. No hay que leerlo como "dos autores".
- Si desde un perfil sigues deslizando, Instagram puede empezar a meter reels recomendados en el mismo visor, que sigue teniendo "Volver". El contexto se hereda: falta probarlo.

## Prueba 3 (17:11): reel empujado, cuenta no seguida, pestaña Reels

Recorrido: pestaña Reels → reel de `historiantique` (no la sigues) → menú de opciones → "No me interesa".

| | Elegido (prueba 2) | Empujado (pruebas 1 y 3) |
| --- | --- | --- |
| Botón arriba a la izquierda | `Volver` | `Crear un reel` |
| `tab_bar` con `clips_tab` | no | sí |
| Caja de comentario | sí | no |
| Autor en el árbol | sí (`Reel de …`) | sí (`Reel de …`) |
| Relación con el autor | no se ve en el reel | no se ve en el reel |

Tres pruebas, el mismo patrón: **el contexto separa elegido de empujado; la relación con el autor sigue sin verse dentro del visor.**

**Lo nuevo:**
- El menú de opciones del reel trae `Por qué ves esta publicación`, `Me interesa` y `No me interesa`. Instagram nombra su propia recomendación en el árbol.
- Al tocar "No me interesa", el componente de info se reemplaza por `Gracias por los comentarios. Te sugeriremos menos publicaciones como esta.` Ese texto es un rechazo explícito y legible.
- Aparecieron tres autores seguidos (`historiantique`, `shitpost.fau`, `index.opensource`): el actual y los precargados.

**Ruido a corregir:**
- La sonda volcó también el resumen de Love Yourself ("uf, gracias", "Estuviste 1 minuto"): `rootInActiveWindow` puede ser nuestra propia ventana. Filtro posible: saltar si el paquete de la raíz no es Instagram.
- Cada volcado trae también la página del feed (historias, publicaciones) dentro de `swipeable_tab_view_pager`, oculta detrás del visor de Reels.

## Prueba 4 (17:16): cuenta SEGUIDA que aparece en la pestaña Reels (el cruce)

Recorrido: pestaña Reels → pasar reels → aparece `succeedo_cashwell` (Daniel Raphael, que sí sigues) → deslizar casi entero y volver, cambiar, volver, deslizar apenas → abrir su perfil.

| | Seguida, en pestaña Reels | No seguida, en pestaña Reels |
| --- | --- | --- |
| Botón arriba a la izquierda | `Crear un reel` | `Crear un reel` |
| `tab_bar` con `clips_tab` | sí | sí |
| Caja de comentario | no | no |
| Autor | `Reel de succeedo_cashwell` | `Reel de historiantique` |

**Resultado (corregido, ver prueba 6):** el contexto es idéntico; lo de "seguir" no se pudo ver por el tope de profundidad de la sonda. Seguir o no seguir no cambia el contexto del visor. Lo que la sonda distingue es **por dónde llegaste**, no a quién sigues. La relación solo aparece al entrar al perfil (`Siguiendo` · `Sigues a Daniel Raphael`), y ese perfil ya trae `Volver` porque se apila encima.

**Lo nuevo: los anuncios sí se distinguen.**
- Reels de `verisurecl`, `mycocos.cl`, `meltpizzas`, `mdf.chile` traen `clips_single_image_ads_media_content` en vez de `clips_single_media_component`.
- Algunos traen el texto del anuncio: `🏡 Protege tu hogar con ZeroVision…`, `Cocos Week: Hasta 50% OFF 🚨`.
- En ~30 s de pestaña Reels aparecieron 4 anuncios.

## Prueba 5 (19-10, 20:00): reel enviado por mensaje y lo que viene después

Recorrido: chat → reel que mandó Miguel (`amoi.builds`) → siguiente (`salopeyjoey`) → siguiente (anuncio `bitepal_app`).

| | Reel enviado | Siguientes |
| --- | --- | --- |
| Botón izquierda | `Volver` | `Volver` (en el anuncio no apareció) |
| Título arriba | `sender_username_or_fullname text=Miguel` + `sender_timestamp text=Hace 1 sem` | `suggested_title text=Sugerencias` |
| Abajo | `reply_bar_edittext text=Responder a Miguel` | `send_cta text=Enviar al chat` |

**Hallazgo principal: "Volver" no basta.** El visor sigue mostrando "Volver" cuando ya entró contenido del algoritmo. La frontera la marca Instagram con dos ids distintos:
- `sender_username_or_fullname` → lo eligió otra persona (te lo mandaron).
- `suggested_title` = "Sugerencias" → lo puso el algoritmo.
(En pantalla se leía "Recomendados"; en el árbol dice "Sugerencias". El id es lo que importa.)

**Lo nuevo 2: aparece la relación con el autor.** Esta vez `clips_media_info_component` sí trae hijos:
- `clips_author_username text=amoi.builds`
- `inline_follow_button text=Seguir desc=Seguir a amoi` → no lo sigues.
En las pruebas 1–4 ese componente salía vacío (¿actualización de Instagram o diferencia del visor?). Falta ver qué muestra con una cuenta seguida: ¿"Siguiendo" o nada?

**Lo nuevo 3: anuncios con etiqueta explícita.** `clips_video_container desc=Reel patrocinado de bitepal_app` y un nodo `text=Publicidad`.

**Nota:** la detección actual de Love Yourself (`sesión inicia`) recién se activó en el tercer reel, ya en "Sugerencias".

## Prueba 6 (19-10, 20:10): perfil seguido, deslizar hasta el final

Recorrido: perfil de `clifford1one` → reel de su grilla → deslizar uno por uno.

- El visor nunca metió reels de otras cuentas: **al terminarse los de la cuenta, no deja seguir deslizando.**
- Todo el rato: `Volver` + `clips_viewer_action_bar_title text=Reels` + `comment_composer_text_view text=Agrega un comentario…`. Nunca aparece `suggested_title`.
- Una vez apareció precargado `Reel de lid.udp` (¿colaboración o repost en la grilla? sin confirmar).

**Conclusión:** desde un perfil no hay fuga hacia el algoritmo. "Volver" + título "Reels" = elegido, con fin natural.

## Prueba 7 (21-10, 08:26): pestaña Reels con sonda v3 (profundidad 40)

Cuenta seguida: `succeedo_cashwell`. No seguida: `marcosalamancacea`. Todo en la pestaña Reels ("Crear un reel" + título "Reels" + tab_bar).

| Reel | `inline_follow_button` | Lectura |
|---|---|---|
| succeedo_cashwell (08:26:39, :40, :42, :53) | **no aparece** | seguida |
| marcosalamancacea (08:26:50, :56, :58, 08:27:01) | `text=Seguir desc=Seguir a Marco Salamanca Cea` | no seguida |
| abeandchloe, raree200x, setupsai, lindenwhitt, nick.vost, cabrodecente, crocodile.jackson, farigatouu | `Seguir` | no seguidas |
| 10l80.cisco y 3 personas más (colab) | no aparece | **ambiguo** |
| carnavalonline, frutosdelmaipo, skool_com, getsociallite | — | anuncios ("Reel patrocinado", "Publicidad", `clips_single_image_ads_media_content`) |

**Conclusión:** con profundidad suficiente, el árbol expone la relación con el autor. El botón "Seguir" está si y solo si no sigues la cuenta (sin botón "Siguiendo": simplemente desaparece).

Cuidados para el clasificador:
- **Reel precargado:** hay varios `clips_media_component`; el actual es el que trae hijos en `clips_author_info_component`.
- **Colaborativos y reels propios:** sin botón, pero no implican "seguida". Tratarlos como categoría aparte o mirar el texto "y N personas más".

## ✅ Estado del Probe 2: resuelto (positivo)

El árbol no etiqueta "algoritmo", pero combinando señales se puede clasificar el origen:

| Señal | Nodo | Indica |
|---|---|---|
| Visor de DM | `sender_username_or_fullname` | lo eligió alguien que conoces |
| Sugerencias tras DM | `suggested_title text=Sugerencias` | algoritmo |
| Pestaña Reels | "Crear un reel" + título "Reels" + tab_bar | algoritmo |
| Visor de perfil | "Volver" + título Reels, sin sugerencias | elección propia |
| Relación con autor | `inline_follow_button` presente/ausente | no seguida / seguida |
| Anuncio | "Reel patrocinado" / `Publicidad` | publicidad |

## ⚠️ Corrección: el tope de profundidad escondía datos

En las pruebas 1–4 y 6, `clips_media_info_component` salía "sin hijos". En la prueba 5 sí trajo hijos (autor, botón Seguir). La diferencia: en las pruebas 1–4 y 6 el visor está más hundido en el árbol (debajo del feed) y ese componente queda en el nivel ~25, justo donde la sonda dejaba de bajar (`PROBE2_MAX_PROFUNDIDAD = 25`). En la prueba 5 el visor está más arriba, así que el tope no lo cortó.

**Consecuencia:** la conclusión de la prueba 4 ("seguir o no seguir no cambia nada en el visor") **no es válida**. Probablemente el botón Seguir/Siguiendo sí está, pero la sonda no llegaba a verlo.

**Arreglo (sonda v3):** profundidad 40, 400 nodos, y se ignoran los volcados cuando la ventana activa no es Instagram.

## Lo que todavía no sabemos

- ~~Falta el volcado del reel elegido~~: resuelto en la prueba 2 (hay que deslizar apenas para que la sonda se dispare).
- ~~Falta el caso "no seguido" desde la pestaña Reels~~: resuelto en la prueba 3.
- ~~**En el reel de la pestaña Reels no aparece el botón Seguir.**~~ Era el tope de profundidad (ver corrección). `clips_media_info_component` y `clips_ufi_component` salen sin hijos: el nombre, el botón y los "me gusta" no se exponen como nodos sueltos (o quedan bajo el tope). Solo el autor, dentro del `desc`.
- ~~Falta el cruce~~: resuelto en la prueba 4 para el contexto. **Resuelto en la prueba 7:** en una cuenta seguida el botón no aparece.

## Hipótesis de trabajo

El árbol no dice "esto es algoritmo", pero sí deja ver **el camino**: un reel que vive dentro del perfil (`profile_viewpager`) llegó por elección; uno que vive bajo `root_clips_layout` con título "Reels", o bajo "Sugerencias para ti", llegó empujado. **Distinguir por contexto es posible, pero la señal fiable no es "Volver" sino el título del visor: remitente / "Reels" / "Sugerencias". La relación con el autor está en `inline_follow_button` (confirmado en la prueba 7). Los anuncios se distinguen por id y por "Publicidad".**

## Próxima prueba

Ninguna para el Probe 2. Siguiente paso: definir "arrastre" y convertir la sonda en clasificador.

## Ojo con publicar esto

Los volcados traen nombres de usuario de terceros (quiénes siguen la cuenta) y el widget de tiempo de uso del teléfono. Antes de subirlos a GitHub, anonimizar o dejar solo los fragmentos de `volcados-clave.txt`.

## Archivos

- `volcados-clave.txt`: las líneas que sostienen cada fila de las tablas, con su hora.
- `volcado-completo.txt`: pegar aquí el log entero de Logcat (no lo incluyo: es tuyo y trae datos personales).
