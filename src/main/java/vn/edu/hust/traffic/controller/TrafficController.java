package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.map.CrossIntersection;
import vn.edu.hust.traffic.model.map.Intersection;
import vn.edu.hust.traffic.model.map.ThreeWayIntersection;
import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.vehicle.Ambulance;
import vn.edu.hust.traffic.model.vehicle.Bus;
import vn.edu.hust.traffic.model.vehicle.Car;
import vn.edu.hust.traffic.model.vehicle.FireTruck;
import vn.edu.hust.traffic.model.vehicle.Motorbike;
import vn.edu.hust.traffic.model.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrafficController {
    private static final int NETWORK_WIDTH = 1400;
    private static final int CROSS_WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int THREE_WAY_WORLD_MIN_X = 600;

    private static final double CROSS_X = 400.0;
    private static final double THREE_WAY_X = 1000.0;

    private static final double LANE_PRIORITY = 15;
    private static final double LANE_CAR = 40;
    private static final double LANE_BIKE = 65;

    private final SimulationMode mode;
    private final int worldMinX;
    private final int worldMaxX;

    private final double[] spawnTimers = new double[] { 0, 0, 0, 0, 0 };
    private final Random random = new Random();

    private int vehicleCount = 0;
    private List<Intersection> intersections;
    private List<TrafficLight> lights1;
    private List<TrafficLight> lights2;
    private List<Vehicle> vehicles;

    private IntersectionPhaseController phaseController1;
    private ThreeWayPhaseController phaseController2;
    private boolean autoSpawnEnabled = true;

    public TrafficController() {
        this(SimulationMode.ROAD_NETWORK);
    }

    public TrafficController(SimulationMode mode) {
        this.mode = mode == null ? SimulationMode.ROAD_NETWORK : mode;
        this.worldMinX = this.mode == SimulationMode.THREE_WAY_INTERSECTION ? THREE_WAY_WORLD_MIN_X : 0;
        this.worldMaxX = this.mode == SimulationMode.CROSS_INTERSECTION ? CROSS_WIDTH : NETWORK_WIDTH;
        setupSimulation();
    }

    private void setupSimulation() {
        intersections = new ArrayList<>();
        lights1 = new ArrayList<>();
        lights2 = new ArrayList<>();
        vehicles = new ArrayList<>();
        phaseController1 = null;
        phaseController2 = null;

        if (mode == SimulationMode.CROSS_INTERSECTION || mode == SimulationMode.ROAD_NETWORK) {
            for (int i = 0; i < 4; i++) {
                lights1.add(new TrafficLight());
            }
            phaseController1 = new IntersectionPhaseController(lights1);
            intersections.add(new CrossIntersection("cross1", CROSS_X, HEIGHT / 2.0, lights1));
        }

        if (mode == SimulationMode.THREE_WAY_INTERSECTION || mode == SimulationMode.ROAD_NETWORK) {
            for (int i = 0; i < 3; i++) {
                lights2.add(new TrafficLight());
            }
            phaseController2 = new ThreeWayPhaseController(lights2);
            intersections.add(new ThreeWayIntersection("three1", THREE_WAY_X, HEIGHT / 2.0, lights2));
        }
    }

    public void update(double dt) {
        if (phaseController1 != null) {
            phaseController1.update(dt, vehicles);
        }
        if (phaseController2 != null) {
            phaseController2.update(dt, vehicles);
        }

        if (autoSpawnEnabled) {
            for (int sourceIdx : activeSourceIndices()) {
                spawnTimers[sourceIdx] += dt;
                if (spawnTimers[sourceIdx] >= 5.0) {
                    spawnTimers[sourceIdx] = 0;
                    spawnVehicle(sourceIdx);
                }
            }
        }

        for (Vehicle v : vehicles) {
            v.update(dt, vehicles, intersections, worldMaxX, HEIGHT);
        }

        vehicles.removeIf(v -> v.getX() < worldMinX - 200 || v.getX() > worldMaxX + 200
                || v.getY() < -200 || v.getY() > HEIGHT + 200);

        for (Intersection inter : intersections) {
            inter.update();
        }
    }

    private void spawnVehicle(int sourceIdx) {
        vehicleCount++;
        double speed = 60 + random.nextInt(40);
        SpawnPoint spawnPoint = createSpawnPoint(sourceIdx);
        if (spawnPoint == null) {
            return;
        }

        Vehicle v = randomVehicle(spawnPoint.x, spawnPoint.y, speed, spawnPoint.direction);
        v.setTurnIntention(spawnPoint.turnIntention);
        vehicles.add(v);
    }

    public void spawnVehicleManually(String typeStr) {
        int[] sources = activeSourceIndices();
        int dirIdx = sources[random.nextInt(sources.length)];
        double speed = 60 + random.nextInt(40);
        SpawnPoint spawnPoint = createSpawnPoint(dirIdx);
        if (spawnPoint == null) {
            return;
        }

        vehicleCount++;
        Vehicle v = manualVehicle(typeStr, spawnPoint.x, spawnPoint.y, speed, spawnPoint.direction);
        if (v != null) {
            v.setTurnIntention(spawnPoint.turnIntention);
            vehicles.add(v);
        }
    }

    private SpawnPoint createSpawnPoint(int sourceIdx) {
        int turnIntention = randomTurnIntention(sourceIdx);
        double offset = laneOffset(turnIntention);
        double x;
        double y;
        double direction;

        switch (sourceIdx) {
            case 0:
                x = worldMinX - 50;
                y = HEIGHT / 2.0 + offset;
                direction = 0;
                break;
            case 1:
                x = worldMaxX + 50;
                y = HEIGHT / 2.0 - offset;
                direction = Math.PI;
                break;
            case 2:
                x = CROSS_X - offset;
                y = -50;
                direction = Math.PI / 2;
                break;
            case 3:
                x = CROSS_X + offset;
                y = HEIGHT + 50;
                direction = -Math.PI / 2;
                break;
            case 4:
                x = THREE_WAY_X + offset;
                y = HEIGHT + 50;
                direction = -Math.PI / 2;
                if (turnIntention == 0) {
                    turnIntention = random.nextBoolean() ? 1 : 2;
                }
                break;
            default:
                return null;
        }
        return new SpawnPoint(x, y, direction, turnIntention);
    }

    private int randomTurnIntention(int sourceIdx) {
        int turnRand = random.nextInt(10);
        int turnIntention = 0;
        if (turnRand < 2) {
            turnIntention = 1;
        } else if (turnRand < 4) {
            turnIntention = 2;
        }

        if (mode == SimulationMode.THREE_WAY_INTERSECTION || sourceIdx == 4) {
            if (sourceIdx == 0 && turnIntention == 1) {
                turnIntention = random.nextBoolean() ? 0 : 2;
            } else if (sourceIdx == 1 && turnIntention == 2) {
                turnIntention = random.nextBoolean() ? 0 : 1;
            }
        }
        return turnIntention;
    }

    private double laneOffset(int turnIntention) {
        if (turnIntention == 1) {
            return LANE_PRIORITY;
        }
        if (turnIntention == 2) {
            return LANE_BIKE;
        }
        return random.nextBoolean() ? LANE_CAR : LANE_BIKE;
    }

    private Vehicle randomVehicle(double x, double y, double speed, double direction) {
        int type = random.nextInt(100);
        if (type < 5) {
            return new FireTruck("Fire" + vehicleCount, x, y, speed, direction);
        }
        if (type < 10) {
            return new Ambulance("Amb" + vehicleCount, x, y, speed, direction, random.nextBoolean());
        }
        if (type < 20) {
            return new Bus("Bus" + vehicleCount, x, y, speed * 0.7, direction);
        }
        if (type < 50) {
            return new Car("Car" + vehicleCount, x, y, speed, direction, 26, 13, false);
        }
        return new Motorbike("Bike" + vehicleCount, x, y, speed, direction, false);
    }

    private Vehicle manualVehicle(String typeStr, double x, double y, double speed, double direction) {
        return switch (typeStr) {
            case "Emergency" -> new Ambulance("Amb" + vehicleCount, x, y, speed, direction, true);
            case "Ambulance" -> new Ambulance("Amb" + vehicleCount, x, y, speed, direction, false);
            case "FireTruck" -> new FireTruck("Fire" + vehicleCount, x, y, speed, direction);
            case "Bus" -> new Bus("Bus" + vehicleCount, x, y, speed * 0.7, direction);
            case "Car" -> new Car("Car" + vehicleCount, x, y, speed, direction, 26, 13, false);
            case "Motorbike", "Bicycle" -> new Motorbike("Bike" + vehicleCount, x, y, speed, direction, false);
            default -> null;
        };
    }

    private int[] activeSourceIndices() {
        return switch (mode) {
            case CROSS_INTERSECTION -> new int[] { 0, 1, 2, 3 };
            case THREE_WAY_INTERSECTION -> new int[] { 0, 1, 4 };
            case ROAD_NETWORK -> new int[] { 0, 1, 2, 3, 4 };
        };
    }

    public void toggleAutoSpawn() {
        autoSpawnEnabled = !autoSpawnEnabled;
    }

    public void setAutoSpawnEnabled(boolean autoSpawnEnabled) {
        this.autoSpawnEnabled = autoSpawnEnabled;
    }

    public boolean isAutoSpawnEnabled() {
        return autoSpawnEnabled;
    }

    public SimulationMode getMode() {
        return mode;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }

    public List<TrafficLight> getLights1() {
        return lights1;
    }

    public List<TrafficLight> getLights2() {
        return lights2;
    }

    public IntersectionPhaseController getPhaseController1() {
        return phaseController1;
    }

    public ThreeWayPhaseController getPhaseController2() {
        return phaseController2;
    }

    private record SpawnPoint(double x, double y, double direction, int turnIntention) {
    }
}
