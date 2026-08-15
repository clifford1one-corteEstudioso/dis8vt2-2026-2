# Planificación 3 días / 1 semana / 2 semanas

Punto de partida: **miércoles 12 de agosto 2026** (sesión-01).

| Horizonte | Fecha tope | Entregable |
| --- | --- | --- |
| 3 días | **sáb 15 ago** | Metas SMART cumplidas: decisión técnica + app corriendo en mi Android + taxonomía v0.1 de patrones |
| 1 semana | **mié 19 ago** (sesión-02) | Encargo-16: Triple Mapa. 3 láminas A3 impresas + matriz en Miro |
| 2 semanas | **mié 26 ago** (sesión-03) | **Doble carga:** reestructuración de la memoria (Grupo 2, confirmado) + sistema editorial en Affinity Publisher. Correcciones detalladas en [`correcciones-memoria.md`](./correcciones-memoria.md) |
| (horizonte) | **vie 11 sep** | Hito 30 días: memoria completa, redactada y diagramada a alta resolución |

---

## Bloque A — 3 días (jue 13 → sáb 15 ago)

El feedback de clase fue claro: **antes de programar hay que decidir con qué**. Por eso las metas SMART de 3 días son los pasos 2-4 de `meta-SMART-2`, no el paso 1.

### Meta DECISIÓN — Decisión de stack (vence vie 14 ago, 20:00)

> Antes del viernes 14/08 a las 20:00 tendré `sesion-02/decision-tecnica.md` con una matriz comparativa de **3 vías** de construcción — (a) Android nativo/Kotlin, (b) Expo + React Native, (c) Google AI Studio applets — evaluadas contra **5 criterios**: acceso a captura de pantalla en Android (MediaProjection / Accessibility Service), permisos que exige y qué muestra al usuario, si permite instalar el build en **mi** teléfono sin Play Store, curva de aprendizaje realista en menos de 2 semanas, y costo de inferencia por hora de uso. Cierra con **1 vía elegida** y un párrafo de justificación.

- Medible: matriz 3×5 completa + 1 decisión escrita.
- Por qué esta y no "investigar cómo se hacen las IA": esa meta no es medible ni acotable. Esta sí, y desbloquea todo lo demás.

### Meta TELÉFONO — App propia corriendo en mi teléfono (vence sáb 15 ago, 20:00)

> Antes del sábado 15/08 a las 20:00 tendré instalada **en mi Android** una app hecha por mí con la vía elegida en Meta DECISIÓN, que al abrirse solicite el permiso de captura de pantalla y muestre en pantalla si fue concedido o denegado.

- Medible: video de 15 s de la app corriendo en el teléfono + el repo/proyecto commiteado.
- Esto responde directamente al feedback "lograr instalar una app mía en un teléfono".
- Es un *hola mundo con permiso*, no el prototipo. Lo que valida es la vía, no la IA.

### Meta PATRONES — Taxonomía v0.1 de patrones (vence sáb 15 ago, 20:00)

> Antes del sábado 15/08 a las 20:00 tendré `sesion-02/patrones-arrastre-v0.1.md` con **6 patrones** de contenido que promueven el uso por arrastre, cada uno con: nombre, definición operacional en una frase, **señal observable** (qué vería un modelo en el frame o el audio) y 2 ejemplos reales con link + timestamp tomados de mi propio feed.

- Medible: 6 patrones × 4 campos × 2 ejemplos.
- Esta es la meta de **diseño**, y es la que alimenta tanto el prototipo (paso 7: que la IA los detecte) como el marco teórico de la memoria. La "señal observable" es la bisagra: sin ella el paso 7 no tiene qué buscar.

**Para la clase basta con presentar 2** (el encargo pide "al menos dos"). Yo presentaría **DECISIÓN y PATRONES** — son las que muestran criterio de diseño; TELÉFONO es la evidencia de que avancé.

> **Nota de nomenclatura.** Estas tres metas salen todas de `meta-SMART-2` del `sesion-01/README.md`, que es la versión corregida tras el feedback del profe. La primera versión (`meta-SMART`, la de "ver curso de coursera de python") está descartada y no se toca. Les puse nombre en vez de número justamente para que no se confundan con esas dos versiones.

> ⚠️ **Desactualizado desde el sáb 15.** El jueves y el viernes no se avanzó; el sprint se recomprimió en sábado + domingo. Plan vigente: **[`plan-fin-de-semana.md`](./plan-fin-de-semana.md)**. El reparto de abajo queda como referencia de la estimación original.

