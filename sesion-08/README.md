# sesion-08

## avance según feedback

### paso 1: formulacion: obj y hipotesis

#### hipótesis

##### hipótesis antigua

El diseño de una intervención que reintroduzca momentos de decisión consciente en el flujo de uso automático del smartphone mediante una fricción adaptativa que se calibra según el comportamiento y necesidad del usuario, permitirá restituir la capacidad de decisión sobre el comportamiento tecnológico propio, reduciendo el uso no intencional de forma duradera.

###### problemas y oportunidades de mejora

- hay que actualizar el usuario objetivo
- uso compulsivo es poco medible y el término es poco consistente con el resto de la memoria
- contribuir no es evaluable
- el plazo es poco verificable

##### hipótesis actualizada

> Si se introducen interrupciones adaptativas deliberadas dentro del flujo frictionless de el formato de scroll infinito con contenido a pantalla completa, entonces personas que se dedican al rubro creativo de entre 25 a 32 años, en comparación con su tiempo de uso inicial, mostrará una reducción en su tiempo en pantalla, medido mediante métricas de uso, rescatadas por la propia app, desde el primer día y sostenido durante N días.

> Si se introducen interrupciones adaptativas en el flujo del scroll vertical tipo TikTok, activadas al detectar uso de arrastre, entonces personas de entre 25 y 32 años que dependen de redes 	para su trabajo creativo mostrarán una disminución del tiempo de uso de arrastre respecto de su línea base de [N días], sostenida durante [N días] de intervención, sin que su tiempo de uso dirigido disminuya más de [X %]. El uso de arrastre y el uso dirigido se medirán mediante el origen del contenido (elegido por el usuario o propuesto por el algoritmo), registrado por la aplicación desarrollada.

###### alcances

- si se reduce el uso de arratsre, como conescuencia se reduce el uso total. Sin embargo, si mi medidor es solo *uso total* no puedo saber si redujo su uso no intencional por que funciona o porque odió la app y ahora no abre IG nunca
- agregar cuál es el activador específico de la interrupción

1. **TRIVIALIDAD:**
    - **la hipótesis debe apuntar a resolver un porblema que solo mi proyecto podría resolver**
    - Que una interrupción reduzca el tiempo de uso es casi seguro: cualquier fricción lo hace. Entonces, ¿qué aporto yo? ahora mi hipótesis no lo declara
    - actualmetne no menciona mi contribución teórica(arrastre vs dirigido)
2. **POCO COMPROBABLE:**
    - "reducción" no es acertado, pq disminuir 1segundo ya es una reducción
    - tiempo en pantalla es ambiguo(celular entero, instagram entero, o solo reels)
3. **REDUNDANTE:**
    - deliberado no me dice nada
    - está demasiado especificado lo de "scroll infinito a pantalla completa", en el marco especificar aca poner solo *reels* o otra palabra específica
    - no nombrar el instrumento de medición, solo la variable medida
4. **FUERA DE ALCANCE:**
    - cambiar usuario. Ahora está definido por su oficio, deberpia definirlo por la condición que me importa(que necesite tener insta activo)

#### objetivos

los objetivos tienen que ser el camino para comprobar la hipótesis.

### validación

usar escala likert, y tener en cuenta como afectan a las respuesta cuántas niveles posibles de respuestas hay.

darle las preguntas de eso, antes durate y al final del exp.

Hay que correlacionar los datos de uso con las preguntas de likert. Establece un protocolo de interpetación de los datos.

etapas:

1. diseño del insturumento(en este caso usar un referente de base)
2. diseño de la muestra
3. diseño del terreno
4. análisis de los datos

como cruzamos los datos de uso con la sensación de recuperación de agencia que siente el usuario.

***IMPORTANTE** COMO VOY A RELACIONAR LOS DATOS DE LA ESCALA LIKERT CON LOS DATOS DE USO, Y LUEGO COMO METER ESOS INSIGHTS EN ACCIONABLES EN LA APP.

https://docs.google.com/document/d/1z6nvCx0L8lQdWKZmRvX6dDJxyKcv2Vl8-FevX4u9tek/edit?usp=sharing

### objetivos

#### general old

Contribuir a que personas con uso no intencional recurrente del smartphone recuperen la capacidad de decidir conscientemente sobre su comportamiento tecnológico, reduciendo como consecuencia el uso compulsivo en contextos de tiempo no estructurado, mediante una intervención de diseño.

#### general new

alcances:

`1. Qué harás:` diseñar una intervención
`2. Dónde:` en Instagram Reels.
`3. Para quién:` personas de 20 a 30 años que dependen de Instagram para su trabajo creativo.
`4. Qué busca lograr:` reducir el uso de arrastre sin afectar el uso dirigido.
`4. Con qué propósito:` que la persona recupere la capacidad de decidir sobre su uso. 

