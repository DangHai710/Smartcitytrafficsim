package vn.edu.hust.traffic.utils;

import javafx.scene.image.Image;

public class ImageLoader {
	public static Image loadImage(String name) {
		String path = "/assets/images/" + name;
		return new Image(ImageLoader.class.getResourceAsStream(path));
	}
}