**Reparto real (según disponibilidad):**

| Día | Ventana | Horas útiles | Qué va ahí y por qué |
| --- | --- | --- | --- |
| **jue 13** | 19:00 → 22:30 | ~3 h, cansado | **Meta DECISIÓN, investigación.** Leer y llenar la matriz 3×5. Tarea de lectura, no de código: es lo único que rinde después de un día completo. **No instalar nada esta noche.** |
| **vie 14** | almuerzo, 1 h | 1 h | **Cerrar Meta DECISIÓN**: escribir el párrafo de decisión. Y dejar **descargando** el SDK/toolchain de la vía elegida — que baje solo mientras estás ocupado. |
| **vie 14** | 22:00 → 00:00 | ~2 h, fundido | **Meta PATRONES, recolección.** Barrer tu propio feed anotando ejemplos con timestamp. Es tarea de captura, no de análisis: aguanta el cansancio. (Recolectar patrones de arrastre a medianoche desde tu propio feed es, además, trabajo de campo bastante honesto.) |
| **sáb 15** | 10:00 → 18:00 | ~6 h, tu mejor bloque | 10:00–13:00 **Meta TELÉFONO con timebox duro**. 14:00–17:00 **Meta PATRONES, redacción** de los 6 patrones. 17:00 cierre y a arreglarte. |
| **dom 16** | 14:00 → 21:00 | ~5 h, con resaca | Ya es Bloque B (ver abajo). Nada de código. |

**Total real del sprint de 3 días: ~12 horas útiles.** Las tres metas SMART completas piden entre 12 y 18 h, y la que tiene toda la varianza es Meta TELÉFONO.

### Regla de corte para Meta TELÉFONO (importante)

Montar por primera vez un toolchain de Android puede tomar 2 horas o puede tomar 8. Es la única tarea de la semana que puede comerse el sábado entero y dejarte sin Meta PATRONES — que es la que sí vas a presentar.

> **A las 13:00 del sábado paro, pase lo que pase.** Si la app no está en el teléfono, escribo en `decision-tecnica.md` en qué paso exacto se cayó (qué comando, qué error) y sigo con Meta PATRONES.

Ese registro de la falla no es tiempo perdido: es material directo para el **Mapa 3 (flujo técnico)** del miércoles y para el punto de viabilidad técnica de la memoria (§B4). "No pude instalarlo por X" es una respuesta legítima y fundamentada; "no alcancé a mirarlo" no.

Y recuerda que el encargo pide **dos** metas SMART: presentas **DECISIÓN** y **PATRONES**. Meta TELÉFONO es evidencia extra, no requisito. No dejes que la opcional se coma a las obligatorias.

---

## Bloque B — 1 semana: Encargo-16, Triple Mapa (dom 16 → mié 19 ago)

Tres mapas **por cada perfil de usuario**, en Miro (digital) y en A3 impreso (físico).

### Antes de dibujar: definir perfiles

Un perfil es una decisión de proyecto, no un trámite. Para una intervención de fricción y consciencia sobre el uso del smartphone, los candidatos son:

1. **Usuario intensivo** (perfil primario — el que sufre el arrastre y quiere recuperar la decisión).
2. **Testigo / vínculo cercano** (pareja, familia, compañero de pieza — quien nota el problema antes que el usuario y muchas veces gatilla el *Awareness*).
3. **Creador de contenido** (aparece en `sesion-01` como fuente de los patrones — pero ojo: es *productor* del problema, no usuario de la solución; incluirlo cambia el alcance del proyecto).

**Recomendación:** hacer el perfil 1 completo y profundo (3 A3, es el mínimo que exige la checklist física) y dejar el perfil 2 como matriz en Miro sin imprimir. Si Sergio/Simón piden los dos impresos, son 6 láminas A3 — hay que saberlo el jueves, no el martes. **→ Preguntar en clase o por mensaje cuántos perfiles esperan impresos.**

### Los tres mapas

- **Mapa 1 — Viaje del usuario (zoom out).** Las 6 etapas: Awareness → Consideración → Adopción/Onboarding → Uso → Retención → Advocacy. Por etapa: acciones, pensamiento, emoción, puntos de dolor, oportunidad de diseño. El punto difícil de este proyecto está en **Retención**: una app cuyo éxito es que la uses menos tiene un problema de retención estructural, y eso es material de memoria, no solo del mapa.
- **Mapa 2 — Flujo funcional.** Lógica de navegación desde la experiencia. Sintaxis **Garrett** estricta: rombo = decisión, rectángulo = proceso, conectores con dirección explícita. Referencia: `jjg.net/ia/visvocab/spanish.html`.
- **Mapa 3 — Flujo técnico.** Aquí se cobra lo de Meta DECISIÓN: arquitectura del sistema, flujo de permisos de Android, qué pasa si el usuario revoca el permiso de captura, error y recuperación, tiempos de carga de la inferencia, dónde corre el modelo (en el teléfono o en servidor) y qué se envía. **La decisión técnica del jueves es literalmente el insumo de este mapa** — por eso va primero en la semana.