Diseñar una intervención que, mediante interrupciones adaptativas, reduzca el uso de arrastre de Instagram Reels sin afectar el uso dirigido, en personas de entre 20 y 30 años que dependen de la plataforma para su trabajo creativo, con el fin de favorecer que recuperen la capacidad de decidir conscientemente sobre su uso.

#### específicos old

##### Objetivos de Investigación

Caracterizar las estrategias de autorregulación que los usuarios emplean actualmente y las razones de su fracaso, para fundamentar los principios de la intervención en evidencia real.

##### Objetivos de Prototipado

Definir los principios de diseño de una intervención que interrumpa el uso automático y favorezca la decisión consciente, traduciendo los hallazgos de la investigación en una propuesta testeable.

##### Objetivos de Validación

Evaluar mediante testeo con usuarios en qué medida la intervención favorece la recuperación de agencia y reduce el uso no intencional, y si esta perdura en el tiempo, para validar su efectividad y orientar su desarrollo.

## VALIDACIÓN

### Plan hacia la validación

> Hipótesis: si se introducen interrupciones adaptativas en el flujo de Instagram Reels, activadas al detectar uso de arrastre, entonces personas de entre 25 y 32 años que dependen de Instagram para su trabajo creativo mostrarán una disminución del tiempo de uso de arrastre respecto de su línea base, sostenida durante la intervención, sin que su uso dirigido disminuya más allá de un umbral definido.

---

#### 0. Restricción de tiempo

- [ ] Fecha límite de entrega: `[fecha]`
- [ ] Calcular semanas disponibles para línea base + intervención + análisis
- [ ] Verificar que la intervención supere el período en que fallaban las herramientas de fricción (horas o días). Si no alcanza, declararlo como limitación.

---

#### 1. Factibilidad del proxy (urgente)

- [ ] **Probe 1:** ¿MediaProjection entrega imagen real o cuadro negro?
- [ ] **Probe 2:** ¿El árbol de AccessibilityNodeInfo expone el origen del contenido en Reels (elegido por el usuario o propuesto por el algoritmo)?
- [ ] Si el Probe 2 falla: definir un proxy alternativo o replantear la hipótesis

---

#### 2. Margen de error del proxy

- [ ] Prueba breve: comparar lo que registra la app con lo que reporta el usuario
- [ ] Calcular falsos positivos (uso dirigido clasificado como arrastre)
- [ ] Calcular falsos negativos (arrastre no detectado)
- [ ] Documentar el margen de error para la metodología

---

#### 3. Investigación (objetivo específico 1)

- [ ] Definir instrumento: entrevistas y/o encuesta
- [ ] Reclutar participantes del perfil (25–32, dependen de Instagram para trabajar)
- [ ] Indagar:
  - [ ] Estrategias de autorregulación actuales
  - [ ] Razones de su fracaso
  - [ ] Cómo distinguen su uso dirigido de su uso de arrastre
- [ ] Sintetizar hallazgos en principios de diseño

---

#### 4. Prototipado (objetivo específico 2)

- [ ] Traducir los principios en reglas de la intervención
- [ ] Definir qué dispara una interrupción (detección de arrastre)
- [ ] Verificar que la intervención no actúe sobre el uso dirigido
- [ ] Prueba piloto interna

---

#### 5. Definiciones del estudio

| Parámetro | Valor | Justificación |
|---|---|---|
| Duración de la línea base | `[N días]` | Mínimo una semana completa (días hábiles y fin de semana) |
| Duración de la intervención | `[N días]` | Debe superar el período de falla de las herramientas de fricción |
| Umbral de disminución del uso dirigido | `[X %]` | Variación natural del uso dirigido observada en la línea base |
| Número de participantes | `[n]` | `[criterio]` |

---

#### 6. Protocolo

- [ ] Consentimiento informado (registro de datos de uso)
- [ ] Revisar si la escuela exige un procedimiento ético
- [ ] Instalación y configuración de la app en los dispositivos
- [ ] Fase A — Línea base: la app registra sin intervenir
- [ ] Fase B — Intervención: la app registra e interviene
- [ ] Plan de respaldo de datos

---

#### 7. Validación (objetivo específico 3)

- [ ] Comparar el tiempo de uso de arrastre entre la fase A y la fase B
- [ ] Verificar que el uso dirigido no disminuya más allá del umbral
- [ ] Analizar si la reducción se sostiene a lo largo de la fase B (no solo los primeros días)
- [ ] Opcional: medir la percepción de agencia (autorreporte)

---

#### 8. Limitaciones a declarar

- [ ] Margen de error del proxy
- [ ] Ausencia de grupo de control
- [ ] Efecto de saber que se está siendo medido durante la línea base
- [ ] Tamaño de la muestra
- [ ] Duración acotada por el calendario
- [ ] Resultados válidos solo para Instagram Reels (la generalización queda como proyección)

---

#### Registro de decisiones

| Fecha | Decisión | Razón |
|---|---|---|
| | | |