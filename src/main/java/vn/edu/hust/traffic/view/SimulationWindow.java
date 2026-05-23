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
import vn.edu.hust.traffic.controller.ThreeWayPhaseController;

import java.util.List;

import vn.edu.hust.traffic.controller.TrafficController;

public class SimulationWindow extends Application {
    private static final int WIDTH = 1400;
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
        List<TrafficLight> lights1 = controller.getLights1();
        List<TrafficLight> lights2 = controller.getLights2();
        List<Vehicle> vehicles = controller.getVehicles();
        IntersectionPhaseController phase1 = controller.getPhaseController1();
        ThreeWayPhaseController phase2 = controller.getPhaseController2();

        // 1. Vẽ mặt đường chính
        gc.setFill(javafx.scene.paint.Color.web("#333333"));
        gc.fillRect(0, HEIGHT / 2.0 - 80, WIDTH, 160);
        
        double cx1 = 400.0;
        gc.fillRect(cx1 - 80, 0, 160, HEIGHT);
        
        double cx2 = 1000.0;
        gc.fillRect(cx2 - 80, HEIGHT / 2.0, 160, HEIGHT / 2.0); // Chỉ có nhánh dưới

        double hy = HEIGHT / 2.0;

        // Vẽ các góc bo/đảo giao thông cho Ngã 4
        drawIntersectionCorner(gc, cx1, hy, -1, -1); // Top-Left
        drawIntersectionCorner(gc, cx1, hy, 1, -1);  // Top-Right
        drawIntersectionCorner(gc, cx1, hy, -1, 1);  // Bottom-Left
        drawIntersectionCorner(gc, cx1, hy, 1, 1);   // Bottom-Right
        
        // Vẽ các góc bo/đảo giao thông cho Ngã 3
        drawIntersectionCorner(gc, cx2, hy, -1, 1);  // Bottom-Left
        drawIntersectionCorner(gc, cx2, hy, 1, 1);   // Bottom-Right

        // Hàm helper để vẽ các đoạn
        double[] hSegs = {0, cx1-100, cx1+100, cx2-100, cx2+100, WIDTH};
        double[] vSegs1 = {0, hy-100, hy+100, HEIGHT};
        double[] vSegs2 = {hy+100, HEIGHT};

        // --- VẠCH NGANG ---
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(15);
        for (int i=0; i<hSegs.length; i+=2) {
            gc.strokeLine(hSegs[i], hy - 53, hSegs[i+1], hy - 53);
            gc.strokeLine(hSegs[i], hy - 27, hSegs[i+1], hy - 27);
            gc.strokeLine(hSegs[i], hy + 27, hSegs[i+1], hy + 27);
            gc.strokeLine(hSegs[i], hy + 53, hSegs[i+1], hy + 53);
        }
        
        // Dải phân cách vàng ngang
        gc.setLineDashes(0);
        gc.setStroke(javafx.scene.paint.Color.YELLOW);
        gc.setLineWidth(2);
        for (int i=0; i<hSegs.length; i+=2) {
            gc.strokeLine(hSegs[i], hy - 2, hSegs[i+1], hy - 2);
            gc.strokeLine(hSegs[i], hy + 2, hSegs[i+1], hy + 2);
        }

        // --- VẠCH DỌC NGÃ 4 ---
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(15);
        for (int i=0; i<vSegs1.length; i+=2) {
            gc.strokeLine(cx1 - 53, vSegs1[i], cx1 - 53, vSegs1[i+1]);
            gc.strokeLine(cx1 - 27, vSegs1[i], cx1 - 27, vSegs1[i+1]);
            gc.strokeLine(cx1 + 27, vSegs1[i], cx1 + 27, vSegs1[i+1]);
            gc.strokeLine(cx1 + 53, vSegs1[i], cx1 + 53, vSegs1[i+1]);
        }
        
        // Dải phân cách vàng dọc ngã 4
        gc.setLineDashes(0);
        gc.setStroke(javafx.scene.paint.Color.YELLOW);
        for (int i=0; i<vSegs1.length; i+=2) {
            gc.strokeLine(cx1 - 2, vSegs1[i], cx1 - 2, vSegs1[i+1]);
            gc.strokeLine(cx1 + 2, vSegs1[i], cx1 + 2, vSegs1[i+1]);
        }

