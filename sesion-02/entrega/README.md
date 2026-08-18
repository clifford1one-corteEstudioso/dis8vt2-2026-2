# proceso

se me ocurre que mi proyecto puede ser una carcasa para celular, que complemente a la IA que analiza el algoritmo. A través de la carcasa se da feedback al usuario. Este puede ser vibración, color de Led, etc. Se me ocurre que puede ser algo que incomode al usuario, de modo que asocie el contenido oscuro a sensaciones desagradables.
Incluso pueden ser varios a la vez, y ir aumentando con el tiempo. Quizás si llevas 1 minutos viendo un reel engañoso, solo led; si llevas 6 minutos, vibra; si llevas 20 minutos te pincha.

se me ocurre v2: que la primera vez que uses la app te muestre ejemplos y casos que ayuden al usuairo a tomarle peso a la problemática.

ideas:

- oportunidad-advocacy: generar perfiles de los creadores de contenido, de modo que se genere conversación
- oportunidad - retención: generar insights externos, de modo que si no le está sirviendo al usuario, que sirva como información para propósitos investigativos

## mapa-1: Viaje del Usuario (User Journey Map)

### plantilla - m1

1. **Awareness (Conciencia):** El usuario reconoce el problema o necesidad.  
2. **Consideración:** Evaluación contextualizada de alternativas existentes.  
3. **Adopción / Onboarding:** Adquisición, descarga, configuración o guía inicial.  
4. **Uso:** Interacción cotidiana y recurrente con la solución.  
5. **Retención:** Estrategias de diseño para mantener la vinculación activa.  
6. **Advocacy / Evangelización:** Integración a la red o promoción activa de la solución.

### mi versión - m1

1. awareness: ocurre cuando un usuario reconoce su uso como problemático. O cuando desea apoyo para usar menos su celular
2. consideración: utiliza distintas alternativas, aplicaciones como digital wellbeing, oneSec, Freedom, etc.
3. onboarding:
   - instalación de la app/APK
   - Determinar niveles de intensidad del feedback.
   - activar permisos de accesibilidad
4. uso: la APK corre por encima de otras apps, capaz de leer lo que hay en pantalla. Cuando sale un reel es capaz de leer el nivel de oscuridad, e indica el nivel mediante feedback
5. retención: no hay mecanismo que fuerce la atención, el sello está siempre disponible, pero el usuario decide si le presta atención o lo ignora
6. advocacy: el usuario termina por aprender a reconocer reels oscuros de manera autónoma

| - | awareness | consideration | onboarding | use | retención | advocacy |
| - | - | - | - | - | - | - |
| **acciones** | usuario ve Instagram durante horas | Usa distintas alternativas: apps como Digital Wellbeing, OneSec, Freedom | Descarga APK, activa AccessibilityService e instalación desde fuentes desconocidas | App corre sobre Instagram, lee lo que hay en pantalla, VLM analiza y muestra nivel de oscuridad | cuando scrollea, espera ver un análisis del contenido consumido | 1. compartir briefs de la oscuridad del contenido por redes sociales o en conversaciones 2. recomendar a amistades con uso problemático del celular |
| **pensamiento** | "puedo estar todo un día sin ver Instagram, pero una vez empiezo, me cuesta mucho detenerme" | estas alternativas ejercen bloqueos ciegos, no me informan nada sobre qué los hace tomar las decisiones y parecieran no tener criterios más allá de los temporales que yo establecí | tener que activar permisos de accesibilidad me hace pensar que no es seguro | no me había dado cuenta cuánto del contenido que consumo es oscuro | realmente estoy absorbiendo la info sobre la oscuridad del contenido o solo corre por encima | este filtro te permite ser más consciente de los procesos detrás del contenido que consumes (cuando te están tratando de cagar) |
| **emoción** | frustración con sigo mismo | frustración hacia la app(lo hacen de la manera más sencilla y no dedicado al usuario) | desconfianza, curiosidad | cuestionar el comportamiento propio anterior, y a los creadores de contenido | neutralidad | frustración, al ver la cantidad de patrones oscuros utilizados por los creadores de contenido que solías estimar |
| **dolor** | dejo de invertir tiempo en cosas que me importan | mecanismos de bloqueo se activan en momentos donde sí quiero usar Instagram | ceder información personal sobre mi conducta digital y algoritmo | sentir que la responsabilidad finalmente recae en ti y pasarle esa responsabilidad a un tercero solo estira el chicle | ser consciente de lo inútil/insustancial que es aquello en lo que estás invirtiendo tiempo | 1. la decisión final cae en el usuario. 2. riesgo de habituación |
| **oportunidad** | una intervención que apunte a concientizar sin imponer | una intervención que te ayude a tomar consciencia sobre el contenido que consumes | al inicio mostrar un video del funcionamiento real de la app, y explicar por qué pide esos permisos | mostrar en tiempo real lo que se va analizando y qué hace que se define el nivel de oscuridad | variar la forma en que se presenta la visualidad del análisis para evitar habituación | generar briefs con la info detectada tipo Spotify wrapped |


no hay mecanismo que refuerce la atención, pues, es justo en contra de ello donde se trabaja

## mapa-2: Diagrama de Flujo Funcional

### plantilla - m2

- **Objetivo:** Definir la lógica de navegación y los procesos desde la perspectiva de la experiencia del usuario.  

- **Sintaxis:** Utilizar el **Lenguaje Visual de Garrett** (rombos para decisiones, rectángulos para procesos, conectores con direccionalidad explícita).

