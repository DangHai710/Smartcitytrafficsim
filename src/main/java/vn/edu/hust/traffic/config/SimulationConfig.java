package vn.edu.hust.traffic.config;

public class SimulationConfig {

	public static boolean autoMode = true;

	public static boolean soundEnabled = true;

	public static boolean graphicMode = false;

	/**
	 * 1 = LOW 2 = MEDIUM 3 = HIGH
	 */
	public static int density = 2;

	private SimulationConfig() {
		// Utility class: không cho tạo object
	}
}