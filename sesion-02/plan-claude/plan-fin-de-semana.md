# Plan sáb 15 y dom 16 — recuperar el sprint

Jueves y viernes no se avanzó. Quedan **7 h 15 hoy** (12:00 → 19:15) y **~10 h mañana**, y después lun 17 (menos 3 h de la propuesta externa) y mar 18 completos.

## La decisión que ordena todo

Lo que se entrega el miércoles son **2 metas SMART + 3 mapas A3 + Miro**. La app en el teléfono (Meta TELÉFONO) **no se entrega, no se presenta y no tiene fecha**. Es la única pieza prescindible, así que sale del camino crítico y queda como bonus del domingo por la noche.

Prioridad, en orden:

1. **Meta DECISIÓN** (decisión técnica) — hoy. Es el insumo del Mapa 3 del lunes y del punto de viabilidad de la memoria. Si esta no está lista, el lunes se cae.
2. **Meta PATRONES** (taxonomía de patrones) — hoy. Es la otra meta que presentas.
3. **Mapas 1 y 2** — mañana.
4. **Meta TELÉFONO** — solo si sobra domingo por la noche.

---

## Hoy, sábado 15 — 12:00 a 19:15

**5 h 45 de trabajo efectivo, 1 h 5 de pausas.** Suena poco trabajo para 7 h: es correcto, y por eso funciona.

| Hora | Bloque | Qué |
| --- | --- | --- |
| **12:00 – 12:10** | Arranque | Crear `decision-tecnica.md` y `patrones-arrastre-v0.1.md` con sus títulos y la tabla vacía. Nada más. Es para no pelear con la hoja en blanco a las 14:00 |
| **12:10 – 13:40** | 🔴 90 min | **Meta DECISIÓN — parte 1.** Las 3 vías (Kotlin nativo / Expo / AI Studio) contra los 3 criterios duros: acceso a captura de pantalla, qué permiso exige, si puedo instalar el build en mi teléfono sin Play Store |
| **13:40 – 14:10** | ☕ 30 min | **Almuerzo. Lejos del computador.** No es negociable: el bloque de las 14:10 es el que cierra Meta DECISIÓN |
| **14:10 – 15:40** | 🔴 90 min | **Meta DECISIÓN — parte 2.** Criterios 4 y 5 (curva de aprendizaje, costo de inferencia), llenar la matriz 3×5 y **escribir el párrafo de decisión**. Al terminar esto, Meta DECISIÓN está cerrada ✅ |
| **15:40 – 16:00** | ☕ 20 min | Pausa. Caminar, agua, mirar por la ventana |
| **16:00 – 17:15** | 🟡 75 min | **Meta PATRONES — recolección.** Barrer tu feed y capturar ejemplos con timestamp. Tarea mecánica: va acá a propósito, después de 3 h de lectura densa |
| **17:15 – 17:30** | ☕ 15 min | Pausa corta |
| **17:30 – 19:00** | 🔴 90 min | **Meta PATRONES — redacción.** Los 6 patrones con sus 4 campos. Al terminar, Meta PATRONES cerrada ✅ |
| **19:00 – 19:15** | 📌 15 min | `git commit` y escribir 3 líneas de "dónde quedé y qué sigue". Mañana partes leyendo eso, no reconstruyendo |

### Reglas para que Meta DECISIÓN no se desborde

Es investigación abierta: es el bloque que más fácil se convierte en cinco horas de pestañas.

- **40 minutos por vía, cronómetro andando.** Cuando suena, escribes lo que tengas en la matriz y pasas a la siguiente. Una celda que diga "no logré confirmarlo en 40 min" es un dato válido.
- **Busca la página de permisos, no el tutorial.** Lo que decide es qué te deja hacer el sistema operativo, no qué tan bonito es el framework.
- **Prohibido instalar nada hoy.** Instalar es Meta TELÉFONO, y Meta TELÉFONO es mañana.

### Trampa del bloque de las 16:00

Vas a abrir tu propio feed a recolectar patrones de arrastre. Es, literalmente, la tarea de mayor riesgo del fin de semana. Tres defensas:

- **Timer a la vista** y el documento abierto al lado: anotas mientras miras, no después.
- **Grabar la pantalla** 10 minutos y analizar la grabación, en vez de navegar en vivo. Además te deja evidencia citable para la memoria.
- Si a los 75 min tienes 4 patrones y no 6, **son 4**. Los otros dos salen del análisis de referentes.

---

## Mañana, domingo 16

Vienes de un cumpleaños que termina a las 2. El plan asume **partida a las 11:00**; si despiertas más tarde, abajo está qué cortar.

| Hora | Bloque | Qué |
| --- | --- | --- |
| **11:00 – 11:30** | 🟢 30 min | Partida suave: leer tus 3 líneas de anoche. **Definir el perfil primario** en un párrafo (quién es, por qué el smartphone es herramienta de trabajo y trampa a la vez) |
| **11:30 – 13:00** | 🔴 90 min | **Mapa 1 — Viaje del usuario**, a mano, en papel. Las 6 etapas × 5 filas (acciones, pensamiento, emoción, dolor, oportunidad) |
| **13:00 – 14:00** | ☕ 60 min | Almuerzo largo. Te lo ganaste y lo necesitas |
| **14:00 – 15:30** | 🔴 90 min | Terminar Mapa 1. La etapa **Retención** es la difícil: una app cuyo éxito es que la uses menos tiene un problema de retención estructural. Ahí piensa, no rellenes |
| **15:30 – 15:50** | ☕ 20 min | Pausa |
| **15:50 – 17:20** | 🔴 90 min | 20 min releyendo el vocabulario de Garrett + arrancar **Mapa 2 — Flujo funcional**. Rombos decisión, rectángulos proceso, flechas con dirección |
| **17:20 – 17:40** | ☕ 20 min | Pausa |
| **17:40 – 19:10** | 🔴 90 min | Terminar Mapa 2 ✅ |
| **19:10 – 19:40** | ☕ 30 min | Comida |
| **19:40 – 21:00** | 🔵 80 min | **Meta TELÉFONO, bonus con corte duro.** Instalar el toolchain y un hola mundo en el teléfono. **A las 21:00 se acaba**, esté o no. Si falla, anotas en qué comando exacto se cayó y cierras |

**Por qué el Mapa 2 va hoy y no el lunes:** el lunes tienes 3 h comprometidas con la propuesta externa, y las cosas ajenas se atrasan. Si el Mapa 2 sale el domingo, el lunes queda solo el Mapa 3 y el atraso de la propuesta deja de ser un problema tuyo.

**Si despiertas a las 14:00:** corre todo 3 h y **borra Meta TELÉFONO**. Terminas con Mapa 1 completo y Mapa 2 esbozado, que es suficiente para que el lunes funcione.

**Si a las 19:40 estás muerto:** ándate a dormir. Meta TELÉFONO tiene una segunda ventana el martes en la tarde, y llegar con energía al lunes vale más que un toolchain instalado a medias.

---

## Cómo queda el resto de la semana

- **lun 17:** 09:00–12:00 propuesta externa (primero, tiene deadline ajeno). 14:00–19:00 Mapa 3 técnico, montado sobre la decisión de Meta DECISIÓN.
- **mar 18:** digitalizar los tres, Miro, exportar A3 con marcas y sangrado. **Imprimir de noche.** Si sobra tiempo: borrador del capítulo Usuario (§A3) mientras el perfil está fresco.
- **mié 19:** sesión-02.

Sigue en pie lo del Bloque C: ver [`planificacion.md`](./planificacion.md) y [`correcciones-memoria.md`](./correcciones-memoria.md).