### mi versión - m2

```mermaid
flowchart TB
    n3["¿opciones de accesibilidad activas?"] --> n4["Sí"] & n5["No"]
    n5 --> n6["Activar opciones de accesibilidad"]
    n7["¿opción de instalar desde fuentes desconocidas activada?"] --> n8["Sí"] & n9["No"]
    n1["Inicio"] --> n12["descargar archivo .zip"]
    n8 --> n10["Ejecutar APK"]
    n10 --> n11["Abrir app"]
    n11 --> n3
    n12 --> n7
    n9 --> n13["Permitir instalar desde fuentes desconocidas"]
    n13 --> n7
    n6 --> n3
    n4 --> n14["Usuario scrollea en Instagram"]
    n14 --> n15["App detecta contenido en pantalla"]
    n16["¿hay contenido sustancial para analizar?(que entre dentro de los parámetros predeterminados)"] --> n17["Sí"] & n18["No"]
    n17 --> n21["Se muestran y destacan los elementos y patrones oscuros"]
    n21 --> n22["Se muestra indicador del nivel de oscuridad"]
    n15 --> n23["VLM analiza el contenido en pantalla"]
    n18 --> n14
    n23 --> n16
    n22 --> n14

    n3@{ shape: diam}
    n7@{ shape: diam}
    n1@{ shape: text}
    n16@{ shape: diam}
    style n1 stroke-width:2px,stroke-dasharray: 0
```

- [VER EN EL EDITOR](https://mermaid.ai/app/projects/2754080d-5a23-45b5-857f-8bedf2b56a8e/diagrams/c04bd30d-d449-4ea2-a8c5-1dcb3315e7a4/share/invite/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkb2N1bWVudElEIjoiYzA0YmQzMGQtZDQ0OS00ZWEyLWE4YzUtMWRjYjMzMTVlN2E0IiwiYWNjZXNzIjoiVmlldyIsImlhdCI6MTc4NjkyNDc0NH0.yZLcGxkb04tXgjUpW9VpBzJV1bHkOZmyuuE4KwqbPns?entryPoint=share-modal)

## mapa-3: Diagrama de Flujo Técnico

### plantilla - m3

Objetivo: Detallar la arquitectura del sistema, respuestas del hardware/software y gestión de excepciones.
Detalle Requerido:

- Software: Flujos de error, recuperación de credenciales, tiempos de carga, permisos de sistema.

- Hardware: Patrones de iluminación LED, tiempos de pulsación de botones para vinculación (Bluetooth), respuestas mecánicas, sensores y tolerancias de material.

### mi versión - m3

```mermaid
flowchart TB
    n0["inicio"] --> n0b["¿SYSTEM_ALERT_WINDOW activo?<br>canDrawOverlays()"]
    n0b -- sí --> n1["funciona en segundo plano"]
    n0b -- No --> n0c@{ label: "mensaje:<br>'Se requiere permiso para mostrar sobre otras apps.<br>Active el permiso para continuar.'" }
    n0c --> n0d["redirigir vía ACTION_MANAGE_OVERLAY_PERMISSION"]
    n0d --> n0b
    n1 --> n2["¿servicio de accesibilidad conectado?<br>onServiceConnected()"]
    n2 -- Sí --> n3["detecta contenido en pantalla"]
    n2 -- no --> n4["guardar datos completos.<br>Descartar datos parciales/incompletos"]
    n4 --> n4b@{ label: "mensaje:<br>'Las opciones de accesibilidad han sido desactivadas.<br>Para continuar con el análisis, por favor enciéndalas.'" }
    n4b --> n5["redirigir a configuración &gt; accesibilidad"]
    n5 --> n2
    n3 --> n6["extrae accessibilityNodeInfo del contenido"]
    n6 --> n7["envía a VLM local (Gemma 3n)"]
    n7 --> n8["¿inferencia completa dentro de tiempo esperado?"]
    n8 -- Sí --> n9["retorna nivel de oscuridad"]
    n8 -- timeout / error de inferencia --> n10@{ label: "mostrar estado 'analizando' sin bloquear scroll" }
    n10 --> n3
    n9 --> n11["¿SYSTEM_ALERT_WINDOW sigue activo?"]
    n11 -- sí --> n12["renderiza indicador (overlay) sobre el contenido"]
    n11 -- no, revocado a mitad de sesión --> n11b["descartar resultado de análisis<br>(no se puede mostrar sin overlay)"]
    n11b --> n0c
    n12 --> n3

    n0b@{ shape: diam}
    n0c@{ shape: rect}
    n2@{ shape: diam}
    n4b@{ shape: rect}
    n8@{ shape: diam}
    n10@{ shape: rect}
    n11@{ shape: diam}
```

- [VER EN EL EDITOR](https://mermaid.ai/app/projects/2754080d-5a23-45b5-857f-8bedf2b56a8e/diagrams/16b86a90-fb27-487a-bb4c-344fc9c7d7b0/share/invite/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkb2N1bWVudElEIjoiMTZiODZhOTAtZmIyNy00ODdhLWJiNGMtMzQ0ZmM5YzdkN2IwIiwiYWNjZXNzIjoiVmlldyIsImlhdCI6MTc4NzA3NzUyNn0.t2hhYEe-KFIbSAh6KTP00Mi0RlJHfH3_ZsvSB52fF2U?entryPoint=share-modal)