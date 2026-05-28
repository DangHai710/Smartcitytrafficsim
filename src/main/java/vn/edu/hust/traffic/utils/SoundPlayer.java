package vn.edu.hust.traffic.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundPlayer {
    private static final String SOUND_DIR = "/assets/sounds/";
    private static final List<MediaPlayer> ACTIVE_PLAYERS = new ArrayList<>();
    private static MediaPlayer backgroundPlayer;
    private static double volume = 0.45;
    private static boolean enabled = true;

    public static void playHorn() {
        playSound("horn.mp3");
    }

    public static void playSignal() {
        playSound("signal.mp3");
    }

    public static void playAmbulance() {
        playSound("ambulance.mp3");
    }

    public static void playSound(String name) {
        if (!enabled) {
            return;
        }

        String soundUri = resolveSoundUri(name);
        if (soundUri == null) {
            System.err.println("Sound not found: " + name);
            return;
        }

        cleanupFinishedPlayers();

        MediaPlayer player = new MediaPlayer(new Media(soundUri));
        player.setVolume(volume);
        ACTIVE_PLAYERS.add(player);
        player.setOnEndOfMedia(() -> {
            player.dispose();
            ACTIVE_PLAYERS.remove(player);
        });
        player.setOnError(() -> {
            System.err.println("Cannot play sound: " + name + " - " + player.getError());
            player.dispose();
            ACTIVE_PLAYERS.remove(player);
        });
        player.play();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        updateBackgroundMusicState();
    }

    public static void setVolume(double value) {
        volume = Math.max(0.0, Math.min(1.0, value));
        if (backgroundPlayer != null) {
            backgroundPlayer.setVolume(backgroundVolume());
        }
        for (MediaPlayer player : ACTIVE_PLAYERS) {
            player.setVolume(volume);
        }
    }

    public static void updateBackgroundMusicState() {
        if (!enabled) {
            if (backgroundPlayer != null) {
                backgroundPlayer.pause();
            }
            return;
        }

        if (backgroundPlayer == null) {
            String soundUri = resolveSoundUri("background.mp3");
            if (soundUri == null) {
                System.err.println("Sound not found: background.mp3");
                return;
            }

            backgroundPlayer = new MediaPlayer(new Media(soundUri));
            backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundPlayer.setVolume(backgroundVolume());
            backgroundPlayer.setOnError(() -> System.err.println(
                    "Cannot play background sound: " + backgroundPlayer.getError()));
        }

        backgroundPlayer.play();
    }

    private static double backgroundVolume() {
        return volume * 0.35;
    }

    private static String resolveSoundUri(String name) {
        URL resource = SoundPlayer.class.getResource(SOUND_DIR + name);
        if (resource != null) {
            return resource.toExternalForm();
        }

        File sourceFile = new File("src/main/resources/assets/sounds", name);
        if (sourceFile.isFile()) {
            return sourceFile.toURI().toString();
        }

        File targetFile = new File("target/classes/assets/sounds", name);
        if (targetFile.isFile()) {
            return targetFile.toURI().toString();
        }

        File binTargetFile = new File("bin/target/classes/assets/sounds", name);
        if (binTargetFile.isFile()) {
            return binTargetFile.toURI().toString();
        }

        return null;
    }

    private static void cleanupFinishedPlayers() {
        Iterator<MediaPlayer> iterator = ACTIVE_PLAYERS.iterator();
        while (iterator.hasNext()) {
            MediaPlayer player = iterator.next();
            MediaPlayer.Status status = player.getStatus();
            if (status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.DISPOSED) {
                player.dispose();
                iterator.remove();
            }
        }
    }
}
