package vn.edu.hust.traffic.utils;

import javafx.animation.PauseTransition;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import vn.edu.hust.traffic.config.SimulationConfig;

public class SoundPlayer {
	private static MediaPlayer backgroundPlayer;
	private static final String SOUND_FOLDER = "/assets/sounds/";

	public static void playSound(String fileName) {
		if (!SimulationConfig.soundEnabled) {
			return;
		}

		try {
			String path = SoundPlayer.class.getResource(SOUND_FOLDER + fileName).toExternalForm();

			AudioClip clip = new AudioClip(path);
			clip.play();
		} catch (Exception e) {
			System.out.println("Cannot play sound: " + fileName);
		}
	}

	public static void playBackgroundMusic() {
		if (!SimulationConfig.soundEnabled) {
			return;
		}

		try {
			if (backgroundPlayer == null) {
				String path = SoundPlayer.class.getResource(SOUND_FOLDER + "background.mp3").toExternalForm();

				Media media = new Media(path);
				backgroundPlayer = new MediaPlayer(media);
				backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
				backgroundPlayer.setVolume(0.25);
			}

			backgroundPlayer.play();
		} catch (Exception e) {
			System.out.println("Cannot play background music");
		}
	}

	public static void pauseBackgroundMusic() {
		if (backgroundPlayer != null) {
			backgroundPlayer.pause();
		}
	}

	public static void updateBackgroundMusicState() {
		if (SimulationConfig.soundEnabled) {
			playBackgroundMusic();
		} else {
			pauseBackgroundMusic();
		}
	}

	public static void playHorn() {
		playSoundForSeconds("horn.mp3", 3);
	}

	public static void playSignal() {
		playSoundForSeconds("signal.mp3", 3);
	}

	public static void playAmbulance() {
		playSoundForSeconds("ambulance.mp3", 3);
	}

	public static void playSoundForSeconds(String fileName, double seconds) {
		if (!SimulationConfig.soundEnabled) {
			return;
		}

		try {
			String path = SoundPlayer.class.getResource(SOUND_FOLDER + fileName).toExternalForm();

			AudioClip clip = new AudioClip(path);
			clip.play();

			PauseTransition delay = new PauseTransition(Duration.seconds(seconds));
			delay.setOnFinished(e -> clip.stop());
			delay.play();
		} catch (Exception e) {
			System.out.println("Cannot play sound: " + fileName);
		}
	}
}