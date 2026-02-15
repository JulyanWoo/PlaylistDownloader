# YouTube Downloader - Descargador de Música

Aplicación de escritorio desarrollada en **JavaFX** para descargar música desde YouTube de manera eficiente, gestionando colas de descarga y detectando duplicados. Utiliza la potencia de **yt-dlp** y **FFmpeg** para el procesamiento de audio.

##  Características Principales

- **Descarga de Audio:** Extrae audio de videos y listas de reproducción de YouTube en formato MP3.
- **Gestión de Colas:** Agrega múltiples enlaces para descargar de forma secuencial.
- **Detección de Duplicados:** Evita descargar canciones que ya existen en tu biblioteca local.
- **Interfaz Gráfica Intuitiva:** Visualiza el progreso de descarga, velocidad y estado en tiempo real.
- **Visor de Logs:** Panel integrado para monitorear la actividad interna y depurar errores.
- **Configuración Persistente:** Guarda tus preferencias de descarga automáticamente.

## Requisitos del Sistema

Para ejecutar o compilar este proyecto necesitas:

- **Java JDK 17** o superior.
- **Maven** (incluido mediante wrapper `mvnw`).
- **Conexión a Internet** activa.
- **Sistema Operativo:** Windows (probado), aunque debería funcionar en Linux/macOS con los binarios adecuados.

### Dependencias Externas (no incluidas por defecto)
La aplicación usa dos herramientas externas:
1. **yt-dlp:** Motor de descarga.
2. **FFmpeg:** Conversión de audio.

Puedes instalarlas en el sistema o colocarlas manualmente en rutas locales. La app acepta dos métodos:
- Variables de entorno `YT_DLP_PATH` y `FFMPEG_PATH`.
- Ejecutables locales en `Libs/` con rutas por defecto (ver abajo).

##  Instalación y Ejecución

### 1. Clonar el repositorio
```bash
git clone <URL-DEL-REPOSITORIO>
cd PlaylistDownloader
```

### 2. Requisitos previos

Instala Java JDK 17 y configura la variable de entorno `JAVA_HOME`.

Verifica la instalación con:
```bash
java -version
```
Si ves el mensaje `JAVA_HOME not found`, instala JDK 17 y configura `JAVA_HOME` antes de continuar.

### 3. Instalar dependencias

**Windows (recomendado con winget):**
```powershell
winget install -e yt-dlp.yt-dlp
winget install -e FFmpeg.FFmpeg
```

Configura variables para que la app detecte los binarios:
```powershell
$yt = (Get-Command yt-dlp.exe).Source
$ff = (Get-Command ffmpeg.exe).Source
setx YT_DLP_PATH "$yt" /M
setx FFMPEG_PATH "$ff" /M
```

**Alternativa local (sin variables):** coloca los ejecutables en el proyecto:
```
src/main/
├── Libs/
│   ├── yt-dlp.exe
│   └── ffmpeg-<version>-full_build/
│       └── ffmpeg-<version>-full_build/
│           └── bin/
│               └── ffmpeg.exe
```
Rutas por defecto utilizadas por la app:
- `src/main/Libs/yt-dlp.exe` o `Libs/yt-dlp.exe` en la raíz
- `src/main/Libs/.../bin/ffmpeg.exe` o la ruta equivalente en `Libs/`

La app detecta automáticamente `yt-dlp` y `ffmpeg` en `PATH`. Si no están en `PATH`, usa `YT_DLP_PATH`/`FFMPEG_PATH`. Si tampoco existen, busca en `src/main/Libs` y luego en `Libs/` en la raíz.

### 3. Ejecutar la aplicación
Usa el wrapper de Maven para iniciar la aplicación sin instalar nada extra:

**En Windows:**
```powershell
./mvnw.cmd clean javafx:run
```

**En Linux/macOS:**
```bash
./mvnw clean javafx:run
```

### 4. Generar ejecutable (Opcional)
Para crear un archivo JAR con todas las dependencias:
```powershell
./mvnw.cmd -DskipTests package
```
El archivo se generará en `target/interfaz-1.2-SNAPSHOT-shaded.jar`.

##  Estructura del Proyecto

El código fuente se encuentra en `src/main/java/com/example/interfaz` y sigue una arquitectura MVC:

- **`app/`**: Punto de entrada (`Main.java`).
- **`controller/`**: Lógica de la interfaz gráfica.
  - `MainController`: Coordinador principal.
  - `QueueController`: Gestión de la lista de descargas.
  - `ProgressController`: Actualización de barras de progreso.
  - `LogsController`: Ventana de registros.
- **`service/`**: Lógica de negocio.
  - `YouTubeDownloadService`: Wrapper para ejecutar `yt-dlp`.
  - `ProgressReporter`: Parsea la salida de consola para actualizar la UI.
  - `SongFilterService`: Lógica para filtrar canciones duplicadas.
- **`model/`**: Clases de datos como `Song`.
- **`util/`**: Utilidades para manejo de archivos (`FileUtils`) y parseo.
- **`config/`**: Gestión de configuración (`AppConfig`).

##  Configuración

La aplicación crea un archivo `user-config.properties` en la raíz para guardar configuraciones como la carpeta de destino de las descargas. Puedes editarlo manualmente o desde la interfaz (si está implementado).

## Créditos

- **yt-dlp:** Herramienta de línea de comandos para descargar videos.
- **FFmpeg:** Framework multimedia para decodificar y codificar.
- **JavaFX:** Framework para la interfaz gráfica.
