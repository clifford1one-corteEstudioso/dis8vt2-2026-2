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

1. crear una app capaz de capturar un png de lo que se ve en pantalla

2. agregar tiempo de espera antes de guardar la imagen, para alcanzar a abrir los reels

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

### fase-03

Muestrear la pantalla en continuo, con la app en segundo plano, y ver si de ahí sale una señal para contar cortes. Es el último riesgo grande: si no se puede, la métrica central de la tesis no es medible.

Corre 4 veces por segundo. Cada muestra lee 32×32 puntos sueltos y los compara con la muestra anterior. Escribe un CSV, una fila por muestra.

**No decide qué es un corte.** Guarda el número crudo de cuánto cambió la pantalla. El umbral se elige después, mirando los datos.

#### resultado — prueba de 83 s

| | |
| --- | --- |
| Ritmo | 3.7 muestras/s, estable |
| Frames llegando | ~51/s |
| El servicio | Vivo hasta que lo detuve yo |

La captura sostenida funciona. Falta la prueba de 20 minutos.

#### la señal se separa sola

Distribución de las primeras 49 muestras:

```
   0-  2  ######################       22
   2-  5  #########                     9
   5- 10  ####                          4
  10- 20  ##                            2
  20- 40  #####                         5
  40- 80  #####                         5
  80-130  ##                            2
```

Dos poblaciones con un hueco vacío entre medio: **ni un valor entre 17 y 25**. O la pantalla casi no cambia (mediana 2.26) o cambia muchísimo. No hay zona gris.

Por eso el umbral casi no importa:

| Umbral | Eventos |
| --- | --- |
| 18 | 12 |
| 20 | 12 |
| 25 | 12 |

Se puede mover 40% y da lo mismo. Si la medición fuera frágil, cada valor daría un número distinto y ninguno sería creíble.

Da 0.83 eventos/s — uno cada 1.2 s. Plausible para reels.

#### lo que el dato todavía no distingue

**Corte de plano o swipe al siguiente reel.** Los dos cambian la pantalla entera. Los valores más altos —122.95, 97.50, 67.34— parecen cambios de reel, no cortes dentro de uno.

Puede importar mucho: los cortes miden cuánto me estimula el contenido, los swipes cuánto me muevo yo. Dos historias distintas, hoy mezcladas en la misma columna.

**Cuatro segundos casi muertos** entre los 4.9 y los 8.8 s, valores entre 0.7 y 1.6. ¿Reel pausado, foto, o todavía no llegaba a Instagram? Saberlo dice si el detector ve lo que creo.

#### pendiente

1. Prueba de 20 minutos. 83 segundos no prueban que aguante.
2. Grabar un reel con los cortes contados a mano y comparar contra el CSV. Con ese hueco en la distribución, debería salir limpio.
3. Anotar batería y temperatura antes y después.

### fase-04

La 03 mide; esta además muestra. Caja encima de Instagram con el tiempo de sesión y los cortes por segundo de los últimos 15 s.

Ventana móvil, no acumulado: el promedio de veinte minutos se aplana y deja de decir nada.

**Comprobado** (56 s, 9 sep): el overlay se dibuja (`ty=2038`, 540×77 px) y se quita limpio. Muestreo estable entre 3.0 y 3.6/s en segundo plano.

Los toques la atraviesan. Muestra, no interrumpe.

### fase-05 — la app

Deja de ser una prueba. Se enciende sola, mide sola, y a los quince minutos devuelve la decisión.

#### por qué se parte en dos capas

El permiso de grabar pantalla no se puede dejar concedido: desde Android 14 es de un solo uso, cada medición exige aceptar un diálogo. Así que el conteo de cortes **nunca** podrá arrancar solo.

Y una intervención que hay que encender a mano no sirve: quien se acuerda de abrirla ya está siendo consciente, y es justamente quien no la necesita.

