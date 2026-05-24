package vn.edu.hust.traffic;

import org.junit.jupiter.api.Test;
import vn.edu.hust.traffic.controller.SimulationMode;
import vn.edu.hust.traffic.controller.TrafficController;
import vn.edu.hust.traffic.model.map.Intersection;
import vn.edu.hust.traffic.model.map.RoundaboutIntersection;
import vn.edu.hust.traffic.model.vehicle.Car;
import vn.edu.hust.traffic.model.vehicle.Vehicle;
import vn.edu.hust.traffic.model.map.ThreeWayIntersection;
import vn.edu.hust.traffic.model.map.TrafficLight;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrafficTest {
    @Test
    public void testExample() {
        // TODO: Write test cases for simulation components.
    }

    @Test
    public void roadNetworkRoundaboutWithFourExitsDoesNotCrash() throws Exception {
        double[] roadNetworkAngles = new double[] {
                0,
                -Math.PI / 2,
                Math.PI,
                Math.PI / 2
        };
        List<Intersection> intersections = List.of(
                new RoundaboutIntersection("roundabout1", 1200.0, -500.0, 100.0, roadNetworkAngles));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 1200.0, -1000.0, 80.0, Math.PI / 2, 26, 13, false);
        vehicles.add(vehicle);
        Field targetExitIndex = Vehicle.class.getDeclaredField("targetExitIndex");
        targetExitIndex.setAccessible(true);
        targetExitIndex.setInt(vehicle, 4);

        assertDoesNotThrow(() -> vehicle.update(0.016, vehicles, intersections, 1400, 600));
    }

    @Test
    public void threeWaySpawnDoesNotChooseMissingRoadTurns() throws Exception {
        TrafficController controller = new TrafficController(SimulationMode.THREE_WAY_INTERSECTION);
        Method createSpawnPoint = TrafficController.class.getDeclaredMethod("createSpawnPoint", int.class);
        createSpawnPoint.setAccessible(true);

        for (int i = 0; i < 200; i++) {
            assertNotEquals(2, turnIntention(createSpawnPoint.invoke(controller, 0)));
            assertNotEquals(1, turnIntention(createSpawnPoint.invoke(controller, 1)));
            assertNotEquals(0, turnIntention(createSpawnPoint.invoke(controller, 4)));
        }
    }

    @Test
    public void roadNetworkRoundaboutHasFiveLogicalBranches() {
        TrafficController controller = new TrafficController(SimulationMode.ROAD_NETWORK);
        RoundaboutIntersection roundabout = controller.getIntersections().stream()
                .filter(RoundaboutIntersection.class::isInstance)
                .map(RoundaboutIntersection.class::cast)
                .findFirst()
                .orElseThrow();

        assertEquals(5, roundabout.getRoadAngles().length);
    }

    @Test
    public void threeWayVehiclesExitOntoExistingRoads() {
        Vehicle leftToNorth = runThreeWayVehicle("leftToNorth", 550.0, 315.0, 0.0, 1, 12.0);
        assertEquals(-Math.PI / 2.0, leftToNorth.getDirection(), 0.01);
        assertTrue(leftToNorth.getX() > 1200.0);
        assertTrue(leftToNorth.getY() < 250.0);

        Vehicle rightToNorth = runThreeWayVehicle("rightToNorth", 1450.0, 235.0, Math.PI, 2, 12.0);
        assertEquals(-Math.PI / 2.0, rightToNorth.getDirection(), 0.01,
                "x=" + rightToNorth.getX() + ", y=" + rightToNorth.getY()
                        + ", speed=" + rightToNorth.getSpeed());
        assertTrue(rightToNorth.getX() > 1200.0);
        assertTrue(rightToNorth.getY() < 250.0);

        Vehicle northToEast = runThreeWayVehicle("northToEast", 1185.0, -250.0, Math.PI / 2.0, 1, 12.0);
        assertEquals(0.0, northToEast.getDirection(), 0.01);
        assertTrue(northToEast.getX() > 1250.0);
        assertTrue(northToEast.getY() > 300.0);

        Vehicle northToWest = runThreeWayVehicle("northToWest", 1135.0, -250.0, Math.PI / 2.0, 2, 12.0);
        assertEquals(Math.PI, northToWest.getDirection(), 0.01);
        assertTrue(northToWest.getX() < 1150.0);
        assertTrue(northToWest.getY() < 300.0);
    }

    @Test
    public void threeWayAndFiveWayModesRunWithoutRuntimeErrors() {
        assertDoesNotThrow(() -> runControllerForSeconds(
                new TrafficController(SimulationMode.THREE_WAY_INTERSECTION), 90));
        assertDoesNotThrow(() -> runControllerForSeconds(
                new TrafficController(SimulationMode.FIVE_WAY_ROUNDABOUT), 90));
    }

    @Test
    public void fiveWayRoundaboutKeepsVehiclesInMotion() {
        TrafficController controller = new TrafficController(SimulationMode.FIVE_WAY_ROUNDABOUT);
        runControllerForSeconds(controller, 45);

        assertFalse(controller.getVehicles().isEmpty());
        assertFalse(controller.getVehicles().stream().allMatch(vehicle -> vehicle.getSpeed() == 0));
    }

    @Test
    public void fiveWayRoundaboutCirculatesCounterClockwise() {
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", 600.0, 300.0, 100.0));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 781.0, 260.0, 80.0, Math.PI, 26, 13, false);
        vehicles.add(vehicle);

        for (int i = 0; i < 10 && !vehicle.insideRoundabout; i++) {
            vehicle.update(0.05, vehicles, intersections, 1400, 600);
        }

        assertTrue(vehicle.insideRoundabout);
        assertTrue(vehicle.roundaboutAngle > 0.0);
        assertEquals(Math.PI / 2.0, vehicle.getDirection(), 0.25);
    }

    private int turnIntention(Object spawnPoint) throws Exception {
        Method turnIntention = spawnPoint.getClass().getDeclaredMethod("turnIntention");
        turnIntention.setAccessible(true);
        return (Integer) turnIntention.invoke(spawnPoint);
    }

    private Vehicle runThreeWayVehicle(String id, double x, double y, double direction, int turnIntention,
            double seconds) {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.GREEN, 999);
            light.forceLeftTurnState(TrafficLight.State.GREEN, 999);
            lights.add(light);
        }
        List<Intersection> intersections = List.of(new ThreeWayIntersection("three1", 1200.0, 300.0, lights));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car(id, x, y, 80.0, direction, 26, 13, false);
        vehicle.setTurnIntention(turnIntention);
        vehicles.add(vehicle);

        double dt = 0.05;
        for (int i = 0; i < seconds / dt; i++) {
            vehicle.update(dt, vehicles, intersections, 1400, 600);
        }
        return vehicle;
    }

    private void runControllerForSeconds(TrafficController controller, int seconds) {
        double dt = 0.05;
        for (int i = 0; i < seconds / dt; i++) {
            controller.update(dt);
        }
    }
}
