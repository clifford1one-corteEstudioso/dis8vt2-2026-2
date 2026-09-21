# plan de avance — post sesión 05

Estado al 21 sep. Cinco fases construidas, wireframes listos, y el diseño se
movió respecto a lo que está escrito en código.

## lo que cambió

| | Lo construido (fase 05) | Los wireframes |
| --- | --- | --- |
| Momento de la intervención | Interrumpe a los 15 min | Indicador al margen + **brief al salir** |
| Métrica | Cortes por segundo | **Densidad de patrones oscuros**, videos, % oscuridad |
| Memoria | Ninguna | **Acumulado semanal** vs actividad declarada |
| Origen del contenido | Descartado en septiembre | **Vuelve**: arrastrado vs buscado |
| Onboarding | No existe | Permisos + declaración de actividad |

Decisión tomada: **la interrupción a los 15 min se mantiene**, conviviendo con
el brief. Son dos estrategias de fricción distintas y la semana de uso dirá si
juntas son demasiado.

## lo que sobrevive del código

- Captura sostenida, sin cambios.
- Detector de 32×32 puntos: sirve como **insumo**, ya no como métrica final.
- Regla de sesión con tolerancia de 30 s: sirve, hay que agregarle los bordes nuevos.
- Overlay que deja pasar los toques: sirve, cambia el contenido.

---

## el hueco más grande

**Qué es operativamente un patrón oscuro.** El wireframe dice `78% MATCH` y
`oscuridad 68%`, pero no hay definición detrás de esos números. La intención es
que la app lea la pantalla y saque conclusiones.

Es el mismo error que evitamos con el umbral de cortes, y esta vez es peor: un
porcentaje promete una precisión que hoy no existe. Si alguien pregunta *78% de
qué*, no hay respuesta.

Dos caminos, y hay que elegir antes de construir:

| | Cómo | Costo |
| --- | --- | --- |
| **Índice compuesto** | Se arma con cosas medibles: ritmo de edición, duración del loop, ausencia de final | Se puede hacer con lo que ya existe. Hay que justificar los pesos |
| **Reconocimiento visual** | Un modelo identifica elementos manipuladores en los píxeles | Mucho más ambicioso, sin validar, y exige datos de entrenamiento |

Mientras no esté definido, **mostrar el sello sin porcentaje**. Decir "patrón
oscuro detectado" es defendible; decir 78% no lo es todavía.

---

## fases

Ordenadas por lo que el diseño necesita y por lo que puede invalidar trabajo.

### fase 06 — bordes de sesión y brief de cierre

Es el corazón del diseño nuevo y no depende de nada sin resolver.

**Cerrar la sesión:**

- Botón de inicio → brief
- Cerrar desde apps recientes → brief
- Bloquear el celular → pausa. Si la pausa supera 3 min → brief

**Abrir la sesión** — distinguir uso intencional de arrastre:

- Primer swipe dentro del feed infinito
- Botón de reels
- No al entrar a Instagram

El caso que define la regla: entrar porque un amigo te mandó algo, verlo, y
recién ahí empezar a scrollear. La sesión arranca en ese momento, no antes.

**El brief** (WF3): tiempo, videos, `recuerdas ?`, oscuridad, y la barra que
parte la sesión en arrastrado / buscado.

Pendiente de definir: qué cuenta como *buscado*. La versión barata es "estabas
en el pozo infinito o no", que se mide con lo que ya hay.

### fase 07 — persistencia y acumulado

El brief sin memoria no dice nada: la leyenda misma explica que *el costo del
uso no es perceptible por episodio sino por agregado*. Ahí entra Room.

- Guardar cada sesión.
- Acumulado semanal en arrastre.
- Contraste con la actividad declarada.

### fase 08 — onboarding

- **WF1a** permisos, con el *por qué* de cada uno. Hoy la app los pide a secas.
- **WF1b** declaración: *¿qué te gustaría hacer más y no alcanzas?* Campo libre,
  omitible. Es lo que le da sentido al contraste del brief.

Sin esta fase, la línea "3h 40 en [actividad que te gusta]" no tiene de dónde
salir.

### fase 09 — índice de oscuridad

**Bloqueada hasta que exista la definición de arriba.** Construir el número
antes de saber qué mide repetiría el error del umbral, solo que esta vez el
error quedaría escrito en la interfaz.

### transversal — bajar el costo de captura

Cuatro muestras por segundo a resolución completa es caro. Dos palancas:

- Bajar a 1–2 muestras por segundo. Para ritmo de edición basta.
- Bajar la resolución del `ImageReader`, no solo el muestreo de puntos.

Medir batería antes y después. Es el único de estos pasos que se puede validar
sin decidir nada de diseño.

---

## lo que no haría todavía

- Poner porcentajes en el sello de patrón oscuro.
- Pulir la interfaz del brief más allá del wireframe.
- Agregar ajustes configurables por el usuario.

Los tres se deciden mejor después de una semana de uso real.

## lo que sigue pendiente de validar

1. Sesión de 20 minutos seguidos con captura. Lo más largo medido: 83 s.
2. Video con cortes contados a mano contra el CSV, para fijar el umbral de 18.
3. La app se enciende con cualquier scroll, no solo con contenido a pantalla
   completa. Se cierra con la regla de la fase 06.