| Capa | Qué mide | ¿Arranca sola? |
| --- | --- | --- |
| Accesibilidad | Tiempo de sesión, swipes por minuto | **Sí**, siempre |
| Captura — modo investigación | Cortes por segundo | No, exige el diálogo |

#### las tres etapas

| | Cuándo | Qué pasa |
| --- | --- | --- |
| Espejo | Primer deslizamiento | Caja discreta, semitransparente |
| Presencia | 5 min | Crece, opaca, con borde |
| Decisión | 15 min | Pantalla completa: *¿querías estar todo este rato?* |

Ninguna bloquea. En la decisión los dos botones pesan igual — sin cuenta regresiva, sin castigo. Una fricción que castiga se desinstala; una que interrumpe, se piensa.

Los cuatro números (5, 15, 10 de repregunta, 30 s de tolerancia) están en `Config.kt` y **ninguno está validado**. Salen de intuición.

#### se enciende con cualquier scroll

La regla acordada era *primer scroll sobre contenido a pantalla completa*. La segunda mitad **no está implementada**: esa condición sale de los píxeles, y la capa de accesibilidad no los ve. Solo sabe que hubo un scroll.

O sea hoy mide cuánto scrolleo en general, no cuánto caigo en el pozo de reels. Se enciende igual en WhatsApp, la galería o el navegador.

Dos formas de cerrarlo:

| | Cómo | Costo |
| --- | --- | --- |
| Lista de apps | Solo cuenta Instagram, TikTok, Shorts | Deja de ser agnóstico. Hay que mantener la lista |
| Duración mínima | El contador aparece recién tras N minutos de scroll continuo | Sigue agnóstico. Ignora el scroll corto y funcional |

Me inclino por la segunda, y el argumento es de la tesis: **scrollear no es el problema**. Bajar por una conversación buscando algo es scroll con intención. Lo que ataco es el scroll que se estira solo. Un mínimo de duración distingue eso sin nombrar ninguna app, que era lo bueno de la definición original.

## siguientes pasos de la app

Ordenados por lo que puede invalidar el trabajo, no por comodidad.

### 1. Usarla una semana

Instalar la fase 05 y vivir con ella. Es lo único que responde si los 15 minutos son el momento correcto, si la caja estorba, y si la pantalla de decisión se siente como ayuda o como reto.

Anotar cada vez: qué elegí, y si me arrepentí.

Sin esto, todo lo demás es decorar supuestos.

### 2. Cerrar la validación pendiente

- Sesión de 20 minutos seguidos con el modo investigación. Sigue sin probarse que aguante; lo más largo fue 83 s.
- Video con los cortes contados a mano contra el CSV, para fijar el umbral de 18.
- Batería y temperatura antes y después.

### 3. Cerrar la condición de pantalla completa

Hoy la app se enciende con cualquier scroll. Decidir entre lista de apps o duración mínima, e implementarlo. Es un número más en `Config.kt` si va la segunda.

### 4. Decidir qué hace "Salir"

Hoy manda al inicio del teléfono. No se puede cerrar Instagram por el usuario, pero mandarlo al inicio se acerca a bloquear — y choca con la idea de no prohibir.

La alternativa es que solo cierre el overlay y lo deje ahí. Es una decisión de diseño, no técnica.

### 5. Separar corte de plano de swipe

Hoy los dos suben la misma columna. Miden cosas distintas: uno cuánto me estimula el contenido, otro cuánto me muevo yo. Con la capa de accesibilidad ya tengo los swipes por separado, así que se puede restar.

### 6. Persistencia

La sesión no sobrevive a un reinicio. No hace falta para la intervención, sí para mostrar historial acumulado o para tener datos de una semana. Ahí entra Room.

### lo que NO haría todavía

Pulir la interfaz de configuración, agregar ajustes para el usuario, o diseñar más etapas de fricción. Todo eso se decide mejor después de la semana de uso, y hacerlo antes es inventar números como inventamos el umbral de cortes.