        // --- VẠCH DỌC NGÃ 3 ---
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(15);
        for (int i=0; i<vSegs2.length; i+=2) {
            gc.strokeLine(cx2 - 53, vSegs2[i], cx2 - 53, vSegs2[i+1]);
            gc.strokeLine(cx2 - 27, vSegs2[i], cx2 - 27, vSegs2[i+1]);
            gc.strokeLine(cx2 + 27, vSegs2[i], cx2 + 27, vSegs2[i+1]);
            gc.strokeLine(cx2 + 53, vSegs2[i], cx2 + 53, vSegs2[i+1]);
        }
        
        // Dải phân cách vàng dọc ngã 3
        gc.setLineDashes(0);
        gc.setStroke(javafx.scene.paint.Color.YELLOW);
        for (int i=0; i<vSegs2.length; i+=2) {
            gc.strokeLine(cx2 - 2, vSegs2[i], cx2 - 2, vSegs2[i+1]);
            gc.strokeLine(cx2 + 2, vSegs2[i], cx2 + 2, vSegs2[i+1]);
        }
        
        // --- VẠCH NÉT ĐỨT DẪN ĐƯỜNG RẼ (GUIDING LINES) ---
        gc.setStroke(javafx.scene.paint.Color.web("#888888"));
        gc.setLineWidth(1.5);
        gc.setLineDashes(6);
        
        // Ngã 4
        // LTR -> North
        gc.beginPath();
        gc.moveTo(cx1 - 100, hy + 15);
        gc.quadraticCurveTo(cx1 + 15, hy + 15, cx1 + 15, hy - 100);
        gc.stroke();
        
        // RTL -> South
        gc.beginPath();
        gc.moveTo(cx1 + 100, hy - 15);
        gc.quadraticCurveTo(cx1 - 15, hy - 15, cx1 - 15, hy + 100);
        gc.stroke();
        
        // BTT -> West
        gc.beginPath();
        gc.moveTo(cx1 + 15, hy + 100);
        gc.quadraticCurveTo(cx1 + 15, hy - 15, cx1 - 100, hy - 15);
        gc.stroke();
        
        // TTB -> East
        gc.beginPath();
        gc.moveTo(cx1 - 15, hy - 100);
        gc.quadraticCurveTo(cx1 - 15, hy + 15, cx1 + 100, hy + 15);
        gc.stroke();
        
        // Ngã 3 (Chỉ có BTT -> West)
        gc.beginPath();
        gc.moveTo(cx2 + 15, hy + 100);
        gc.quadraticCurveTo(cx2 + 15, hy - 15, cx2 - 100, hy - 15);
        gc.stroke();

        // --- MŨI TÊN CHỈ HƯỚNG ---
        // Vẽ bằng vector trong drawLanesArrows
        drawLanesArrows(gc, cx1 - 100, hy, 0, "left", "straight", "right");   // LTR Ngã 4
        drawLanesArrows(gc, cx1 + 100, hy, 180, "left", "straight", "right"); // RTL Ngã 4
        drawLanesArrows(gc, cx1, hy - 100, 90, "left", "straight", "right");  // TTB Ngã 4
        drawLanesArrows(gc, cx1, hy + 100, 270, "left", "straight", "right"); // BTT Ngã 4
        
        drawLanesArrows(gc, cx2 - 100, hy, 0, "straight", "straight", "right");   // LTR Ngã 3
        drawLanesArrows(gc, cx2 + 100, hy, 180, "left", "straight", "straight"); // RTL Ngã 3
        drawLanesArrows(gc, cx2, hy + 100, 270, "left", "left_right", "right"); // BTT Ngã 3

        gc.setLineDashes(0);

        // 3. Vạch dừng (Stop Lines) - Ngã 4
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(4);
        gc.strokeLine(cx1 - 100, HEIGHT/2.0 + 5, cx1 - 100, HEIGHT/2.0 + 75); // Hướng LTR
        gc.strokeLine(cx1 + 100, HEIGHT/2.0 - 5, cx1 + 100, HEIGHT/2.0 - 75); // Hướng RTL
        gc.strokeLine(cx1 - 75, HEIGHT/2.0 - 100, cx1 - 5, HEIGHT/2.0 - 100); // Hướng TTB
        gc.strokeLine(cx1 + 75, HEIGHT/2.0 + 100, cx1 + 5, HEIGHT/2.0 + 100); // Hướng BTT
        
