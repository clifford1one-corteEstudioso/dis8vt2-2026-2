# Bitácora: factibilidad del proxy de arrastre

**Proyecto:** Cautiverio Voluntario · app Love Yourself (Android, Kotlin)
**Período:** 14 al 21 de octubre de 2026
**Herramienta:** sonda de accesibilidad `com.love.yourself.sonda`, una copia de la app con una sonda de diagnóstico

---

## 1. Qué había que responder

La tarea partía de este checklist:

**1. Factibilidad del proxy (urgente)**
- Probe 1: ¿MediaProjection entrega una imagen real o un cuadro negro?
- Probe 2: ¿el árbol de `AccessibilityNodeInfo` expone el origen del contenido en Reels, es decir, si lo eligió el usuario o lo propuso el algoritmo?
- Si el Probe 2 falla, definir un proxy alternativo o replantear la hipótesis.

**2. Margen de error del proxy**
- Comparar lo que registra la app con lo que reporta el usuario.
- Calcular los falsos positivos y los falsos negativos.
- Documentar el margen de error para la metodología.

**Orden de trabajo que acordamos:**
1. Cerrar el Probe 2.
2. Definir "arrastre".
3. Decidir sobre el Probe 1.
4. Construir el clasificador.
5. Hacer el piloto de validación.

| Paso | Estado |
|---|---|
| 1. Probe 2 | ✅ resuelto, con resultado positivo |
| 2. Definición de arrastre | ✅ escrita |
| 3. Probe 1 | ⏸ sin decidir; la propuesta es descartarlo (ver §6) |
| 4. Clasificador | ✅ funciona en la pestaña Reels, en DM y en Sugerencias. Explorar quedó aparcado |
| 5. Validación | ⏭ siguiente |

---

## 2. Método: la sonda

Un `AccessibilityService` lee el árbol de la pantalla de Instagram: ids, textos y descripciones de cada nodo. **No captura imagen.** En las primeras versiones escribía el árbol completo en Logcat (tag `PROBE2`). Desde la v4 lo interpreta y escribe una sola línea por reel (tag `CLASIF`).

- **Condición de lectura.** Solo lee cuando Instagram emite un evento, así que en las pruebas había que deslizar un poco para forzar una lectura.
- **Hilo propio y topes.** Lee en un hilo separado, como mucho una vez cada 1,5 s, y con tope de nodos y de profundidad.
- **Datos guardados.** Todo queda en Logcat, en el teléfono. No se guarda en archivos ni se envía a ninguna parte.

### Versiones

| Versión | Cambio | Por qué |
|---|---|---|
| v1 | Vuelca el árbol completo en el hilo principal | Primera prueba. **El teléfono se apagaba** |
| v2 | Hilo propio, una lectura a la vez, topes de 300 nodos y 25 niveles; nuevo applicationId | Estabilidad, y evitar el conflicto de firma con la app instalada |
| v3 | Topes de 400 nodos y 40 niveles; ignora ventanas que no son de Instagram | **Con 25 niveles se cortaba la información del autor** (ver §3.2); además la sonda volcaba nuestra propia pantalla de resumen |
| v4 | Pasa de volcar a **clasificar**: segmentos dirigido/arrastre, anuncios y relación con el autor | Paso 4 del plan |
| v5 | Lee **solo los nodos visibles** (`isVisibleToUser`); nuevo orden de reglas; vuelca el árbol cuando no reconoce un visor; cierra el segmento al salir de Instagram | La pestaña Reels oculta contaminaba la lectura de lo que se abría encima |

---

## 3. Probe 2: hallazgos

### 3.1 Pruebas

| # | Fecha | Situación | Qué mostró |
|---|---|---|---|
| 1 | 14-10 | Primera lectura: perfil, grilla, inicio y pestaña Reels | El árbol expone ids con nombre (`root_clips_layout`, `clips_media_component`, etc.) y el autor de cada reel en `desc` |
| 2 | 14-10 | Reel elegido desde la grilla de un perfil seguido | El visor muestra "Volver", el título "Reels" y un campo de comentarios, y **no tiene barra de pestañas** |
| 3 | 14-10 | Reel empujado en la pestaña Reels, de una cuenta no seguida | El visor muestra "Crear un reel", el título "Reels" y `tab_bar` |
| 4 | 14-10 | Cuenta **seguida** que aparece en la pestaña Reels | El contexto es idéntico al de una cuenta no seguida. La conclusión de entonces, "no se ve si la sigues", resultó **inválida** (ver §3.2) |
| 5 | 19-10 | Reel enviado por DM, y luego deslizar | Primer reel: `sender_username_or_fullname` (quién lo envió) y "Responder a…". Al deslizar aparece `suggested_title = "Sugerencias"`: **el algoritmo entra en el mismo visor**. "Volver" se mantiene, así que no sirve como señal |
| 6 | 19-10 | Reels de un perfil, hasta el final | Nunca se mezclan reels de otras cuentas. Al terminar la grilla no se puede seguir deslizando |
| 7 | 21-10 | Pestaña Reels con v3: una cuenta seguida y otra no | **Cuenta no seguida: aparece `inline_follow_button = "Seguir"`. Cuenta seguida: el botón no aparece.** Se repitió en todas las lecturas |

