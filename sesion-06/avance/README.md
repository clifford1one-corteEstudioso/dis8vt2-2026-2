# entendiendo un arhcivo kotlin

explicación de las carpetas y arhcivos dentro de `love-yourself-app-v5`.

## love-yourself-fase-05/

al entrar te encuentras con 2 carpetas:

- **`app/`**: todo lo referente a mi código
- `Gradle Scripts/`: aquí se juntan todos los archivos de configuración de la build, librerías que usa, versión de android, cómo se empaqueta, etc.

> love-yourself-app/
> ├── app/
> └── Gradle Scripts

### app/

al entrar te encuentras con 3 carpetas:

- `manifests/`: es cmo la identificación del programa(nombre e ícono app, que se abre al clickar el ícono, permisos que pide, servicio de accesibilidad,etc.).
- **`kotlin+java/`**: aquí existe todo lo que es mi código
- `res/`: viene de resolution, contiene todos los recursos que no son código(imágenes, archivos, paleta de colores, etc)

> app/
> ├── manifests/
> ├── kotlin+java/
> └── res/

#### kotlin+java

al entrar te encuentras con 1 carpeta:

- **`com.love.yourself/`**: esta carpeta es desde donde se aplica la empaquetación. EL propósito de la empaquetación es darle un distintivo único a su contenido. De esta forma pueden existir 100 archivos `config`, pero cada uno actuar solo dentro de su propio paquete.

> kotlin+java/
> └── com.love.yourself/

##### com.love.yourself/

al entrar te encuentras con 2 carpetas y un archivo:

- **`lab/`**: aquí está todo el funcionamiento de la app
- `ui.theme/`: aplica estilos a `MainActivity.kt`
- `MainActivity.kt`: puerta de entrada, lo que hace cuando se toca el ícono de la app

> com.love.yourself/
> ├── lab/
> ├── ui.theme/
> └── MainActivity.kt

###### lab/

al entrar encuentras 7 archivos:

- `CapturaContinuaService`:
- `Config.kt`:
- `DetectroCortes`:
- `EstadoSesion`:
- `OverlayFriccion`: es la capa visible de la intervención, usa WindowManager para dibujar por encima de otras app. `SesionService` le dice qué mostrar y cuándo, y este archivo solo ejecuta.
- `RegistroCSV`:
- `SesionService`:

> lab/
> ├── CapturaContinuaService
> ├── Config.kt
> ├── DetectroCortes
> ├── EstadoSesion
> ├── OverlayFriccion
> ├── RegistroCSV
> └── SesionService
