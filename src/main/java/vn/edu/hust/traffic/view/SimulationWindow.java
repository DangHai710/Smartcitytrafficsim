package vn.edu.hust.traffic.view;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.vehicle.*;
import vn.edu.hust.traffic.controller.IntersectionPhaseController;

import java.util.List;

import vn.edu.hust.traffic.controller.TrafficController;

public class SimulationWindow extends Application {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private Canvas canvas;
    private GraphicsContext gc;

    private TrafficController controller;
    private VehicleRenderer vehicleRenderer;

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        Pane root = new Pane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case C: controller.spawnVehicleManually("Car"); break;
                case B: controller.spawnVehicleManually("Bus"); break;
                case M: controller.spawnVehicleManually("Motorbike"); break;
                case E: controller.spawnVehicleManually("Emergency"); break;
                case A: controller.spawnVehicleManually("Ambulance"); break;
                case F: controller.spawnVehicleManually("FireTruck"); break;
                case P: controller.toggleAutoSpawn(); break;
                default: break;
            }
        });

        setupSimulation();

        primaryStage.setTitle("Traffic Simulation - 6 Lanes Two-Way");
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
        vehicleRenderer = new VehicleRenderer();
    }

    private void update(double dt) {
        controller.update(dt);
    }

    private void render() {
        gc.clearRect(0, 0, WIDTH, HEIGHT);
        List<TrafficLight> lights = controller.getLights();
        List<Vehicle> vehicles = controller.getVehicles();
        IntersectionPhaseController phase = controller.getPhaseController();

        // 1. Vẽ đường ngang (Rộng 160px)
        gc.setFill(javafx.scene.paint.Color.web("#333333"));
        gc.fillRect(0, HEIGHT / 2.0 - 80, WIDTH, 160);
        
        // Vạch trắng phân làn ngang
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(15);
        gc.strokeLine(0, HEIGHT/2.0 - 53, WIDTH, HEIGHT/2.0 - 53);
        gc.strokeLine(0, HEIGHT/2.0 - 27, WIDTH, HEIGHT/2.0 - 27);
        gc.strokeLine(0, HEIGHT/2.0 + 27, WIDTH, HEIGHT/2.0 + 27);
        gc.strokeLine(0, HEIGHT/2.0 + 53, WIDTH, HEIGHT/2.0 + 53);
        
        // Dải phân cách vàng đôi ngang
        gc.setLineDashes(0);
        gc.setStroke(javafx.scene.paint.Color.YELLOW);
        gc.setLineWidth(2);
        gc.strokeLine(0, HEIGHT/2.0 - 2, WIDTH, HEIGHT/2.0 - 2);
        gc.strokeLine(0, HEIGHT/2.0 + 2, WIDTH, HEIGHT/2.0 + 2);

        // 2. Vẽ đường dọc (Rộng 160px)
        gc.setFill(javafx.scene.paint.Color.web("#333333"));
        gc.fillRect(WIDTH / 2.0 - 80, 0, 160, HEIGHT);
        
        // Vạch trắng phân làn dọc
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(15);
        gc.strokeLine(WIDTH/2.0 - 53, 0, WIDTH/2.0 - 53, HEIGHT);
        gc.strokeLine(WIDTH/2.0 - 27, 0, WIDTH/2.0 - 27, HEIGHT);
        gc.strokeLine(WIDTH/2.0 + 27, 0, WIDTH/2.0 + 27, HEIGHT);
        gc.strokeLine(WIDTH/2.0 + 53, 0, WIDTH/2.0 + 53, HEIGHT);
        
        // Dải phân cách vàng đôi dọc
        gc.setLineDashes(0);
        gc.setStroke(javafx.scene.paint.Color.YELLOW);
        gc.strokeLine(WIDTH/2.0 - 2, 0, WIDTH/2.0 - 2, HEIGHT);
        gc.strokeLine(WIDTH/2.0 + 2, 0, WIDTH/2.0 + 2, HEIGHT);

        // 3. Vạch dừng (Stop Lines)
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(4);
        gc.strokeLine(WIDTH/2.0 - 100, HEIGHT/2.0 + 5, WIDTH/2.0 - 100, HEIGHT/2.0 + 75); // Hướng LTR
        gc.strokeLine(WIDTH/2.0 + 100, HEIGHT/2.0 - 5, WIDTH/2.0 + 100, HEIGHT/2.0 - 75); // Hướng RTL
        gc.strokeLine(WIDTH/2.0 - 75, HEIGHT/2.0 - 100, WIDTH/2.0 - 5, HEIGHT/2.0 - 100); // Hướng TTB
        gc.strokeLine(WIDTH/2.0 + 75, HEIGHT/2.0 + 100, WIDTH/2.0 + 5, HEIGHT/2.0 + 100); // Hướng BTT

        // 4. Vẽ phương tiện bằng VehicleRenderer (animation + hình vẽ chi tiết)
        vehicleRenderer.tick();
        for (Vehicle v : vehicles) {
            vehicleRenderer.renderVehicle(gc, v);
        }

        // 5. Đèn giao thông — CỤM 3 TÍN HIỆU: [Đèn thẳng] [Mũi tên rẽ trái] [Mũi tên rẽ phải]
        for (int i = 0; i < 4; i++) {
            TrafficLight light = lights.get(i);
            double lx = 0, ly = 0;
            switch(i) {
                case 0: lx = WIDTH/2.0-150; ly = HEIGHT/2.0-205; break;
                case 1: lx = WIDTH/2.0+105; ly = HEIGHT/2.0-205; break;
                case 2: lx = WIDTH/2.0-150; ly = HEIGHT/2.0+115; break;
                case 3: lx = WIDTH/2.0+105; ly = HEIGHT/2.0+115; break;
            }
            
            // === CỤM 1: Đèn đi thẳng (3 đèn Đỏ-Vàng-Xanh) ===
            gc.setFill(javafx.scene.paint.Color.web("#222222"));
            gc.fillRoundRect(lx-3, ly-3, 30, 88, 8, 8);
            
            gc.setFill(light.getState() == TrafficLight.State.RED ? javafx.scene.paint.Color.RED : javafx.scene.paint.Color.web("#440000"));
            gc.fillOval(lx+2, ly+2, 18, 18);
            gc.setFill(light.getState() == TrafficLight.State.YELLOW ? javafx.scene.paint.Color.YELLOW : javafx.scene.paint.Color.web("#444400"));
            gc.fillOval(lx+2, ly+28, 18, 18);
            gc.setFill(light.getState() == TrafficLight.State.GREEN ? javafx.scene.paint.Color.LIME : javafx.scene.paint.Color.web("#004400"));
            gc.fillOval(lx+2, ly+54, 18, 18);
            
            // Nhãn "T" (Thẳng) dưới cụm
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
            gc.setFill(javafx.scene.paint.Color.web("#aaaaaa"));
            gc.fillText("T", lx+8, ly+84);

            // === CỤM 2: Đèn rẽ trái (mũi tên ↰) ===
            double leftX = lx + 32;
            gc.setFill(javafx.scene.paint.Color.web("#1a1a1a"));
            gc.fillRoundRect(leftX-2, ly-3, 26, 52, 6, 6);
            
            // Mũi tên rẽ trái — màu theo state
            TrafficLight.State ltState = light.getLeftTurnState();
            javafx.scene.paint.Color ltColor;
            if (ltState == TrafficLight.State.GREEN) ltColor = javafx.scene.paint.Color.LIME;
            else if (ltState == TrafficLight.State.YELLOW) ltColor = javafx.scene.paint.Color.YELLOW;
            else ltColor = javafx.scene.paint.Color.web("#550000");
            
            gc.setFill(ltColor);
            gc.setFont(Font.font("System", FontWeight.BOLD, 22));
            gc.fillText("↰", leftX + 1, ly + 24);
            
            // Countdown rẽ trái
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.fillText(String.valueOf(light.getLeftTurnTimeLeft()), leftX+4, ly+46);

            // === CỤM 3: Đèn rẽ phải (luôn xanh) ===
            double rightX = lx + 32;
            double rightY = ly + 54;
            gc.setFill(javafx.scene.paint.Color.web("#1a1a1a"));
            gc.fillRoundRect(rightX-2, rightY-3, 26, 34, 6, 6);
            
            gc.setFill(javafx.scene.paint.Color.LIME);
            gc.setFont(Font.font("System", FontWeight.BOLD, 18));
            gc.fillText("↱", rightX + 2, rightY + 18);
            gc.setFont(Font.font("Consolas", 7));
            gc.setFill(javafx.scene.paint.Color.web("#00ff00", 0.8));
            gc.fillText("R", rightX + 8, rightY + 28);

            // === Countdown đèn thẳng ===
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.fillText(String.valueOf(light.getTimeLeft()), lx+5, ly+100);
        }

        // 6. Phase HUD — 12 phase (Kịch bản B)
        String[] phaseLabels = {
            "LTR Xanh (Thang+Trai)",   // 0
            "LTR Trai Vang",           // 1
            "Ngang 2 chieu Thang",     // 2
            "LTR Thang Vang",          // 3
            "RTL Xanh (Thang+Trai)",   // 4
            "RTL Vang",                // 5
            "TTB Xanh (Thang+Trai)",   // 6
            "TTB Trai Vang",           // 7
            "Doc 2 chieu Thang",       // 8
            "TTB Thang Vang",          // 9
            "BTT Xanh (Thang+Trai)",   // 10
            "BTT Vang"                 // 11
        };
        javafx.scene.paint.Color[] phaseColors = {
            javafx.scene.paint.Color.LIMEGREEN,
            javafx.scene.paint.Color.YELLOW,
            javafx.scene.paint.Color.LIMEGREEN,
            javafx.scene.paint.Color.YELLOW,
            javafx.scene.paint.Color.LIMEGREEN,
            javafx.scene.paint.Color.YELLOW,
            javafx.scene.paint.Color.CYAN,
            javafx.scene.paint.Color.YELLOW,
            javafx.scene.paint.Color.CYAN,
            javafx.scene.paint.Color.YELLOW,
            javafx.scene.paint.Color.CYAN,
            javafx.scene.paint.Color.YELLOW
        };
        int currentPhase = phase.getCurrentPhase();
        int phaseTimeLeft = (int) Math.ceil(phase.getPhaseTimeLeft());

        gc.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.65));
        gc.fillRoundRect(8, 8, 310, 65, 10, 10);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.setFill(phaseColors[currentPhase]);
        gc.fillText("P" + currentPhase + ": " + phaseLabels[currentPhase], 16, 28);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText("Con lai: " + phaseTimeLeft + "s  |  Xe: " + vehicles.size() + "  |  Re phai: LUON", 16, 46);
        gc.setFont(Font.font("Consolas", 9));
        gc.setFill(javafx.scene.paint.Color.web("#888888"));
        gc.fillText("Kich ban B: Lech gio (Xanh som / Do muon)", 16, 60);

        // 7. Chú thích xe (Legend) — góc phải dưới
        renderLegend(gc);
    }

    /**
     * Vẽ bảng chú thích loại xe ở góc phải dưới màn hình.
     */
    private void renderLegend(GraphicsContext gc) {
        double lx = WIDTH - 160;
        double ly = HEIGHT - 130;

        gc.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.55));
        gc.fillRoundRect(lx - 8, ly - 8, 160, 125, 8, 8);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText("== CHU THICH ==", lx, ly + 8);

        String[][] items = {
            {"#3498db", "Car (C)"},
            {"#27ae60", "Bus (B)"},
            {"#e67e22", "Motorbike (M)"},
            {"#ecf0f1", "Ambulance (A/E)"},
            {"#e74c3c", "FireTruck (F)"}
        };

        for (int i = 0; i < items.length; i++) {
            double iy = ly + 22 + i * 18;
            gc.setFill(javafx.scene.paint.Color.web(items[i][0]));
            gc.fillRoundRect(lx, iy, 14, 10, 3, 3);
            gc.setStroke(javafx.scene.paint.Color.web("#555555"));
            gc.setLineWidth(0.5);
            gc.strokeRoundRect(lx, iy, 14, 10, 3, 3);
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setFont(Font.font("Consolas", 10));
            gc.fillText(items[i][1], lx + 18, iy + 9);
        }
    }

    public static void main(String[] args) { launch(args); }
}

