package com.example.interfaz.service.ui.analyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Owns the single JavaFX MediaPlayer used to preview duplicate tracks.
 * JavaFX Media handles the local MP3, AAC/M4A and WAV formats without
 * introducing another playback dependency.
 */
public final class AudioPreviewService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioPreviewService.class);
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp3", "aac", "m4a", "wav");

    private MediaPlayer mediaPlayer;
    private Path currentFile;
    private Runnable onPlaybackChanged = () -> {};
    private Consumer<Throwable> onPlaybackError = error -> {};

    public static boolean isSupported(Path file) {
        if (file == null || file.getFileName() == null) {
            return false;
        }

        String name = file.getFileName().toString();
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == name.length() - 1) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(name.substring(extensionIndex + 1).toLowerCase(Locale.ROOT));
    }

    public static boolean isPreviewAvailable(Path file) {
        return isSupported(file) && Files.isRegularFile(file);
    }

    public boolean isPlaying(Path file) {
        if (file == null || currentFile == null || mediaPlayer == null) {
            return false;
        }

        return currentFile.equals(normalize(file))
                && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public void toggle(
            Path file,
            Runnable onPlaybackChanged,
            Consumer<Throwable> onPlaybackError
    ) {
        Runnable action = () -> toggleOnFxThread(file, onPlaybackChanged, onPlaybackError);
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            try {
                Platform.runLater(action);
            } catch (IllegalStateException exception) {
                reportError(onPlaybackError, exception);
            }
        }
    }

    private void toggleOnFxThread(
            Path file,
            Runnable playbackChanged,
            Consumer<Throwable> playbackError
    ) {
        if (!isPreviewAvailable(file)) {
            reportError(playbackError, new IllegalArgumentException(
                    "La vista previa está disponible únicamente para archivos MP3, AAC/M4A o WAV existentes."));
            return;
        }

        this.onPlaybackChanged = playbackChanged != null ? playbackChanged : () -> {};
        this.onPlaybackError = playbackError != null ? playbackError : error -> {};

        Path normalizedFile = normalize(file);
        if (normalizedFile.equals(currentFile) && mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
            notifyPlaybackChanged();
            return;
        }

        disposeCurrentPlayer();

        try {
            Media media = new Media(normalizedFile.toUri().toString());
            MediaPlayer nextPlayer = new MediaPlayer(media);
            mediaPlayer = nextPlayer;
            currentFile = normalizedFile;

            media.setOnError(() -> handlePlaybackError(nextPlayer, media.getError()));
            nextPlayer.setOnReady(() -> {
                if (mediaPlayer == nextPlayer) {
                    nextPlayer.play();
                    notifyPlaybackChanged();
                }
            });
            nextPlayer.setOnPlaying(this::notifyPlaybackChanged);
            nextPlayer.setOnPaused(this::notifyPlaybackChanged);
            nextPlayer.setOnStopped(this::notifyPlaybackChanged);
            nextPlayer.setOnEndOfMedia(() -> {
                if (mediaPlayer == nextPlayer) {
                    nextPlayer.stop();
                    notifyPlaybackChanged();
                }
            });
            nextPlayer.setOnError(() -> handlePlaybackError(nextPlayer, nextPlayer.getError()));
            notifyPlaybackChanged();
        } catch (RuntimeException exception) {
            handlePlaybackError(mediaPlayer, exception);
        }
    }

    private void handlePlaybackError(MediaPlayer failedPlayer, Throwable error) {
        if (failedPlayer != null && failedPlayer != mediaPlayer) {
            return;
        }

        Throwable cause = error != null ? error
                : new IllegalStateException("JavaFX no pudo cargar este archivo de audio.");
        LOGGER.warn("No se pudo reproducir la vista previa de {}", currentFile, cause);
        disposeCurrentPlayer();
        notifyPlaybackChanged();
        onPlaybackError.accept(cause);
    }

    private void disposeCurrentPlayer() {
        MediaPlayer playerToDispose = mediaPlayer;
        mediaPlayer = null;
        currentFile = null;

        if (playerToDispose == null) {
            return;
        }

        try {
            playerToDispose.setOnReady(null);
            playerToDispose.setOnPlaying(null);
            playerToDispose.setOnPaused(null);
            playerToDispose.setOnStopped(null);
            playerToDispose.setOnEndOfMedia(null);
            playerToDispose.setOnError(null);
            playerToDispose.stop();
            playerToDispose.dispose();
        } catch (RuntimeException exception) {
            LOGGER.debug("No se pudo liberar por completo el reproductor de vista previa", exception);
        }
    }

    private void notifyPlaybackChanged() {
        onPlaybackChanged.run();
    }

    private void reportError(Consumer<Throwable> errorHandler, Throwable error) {
        Consumer<Throwable> handler = errorHandler != null ? errorHandler : onPlaybackError;
        LOGGER.warn("Vista previa de audio no disponible", error);
        handler.accept(error);
    }

    private Path normalize(Path file) {
        return file.toAbsolutePath().normalize();
    }

    @Override
    public void close() {
        Runnable disposeAction = this::disposeCurrentPlayer;
        if (Platform.isFxApplicationThread()) {
            disposeAction.run();
        } else {
            try {
                Platform.runLater(disposeAction);
            } catch (IllegalStateException ignored) {
                mediaPlayer = null;
                currentFile = null;
            }
        }
    }
}
