package vn.edu.hust.traffic.view;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import vn.edu.hust.traffic.model.map.CrossIntersection;
import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.vehicle.Car;

import java.util.ArrayList;
import java.util.List;

import vn.edu.hust.traffic.controller.TrafficController;

public class SimulationWindow extends Application {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private Canvas canvas;
    private GraphicsContext gc;

    private TrafficController controller;

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        Pane root = new Pane();
         
           ControlPanel panel = new ControlPanel();
           panel.setLayoutX(820);
panel.setLayoutY(20);
    root.getChildren().addAll(canvas, panel);
        Scene scene = new Scene(root, WIDTH + 200, HEIGHT);

        setupSimulation();

        primaryStage.setTitle("Traffic Simulation - Cross Intersection");
        primaryStage.setScene(scene);
        primaryStage.show();

        AnimationTimer timer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (lastUpdate > 0) {
                    double dt = (now - lastUpdate) / 1e9;
                    update(dt);
                    render();
                }
                lastUpdate = now;
            }
        };
        timer.start();
    }

    private void setupSimulation() {
        controller = new TrafficController();
    }

    private void update(double dt) {
        controller.update(dt);
    }

    private void render() {
        gc.clearRect(0, 0, WIDTH, HEIGHT);
        // Vẽ đường ngang
        gc.strokeRect(0, HEIGHT / 2.0 - 40, WIDTH, 80);
        // Vẽ đường dọc
        gc.strokeRect(WIDTH / 2.0 - 40, 0, 80, HEIGHT);
        // Vẽ vạch dừng
        gc.strokeLine(WIDTH / 2.0 - 60, HEIGHT / 2.0 - 40, WIDTH / 2.0 - 60, HEIGHT / 2.0 + 40);
        gc.strokeLine(WIDTH / 2.0 + 60, HEIGHT / 2.0 - 40, WIDTH / 2.0 + 60, HEIGHT / 2.0 + 40);
        gc.strokeLine(WIDTH / 2.0 - 40, HEIGHT / 2.0 - 60, WIDTH / 2.0 + 40, HEIGHT / 2.0 - 60);
        gc.strokeLine(WIDTH / 2.0 - 40, HEIGHT / 2.0 + 60, WIDTH / 2.0 + 40, HEIGHT / 2.0 + 60);

        // Lấy dữ liệu từ controller
        List<TrafficLight> lights = controller.getLights();
        List<Car> cars = controller.getCars();

        // Vẽ đèn giao thông và số giây còn lại
        gc.setFill(lights.get(0).getState() == TrafficLight.State.GREEN ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);
        gc.fillOval(WIDTH / 2.0 - 70, HEIGHT / 2.0 - 70, 20, 20);
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillText(String.valueOf(lights.get(0).getTimeLeft()), WIDTH / 2.0 - 65, HEIGHT / 2.0 - 55);

        gc.setFill(lights.get(1).getState() == TrafficLight.State.GREEN ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);
        gc.fillOval(WIDTH / 2.0 + 50, HEIGHT / 2.0 - 70, 20, 20);
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillText(String.valueOf(lights.get(1).getTimeLeft()), WIDTH / 2.0 + 55, HEIGHT / 2.0 - 55);

        gc.setFill(lights.get(2).getState() == TrafficLight.State.GREEN ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);
        gc.fillOval(WIDTH / 2.0 - 70, HEIGHT / 2.0 + 50, 20, 20);
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillText(String.valueOf(lights.get(2).getTimeLeft()), WIDTH / 2.0 - 65, HEIGHT / 2.0 + 65);

        // Đèn trên (lights.get(3))
        gc.setFill(lights.get(3).getState() == TrafficLight.State.GREEN ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);
        gc.fillOval(WIDTH / 2.0 + 50, HEIGHT / 2.0 + 50, 20, 20);
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillText(String.valueOf(lights.get(3).getTimeLeft()), WIDTH / 2.0 + 55, HEIGHT / 2.0 + 65);

        // Vẽ xe
        gc.setFill(javafx.scene.paint.Color.BLUE);
        for (Car car : cars) {
            gc.fillRect(car.getX(), car.getY(), car.getWidth(), car.getHeight());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
