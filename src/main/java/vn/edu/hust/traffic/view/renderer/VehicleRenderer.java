package vn.edu.hust.traffic.view.renderer;

import java.util.List;

import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import vn.edu.hust.traffic.model.vehicle.Vehicle;
import vn.edu.hust.traffic.view.RenderMode;
import vn.edu.hust.traffic.view.SimulationViewSettings;
import vn.edu.hust.traffic.view.assets.SpriteManager;
import vn.edu.hust.traffic.view.camera.Camera;

public class VehicleRenderer {
    private final SpriteManager spriteManager = new SpriteManager();
    private long animationFrame;

    public void tick(long now) {
        animationFrame = now / 80_000_000L;
    }

    public void render(GraphicsContext gc, List<Vehicle> vehicles, Camera camera, SimulationViewSettings settings) {
        for (Vehicle vehicle : vehicles) {
            renderVehicle(gc, vehicle, camera, settings);
        }
    }

    private void renderVehicle(GraphicsContext gc, Vehicle vehicle, Camera camera, SimulationViewSettings settings) {
        Point2D p = camera.worldToScreen(vehicle.getX(), vehicle.getY());
        double scale = camera.getScale();
        double width = Math.max(5.0, vehicle.getWidth() * scale);
        double height = Math.max(4.0, vehicle.getHeight() * scale);
        String type = vehicleType(vehicle);

        gc.save();
        gc.translate(p.getX(), p.getY());
        gc.rotate(Math.toDegrees(vehicle.getDirection()));
        gc.setEffect(new DropShadow(Math.max(2.0, 4.0 * scale), 1.5, 1.5, Color.color(0, 0, 0, 0.35)));

        if (settings.getRenderMode() == RenderMode.GRAPHIC) {
            renderGraphic(gc, vehicle, type, width, height);
        } else {
            renderBasic(gc, vehicle, type, width, height);
        }

        gc.setEffect(null);
        gc.restore();

        if (camera.getScale() > 0.55 && settings.getMapType() != vn.edu.hust.traffic.view.MapType.ROAD_NETWORK) {
            drawVehicleId(gc, vehicle, p, height);
        }
    }

    private void renderBasic(GraphicsContext gc, Vehicle vehicle, String type, double width, double height) {
        gc.setFill(colorFor(type, vehicle.isPriorityVehicle()));
        gc.fillRoundRect(-width / 2.0, -height / 2.0, width, height, 4, 4);
        gc.setStroke(Color.web("#20242a"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(-width / 2.0, -height / 2.0, width, height, 4, 4);

        if (width >= 22 && height >= 10) {
            gc.setFill(textColor(type));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, Math.max(7, Math.min(11, height * 0.62))));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(labelFor(type), 0, 0);
        }
    }

    private void renderGraphic(GraphicsContext gc, Vehicle vehicle, String type, double width, double height) {
        Image image = spriteManager.getVehicleImage(type);
        if (image != null) {
            gc.drawImage(image, -width / 2.0, -height / 2.0, width, height);
        } else {
            renderVectorSprite(gc, vehicle, type, width, height);
        }

        if (vehicle.isPriorityVehicle() || "firetruck".equals(type)) {
            renderEmergencyFlash(gc, width, height);
        }
    }

    private void renderVectorSprite(GraphicsContext gc, Vehicle vehicle, String type, double width, double height) {
        if ("motorbike".equals(type) || "bicycle".equals(type)) {
            renderTwoWheeler(gc, type, width, height);
            return;
        }
        if ("bus".equals(type)) {
            renderBus(gc, width, height);
            return;
        }
        if ("ambulance".equals(type)) {
            renderAmbulance(gc, vehicle.isPriorityVehicle(), width, height);
            return;
        }
        if ("firetruck".equals(type)) {
            renderFireTruck(gc, width, height);
            return;
        }
        renderCar(gc, width, height, colorFor(type, false));
    }

