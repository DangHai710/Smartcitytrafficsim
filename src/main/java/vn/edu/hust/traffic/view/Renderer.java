package vn.edu.hust.traffic.view;

import java.util.OptionalInt;

import javafx.scene.canvas.GraphicsContext;
import vn.edu.hust.traffic.view.camera.Camera;

public interface Renderer {
    void render(GraphicsContext gc, SimulationSnapshot snapshot, Camera camera, SimulationViewSettings settings);

    default OptionalInt pickTrafficLight(
            double screenX,
            double screenY,
            SimulationSnapshot snapshot,
            Camera camera,
            SimulationViewSettings settings) {
        return OptionalInt.empty();
    }
}