        // Vạch dừng (Stop Lines) - Ngã 3
        gc.strokeLine(cx2 - 100, HEIGHT/2.0 + 5, cx2 - 100, HEIGHT/2.0 + 75); // Hướng LTR
        gc.strokeLine(cx2 + 100, HEIGHT/2.0 - 5, cx2 + 100, HEIGHT/2.0 - 75); // Hướng RTL
        gc.strokeLine(cx2 + 75, HEIGHT/2.0 + 100, cx2 + 5, HEIGHT/2.0 + 100); // Hướng BTT

        // 3.5 Vạch đi bộ (Zebra Crossings) - Ngã 4
        drawZebraCrossing(gc, cx1 - 95, hy - 75, cx1 - 85, hy + 75, true);   // West
        drawZebraCrossing(gc, cx1 + 85, hy - 75, cx1 + 95, hy + 75, true);   // East
        drawZebraCrossing(gc, cx1 - 75, hy - 95, cx1 + 75, hy - 85, false);  // North
        drawZebraCrossing(gc, cx1 - 75, hy + 85, cx1 + 75, hy + 95, false);  // South
        
        // Vạch đi bộ (Zebra Crossings) - Ngã 3
        drawZebraCrossing(gc, cx2 - 95, hy - 75, cx2 - 85, hy + 75, true);   // West
        drawZebraCrossing(gc, cx2 + 85, hy - 75, cx2 + 95, hy + 75, true);   // East
        drawZebraCrossing(gc, cx2 - 75, hy + 85, cx2 + 75, hy + 95, false);  // South

        // 4. Vẽ phương tiện bằng VehicleRenderer (animation + hình vẽ chi tiết)
        vehicleRenderer.tick();
        for (Vehicle v : vehicles) {
            vehicleRenderer.renderVehicle(gc, v);
        }

        // 5. Đèn giao thông
        // Vẽ đèn cho Ngã 4
        for (int i = 0; i < 4; i++) {
            TrafficLight light = lights1.get(i);
            double lx = 0, ly = 0;
            double cy = HEIGHT / 2.0;
            switch(i) {
                case 0: lx = cx1 - 136; ly = cy - 148; break; // Top-Left Island
                case 1: lx = cx1 + 94;  ly = cy - 148; break; // Top-Right Island
                case 2: lx = cx1 - 136; ly = cy + 82;  break; // Bottom-Left Island
                case 3: lx = cx1 + 94;  ly = cy + 82;  break; // Bottom-Right Island
            }
            drawTrafficLightGroup(gc, light, lx, ly, true, true);
        }
        
        // Vẽ đèn cho Ngã 3
        for (int i = 0; i < 3; i++) {
            TrafficLight light = lights2.get(i);
            double lx = 0, ly = 0;
            double cy = HEIGHT / 2.0;
            switch(i) {
                case 0: lx = cx2 - 136; ly = cy + 82;  break; // LTR: Bottom-Left Island
                case 1: lx = cx2 + 94;  ly = cy - 148; break; // RTL: Top-Right (grass)
                case 2: lx = cx2 + 94;  ly = cy + 82;  break; // BTT: Bottom-Right Island
            }
            if (i == 0) {
                drawTrafficLightGroup(gc, light, lx, ly, true, false);
            } else if (i == 1) {
                drawTrafficLightGroup(gc, light, lx, ly, true, true);
            } else {
                drawTrafficLightGroup(gc, light, lx, ly, false, true);
            }
        }

        // 6. Phase HUD — 12 phase (Ngã 4)
        String[] phaseLabels = {
            "LTR Xanh (Thang+Trai)", "LTR Trai Vang", "Ngang 2 chieu Thang", "LTR Thang Vang",
            "RTL Xanh (Thang+Trai)", "RTL Vang", "TTB Xanh (Thang+Trai)", "TTB Trai Vang",
            "Doc 2 chieu Thang", "TTB Thang Vang", "BTT Xanh (Thang+Trai)", "BTT Vang"
        };
        int currentPhase1 = phase1.getCurrentPhase();
        int phaseTimeLeft1 = (int) Math.ceil(phase1.getPhaseTimeLeft());

