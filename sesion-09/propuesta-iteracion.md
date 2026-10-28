# Propuesta de iteración de la app

hallazgo -> reformulación -> cambio de diseño -> repensar testeo

> Datos de línea base al 28 de octubre, 8 participantes. Cifras en `analisis-linea-base.xlsx`.

## Contexto

La app tiene un feature donde se le presenta al usuario una mini encuesta luego de ciertas sesiones (máximo 1 vez cada 90 minutos, a menos que sea su sesión más larga del día, en cuyo caso siempre pregunta). La encuesta se compone de 3 partes:

1. ¿A qué entraste?
> a. A pasar el rato
> b. A algo puntual
> c. No sé

2. ¿Qué tan significativo fue ese rato para ti?

> Calificar del 1-7

3. ¿Sentiste que tú llevabas el control?

> Calificar del 1-7

## Hallazgos

### H1 · Pérdida de registro en Xiaomi

registros perdidos en todos los Xiaomi que participaron: **RESUELTO**. Para ello, tuve que activar la opción de encendido automático de la app, esta opción permite que al apagarse se vuelva a iniciar la app de manera automática.

### H2 · Muchos reels quedan como DESCONOCIDO

muchos reels quedan clasificados como DESCONOCIDO. se registra que es un reel pero no se conoce el origen.

### H3 · Sesiones cortadas en pedazos

sesiones se perciben como terminadas cuando en verdad es una pausa. Lo sé por que hay mucha cantidad de sesiones (800+) breves (10-15 s)

### H4 · El propósito declarado no predice el arrastre

el 61 % de las veces que respondieron que entraron "a pasar el rato", no entraron a reels.
el porcentaje de arrastre entre las visitas clasificadas como pasar el rato y algo puntual es del 19 % y 13 %, respectivamente, una diferencia leve.

En cambio, el control declarado (pregunta 3) sí baja cuando hay arrastre: en promedio es 4,6 en visitas sin reels, 3,7 en visitas con arrastre y 3,3 cuando el arrastre supera los 3 minutos. es una tendencia débil (33 respuestas con arrastre), pero indica que el indicador debe validarse contra el control y no contra el propósito.

### H5 · El arrastre varía mucho entre personas

Añadir fricción: Debido a la diferencia en cómo interactúan las personas usuarias, hay algunas donde la intervención aparece muy poco, y otras donde aparece aunque mantengan el uso habitual.

## Reformulación

- El propósito (pregunta 1) y el control (pregunta 3) son dos ejes distintos. Así evito confundir la *decisión de distraerse* con el *error*.
- El indicador se valida contra el control (pregunta 3), no contra el propósito (pregunta 1). La pregunta 3 sirve para validar la variable `ARRASTRE_INFERIDO`.
- *¿cuenta como uso dirigido decidir conscientemente pasar el rato?*

## Love-Yourself-v8.4

| Hallazgo | Cambio en v8.4 |
| --- | --- |
| H1 | Registrar huecos del servicio al reiniciar; guiar en el onboarding a activar inicio automático y batería sin restricciones |
| H2 | DESCONOCIDO que hereda una entrada de explorar o perfil, o que trae anuncio, pasa a `ARRASTRE_INFERIDO` con su motivo; la categoría observada se conserva |
| H3 | Visitas separadas por ≤ 30s forman una sesión; la encuesta se dispara al cerrar la sesión |
| H4 | Validar el indicador y calibrar el umbral con la pregunta 3 (control), en vez de la pregunta 1 |
| H5 | Umbral de intervención por persona = p75 de su línea base |

## Repensar testeo

- **Reproducción offline:** correr el código de v8.4 sobre los registros reales y comparar antes/después por participante.
- **Datos nuevos:** las reglas se derivaron con datos al 28 de octubre; se prueban con los días 28 a 30 de octubre, no usados para derivarlas.
- **Entrevistas:** 3 entrevistas de 10 min, mostrando a los usuarios datos reales extraídos de sus sesiones, y preguntarles por ello.
- **Criterios de aceptación:**
  - DESCONOCIDO ≤ 10% del tiempo en reels por participante.
  - Preguntas descartadas por volver a Instagram ≤ 20% (hoy 45%).
  - DIRIGIDO no cambia.
