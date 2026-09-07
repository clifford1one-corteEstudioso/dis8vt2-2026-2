# avance pre sesion-04

## APK

### plan

| Paso | Qué haces | Qué confirmas | Si falla |
| --- | --- | --- | --- |
| **0** | Proyecto vacío en Android Studio → instalarlo en tu celu (modo desarrollador + depuración USB) | Que la cadena build → APK → celu funciona | Drivers, firma o versiones. No avances sin esto |
| **1a** | App desechable: MediaProjection captura un frame y lo guarda | Que puedes obtener píxeles con consentimiento por sesión | Revisar foreground service y notificación persistente |
| **1b** | App desechable: AccessibilityService loguea eventos con Instagram abierto | **Si Instagram entrega un árbol de nodos legible** | Tu métrica de origen no existe → replantearla ahora |
| **1c** | App desechable: dibujar un cuadrado sobre otra app | Que el overlay se renderiza sin bloquear | Permiso SYSTEM_ALERT_WINDOW |
| **2** | Andamiaje por tandas: Gradle+Manifest → servicios → analizador+rastreador → resto | Estructura correcta antes que lógica completa | — |
| **3** | Video con cortes contados a mano vs. lo que mide la app | Validez de la medición, no solo que corra | Ajustar umbral; ver cuánto ensucia el movimiento de cámara |
| **4** | Room y overlay definitivos | Persistencia y sello visual | Lo más mecánico, bajo riesgo |
| **5** | APK release firmado | Solo si alguien más lo instala | El APK de debug ya te sirve para trabajar |

## investigar

- <https://youtu.be/xT8oP0wy-A0>

## avance apps

### fase-0

crear una app vacío y que exista en mi celular

### fase-01

1. crear una app capqaz de capturar un png de lo que se ve en pantalla

2. agregar tiempo de esperar antes de guardar la imagen, para alcanzar a abrir los reels

### fase-02

La fase 01 ya confirmó que puedo capturar la imagen de un reel. Ahora quiero saber otra cosa: **por qué camino llegué a él** — feed, perfil, búsqueda o DM.

### el problema

Android sí me deja ver qué hay en pantalla, pero manda esa información tan rápido y en tanta cantidad que se pierde antes de que alcance a leerla.

Si no la leo, voy a creer que Instagram no entrega nada. Y no sería cierto: entregó, pero se me escapó. Es el mismo error que evité en la fase 01 poniendo un botón de control.

Además esa información se lee con el celular conectado al PC por cable. Medir el scroll automático estando enchufado al escritorio no es medir el uso real.

### opciones

| Opción | Qué hace | Problema |
| --- | --- | --- |
| **1** Leer en vivo, más lento | Muestra menos datos, pero legibles | Puede botar justo el dato que importa. Obliga a usar cable |
| **2** Guardar todo en un archivo | No pierde nada. Uso el celular suelto | No sé si funciona hasta revisar el archivo después |
| **3** Las dos | — | Un poco más de trabajo |

### decisión

**3.** Porque responden preguntas distintas:

- **En vivo** me dice si el sistema está funcionando.
- **El archivo** me dice si hay una señal que distinga los cuatro caminos, y me deja usar Instagram desconectado, como lo usaría normalmente.
