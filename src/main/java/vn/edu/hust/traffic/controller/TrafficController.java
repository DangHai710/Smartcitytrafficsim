package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.vehicle.Vehicle;
import vn.edu.hust.traffic.model.map.Intersection;
import vn.edu.hust.traffic.model.map.CrossIntersection;
import vn.edu.hust.traffic.model.map.ThreeWayIntersection;
import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.vehicle.Car;
import vn.edu.hust.traffic.model.vehicle.Motorbike;
import vn.edu.hust.traffic.model.vehicle.Bus;
import vn.edu.hust.traffic.model.vehicle.Ambulance;
import vn.edu.hust.traffic.model.vehicle.FireTruck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrafficController {
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 600;
    
    private static final double LANE_PRIORITY = 15;
    private static final double LANE_CAR = 40;
    private static final double LANE_BIKE = 65;
    
    private double[] spawnTimers = new double[] {0, 0, 0, 0, 0};
    private int vehicleCount = 0;
    
    private List<Intersection> intersections;
    private List<TrafficLight> lights1; // Ngã 4
    private List<TrafficLight> lights2; // Ngã 3
    private List<Vehicle> vehicles;
    
    private IntersectionPhaseController phaseController1;
    private ThreeWayPhaseController phaseController2;
    private Random random = new Random();
    private boolean autoSpawnEnabled = true;

    public TrafficController() {
        setupSimulation();
    }

    private void setupSimulation() {
        intersections = new ArrayList<>();
        
        // Ngã 4 tại X=400
        lights1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) lights1.add(new TrafficLight());
        phaseController1 = new IntersectionPhaseController(lights1);
        intersections.add(new CrossIntersection("cross1", 400.0, HEIGHT / 2.0, lights1));
        
        // Ngã 3 tại X=1000
        lights2 = new ArrayList<>();
        for (int i = 0; i < 3; i++) lights2.add(new TrafficLight()); // 0=LTR, 1=RTL, 2=BTT
        phaseController2 = new ThreeWayPhaseController(lights2);
        intersections.add(new ThreeWayIntersection("three1", 1000.0, HEIGHT / 2.0, lights2));
        
        vehicles = new ArrayList<>();
    }

    public void update(double dt) {
        phaseController1.update(dt, vehicles);
        phaseController2.update(dt, vehicles);

        if (autoSpawnEnabled) {
            for (int i = 0; i < 5; i++) {
                spawnTimers[i] += dt;
                if (spawnTimers[i] >= 5.0) {
                    spawnTimers[i] = 0;
                    spawnVehicle(i);
                }
            }
        }

        for (Vehicle v : vehicles) {
            v.update(dt, vehicles, intersections, WIDTH, HEIGHT);
        }
        
        vehicles.removeIf(v -> v.getX() < -200 || v.getX() > WIDTH + 200 || 
                               v.getY() < -200 || v.getY() > HEIGHT + 200);
                               
        for (Intersection inter : intersections) {
            inter.update();
        }
    }

    private void spawnVehicle(int sourceIdx) {
        vehicleCount++;
        double speed = 60 + random.nextInt(40);
        double x = 0, y = 0, dir = 0;
        
        int type = random.nextInt(100);
        Vehicle v;
        
        int turnRand = random.nextInt(10);
        int turnIntention = 0; // Thẳng
        if (turnRand < 2) turnIntention = 1; // Rẽ trái
        else if (turnRand < 4) turnIntention = 2; // Rẽ phải

        // BẢO VỆ NGÃ 3: RTL không rẽ phải lên Bắc (vì không có đường)
        if (sourceIdx == 1 && turnIntention == 2) {
            turnIntention = random.nextBoolean() ? 0 : 1;
        }

        double offset = 0;
        if (turnIntention == 1) offset = LANE_PRIORITY;
        else if (turnIntention == 2) offset = LANE_BIKE;
        else offset = random.nextBoolean() ? LANE_CAR : LANE_BIKE;

        switch (sourceIdx) {
            case 0: // Trái -> Phải (Vào đường ngang)
                x = -50; y = HEIGHT / 2.0 + offset; dir = 0; break;
            case 1: // Phải -> Trái (Vào đường ngang từ X=1450)
                x = WIDTH + 50; y = HEIGHT / 2.0 - offset; dir = Math.PI; break;
            case 2: // Trên -> Dưới (Vào Ngã 4)
                x = 400.0 - offset; y = -50; dir = Math.PI / 2; break;
            case 3: // Dưới -> Trên (Vào Ngã 4)
                x = 400.0 + offset; y = HEIGHT + 50; dir = -Math.PI / 2; break;
            case 4: // Dưới -> Trên (Vào Ngã 3 ở X=1000)
                x = 1000.0 + offset; y = HEIGHT + 50; dir = -Math.PI / 2; 
                if (turnIntention == 0) turnIntention = (random.nextBoolean() ? 1 : 2); // Buộc phải rẽ
                break;
        }

        if (type < 5) v = new FireTruck("Fire" + vehicleCount, x, y, speed, dir);
        else if (type < 10) v = new Ambulance("Amb" + vehicleCount, x, y, speed, dir, random.nextBoolean());
        else if (type < 20) v = new Bus("Bus" + vehicleCount, x, y, speed * 0.7, dir);
        else if (type < 50) v = new Car("Car" + vehicleCount, x, y, speed, dir, 26, 13, false);
        else v = new Motorbike("Bike" + vehicleCount, x, y, speed, dir, false);
        
        v.setTurnIntention(turnIntention);
        vehicles.add(v);
    }

    public void spawnVehicleManually(String typeStr) {
        int dirIdx = random.nextInt(5);
        double speed = 60 + random.nextInt(40);
        int turnRand = random.nextInt(10);
        int turnIntention = 0;
        if (turnRand < 2) turnIntention = 1;
        else if (turnRand < 4) turnIntention = 2;

        // BẢO VỆ NGÃ 3: RTL không rẽ phải lên Bắc
        if (dirIdx == 1 && turnIntention == 2) {
            turnIntention = random.nextBoolean() ? 0 : 1;
        }

        double offset = 0;
        if (turnIntention == 1) offset = LANE_PRIORITY;
        else if (turnIntention == 2) offset = LANE_BIKE;
        else offset = random.nextBoolean() ? LANE_CAR : LANE_BIKE;

        double x = 0, y = 0, dir = 0;
        switch (dirIdx) {
            case 0: x = -50; y = HEIGHT / 2.0 + offset; dir = 0; break;
            case 1: x = WIDTH + 50; y = HEIGHT / 2.0 - offset; dir = Math.PI; break;
            case 2: x = 400.0 - offset; y = -50; dir = Math.PI / 2; break;
            case 3: x = 400.0 + offset; y = HEIGHT + 50; dir = -Math.PI / 2; break;
            case 4: 
                x = 1000.0 + offset; y = HEIGHT + 50; dir = -Math.PI / 2; 
                if (turnIntention == 0) turnIntention = (random.nextBoolean() ? 1 : 2);
                break;
        }

        vehicleCount++;
        Vehicle v = null;
        switch (typeStr) {
            case "Emergency": v = new Ambulance("Amb" + vehicleCount, x, y, speed, dir, true); break;
            case "Ambulance": v = new Ambulance("Amb" + vehicleCount, x, y, speed, dir, false); break;
            case "FireTruck": v = new FireTruck("Fire" + vehicleCount, x, y, speed, dir); break;
            case "Bus": v = new Bus("Bus" + vehicleCount, x, y, speed * 0.7, dir); break;
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
    public List<Intersection> getIntersections() { return intersections; }
    public List<TrafficLight> getLights1() { return lights1; }
    public List<TrafficLight> getLights2() { return lights2; }
    public IntersectionPhaseController getPhaseController1() { return phaseController1; }
    public ThreeWayPhaseController getPhaseController2() { return phaseController2; }
}
