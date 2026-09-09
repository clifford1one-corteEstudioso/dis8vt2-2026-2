# Love Yourself — fase 05

Ya no es una prueba: es la app. Se enciende sola, mide sola, y a los quince
minutos te devuelve la decisión.

## Por qué se separa en dos capas

El permiso de grabar pantalla **no se puede dejar concedido**. Desde Android 14
es de un solo uso: cada medición exige que el usuario acepte un diálogo. Así
que el conteo de cortes nunca podrá arrancar solo.

Y una intervención que hay que encender a mano no sirve: quien se acuerda de
abrirla ya está siendo consciente, y es justamente quien no la necesita.

| Capa | Qué mide | ¿Arranca sola? |
|---|---|---|
| **Accesibilidad** | Tiempo de sesión, swipes por minuto | **Sí**, siempre |
| **Captura** (modo investigación) | Cortes por segundo | No, exige el diálogo |

La app vive de la primera. La segunda es el instrumento de medición, no parte
del producto.

## Las tres etapas

| | Cuándo | Qué pasa |
|---|---|---|
| **Espejo** | Desde el primer deslizamiento | Caja discreta, 12sp, semitransparente |
| **Presencia** | A los 5 min | La caja crece a 17sp, opaca, con borde |
| **Decisión** | A los 15 min | Pantalla completa: *"Llevas 15 minutos aquí. ¿Querías estar todo este rato?"* |

Ninguna bloquea Instagram. Las dos primeras dejan pasar los toques.

En la decisión, **los dos botones pesan lo mismo**. "Seguir" no cuesta más que
"Salir", no hay cuenta regresiva ni castigo. El proyecto devuelve la decisión;
no la toma por el usuario. Una fricción que castiga se desinstala; una que
interrumpe, se piensa.

Si elige seguir, no se vuelve a preguntar hasta 10 minutos después. Preguntar
al tiro convertiría la fricción en hostigamiento.

## La regla de sesión

```
inicia  = primer deslizamiento dentro de una app
termina = se sale y no se vuelve en 30 segundos
```

Esa tolerancia de 30 s importa: sin ella, responder un WhatsApp y volver
partiría una sesión de cuarenta minutos en pedazos, y el contador nunca
llegaría a los quince.

El reloj corre con un tic por segundo, no solo con los eventos: quedarse quieto
mirando un reel sin deslizar también es estar ahí.

## Instalación

1. Abrir la app.
2. **Dibujar sobre otras apps** → conceder en Ajustes, volver.
3. **Accesibilidad** → activar "Love Yourself", volver.
4. Cerrar la app. No hay que volver a abrirla.

## Todo lo ajustable está en `Config.kt`

```kotlin
MIN_PRESENCIA = 5          // minutos hasta que la caja crece
MIN_DECISION = 15          // minutos hasta la pantalla de decisión
MIN_REPREGUNTA = 10        // espera antes de volver a preguntar
TOLERANCIA_REGRESO_S = 30  // salir y volver sin perder la sesión
```

**Ninguno está validado.** Salen de intuición, no de datos. Cuando tengas
registros de tus propias sesiones vas a querer moverlos, y por eso están todos
en un archivo y no repartidos por el código.

## Pendiente

- Correr una semana y ver si los 15 minutos son el momento correcto, o si llega
  tarde, o temprano.
- Validar el umbral de cortes del modo investigación contra un video contado a
  mano.
- Decidir si "Salir" debe mandar al inicio (hoy lo hace) o solo cerrar el
  overlay. Mandar al inicio se acerca a bloquear.
- La sesión no se guarda entre reinicios. Hoy no hace falta; si quieres mostrar
  historial acumulado, ahí entra Room.