### 3.2 Corrección metodológica: el tope de profundidad

Con un tope de 25 niveles, `clips_media_info_component` (autor y botón Seguir) aparecía **vacío** en los visores más profundos. Por eso se concluyó, erróneamente, que "el botón Seguir no se expone". Con 40 niveles sí aparece.

> **Lección para la metodología:** la ausencia de una señal en un volcado parcial no prueba que la señal no exista.

### 3.3 Segunda corrección: pantallas ocultas (v4 → v5)

Instagram mantiene la pestaña Reels en el árbol aunque haya otra pantalla encima, como un DM, Explorar o la bandeja de mensajes. La v4 leía esos nodos ocultos y por eso:
- marcaba un reel de DM como "pestaña reels",
- en Explorar repetía el último autor de la pestaña oculta, y por eso no registraba reels nuevos.

La v5 filtra por `isVisibleToUser` y el problema desaparece. De paso, también descarta el reel precargado.

### 3.4 Señales disponibles

| Señal | Nodo en el árbol | Qué indica |
|---|---|---|
| Visor de reels | `root_clips_layout`, `clips_viewer_view_pager`, `clips_media_component` | Se está viendo un reel |
| Pestaña Reels | "Crear un reel", o `tab_bar` + título "Reels", sin "Volver" | Contenido empujado |
| Sugerencias | `suggested_title = "Sugerencias"` | Contenido empujado (después de un DM) |
| Enviado por DM | `sender_username_or_fullname` | Lo eligió otra persona |
| Visor de perfil | "Volver" + campo de comentarios | Lo eligió el usuario |
| Relación con el autor | `inline_follow_button` presente o ausente | No seguida / seguida |
| Anuncio | `Publicidad`, "Reel patrocinado de…", `clips_single_image_ads_media_content` | Publicidad |
| Autor del reel actual | `clips_author_username` | Sirve para contar reels |

Otras señales que se vieron y no se usan: "A X le gustó este reel" y "X comentó" (`friendly_bubbles_component`), "X y N personas más siguen esta cuenta", el aviso "Me interesa / No me interesa" y el menú "Por qué ves esta publicación".

### 3.5 Conclusión del Probe 2

**El árbol no etiqueta "algoritmo", pero deja ver el camino por el que llegó el contenido.** Combinando el contexto del visor, la presencia de "Sugerencias", el remitente y la relación con el autor, se puede clasificar el origen. **El Probe 2 es positivo y no hace falta un proxy alternativo.**

---

## 4. Definición operativa de arrastre

Escrita por el autor el 21-10, antes de medir:

> Por lo general, toda visita a Reels es arrastre. Una vez que comienza el scrolleo se difumina la línea entre lo decidido y lo inconsciente. Desde el primer scrolleo ya es arrastre. Depende de dónde viene el contenido: si es en la pestaña Reels, siempre es arrastre, porque aunque sea enriquecedor, llegó por algoritmo y no por búsqueda propia.

Precisiones que se agregaron después:
- **Los anuncios son arrastre.**
- **Deslizar por los reels de un perfil es dirigido**, porque el contenido lo eligió el usuario y es finito.

### Reglas

| Contexto | Clase |
|---|---|
| Pestaña Reels, desde el primer reel | ARRASTRE |
| Sugerencias (después de un DM o de otro reel) | ARRASTRE |
| Anuncio | ARRASTRE |
| Reel enviado por DM (el primero) | DIRIGIDO |
| Reels de un perfil, aunque se deslice | DIRIGIDO |
| Explorar | Sin definir (aparcado) |

**Consecuencia:** el criterio es **el lugar de origen, no el gesto ni el tiempo**. Por eso la relación con el autor (seguida o no) **no decide la clase**. Se registra como dato descriptivo, por ejemplo "¿qué parte del arrastre viene de cuentas que no sigues?".

---

## 5. El clasificador (v5)

### Orden de evaluación

1. Si no hay un visor de reels → FUERA (no se registra).
2. Si aparece "Sugerencias" → **ARRASTRE**.
3. Si hay un remitente → **DIRIGIDO**, enviado por mensaje.
4. Si hay "Volver" y campo de comentarios → **DIRIGIDO**, perfil.
5. Si no hay "Volver" y aparece "Crear un reel" (o `tab_bar` + título "Reels") → **ARRASTRE**, pestaña Reels.
6. En cualquier otro caso → **DESCONOCIDO**, y se vuelca el árbol para estudiarlo.

