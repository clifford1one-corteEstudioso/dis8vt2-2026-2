# proceso

se me ocurre que mi proyecto puede ser una carcasa para celular, que complemente a la IA que analiza el algoritmo. A través de la carcasa se da feedback al usuario. Este puede ser vibración, color de Led, etc. Se me ocurre que puede ser algo que incomode al usuario, de modo que asocie el contenido oscuro a sensaciones desagradables.
Incluso pueden ser varios a la vez, y ir aumentando con el tiempo. Quizás si llevas 1 minutos viendo un tiktok engañoso, solo led; si llevas 6 minutos, vibra; si llevas 20 minutos te pincha.

se me ocurre v2: que la primera vez que uses la app te muestre ejemplos y casos que ayuden al usuairo a tomarle peso a la problemática.

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
4. uso: la APK corre por encima de otras apps, capaz de leer lo que hay en pantalla. Cuando sale un tiktok es capaz de leer el nivel de oscuridad, e indica el nivel mediante feedback
5. retención: no hay mecanismo que fuerce la atención, el sello está siempre disponible, pero el usuario decide si le presta atención o lo ignora
6. advocacy: el usuario termina por aprender a reconocer tiktoks oscuros de manera autónoma

| - | awareness | consideration | onboarding | use | retención | advocacy |
| - | - | - | - | - | - | - |
| **acciones** |  | Usa distintas alternativas: apps como Digital Wellbeing, OneSec, Freedom | Instala APK, activa AccessibilityService y fuentes desconocidas | App corre sobre Instagram, lee lo que hay en pantalla, VLM analiza y muestra nivel de oscuridad |  |  |
| **pensamiento** |  |  |  |  |  |  |
| **emoción** |  |  |  |  |  |  |
| **dolor** |  |  |  |  |  |  |
| **oportunidad** |  |  |  |  |  |  |

## mapa-2: Diagrama de Flujo Funcional

### plantilla - m2

* **Objetivo:** Definir la lógica de navegación y los procesos desde la perspectiva de la experiencia del usuario.  

* **Sintaxis:** Utilizar el **Lenguaje Visual de Garrett** (rombos para decisiones, rectángulos para procesos, conectores con direccionalidad explícita).

### mi versión - m2

```mermaid
flowchart TB
    n3["¿opciones de accesibilidad activas?"] --> n4["Sí"] & n5["No"]
    n5 --> n6["Activar opciones de accesibilidad"]
    n7["¿opcion de instalar desde fuentes desconocidas activada?"] --> n8["Sí"] & n9["No"]
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

- [editor mermaid](https://mermaid.ai/app/projects/2754080d-5a23-45b5-857f-8bedf2b56a8e/diagrams/c04bd30d-d449-4ea2-a8c5-1dcb3315e7a4/share/invite/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkb2N1bWVudElEIjoiYzA0YmQzMGQtZDQ0OS00ZWEyLWE4YzUtMWRjYjMzMTVlN2E0IiwiYWNjZXNzIjoiVmlldyIsImlhdCI6MTc4NjkyNDc0NH0.yZLcGxkb04tXgjUpW9VpBzJV1bHkOZmyuuE4KwqbPns?entryPoint=share-modal)

## mapa-3: Diagrama de Flujo Técnico

### plantilla - m3

Objetivo: Detallar la arquitectura del sistema, respuestas del hardware/software y gestión de excepciones.
Detalle Requerido:

- Software: Flujos de error, recuperación de credenciales, tiempos de carga, permisos de sistema.

- Hardware: Patrones de iluminación LED, tiempos de pulsación de botones para vinculación (Bluetooth), respuestas mecánicas, sensores y tolerancias de material.

### mi versión - m3

to do