    private void renderCar(GraphicsContext gc, double width, double height, Color body) {
        gc.setFill(body);
        gc.fillRoundRect(-width / 2.0, -height / 2.0, width, height, 8, 7);
        gc.setFill(Color.web("#9ed5f0", 0.85));
        gc.fillRoundRect(width / 2.0 - width * 0.25, -height * 0.32, width * 0.16, height * 0.64, 3, 3);
        gc.setFill(Color.web("#dceff8", 0.6));
        gc.fillRoundRect(-width * 0.20, -height * 0.32, width * 0.22, height * 0.64, 3, 3);
        drawWheels(gc, width, height, 4);
        drawHeadTailLights(gc, width, height);
    }

    private void renderBus(GraphicsContext gc, double width, double height) {
        renderCar(gc, width, height, Color.web("#2ca25f"));
        gc.setFill(Color.web("#f2c94c"));
        gc.fillRect(-width * 0.44, -1, width * 0.82, 2);
        gc.setFill(Color.web("#d8f5e4", 0.9));
        int count = Math.max(3, (int) (width / 11));
        for (int i = 0; i < count; i++) {
            double x = -width * 0.35 + i * width * 0.7 / count;
            gc.fillRoundRect(x, -height * 0.34, width * 0.08, height * 0.24, 2, 2);
            gc.fillRoundRect(x, height * 0.10, width * 0.08, height * 0.24, 2, 2);
        }
    }

    private void renderAmbulance(GraphicsContext gc, boolean emergency, double width, double height) {
        renderCar(gc, width, height, Color.web("#f4f7f7"));
        gc.setFill(Color.web("#d83a34"));
        gc.fillRect(-width * 0.42, -height * 0.08, width * 0.78, height * 0.16);
        gc.fillRect(-width * 0.06, -height * 0.35, width * 0.08, height * 0.25);
        gc.fillRect(-width * 0.13, -height * 0.27, width * 0.22, height * 0.08);
        if (emergency) {
            renderEmergencyFlash(gc, width, height);
        }
    }

    private void renderFireTruck(GraphicsContext gc, double width, double height) {
        renderCar(gc, width, height, Color.web("#d83a34"));
        gc.setStroke(Color.web("#ffd166"));
        gc.setLineWidth(Math.max(1.0, height * 0.08));
        gc.strokeLine(-width * 0.38, -height * 0.30, width * 0.18, -height * 0.30);
        gc.strokeLine(-width * 0.38, height * 0.30, width * 0.18, height * 0.30);
        gc.setFill(Color.web("#f6f7f7", 0.8));
        gc.fillRect(-width * 0.44, -height * 0.08, width * 0.82, height * 0.16);
    }

    private void renderTwoWheeler(GraphicsContext gc, String type, double width, double height) {
        Color body = "bicycle".equals(type) ? Color.web("#2d9c68") : Color.web("#f2994a");
        gc.setStroke(Color.web("#20242a"));
        gc.setLineWidth(Math.max(1.0, height * 0.18));
        gc.strokeOval(-width * 0.48, -height * 0.48, height * 0.95, height * 0.95);
        gc.strokeOval(width * 0.22, -height * 0.48, height * 0.95, height * 0.95);
        gc.setFill(body);
        gc.fillRoundRect(-width * 0.24, -height * 0.34, width * 0.48, height * 0.68, 6, 6);
        gc.setStroke(Color.web("#7f8c8d"));
        gc.setLineWidth(Math.max(1.0, height * 0.12));
        gc.strokeLine(width * 0.18, 0, width * 0.46, -height * 0.45);
    }

