package vn.edu.hust.traffic.view.assets;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

public class SpriteManager {
    private final Map<String, Image> cache = new HashMap<>();

    public Image getVehicleImage(String vehicleType) {
        String key = vehicleType.toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        String[] candidates = {
                "/assets/images/" + key + ".png",
                "/assets/sprites/" + key + ".png",
                "/images/" + key + ".png",
                "/" + key + ".png"
        };

        for (String candidate : candidates) {
            URL url = SpriteManager.class.getResource(candidate);
            if (url != null) {
                Image image = new Image(url.toExternalForm(), false);
                if (!image.isError()) {
                    cache.put(key, image);
                    return image;
                }
            }
        }

        cache.put(key, null);
        return null;
    }
}