### Registro

Hay una línea por reel nuevo (cambio de autor) y una línea al cerrar cada segmento (cambio de clase o de contexto, o salida de Instagram):

```
reel 09:15:55 · DIRIGIDO · enviado por mensaje · <autor> · no seguida
SEGMENTO ARRASTRE · pestaña reels · 09:09:31–09:09:47 · 0m 15s · 7 reels (1 anuncios, 5 no seguidas)
```

La relación con el autor se registra como `seguida`, `no seguida`, `colab` o `anuncio`.

### Resultado de la prueba con v5 (21-10, 09:15)

| Contexto | Resultado | ¿Correcto? |
|---|---|---|
| Pestaña Reels | ARRASTRE · pestaña reels | ✅ |
| Anuncios en la pestaña Reels | ARRASTRE · anuncio (adobe_lat, skool_com) | ✅ |
| Reel enviado por DM | DIRIGIDO · enviado por mensaje | ✅ |
| Deslizar después del DM | ARRASTRE · sugerencias | ✅ |
| Un anuncio en la pestaña Reels (boe.cl) | **DESCONOCIDO** | ❌ En ese anuncio no se veían ni "Crear un reel" ni el título "Reels", pero `tab_bar` sí era visible |
| Explorar | **DIRIGIDO · perfil** | ❌ Explorar tiene "Volver" y campo de comentarios, igual que un perfil, y no muestra título |

### Limitaciones conocidas

- **Explorar se confunde con un perfil.** Posible diferencia: el visor de perfil muestra el título "Reels" y Explorar no. Falta confirmarlo. *Aparcado por decisión del autor.*
- **Algunos anuncios de la pestaña Reels ocultan el título.** Posible arreglo: en un visor de reels, `tab_bar` visible sin "Volver" ya indica la pestaña Reels, porque los visores de DM y de perfil no tienen barra de pestañas.
- **Conteo de reels.** Se cuenta un reel nuevo cuando cambia el autor, así que dos reels seguidos de la misma cuenta cuentan como uno.
- **Lectura por eventos.** Si el teléfono no emite eventos (pantalla apagada o video sin interacción), no hay lectura. La duración se mide hasta la última lectura.
- **Segmentos fragmentados.** Al alternar rápido entre contextos aparecen segmentos de 0 s. Para el análisis convendría unir los segmentos consecutivos de la misma clase.
- **Reels colaborativos y propios.** No muestran el botón Seguir, pero eso no significa que la cuenta se siga.
- **Dependencia de Instagram.** Las señales dependen de ids y textos de la interfaz de Instagram en español, que pueden cambiar con cualquier actualización.

---

## 6. Probe 1 (MediaProjection): propuesta

**Propuesta: descartarlo.** El árbol de accesibilidad ya entrega el contexto necesario. Capturar la pantalla sería:
- **innecesario** para la clasificación,
- **más invasivo**, porque graba todo lo que se ve, incluidos mensajes privados,
- **contradictorio** con el principio de la app de medir la estructura y no el contenido.

Falta la decisión del autor. Si se descarta, conviene dejarlo justificado así en la metodología.

---

## 7. Siguiente paso: validación (margen de error)

Tal como está definido el clasificador, los errores técnicos son de contexto: confundir Explorar con un perfil, un anuncio sin señales, etc. Se miden comparando la etiqueta de la app con **dónde estaba realmente** el usuario.

Hay otra comparación posible: la etiqueta de la app contra **cómo lo vivió** el usuario ("esto lo elegí"). En ese caso, un desacuerdo no es necesariamente un error del proxy. Puede ser un hallazgo: la diferencia entre lo que la persona cree que decidió y lo que decidió la plataforma.

**Pregunta abierta para el diseño del piloto:** ¿se mide la precisión del proxy (dónde estaba), la percepción del usuario (cómo lo vivió), o ambas por separado?

---

## 8. Privacidad

- Los logs contienen nombres de cuentas de terceros, nombres de contactos (remitentes de DM) y textos de la interfaz.
- **Antes de subir cualquier volcado a GitHub, hay que anonimizarlo.**
- El clasificador no registra el nombre del remitente de un DM, solo "enviado por mensaje".

## 9. Archivos

| Archivo | Contenido |
|---|---|
| `sesion-08-sonda-v5.zip` | Proyecto completo de la sonda (v5) + `sonda-v5.diff` |
| `sonda-probe2.zip` | README del Probe 2 + `volcados-clave.txt` (fragmentos que respaldan cada hallazgo) |
| `bitacora-proxy-arrastre.md` | Este documento |