### Calendario

Con lunes y martes completos, este bloque deja de ser el problema. Horas disponibles: dom ~5 h (con resaca) + lun ~8 h (menos 3 h de la propuesta externa) + mar ~8 h = **~18 h**. El encargo-16 pide unas 16 h bien contadas. Hay holgura, pero es poca: digitalizar tres A3 en serio se come más tiempo del que uno cree.

- **dom 16 (14:00 → 21:00, ~5 h):** Definir el perfil primario y hacer el **Mapa 1** a mano, en papel. Borrador sucio, sin digitalizar. Si el cuerpo da, empezar el **Mapa 2**; si no, no pasa nada — el lunes lo cubre. Nada de pantallas ni toolchains: papel y plumón, que es justo lo que piden los mapas.
- **lun 17 (09:00 → 12:00):** **La propuesta externa, primero.** Tiene deadline propio y ajeno a esto; sacarla temprano evita que se expanda y te coma la tarde. (Si tiene hora de envío, confírmala — es lo único de la semana que no puedes mover.)
- **lun 17 (14:00 → 19:00):** **Mapa 2** (funcional) y **Mapa 3** (técnico), montado sobre la decisión de Meta DECISIÓN. Revisar el vocabulario de Garrett **antes** de empezar el Mapa 2, no después.
- **mar 18 (mañana y tarde):** Digitalizar los tres, subir la matriz a Miro, exportar A3 a PDF con marcas y sangrado.
- **mar 18 (noche):** **IMPRIMIR.** No dejar la impresión A3 para el miércoles en la mañana — es el punto de falla clásico de esta entrega.
- **mié 19:** Sesión-02, revisión en mesa.

### Qué hacer con la holgura del martes

Si terminas de imprimir con tiempo, hay dos candidatos y **no alcanzan los dos**:

1. **Bajar a texto el capítulo Usuario (§A3)** ← recomendado. Acabas de pasar tres días metido en el perfil primario; escribir las 2–3 personas con eso fresco cuesta la mitad que reconstruirlo la semana siguiente desde cero. Descuenta unas 3 h del Bloque C, que es el que está bajo el agua.
2. **Segundo intento de Meta TELÉFONO** (la app en el teléfono), si el sábado se cayó.

Va primero el 1. La app no tiene fecha de entrega; el 26 sí. Y el punto de viabilidad técnica de la memoria (§B4) se puede responder con la investigación de Meta DECISIÓN aunque no tengas nada compilando.

**Checklist de salida (del encargo-16):**

- [ ] 2 metas SMART redactadas con precisión (vienen del Bloque A)
- [ ] 3 mapas impresos en A3, individuales
- [ ] Matriz completa cargada en el tablero Miro
- [ ] Avance de memoria activado según grupo

---

## Bloque C — 2 semanas: páginas maestras de la memoria (jue 20 → mié 26 ago)

Herramienta: **Affinity Publisher 2** (el encargo dice InDesign; los conceptos —páginas maestras, estilos, secciones, texto encadenado— son equivalentes). Ya existe `sesion-01/files/memoria-diseno-v1.af` como punto de partida.

Material a diagramar: `memoria-clifford-v3.0.docx`, **~8.400 palabras**, 6 capítulos, jerarquía de hasta 4 niveles (H1→H4), y **una sola imagen**. Ese último dato es el que manda: una memoria de título de diseño con 1 imagen no se sostiene gráficamente. Los tres mapas A3 del Bloque B son, convenientemente, tus tres primeras láminas.

### Advertencia de secuencia (importante)

Confirmado: eres **Grupo 2**, entrega el **mié 26**, con encargo de *"reestructuración teórica y metodológica profunda"*. Esta quincena carga dos cosas a la vez, y de las dos **la reestructuración pesa más y se evalúa más**.

El backlog completo de correcciones, cruzado contra el feedback de Héctor Novoa y Joaquín Zerené y contra el texto real de `memoria-v4.5.md`, está en **[`correcciones-memoria.md`](./correcciones-memoria.md)**. Lo imprescindible para el 26:

