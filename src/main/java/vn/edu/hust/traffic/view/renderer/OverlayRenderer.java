package vn.edu.hust.traffic.view.renderer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import vn.edu.hust.traffic.view.ControlMode;
import vn.edu.hust.traffic.view.SimulationSnapshot;
import vn.edu.hust.traffic.view.SimulationViewSettings;

public class OverlayRenderer {
    public void render(GraphicsContext gc, SimulationSnapshot snapshot, SimulationViewSettings settings) {
        drawStatus(gc, snapshot, settings);
        drawLegend(gc);
    }

    private void drawStatus(GraphicsContext gc, SimulationSnapshot snapshot, SimulationViewSettings settings) {
        double x = 0;
        double y = 12;
        double width = 430;
        double height = snapshot.getPhaseIndex() >= 0 ? 92 : 74;
        double textX = x + 150;

        gc.setFill(Color.color(0.05, 0.06, 0.07, 0.82));
        gc.fillRoundRect(x, y, width, height, 8, 8);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        gc.setFill(Color.WHITE);
        gc.fillText("Smart City Traffic Simulation", textX, y + 20);

        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#d7dde2"));
        gc.fillText("Ban do: " + settings.getMapType()
                + " | Hien thi: " + settings.getRenderMode()
                + " | Xe: " + snapshot.getVehicles().size(), textX, y + 40);
        gc.fillText("Dieu khien: " + settings.getControlMode()
                + " | Toc do: " + String.format("%.1fx", settings.getSimulationSpeed())
                + " | Luu luong: " + densityLabel(settings.getTrafficDensity()), textX, y + 58);

        if (snapshot.getPhaseIndex() >= 0) {
            gc.setFill(Color.web("#91e3a7"));
            gc.fillText("Phase: " + snapshot.getPhaseIndex()
                    + " | Con lai: " + (int) Math.ceil(snapshot.getPhaseTimeLeft()) + "s", textX, y + 76);
        }

        if (settings.getControlMode() == ControlMode.MANUAL) {
            gc.setFill(Color.web("#ffd166"));
            gc.fillText("Manual: click truc tiep vao den de doi mau", textX, y + height - 8);
        }
    }

    private void drawLegend(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        double x = width - 166;
        double y = height - 134;

        gc.setFill(Color.color(0.05, 0.06, 0.07, 0.68));
        gc.fillRoundRect(x, y, 150, 120, 8, 8);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.setFill(Color.WHITE);
        double legendTextX = x + 24;
        gc.fillText("Vehicle", legendTextX, y + 18);

        drawLegendItem(gc, legendTextX, y + 36, Color.web("#2f80ed"), "Car");
        drawLegendItem(gc, legendTextX, y + 54, Color.web("#f2994a"), "Motorbike");
        drawLegendItem(gc, legendTextX, y + 72, Color.web("#2d9c68"), "Bicycle");
        drawLegendItem(gc, legendTextX, y + 90, Color.web("#f6f7f7"), "Ambulance");
        drawLegendItem(gc, legendTextX, y + 108, Color.web("#d83a34"), "FireTruck");
    }

    private void drawLegendItem(GraphicsContext gc, double x, double y, Color color, String label) {
        gc.setFill(color);
        gc.fillRoundRect(x, y - 9, 18, 11, 3, 3);
        gc.setStroke(Color.web("#2e3338"));
        gc.strokeRoundRect(x, y - 9, 18, 11, 3, 3);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        gc.fillText(label, x + 26, y);
    }

    private String densityLabel(int density) {
        return switch (density) {
            case 1 -> "Th\u1ea5p";
            case 3 -> "Cao";
            default -> "Trung b\u00ecnh";
        };
    }
}
