package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.vehicle.Vehicle;
import vn.edu.hust.traffic.model.map.CrossIntersection;
import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.vehicle.Car;
import vn.edu.hust.traffic.model.vehicle.Motorbike;
import vn.edu.hust.traffic.model.vehicle.Bus;
import vn.edu.hust.traffic.model.vehicle.Ambulance;
import vn.edu.hust.traffic.model.vehicle.FireTruck;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bộ điều khiển giao thông 
 * Quản lý danh sách các phương tiện và cập nhật mô phỏng.
 */
public class TrafficController {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    
    // Khoảng cách từ tâm đường đến tâm các làn (3 làn mỗi bên)
    private static final double LANE_PRIORITY = 15;
    private static final double LANE_CAR = 40;
    private static final double LANE_BIKE = 65;
    
    private double[] spawnTimers = new double[] {0, 0, 0, 0};
    private int vehicleCount = 0;
    private CrossIntersection intersection;
    private List<TrafficLight> lights;
    private List<Vehicle> vehicles;
    private IntersectionPhaseController phaseController;
    private Random random = new Random();
    private boolean autoSpawnEnabled = true;

    public TrafficController() {
        setupSimulation();
    }

    private void setupSimulation() {
        lights = new ArrayList<>();
        // 0, 1: Hướng Ngang | 2, 3: Hướng Dọc
        // Trạng thái ban đầu sẽ do phaseController khởi tạo
        lights.add(new TrafficLight());
        lights.add(new TrafficLight());
        lights.add(new TrafficLight());
        lights.add(new TrafficLight());

        // IntersectionPhaseController đồng bộ 4 đèn — hướng ngang xanh trước
        phaseController = new IntersectionPhaseController(lights);
        
        intersection = new CrossIntersection("cross1", WIDTH / 2.0, HEIGHT / 2.0, lights);
        vehicles = new ArrayList<>();
    }

    public void update(double dt) {
        // Cập nhật phase đèn (đồng bộ hóa cả 4 đèn) với danh sách xe để tính toán phase động
        phaseController.update(dt, vehicles);

        // Sinh xe mới mỗi 5 giây (nếu auto spawn được bật)
        if (autoSpawnEnabled) {
            for (int i = 0; i < 4; i++) {
                spawnTimers[i] += dt;
                if (spawnTimers[i] >= 5.0) {
                    spawnTimers[i] = 0;
                    spawnVehicle(i);
                }
            }
        }

        for (Vehicle v : vehicles) {
            v.update(dt, vehicles, lights, WIDTH, HEIGHT);
        }
        
        // Trình dọn dẹp bộ nhớ: Huỷ ngay những phương tiện đã khuất lấp khỏi màn hình để bảo vệ Heap tĩnh
        vehicles.removeIf(v -> v.getX() < -200 || v.getX() > WIDTH + 200 || 
                               v.getY() < -200 || v.getY() > HEIGHT + 200);
                               
        intersection.update();
    }

    private void spawnVehicle(int directionIdx) {
        vehicleCount++;
        double speed = 60 + random.nextInt(40);
        double x = 0, y = 0, dir = 0;
        
        int type = random.nextInt(100);
        Vehicle v;
        
        int turnRand = random.nextInt(10);
        int turnIntention = 0;
        if (turnRand < 2) turnIntention = 1; // 20% rẽ trái
        else if (turnRand < 4) turnIntention = 2; // 20% rẽ phải

        double offset = 0;
        if (turnIntention == 1) offset = LANE_PRIORITY; // Sát tim đường để rẽ trái
        else if (turnIntention == 2) offset = LANE_BIKE; // Sát vỉa hè để rẽ phải
        else offset = LANE_CAR; // Đi thẳng ở làn giữa 

        switch (directionIdx) {
            case 0: // Trái -> Phải (Phía dưới tâm đường y > 300)
                x = -50; y = HEIGHT / 2.0 + offset; dir = 0; break;
            case 1: // Phải -> Trái (Phía trên tâm đường y < 300)
                x = WIDTH + 50; y = HEIGHT / 2.0 - offset; dir = Math.PI; break;
            case 2: // Trên -> Dưới (Phía bên trái tâm đường x < 400)
                x = WIDTH / 2.0 - offset; y = -50; dir = Math.PI / 2; break;
            case 3: // Dưới -> Trên (Phía bên phải tâm đường x > 400)
                x = WIDTH / 2.0 + offset; y = HEIGHT + 50; dir = -Math.PI / 2; break;
        }

        if (type < 5) {
            // FireTruck: always priority vehicle
            v = new FireTruck("Fire" + vehicleCount, x, y, speed, dir);
        } else if (type < 10) {
            boolean isEmergency = random.nextBoolean();
            v = new Ambulance("Amb" + vehicleCount, x, y, speed, dir, isEmergency);
        } else if (type < 20) {
            v = new Bus("Bus" + vehicleCount, x, y, speed * 0.7, dir); // Bus chạy chậm hơn từ đầu
        } else if (type < 50) {
            v = new Car("Car" + vehicleCount, x, y, speed, dir, 26, 13, false);
        } else {
            v = new Motorbike("Bike" + vehicleCount, x, y, speed, dir, false);
        }
        
        v.setTurnIntention(turnIntention);

        vehicles.add(v);
    }

    public void spawnVehicleManually(String typeStr) {
        int dirIdx = random.nextInt(4);
        double speed = 60 + random.nextInt(40);
        int turnRand = random.nextInt(10);
        int turnIntention = 0;
        if (turnRand < 2) turnIntention = 1;
        else if (turnRand < 4) turnIntention = 2;

        double offset = 0;
        if (turnIntention == 1) offset = LANE_PRIORITY;
        else if (turnIntention == 2) offset = LANE_BIKE;
        else offset = LANE_CAR;

        double x = 0, y = 0, dir = 0;
        switch (dirIdx) {
            case 0: x = -50; y = HEIGHT / 2.0 + offset; dir = 0; break;
            case 1: x = WIDTH + 50; y = HEIGHT / 2.0 - offset; dir = Math.PI; break;
            case 2: x = WIDTH / 2.0 - offset; y = -50; dir = Math.PI / 2; break;
            case 3: x = WIDTH / 2.0 + offset; y = HEIGHT + 50; dir = -Math.PI / 2; break;
        }

        vehicleCount++;
        Vehicle v = null;
        switch (typeStr) {
            case "Emergency": v = new Ambulance("Amb" + vehicleCount, x, y, speed, dir, true); break;
            case "Ambulance": v = new Ambulance("Amb" + vehicleCount, x, y, speed, dir, false); break;
            case "FireTruck": v = new FireTruck("Fire" + vehicleCount, x, y, speed, dir); break;
            case "Bus": v = new Bus("Bus" + vehicleCount, x, y, speed * 0.7, dir); break; // Bus chậm hơn
            case "Car": v = new Car("Car" + vehicleCount, x, y, speed, dir, 26, 13, false); break;
            case "Motorbike": v = new Motorbike("Bike" + vehicleCount, x, y, speed, dir, false); break;
        }
        
        if (v != null) {
            v.setTurnIntention(turnIntention);
            vehicles.add(v);
        }
    }

    public void toggleAutoSpawn() {
        autoSpawnEnabled = !autoSpawnEnabled;
    }

    public boolean isAutoSpawnEnabled() { return autoSpawnEnabled; }

    public List<Vehicle> getVehicles() { return vehicles; }
    public List<TrafficLight> getLights() { return lights; }
    public CrossIntersection getIntersection() { return intersection; }
    public IntersectionPhaseController getPhaseController() { return phaseController; }
}