1. Invertir el orden del marco teórico (Economía → Psicología Cognitiva → Diseño) y reescribir 2.1, que justifica el orden actual.
2. Renombrar "Biología Humana" → "Psicología Cognitiva y Conductual".
3. Capítulo **Usuario** con 2–3 personas — hoy no existe, y comparte trabajo con el perfil primario del encargo-16.
4. Declarar si el proyecto es app de terceros o manifiesto de diseño ético — sale de Meta DECISIÓN.

**Sobre la maquetación:** no maquetes las 60 páginas esta quincena. Construye el sistema (retícula, estilos, maestras — todo eso es independiente del contenido) y demuéstralo en un piloto. El vertido completo va después, con el texto estable.

**El piloto NO puede ser el capítulo 2**, que es justamente el que se reestructura. Usa el **capítulo 3 (Planteamiento de la Problemática)**: es estable, tiene datos, citas y pide gráfica.

### 1. Fundaciones — van ANTES de las maestras (jue 20 → vie 21)

Una página maestra sin retícula debajo es decoración. En orden:

1. **Formato y márgenes.** Decidir tamaño (17×24 cm es el estándar cómodo para memoria de diseño; A4 vertical si la escuela lo exige) y márgenes asimétricos recto/verso con medianil suficiente para el encuadernado.
2. **Retícula.** Columnas + **baseline grid** (interlínea base, ej. 13 pt). Todo el texto corrido se ancla a ella. Esto es lo que hace que las páginas se vean "a alta resolución gráfica" y no armadas a ojo.
3. **Tipografía.** 2 familias máximo (una para títulos, una para lectura larga) + escala modular. El .docx viene en Arial/Heebo — eso es un default de Word, no una decisión de diseño; reemplazar.
4. **Paleta.** Incluye resolver "color página", que ya está anotado como error pendiente en `sesion-01`. Definirla en modo CMYK si va a imprenta.
5. **Estilos de párrafo y carácter.** H1, H2, H3, H4, cuerpo, primer párrafo sin sangría, cita en bloque, pie de imagen, folio, referencia bibliográfica con sangría francesa. **Todo por estilo, nada manual** — es lo que permite reestructurar sin remaquetar.

### 2. Páginas maestras a construir (lun 24 → mar 25)

Mínimo indispensable, en pliegos (recto/verso):

| Maestra | Uso | Nota |
| --- | --- | --- |
| **A — Base** | Retícula, márgenes, folio, encabezado con campo de texto | Todas las demás heredan de esta |
| **B — Portada / portadilla / créditos** | Preliminares | Sin folio ni encabezado |
| **C — Apertura de capítulo** | Los 6 H1 | Página impar, folio oculto, gran caja tipográfica |
| **D — Texto corrido** | El grueso de la memoria | 1 o 2 columnas, encabezado corriente con nombre de capítulo |
| **E — Texto + imagen** | Marco teórico | Zonas de imagen ancladas a la retícula |
| **F — Lámina completa / a sangre** | Los 3 mapas del Bloque B, diagramas | Sin folio; con sangrado de 3–5 mm |
| **G — Cita destacada / respiro** | Transiciones entre secciones | Rompe el gris tipográfico |
| **H — Bibliografía** | Cap. 6 | Sangría francesa, APA |
| **I — Índice** | Preliminares | Con TOC generado desde estilos, no a mano |

Además: **secciones y numeración** (romanos en preliminares, arábigos desde el cap. 1) y encabezados corrientes por **campo de texto**, no escritos a mano — si no, cada cambio de título obliga a repasar 60 páginas.

### 3. Maqueta piloto (mar 25)

Verter **el capítulo 3 (Planteamiento de la Problemática)** usando el sistema. Entre 8 y 12 páginas. Ese piloto es lo que llevas a mesa: prueba que el sistema funciona, sin haber maquetado una memoria que aún se está reescribiendo.

### 4. Trabajo de contenido, en paralelo (jue 20 → mar 25)

Esta es la mitad que **más pesa** en la evaluación del 26. Ver [`correcciones-memoria.md`](./correcciones-memoria.md).

