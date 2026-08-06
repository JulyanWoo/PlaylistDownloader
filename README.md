# Playlist Downloader

A modern **JavaFX** desktop application designed for fast, efficient, and reliable music and playlist downloading from YouTube and other supported platforms. Built with a decoupled event-driven architecture, robust thread safety, and styled with **AtlantaFX Primer Dark**. Uses **yt-dlp** and **FFmpeg** for high-quality audio extraction and processing.

---

## Key Features

- **Audio Extraction:** Download tracks and full playlists in MP3 format with metadata tags.
- **Queue Management:** Add, remove, and manage download queues asynchronously.
- **Smart Duplicate Detection:** Prevents downloading songs already present in your local music library using fuzzy title similarity matching (Levenshtein + Jaccard distance).
- **Modern Event-Driven UI:** Real-time progress updates, download speed, ETA, and state indicators powered by an internal `EventBus`.
- **AtlantaFX Dark Theme & Icons:** Sleek, modern user interface with **AtlantaFX Primer Dark** and **Ikonli** vector font icons.
- **High-Performance Log Viewer:** Thread-safe, real-time internal console log viewer (`LogService`).
- **Resilient Process Management:** Robust external process management (`ProcessExecutor`) supporting pausing, resuming, graceful termination, and clean shutdown.

---

## Technology Stack

- **Java 17+** (JavaFX 17)
- **AtlantaFX** (Modern CSS Theme System - Primer Dark)
- **Ikonli** (Icon pack framework for JavaFX)
- **SLF4J & Logback** (Structured Logging)
- **yt-dlp** (Command-line media downloader)
- **FFmpeg** (Audio processing and conversion backend)
- **Maven** (Dependency management & build system)

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
├── Libs/
│   ├── yt-dlp.exe
│   └── ffmpeg.exe
```

The application automatically resolves binaries in the following order:

1. Environment variables (`YT_DLP_PATH`, `FFMPEG_PATH`)
2. System `PATH`
3. Project local `src/main/Libs/` or `Libs/` directory

---

## How to Run

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
.\mvnw.cmd -DskipTests package
```

The packaged executable JAR will be located at `target/interfaz-1.2-SNAPSHOT-shaded.jar`.

---

## Project Architecture

Located under `src/main/java/com/example/interfaz`:

- **`app/`**: Application entry point (`Main.java`, `Launcher.java`).
- **`controller/`**: JavaFX UI controllers (`MainController`, `QueueController`, `ProgressController`, `LogsController`).
- **`service/`**: Core business logic and background services:
  - `download/`: `DownloadCoordinator`, `ProcessExecutor`, `YtDlpCommandBuilder`, `BinaryResolver`, `DownloadProgressParser`.
  - `filter/`: `DuplicateFinder`, `SimilarityCalculator`, `TitleNormalizer`.
  - `ui/`: `NavigationService`, `ThemeService`, `DialogService`, `WindowManager`, `FolderChooserService`.
  - `YouTubeDownloadService`: High-level service handling process orchestration.
  - `LogService`: High-performance thread-safe log capturing ($O(1)$ ring buffer).
- **`event/`**: Decoupled `EventBus` pub-sub pattern for application events (`DownloadEvent`).
- **`factory/`**: `ServiceFactory` managing singleton lifecycle and clean resource disposal.
- **`model/`**: Domain models (`Song`).
- **`util/`**: File utilities and persistent storage helpers (`FileUtils`).

---

## License & Credits

- **yt-dlp**: https://github.com/yt-dlp/yt-dlp
- **FFmpeg**: https://ffmpeg.org/
- **AtlantaFX**: https://mkpaz.github.io/atlantafx/
- **Ikonli**: https://kordamp.org/ikonli/