    private void drawWheels(GraphicsContext gc, double width, double height, int wheelCount) {
        gc.setFill(Color.web("#15181c"));
        double wheelWidth = Math.max(4, width * 0.16);
        double wheelHeight = Math.max(3, height * 0.26);
        double leftX = -width / 2.0 + width * 0.14;
        double rightX = width / 2.0 - width * 0.24;
        gc.fillRoundRect(leftX, -height / 2.0 - wheelHeight * 0.35, wheelWidth, wheelHeight, 3, 3);
        gc.fillRoundRect(leftX, height / 2.0 - wheelHeight * 0.65, wheelWidth, wheelHeight, 3, 3);
        gc.fillRoundRect(rightX, -height / 2.0 - wheelHeight * 0.35, wheelWidth, wheelHeight, 3, 3);
        gc.fillRoundRect(rightX, height / 2.0 - wheelHeight * 0.65, wheelWidth, wheelHeight, 3, 3);

        if (wheelCount >= 6) {
            gc.fillRoundRect(-wheelWidth / 2.0, -height / 2.0 - wheelHeight * 0.35, wheelWidth, wheelHeight, 3, 3);
            gc.fillRoundRect(-wheelWidth / 2.0, height / 2.0 - wheelHeight * 0.65, wheelWidth, wheelHeight, 3, 3);
        }
    }

    private void drawHeadTailLights(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.web("#fff0a8"));
        gc.fillOval(width / 2.0 - 3, -height * 0.32, 4, 4);
        gc.fillOval(width / 2.0 - 3, height * 0.16, 4, 4);
        gc.setFill(Color.web("#a51e22"));
        gc.fillOval(-width / 2.0, -height * 0.32, 3, 3);
        gc.fillOval(-width / 2.0, height * 0.20, 3, 3);
    }

    private void renderEmergencyFlash(GraphicsContext gc, double width, double height) {
        boolean leftOn = animationFrame % 8 < 4;
        gc.setEffect(new Glow(0.75));
        gc.setFill(leftOn ? Color.RED : Color.color(1, 0, 0, 0.18));
        gc.fillOval(-width * 0.38, -height / 2.0 - 4, 7, 7);
        gc.setFill(!leftOn ? Color.DODGERBLUE : Color.color(0, 0.35, 1, 0.18));
        gc.fillOval(width * 0.28, -height / 2.0 - 4, 7, 7);
        gc.setEffect(null);
    }

    private void drawVehicleId(GraphicsContext gc, Vehicle vehicle, Point2D p, double height) {
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.BOTTOM);
        gc.setFill(Color.color(1, 1, 1, 0.85));
        gc.fillText(vehicle.getId(), p.getX(), p.getY() - height / 2.0 - 4);
    }

    private String vehicleType(Vehicle vehicle) {
        String simpleName = vehicle.getClass().getSimpleName().toLowerCase();
        String id = vehicle.getId() == null ? "" : vehicle.getId().toLowerCase();
        String value = simpleName + " " + id;
        if (value.contains("fire")) {
            return "firetruck";
        }
        if (value.contains("ambulance") || value.contains("ambu") || value.contains("amb")) {
            return "ambulance";
        }
        if (value.contains("bus")) {
            return "bus";
        }
        if (value.contains("motor") || value.contains("moto")) {
            return "motorbike";
        }
        if (value.contains("bike") || value.contains("bicycle")) {
            return "bicycle";
        }
        return "car";
    }

    private Color colorFor(String type, boolean priority) {
        if ("ambulance".equals(type)) {
            return priority ? Color.web("#f6f7f7") : Color.web("#f5b6c8");
        }
        if ("firetruck".equals(type)) {
            return Color.web("#d83a34");
        }
        if ("bus".equals(type)) {
            return Color.web("#2ca25f");
        }
        if ("motorbike".equals(type)) {
            return Color.web("#f2994a");
        }
        if ("bicycle".equals(type)) {
            return Color.web("#2d9c68");
        }
        return Color.web("#2f80ed");
    }

    private Color textColor(String type) {
        if ("ambulance".equals(type)) {
            return Color.web("#9b1c1f");
        }
        return Color.WHITE;
    }

    private String labelFor(String type) {
        return switch (type) {
            case "ambulance" -> "Ambu";
            case "firetruck" -> "Fire";
            case "bus" -> "Bus";
            case "motorbike" -> "Moto";
            case "bicycle" -> "Bike";
            default -> "Car";
        };
    }
}