- **Typos y redacción:** pasada completa con la [herramienta de corrección](https://ai.studio/apps/c238e6f7-cb8b-416d-af3d-b236aeb1d549?fullscreenApplet=true) enlazada en el encargo. Hacerlo **sobre el texto, antes de verter a Publisher** — corregir texto ya maquetado es el doble de trabajo.
- **Déficit gráfico:** la memoria tiene **1 sola imagen** en ~9.170 palabras. Los candidatos obvios: los 3 mapas del Bloque B, una matriz visual de los 15 referentes (§A4), el mapa de actores 4.3 (ya existe), la metodología 4.5 y el escenario de uso 5.3. Este es el riesgo real del hito de 30 días, y no lo resuelven las páginas maestras.

### Calendario

**Contenido (mañanas):**

- **jue 20 – vie 21:** Reordenar marco teórico + reescribir 2.1 + renombrar 2.2 (§A1, A2).
- **sáb 22 – dom 23:** Capítulo Usuario con 2–3 personas (§A3), reciclando el perfil del encargo-16. Ludopatía con Schüll (§B1). Activar fuentes ya listadas y no citadas (§C2).
- **lun 24:** Declarar alcance técnico app vs. manifiesto (§B4, desde Meta DECISIÓN). Nombrar habituación (§B3).
- **mar 25:** Pasada de typos y coherencia sobre el texto ya reestructurado.

**Maquetación (tardes):**

- **jue 20:** Formato, márgenes, retícula, baseline. Elección tipográfica.
- **vie 21:** Paleta + set completo de estilos de párrafo y carácter.
- **lun 24:** Maestras A–E.
- **mar 25:** Maestras F–I + secciones/numeración + piloto del cap. 3. Exportar PDF.
- **mié 26:** Sesión-03. Mesa con memoria reestructurada + sistema editorial + piloto.

---

### Carga real del Bloque C

Sumando lo imprescindible:

| Trabajo | Horas estimadas |
| --- | --- |
| Reordenar marco teórico + reescribir 2.1 + renombrar 2.2 (§A1, A2) | 6–8 h |
| Capítulo Usuario con personas (§A3) — 4 h, o **1 h** si dejaste el borrador escrito el martes 18 | 1–4 h |
| Ludopatía con Schüll (§B1) | 2 h |
| Alcance técnico app vs. manifiesto (§B4) | 2 h |
| Fuentes no citadas (§C2) + habituación (§B3) | 3 h |
| **Subtotal contenido** | **14–19 h** |
| Retícula + tipografía + paleta | 3 h |
| Set completo de estilos | 3 h |
| Maestras A–I | 5 h |
| Maqueta piloto cap. 3 | 4 h |
| **Subtotal maquetación** | **15 h** |
| **TOTAL** | **~30–33 h en 6 días** |

Son unas 5 h diarias sostenidas. Si esa semana se parece a esta (un día completamente ocupado, otro con una hora de almuerzo), **no entra**, y hay que cortar antes y no a las 2 de la mañana del martes 25. Si en cambio se parece al lunes y martes, entra con holgura.

**Orden de sacrificio, de lo primero que se cae a lo último:**

1. **La maqueta piloto** (−4 h). Puedes llegar a mesa con el sistema y una sola doble página de muestra.
2. **Maestras F–I** (−2 h). Con A–E (base, portada, apertura de capítulo, texto corrido, texto+imagen) ya se demuestra el sistema.
3. **§B3 habituación y §C2 fuentes** (−3 h). Son mejoras, no reestructuración.
4. **Nunca: §A1, A2, A3.** Eso *es* el encargo del Grupo 2. Si llegas el 26 con páginas maestras hermosas y el marco teórico en el orden viejo, entregaste lo que no te pidieron.

## Riesgos a vigilar

1. **La impresión A3 del mar 18.** Punto de falla más probable de toda la quincena.
2. **Diagramar texto inestable.** El texto se reescribe la misma semana en que lo maquetas. Por eso el piloto sobre el cap. 3, no el vertido completo. Y si hay que sacrificar algo el 26, **sacrifica maquetación, no reestructuración**: lo primero se recupera en septiembre, lo segundo es lo que te pidieron.
3. **Meta TELÉFONO se puede comer el fin de semana.** Si el sábado 15 al mediodía la app no compila, corto y entrego Meta DECISIÓN + Meta PATRONES, que son las dos que voy a presentar igual. El teléfono no bloquea el encargo-16.
4. **Cuántos perfiles imprimir.** 1 perfil son 3 A3; 2 perfiles son 6. Resolver el miércoles 12 o jueves 13, no el lunes 17.
5. **8.400 palabras y 1 imagen.** El déficit gráfico de la memoria es el riesgo real del hito de 30 días, y no se resuelve con páginas maestras — se resuelve produciendo diagramas. Por eso el inventario del fin de semana.