        gc.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.65));
        gc.fillRoundRect(8, 8, 310, 65, 10, 10);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.setFill(javafx.scene.paint.Color.LIMEGREEN); // Giản lược màu
        gc.fillText("Nga 4 - P" + currentPhase1 + ": " + phaseLabels[currentPhase1], 16, 28);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText("Con lai: " + phaseTimeLeft1 + "s  |  Xe: " + vehicles.size() + "  |  Re phai: LUON", 16, 46);
        gc.setFont(Font.font("Consolas", 9));
        gc.setFill(javafx.scene.paint.Color.web("#888888"));
        gc.fillText("Kich ban B: Lech gio", 16, 60);

        // 6.5 Phase HUD — 6 phase (Ngã 3)
        String[] phaseLabels2 = {
            "Ngang Xanh", "Ngang Vang", "RTL Trai Xanh", "RTL Trai Vang", "BTT Trai Xanh", "BTT Vang"
        };
        int currentPhase2 = phase2.getCurrentPhase();
        int phaseTimeLeft2 = (int) Math.ceil(phase2.getPhaseTimeLeft());

        gc.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.65));
        gc.fillRoundRect(330, 8, 250, 65, 10, 10);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.setFill(javafx.scene.paint.Color.CYAN);
        gc.fillText("Nga 3 - P" + currentPhase2 + ": " + phaseLabels2[currentPhase2], 338, 28);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText("Con lai: " + phaseTimeLeft2 + "s", 338, 46);

        // 7. Chú thích xe (Legend) — góc phải dưới
        renderLegend(gc);
    }

    private void drawTrafficLightGroup(GraphicsContext gc, TrafficLight light, double lx, double ly, boolean showStraight, boolean showLeft) {
        gc.save();
        gc.translate(lx, ly);
        gc.scale(0.75, 0.75);
        lx = 0;
        ly = 0;

        if (showStraight) {
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
        }

        if (showLeft) {
            // === CỤM 2: Đèn rẽ trái (mũi tên ↰) ===
            double leftX = showStraight ? (lx + 32) : lx;
            gc.setFill(javafx.scene.paint.Color.web("#1a1a1a"));
            gc.fillRoundRect(leftX-2, ly-3, 26, 52, 6, 6);
            
            TrafficLight.State ltState = light.getLeftTurnState();
            javafx.scene.paint.Color ltColor;
            if (ltState == TrafficLight.State.GREEN) ltColor = javafx.scene.paint.Color.LIME;
            else if (ltState == TrafficLight.State.YELLOW) ltColor = javafx.scene.paint.Color.YELLOW;
            else ltColor = javafx.scene.paint.Color.web("#550000");
            
            gc.setFill(ltColor);
            gc.setFont(Font.font("System", FontWeight.BOLD, 22));
            gc.fillText("↰", leftX + 1, ly + 24);
            
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.fillText(String.valueOf(light.getLeftTurnTimeLeft()), leftX+4, ly+46);
        }

        // === CỤM 3: Đèn rẽ phải (luôn xanh) ===
        double rightX = showStraight ? (lx + 32) : lx;
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
        if (showStraight) {
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.fillText(String.valueOf(light.getTimeLeft()), lx+5, ly+100);
        }
        gc.restore();
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

    private void drawLanesArrows(GraphicsContext gc, double x, double y, double rotationDegree, String l1, String l2, String l3) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(rotationDegree);
        
        // Cấu hình nét vẽ cho mũi tên sơn đường (màu trắng, dày)
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(2.0);
        gc.setLineDashes(0);
        
        // Vẽ 3 mũi tên tương ứng 3 làn (ở khoảng cách -25px trước vạch dừng)
        double arrowX = -25; // Lùi lại ngay sát sau vạch dừng đèn đỏ
        
        // 1. Làn trong cùng (ưu tiên): l1 (offset 15)
        drawSingleArrow(gc, arrowX, 15, l1);
        
        // 2. Làn giữa: l2 (offset 40)
        drawSingleArrow(gc, arrowX, 40, l2);
        
        // 3. Làn ngoài cùng: l3 (offset 65)
        drawSingleArrow(gc, arrowX, 65, l3);
        
        gc.restore();
    }

    private void drawSingleArrow(GraphicsContext gc, double x, double y, String type) {
        if ("none".equals(type)) return;
        gc.save();
        gc.translate(x, y);
        
        if ("straight".equals(type)) {
            // Thân mũi tên đi thẳng (hướng +X)
            gc.strokeLine(-10, 0, 10, 0);
            // Đầu mũi tên
            gc.strokeLine(10, 0, 5, -4);
            gc.strokeLine(10, 0, 5, 4);
        } else if ("left".equals(type)) {
            // Thân đi thẳng rồi rẽ trái (hướng -Y)
            gc.strokeLine(-10, 0, 3, 0);
            gc.strokeLine(3, 0, 3, -10);
            // Đầu mũi tên rẽ trái
            gc.strokeLine(3, -10, 0, -7);
            gc.strokeLine(3, -10, 6, -7);
        } else if ("right".equals(type)) {
            // Thân đi thẳng rồi rẽ phải (hướng +Y)
            gc.strokeLine(-10, 0, 3, 0);
            gc.strokeLine(3, 0, 3, 10);
            // Đầu mũi tên rẽ phải
            gc.strokeLine(3, 10, 0, 7);
            gc.strokeLine(3, 10, 6, 7);
        } else if ("left_right".equals(type)) {
            // Thân đi thẳng rồi phân nhánh rẽ cả trái và phải
            gc.strokeLine(-10, 0, 3, 0);
            // Nhánh trái (hướng -Y)
            gc.strokeLine(3, 0, 3, -10);
            gc.strokeLine(3, -10, 0, -7);
            gc.strokeLine(3, -10, 6, -7);
            // Nhánh phải (hướng +Y)
            gc.strokeLine(3, 0, 3, 10);
            gc.strokeLine(3, 10, 0, 7);
            gc.strokeLine(3, 10, 6, 7);
        }
        
        gc.restore();
    }

    private void drawZebraCrossing(GraphicsContext gc, double startX, double startY, double endX, double endY, boolean isHorizontalRoad) {
        gc.save();
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(5);
        gc.setLineDashes(0);
        if (isHorizontalRoad) {
            for (double y = startY; y <= endY; y += 12) {
                gc.strokeLine(startX, y, endX, y);
            }
        } else {
            for (double x = startX; x <= endX; x += 12) {
                gc.strokeLine(x, startY, x, endY);
            }
        }
        gc.restore();
    }

    private void drawIntersectionCorner(GraphicsContext gc, double cx, double cy, double signX, double signY) {
        // 1. Vẽ nền đường rẽ tắt (Flare - vát chéo góc đường)
        gc.setFill(javafx.scene.paint.Color.web("#333333"));
        gc.fillPolygon(
            new double[]{cx + signX*250, cx + signX*80, cx + signX*80},
            new double[]{cy + signY*80, cy + signY*250, cy + signY*80},
            3
        );

        // Vẽ vạch liền mép ngoài đường rẽ tắt
        gc.setStroke(javafx.scene.paint.Color.web("#888888"));
        gc.setLineWidth(2);
        gc.strokeLine(cx + signX*250, cy + signY*80, cx + signX*80, cy + signY*250);

        // 2. Vẽ đảo giao thông (Island) hình tam giác
        // Đảo sẽ nằm giữa làn rẽ phải và các làn đi thẳng, tạo hình một vát chéo
        gc.setFill(javafx.scene.paint.Color.web("#a06050")); // Màu gạch lót đường
        gc.fillPolygon(
            new double[]{cx + signX*186, cx + signX*80, cx + signX*80},
            new double[]{cy + signY*80, cy + signY*186, cy + signY*80},
            3
        );
        // Viền đảo giao thông (bó vỉa)
        gc.setStroke(javafx.scene.paint.Color.web("#dddddd"));
        gc.setLineWidth(2);
        gc.strokePolygon(
            new double[]{cx + signX*186, cx + signX*80, cx + signX*80},
            new double[]{cy + signY*80, cy + signY*186, cy + signY*80},
            3
        );

        // 3. Vẽ vạch đứt phân làn giữa đường rẽ tắt (chạy song song với lề)
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(2);
        gc.setLineDashes(10);
        gc.strokeLine(cx + signX*218, cy + signY*80, cx + signX*80, cy + signY*218);
        gc.setLineDashes(0);
    }

    public static void main(String[] args) { launch(args); }
}

