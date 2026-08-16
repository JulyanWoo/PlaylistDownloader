# Playlist Downloader

A high-performance **JavaFX** desktop application designed for fast, efficient, and reliable music and playlist downloading from YouTube and other supported platforms. Built with a decoupled event-driven architecture, robust thread safety, an asynchronous producer-consumer pipeline, and styled with **AtlantaFX Primer Dark**. Uses **yt-dlp** for blazing-fast raw stream downloads and **FFmpeg** for background audio conversion.

---

## Key Features

- **Asynchronous Producer-Consumer Pipeline:** `yt-dlp` streams raw audio at peak speeds (70–100+ MiB/s) while `AudioConversionService` converts tracks to MP3 in parallel on dedicated worker threads.
- **Dynamic Track Resolution & Collision Prevention:** Unique raw file templates (`raw_%(id)s___%(title)s.%(ext)s`) guarantee zero file collisions, avoid duplicate download errors, and preserve individual song titles for all tracks in a playlist.
- **Batch URL Ingestion:** Easily paste multiple whitespace- or comma-separated URLs at once directly into the input bar across Welcome, Main, and Queue views.
- **Smart Duplicate Detection & Library Analyzer:** Prevents duplicate downloads and scans existing local music libraries using fuzzy title similarity matching (Levenshtein + Jaccard distance, n-grams, and diacritic normalization).
- **Modern Event-Driven UI:** Real-time progress updates, download speed, ETA, and state indicators powered by an internal `EventBus`.
- **AtlantaFX Dark Theme & Icons:** Sleek, modern user interface styled with **AtlantaFX Primer Dark** and **Ikonli** vector font icons.
- **High-Performance Log Viewer:** Thread-safe, real-time internal console log viewer (`LogService`) with an $O(1)$ ring buffer.
- **Resilient Process Management:** Robust external process management (`ProcessExecutor`) supporting pausing, resuming, graceful termination, cancel confirmations, and shutdown hooks with complete process tree termination.
- **Native Packaging & Standalone Installer:** Supports native Windows `.exe` wrapping via **Launch4j** and full bundled standalone installers via **jpackage** (`create_installer.ps1`).

---

## Technology Stack

- **Java 17+** (JavaFX 17)
- **AtlantaFX** (Modern CSS Theme System - Primer Dark)
- **Ikonli** (Vector icon pack framework for JavaFX)
- **SLF4J & Logback** (Structured Logging)
- **yt-dlp** (High-speed media extraction backend)
- **FFmpeg** (Parallel audio processing and transcoding backend)
- **Maven** (Dependency management & build system)
- **Launch4j & jpackage** (Native Windows packaging)

---

## System Requirements

- **Java JDK 17** or higher.
- **Maven** (included via wrapper `mvnw`).
- Active **Internet connection**.
- **Operating System:** Windows, macOS, or Linux (with appropriate binaries).

---

## External Dependencies Setup

The application relies on `yt-dlp` and `ffmpeg`.

### Option A: System PATH or Environment Variables (Recommended)

1. **Install binaries (Windows via winget):**
   ```powershell
   winget install -e yt-dlp.yt-dlp
   winget install -e FFmpeg.FFmpeg
   ```
2. **Set Environment Variables (Optional if already in `PATH`):**
   ```powershell
   $yt = (Get-Command yt-dlp.exe).Source
   $ff = (Get-Command ffmpeg.exe).Source
   setx YT_DLP_PATH "$yt" /M
   setx FFMPEG_PATH "$ff" /M
   ```

### Option B: Local Executables in `Libs/` Directory

Alternatively, place executables directly inside the project structure:

```
PlaylistDownloader/
├── Libs/ (or src/main/Libs/)
│   ├── yt-dlp.exe
│   ├── ffmpeg.exe
│   └── qjs.exe
```

The application automatically resolves binaries in the following order:

1. Environment variables (`YT_DLP_PATH`, `FFMPEG_PATH`)
2. System `PATH`
3. Project local `src/main/Libs/` or `Libs/` directory

---

## How to Run & Build

Use the Maven wrapper to build and run the application:

### Windows:

```powershell
.\mvnw.cmd clean javafx:run
```

### Linux / macOS:

```bash
./mvnw clean javafx:run
```

### Run Unit Tests:

```powershell
.\mvnw.cmd test
```

### Package Application JAR:

```powershell
.\mvnw.cmd clean package -DskipTests
```

The packaged executable JAR will be located at `target/PlaylistDownloader.jar` and the native executable at `target/PlaylistDownloader.exe`.

### Generate Standalone Windows Installer:

```powershell
.\create_installer.ps1
```

Generates an installer at `target/installer/PlaylistDownloader-1.0.0.exe` containing an embedded JRE and all dependencies.

---

## Project Architecture

Located under `src/main/java/com/example/interfaz`:

- **`app/`**: Application entry points (`Main.java`, `Launcher.java`).
- **`controller/`**: JavaFX UI controllers (`MainController`, `QueueController`, `ProgressController`, `LogsController`, `WelcomeController`).
- **`service/`**: Core business logic and background services:
  - `download/`:
    - `DownloadCoordinator`: Master orchestrator decoupling queue polling, streaming downloads, and conversion pipelines.
    - `AudioConversionService`: Parallel FFmpeg transcoding worker pool.
    - `ProcessExecutor`: Process tree lifecycle management, streaming logs, pause/resume/kill.
    - `YtDlpCommandBuilder`: Builds optimized yt-dlp commands with raw dynamic output templates.
    - `SongMetadataService`: Metadata caching and title resolution.
    - `BinaryResolver`: Multi-tier binary path resolver.
    - `DownloadProgressParser`: Parses yt-dlp stream progress for real-time UI updates.
    - `MainDownloadFacade`: High-level UI-to-service facade.
  - `analyzer/` & `filter/`:
    - `LibraryAnalyzerService`: Scans local directory audio files and computes metrics.
    - `DuplicateDetectionService`: Multi-strategy duplicate clustering (exact, normalized, fuzzy Levenshtein & Jaccard).
    - `SongMetadataReader`: Audio file ID3/tag extraction via JAudioTagger.
  - `ui/`: `NavigationService`, `ThemeService`, `DialogService`, `WindowManager`, `WindowStageDecorator`.
  - `YouTubeDownloadService`: Service orchestrating raw streaming downloads with yt-dlp.
  - `LogService`: High-performance thread-safe log capturing ($O(1)$ ring buffer).
- **`event/`**: Decoupled `EventBus` pub-sub pattern for application events (`DownloadEvent`).
- **`factory/`**: `ServiceFactory` managing singleton lifecycle, dependency wiring, and clean resource disposal.
- **`model/`**: Domain models (`Song`, analyzer models).
- **`util/`**: File utilities and persistent storage helpers (`FileUtils`, `FormatUtils`).

---

## License & Credits

- **yt-dlp**: https://github.com/yt-dlp/yt-dlp
- **FFmpeg**: https://ffmpeg.org/
- **AtlantaFX**: https://mkpaz.github.io/atlantafx/
- **Ikonli**: https://kordamp.org/ikonli/
