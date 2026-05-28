package vn.edu.hust.traffic;

import org.junit.jupiter.api.Test;
import vn.edu.hust.traffic.controller.SimulationMode;
import vn.edu.hust.traffic.controller.TrafficController;
import vn.edu.hust.traffic.model.map.CrossIntersection;
import vn.edu.hust.traffic.model.map.Intersection;
import vn.edu.hust.traffic.model.map.RoundaboutIntersection;
import vn.edu.hust.traffic.model.vehicle.Ambulance;
import vn.edu.hust.traffic.model.vehicle.Car;
import vn.edu.hust.traffic.model.vehicle.FireTruck;
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
    public void trafficDensityMapsToRequestedVehicleLimits() throws Exception {
        TrafficController controller = new TrafficController(SimulationMode.ROAD_NETWORK);

        controller.setTrafficDensity(1);
        assertEquals(20, controller.getMaxVehicles());

        controller.setTrafficDensity(2);
        assertEquals(30, controller.getMaxVehicles());

        controller.setTrafficDensity(3);
        assertEquals(40, controller.getMaxVehicles());

        controller.setTrafficDensity(1);
        Method spawnVehicle = TrafficController.class.getDeclaredMethod("spawnVehicle", int.class);
        spawnVehicle.setAccessible(true);
        for (int i = 0; i < 60; i++) {
            spawnVehicle.invoke(controller, i % 5);
        }

        assertEquals(20, controller.getVehicles().size());
    }

    @Test
    public void crossIntersectionVehicleClearsInsteadOfStoppingInMiddle() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 390.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 420.0, 340.0, 0.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(follower.getSpeed() > 0.0);
        assertTrue(follower.getX() > 390.0);
    }

    @Test
    public void closeVehicleInsideIntersectionKeepsCrawlingToClearDeadlock() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle clearingVehicle = new Car("Clearing", 390.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle crossTraffic = new Car("CrossTraffic", 390.0, 350.0, 0.0, Math.PI / 2.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(clearingVehicle, crossTraffic));

        clearingVehicle.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(clearingVehicle.getSpeed() > 0.0, "speed=" + clearingVehicle.getSpeed());
        assertTrue(clearingVehicle.getX() > 390.0, "x=" + clearingVehicle.getX());
    }

    @Test
    public void vehicleAlreadyInsideIntersectionDoesNotChangeLaneToYield() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle normal = new Car("Normal", 300.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("AmbBehind", 120.0, 340.0, 80.0, 0.0, true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(normal, ambulance));

        normal.update(0.2, vehicles, intersections, 1400, 600);

        assertEquals(340.0, normal.getY(), 0.01);
        assertTrue(normal.getX() > 300.0, "x=" + normal.getX());
    }

    @Test
    public void threeWayVehicleClearsInsteadOfStoppingInMiddle() {
        List<TrafficLight> lights = greenLights(3);
        List<Intersection> intersections = List.of(new ThreeWayIntersection("three1", 1200.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 1190.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 1220.0, 340.0, 0.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(follower.getSpeed() > 0.0);
        assertTrue(follower.getX() > 1190.0);
    }

    @Test
    public void threeWayClearingVehicleCrawlsThroughPathOnlyConflict() {
        List<TrafficLight> lights = greenLights(3);
        List<Intersection> intersections = List.of(new ThreeWayIntersection("three1", 1200.0, 300.0, lights));
        Vehicle clearingVehicle = new Car("ZClearing", 1160.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle crossingVehicle = new Car("ACrossing", 1180.0, 300.0, 80.0, Math.PI / 2.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(clearingVehicle, crossingVehicle));

        clearingVehicle.update(0.5, vehicles, intersections, 1400, 600);

        assertTrue(clearingVehicle.getSpeed() > 0.0, "speed=" + clearingVehicle.getSpeed());
        assertTrue(clearingVehicle.getX() > 1160.0, "x=" + clearingVehicle.getX());
    }

    @Test
    public void normalVehicleYieldsToPriorityBeforeEnteringIntersection() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle normal = new Car("Normal", 260.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("Amb1", 360.0, 120.0, 80.0, Math.PI / 2.0, true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(normal, ambulance));

        normal.update(0.05, vehicles, intersections, 1400, 600);

        assertEquals(0.0, normal.getSpeed(), 0.01);
    }

    @Test
    public void priorityVehicleDoesNotYieldToNormalVehicleAtIntersection() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle ambulance = new Ambulance("Amb1", 260.0, 340.0, 80.0, 0.0, true);
        Vehicle normal = new Car("Normal", 360.0, 120.0, 80.0, Math.PI / 2.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(ambulance, normal));

        ambulance.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(ambulance.getSpeed() > 0.0);
        assertTrue(ambulance.getX() > 260.0);
    }

    @Test
    public void priorityVehicleChangesLaneAroundTurningVehicleInsideIntersection() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle ambulance = new Ambulance("AmbBypass", 300.0, 340.0, 80.0, 0.0, true);
        Vehicle turningVehicle = new Car("Turning", 335.0, 340.0, 0.0, 0.0, 26, 13, false);
        setBooleanField(turningVehicle, "isTurningSmoothly", true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(ambulance, turningVehicle));

        ambulance.update(0.2, vehicles, intersections, 1400, 600);

        assertTrue(ambulance.getSpeed() > 0.0, "speed=" + ambulance.getSpeed());
        assertTrue(ambulance.getX() > 300.0, "x=" + ambulance.getX());
        assertTrue(Math.abs(ambulance.getY() - 340.0) > 8.0, "y=" + ambulance.getY());
    }

    @Test
    public void normalVehicleChangesLaneAroundDifferentTurnInsideIntersection() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle normal = new Car("NormalBypass", 300.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle turningVehicle = new Car("Turning", 335.0, 340.0, 0.0, 0.0, 26, 13, false);
        turningVehicle.setTurnIntention(1);
        setBooleanField(turningVehicle, "isTurningSmoothly", true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(normal, turningVehicle));

        normal.update(0.2, vehicles, intersections, 1400, 600);

        assertTrue(normal.getSpeed() > 0.0, "speed=" + normal.getSpeed());
        assertTrue(normal.getX() > 300.0, "x=" + normal.getX());
        assertTrue(Math.abs(normal.getY() - 340.0) > 8.0, "y=" + normal.getY());
    }

    @Test
    public void normalVehicleDoesNotChangeLaneBeforeEnteringIntersection() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle normal = new Car("NormalBefore", 240.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle turningVehicle = new Car("Turning", 300.0, 340.0, 0.0, 0.0, 26, 13, false);
        turningVehicle.setTurnIntention(1);
        setBooleanField(turningVehicle, "isTurningSmoothly", true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(normal, turningVehicle));

        normal.update(0.05, vehicles, intersections, 1400, 600);

        assertEquals(340.0, normal.getY(), 0.01);
    }

    @Test
    public void ambulanceAndFireTruckCrossRedLight() {
        List<TrafficLight> lights = redLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));

        Vehicle ambulance = new Ambulance("AmbRed", 260.0, 340.0, 80.0, 0.0, false);
        List<Vehicle> ambulanceOnly = new ArrayList<>(List.of(ambulance));
        ambulance.update(0.15, ambulanceOnly, intersections, 1400, 600);

        assertTrue(ambulance.isPriorityVehicle());
        assertTrue(ambulance.getX() + ambulance.getWidth() / 2.0 > 280.0,
                "x=" + ambulance.getX() + ", speed=" + ambulance.getSpeed());

        Vehicle fireTruck = new FireTruck("FireRed", 250.0, 365.0, 80.0, 0.0);
        List<Vehicle> fireTruckOnly = new ArrayList<>(List.of(fireTruck));
        fireTruck.update(0.15, fireTruckOnly, intersections, 1400, 600);

        assertTrue(fireTruck.isPriorityVehicle());
        assertTrue(fireTruck.getX() + fireTruck.getWidth() / 2.0 > 280.0,
                "x=" + fireTruck.getX() + ", speed=" + fireTruck.getSpeed());
    }

    @Test
    public void priorityVehicleChangesToLeastBusyLaneBeforeRedQueue() {
        List<TrafficLight> lights = redLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle ambulance = new Ambulance("AmbLane", 80.0, 340.0, 80.0, 0.0, true);
        Vehicle middleLaneQueue = new Car("Middle", 250.0, 340.0, 0.0, 0.0, 26, 13, false);
        Vehicle outerLaneQueue1 = new Car("Outer1", 230.0, 365.0, 0.0, 0.0, 26, 13, false);
        Vehicle outerLaneQueue2 = new Car("Outer2", 260.0, 365.0, 0.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(
                ambulance, middleLaneQueue, outerLaneQueue1, outerLaneQueue2));

        ambulance.update(0.2, vehicles, intersections, 1400, 600);

        assertTrue(ambulance.getY() < 340.0, "y=" + ambulance.getY());
    }

    @Test
    public void redLightStoppedVehicleCanCrossStopLineToYieldToPriorityBehind() {
        List<TrafficLight> lights = redLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle stoppedCar = new Car("Stopped", 260.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("AmbBehind", 120.0, 340.0, 80.0, 0.0, true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(stoppedCar, ambulance));

        stoppedCar.update(0.15, vehicles, intersections, 1400, 600);

        assertTrue(stoppedCar.getX() + stoppedCar.getWidth() / 2.0 > 280.0,
                "x=" + stoppedCar.getX() + ", speed=" + stoppedCar.getSpeed());
    }

    @Test
    public void yieldingVehicleMovesToLeastBusyLaneAwayFromPriorityVehicle() {
        List<TrafficLight> lights = redLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle stoppedCar = new Car("Yielding", 260.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("AmbBehind", 120.0, 340.0, 80.0, 0.0, true);
        Vehicle outerLaneQueue1 = new Car("Outer1", 220.0, 365.0, 0.0, 0.0, 26, 13, false);
        Vehicle outerLaneQueue2 = new Car("Outer2", 250.0, 365.0, 0.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(
                stoppedCar, ambulance, outerLaneQueue1, outerLaneQueue2));

        stoppedCar.update(0.2, vehicles, intersections, 1400, 600);

        assertEquals(315.0, stoppedCar.getY(), 0.01);
        assertTrue(stoppedCar.getX() + stoppedCar.getWidth() / 2.0 > 280.0,
                "x=" + stoppedCar.getX() + ", speed=" + stoppedCar.getSpeed());
    }

    @Test
    public void yieldingVehicleFinishesFullLaneChangeAfterPriorityTriggerEnds() throws Exception {
        List<TrafficLight> lights = redLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle yieldingCar = new Car("Yielding", 260.0, 355.0, 80.0, 0.0, 26, 13, false);
        setBooleanField(yieldingCar, "yieldingToPriorityVehicle", true);
        setDoubleField(yieldingCar, "yieldTargetLaneOffset", 40.0);
        setIntField(yieldingCar, "yieldLightIdx", 0);
        setStringField(yieldingCar, "yieldIntersectionId", "cross1");
        setStringField(yieldingCar, "yieldPriorityVehicleId", "AmbGone");
        List<Vehicle> vehicles = new ArrayList<>(List.of(yieldingCar));

        yieldingCar.update(0.2, vehicles, intersections, 1400, 600);

        assertEquals(340.0, yieldingCar.getY(), 0.01);
    }

    @Test
    public void yieldingVehicleOnlyMovesOneAdjacentLane() {
        List<TrafficLight> lights = redLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle yieldingCar = new Car("Yielding", 240.0, 365.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("AmbBehind", 120.0, 365.0, 80.0, 0.0, true);
        Vehicle middleLaneQueue1 = new Car("Middle1", 420.0, 340.0, 0.0, 0.0, 26, 13, false);
        Vehicle middleLaneQueue2 = new Car("Middle2", 450.0, 340.0, 0.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(
                yieldingCar, ambulance, middleLaneQueue1, middleLaneQueue2));

        for (int i = 0; i < 12; i++) {
            yieldingCar.update(0.1, vehicles, intersections, 1400, 600);
        }

        assertEquals(340.0, yieldingCar.getY(), 2.0);
        assertTrue(yieldingCar.getY() > 330.0, "y=" + yieldingCar.getY());
    }

    @Test
    public void vehicleOutsidePriorityLaneDoesNotYieldAcrossLanes() {
        List<TrafficLight> lights = redLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle sideLaneCar = new Car("Side", 260.0, 315.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("AmbBehind", 120.0, 365.0, 80.0, 0.0, true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(sideLaneCar, ambulance));

        sideLaneCar.update(0.2, vehicles, intersections, 1400, 600);

        assertEquals(315.0, sideLaneCar.getY(), 0.01);
    }

    @Test
    public void laterNormalVehicleSlowsForEarlierNormalVehicleClearingIntersection() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle first = new Car("First", 285.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle second = new Car("Second", 390.0, 320.0, 80.0, Math.PI / 2.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(first, second));

        first.update(0.05, vehicles, intersections, 1400, 600);
        second.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(second.getSpeed() < 80.0, "speed=" + second.getSpeed());
    }

    @Test
    public void vehicleDoesNotAdvanceIntoOccupiedIntersectionConflict() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle crossingVehicle = new Car("Crossing", 390.0, 340.0, 0.0, 0.0, 26, 13, false);
        Vehicle enteringVehicle = new Car("Entering", 390.0, 330.0, 80.0, Math.PI / 2.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(crossingVehicle, enteringVehicle));

        enteringVehicle.update(0.05, vehicles, intersections, 1400, 600);

        assertEquals(0.0, enteringVehicle.getSpeed(), 0.01);
        assertEquals(330.0, enteringVehicle.getY(), 0.01);
    }

    @Test
    public void clearingVehicleCrawlsInsteadOfStoppingForNonImmediatePriorityConflict() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle clearingVehicle = new Car("Clearing", 380.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("AmbConflict", 400.0, 308.0, 80.0, Math.PI / 2.0, true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(clearingVehicle, ambulance));

        clearingVehicle.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(clearingVehicle.getSpeed() > 0.0, "speed=" + clearingVehicle.getSpeed());
        assertTrue(clearingVehicle.getX() > 380.0, "x=" + clearingVehicle.getX());
    }

    @Test
    public void adjacentLaneVehicleDoesNotBlockGreenLightDeparture() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle adjacentLaneVehicle = new Car("A", 260.0, 340.0, 0.0, 0.0, 26, 13, false);
        Vehicle departingVehicle = new Car("B", 260.0, 365.0, 80.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(adjacentLaneVehicle, departingVehicle));

        departingVehicle.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(departingVehicle.getSpeed() > 0.0, "speed=" + departingVehicle.getSpeed());
        assertTrue(departingVehicle.getX() > 260.0, "x=" + departingVehicle.getX());
    }

    @Test
    public void normalVehicleChangesLaneToOvertakeSlowerVehicle() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 120.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle slowLeader = new Car("Slow", 190.0, 340.0, 20.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, slowLeader));

        follower.update(0.2, vehicles, intersections, 1400, 600);

        assertTrue(follower.getY() < 340.0, "y=" + follower.getY());
        assertTrue(follower.getSpeed() > slowLeader.getSpeed(), "speed=" + follower.getSpeed());
    }

    @Test
    public void normalVehicleDoesNotOvertakeWhenTargetLaneIsUnsafe() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 120.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle slowLeader = new Car("Slow", 160.0, 340.0, 20.0, 0.0, 26, 13, false);
        Vehicle innerLaneCar = new Car("Inner", 125.0, 315.0, 80.0, 0.0, 26, 13, false);
        Vehicle outerLaneCar = new Car("Outer", 125.0, 365.0, 80.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, slowLeader, innerLaneCar, outerLaneCar));

        follower.update(0.2, vehicles, intersections, 1400, 600);

        assertEquals(340.0, follower.getY(), 0.01);
        assertTrue(follower.getSpeed() < 80.0, "speed=" + follower.getSpeed());
    }

    @Test
    public void normalVehicleDoesNotOvertakeVehicleStoppedByRedLight() {
        List<TrafficLight> lights = greenStraightRedLeftLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 120.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle stoppedLeftTurner = new Car("StoppedLeft", 160.0, 340.0, 0.0, 0.0, 26, 13, false);
        stoppedLeftTurner.setTurnIntention(1);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, stoppedLeftTurner));

        follower.update(0.2, vehicles, intersections, 1400, 600);

        assertEquals(340.0, follower.getY(), 0.01);
        assertTrue(follower.getSpeed() < 80.0, "speed=" + follower.getSpeed());
    }

    @Test
    public void normalOvertakeOnlyMovesOneAdjacentLane() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 120.0, 365.0, 80.0, 0.0, 26, 13, false);
        Vehicle slowLeader = new Car("Slow", 190.0, 365.0, 20.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, slowLeader));

        for (int i = 0; i < 4; i++) {
            follower.update(0.2, vehicles, intersections, 1400, 600);
        }

        assertEquals(340.0, follower.getY(), 2.0);
        assertTrue(follower.getY() > 330.0, "y=" + follower.getY());
    }

    @Test
    public void overtakingVehicleReturnsToOriginalLaneAfterPassing() {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 20.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle slowLeader = new Car("Slow", 85.0, 340.0, 10.0, 0.0, 26, 13, false);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, slowLeader));

        for (int i = 0; i < 60; i++) {
            follower.update(0.05, vehicles, intersections, 1400, 600);
        }

        assertTrue(follower.getX() > slowLeader.getX() + 35.0,
                "follower=" + follower.getX() + ", slow=" + slowLeader.getX());
        assertEquals(340.0, follower.getY(), 3.0);
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
        assertTrue(vehicle.roundaboutAngle < 0.0);
        assertEquals(-Math.PI / 2.0, vehicle.getDirection(), 0.35);
    }

    @Test
    public void fiveWayRoundaboutExitsOnRedDotLaneOfTargetRoad() throws Exception {
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", 600.0, 300.0, 100.0));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 781.0, 260.0, 80.0, Math.PI, 26, 13, false);
        setTargetExitIndex(vehicle, 1);
        vehicles.add(vehicle);

        double dt = 0.05;
        for (int i = 0; i < 400 && !vehicle.hasTurned(); i++) {
            vehicle.update(dt, vehicles, intersections, 1400, 600);
        }

        assertTrue(vehicle.hasTurned());
        assertEquals(-Math.PI / 2.0, vehicle.getDirection(), 0.01);
        assertTrue(vehicle.getX() > 600.0, "x=" + vehicle.getX() + ", y=" + vehicle.getY());
    }

    @Test
    public void fiveWayRoundaboutApproachIsClampedToPaintedLane() throws Exception {
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", 600.0, 300.0, 100.0));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 770.0, 210.0, 0.0, Math.PI, 26, 13, false);
        setTargetExitIndex(vehicle, 1);
        vehicles.add(vehicle);

        vehicle.update(0.05, vehicles, intersections, 1400, 600);

        assertEquals(770.0, vehicle.getX(), 0.01);
        assertEquals(235.0, vehicle.getY(), 0.01);
    }

    @Test
    public void roadNetworkVehicleBelowRoundaboutDoesNotTurnAtOffAxisCrossIntersection() {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.GREEN, 999);
            light.forceLeftTurnState(TrafficLight.State.GREEN, 999);
            lights.add(light);
        }
        List<Intersection> intersections = List.of(
                new CrossIntersection("cross2", 400.0, -500.0, lights),
                new RoundaboutIntersection("roundabout1", 1200.0, -500.0, 100.0,
                        new double[] { 0, -Math.PI / 2, Math.PI, Math.PI / 2, -Math.PI / 4 }));
        Vehicle vehicle = new Car("Car1", 1215.0, 80.0, 80.0, -Math.PI / 2, 26, 13, false);
        vehicle.setTurnIntention(2);
        List<Vehicle> vehicles = new ArrayList<>(List.of(vehicle));

        for (int i = 0; i < 60 && vehicle.getY() > -230.0; i++) {
            vehicle.update(0.05, vehicles, intersections, 1400, 600);
        }

        assertEquals(-Math.PI / 2, vehicle.getDirection(), 0.01);
        assertEquals(1215.0, vehicle.getX(), 0.01);
    }

    @Test
    public void threeWayDiagonalRightTurnLocksOntoExitLane() {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.GREEN, 999);
            light.forceLeftTurnState(TrafficLight.State.GREEN, 999);
            lights.add(light);
        }
        List<Intersection> intersections = List.of(new ThreeWayIntersection("three1", 1200.0, 300.0, lights));
        Vehicle vehicle = new Car("Car1", 1265.0, 540.0, 80.0, -Math.PI / 2.0, 26, 13, false);
        vehicle.setTurnIntention(2);
        List<Vehicle> vehicles = new ArrayList<>(List.of(vehicle));

        for (int i = 0; i < 120 && !vehicle.hasTurned(); i++) {
            vehicle.update(0.05, vehicles, intersections, 1400, 600);
        }

        assertTrue(vehicle.hasTurned(), "x=" + vehicle.getX() + ", y=" + vehicle.getY());
        assertEquals(0.0, vehicle.getDirection(), 0.01);
        assertEquals(365.0, vehicle.getY(), 0.01);
    }

    @Test
    public void smoothTurningVehicleSlowsBehindVehicleAlreadyOnTurnPath() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 400.0, 340.0, 80.0, -Math.PI / 2.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 400.0, 298.0, 0.0, -Math.PI / 2.0, 26, 13, false);
        setBooleanField(follower, "isTurningSmoothly", true);
        setDoubleField(follower, "smoothTurnStartX", 400.0);
        setDoubleField(follower, "smoothTurnStartY", 340.0);
        setDoubleField(follower, "smoothTurnEndX", 400.0);
        setDoubleField(follower, "smoothTurnEndY", 240.0);
        setDoubleField(follower, "smoothTurnStartDirection", -Math.PI / 2.0);
        setDoubleField(follower, "smoothTurnEndDirection", -Math.PI / 2.0);
        setDoubleField(follower, "smoothTurnDuration", 0.5);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.1, vehicles, intersections, 1400, 600);

        assertTrue(follower.getSpeed() > 0.0, "speed=" + follower.getSpeed());
        assertTrue(follower.getSpeed() < 80.0, "speed=" + follower.getSpeed());
        assertEquals(400.0, follower.getX(), 0.01);
        assertTrue(follower.getY() < 340.0, "y=" + follower.getY());
    }

    @Test
    public void smoothTurningVehicleStopsOnlyWhenTooCloseOnTurnPath() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle follower = new Car("Follower", 400.0, 340.0, 80.0, -Math.PI / 2.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 400.0, 318.0, 0.0, -Math.PI / 2.0, 26, 13, false);
        setBooleanField(follower, "isTurningSmoothly", true);
        setDoubleField(follower, "smoothTurnStartX", 400.0);
        setDoubleField(follower, "smoothTurnStartY", 340.0);
        setDoubleField(follower, "smoothTurnEndX", 400.0);
        setDoubleField(follower, "smoothTurnEndY", 240.0);
        setDoubleField(follower, "smoothTurnStartDirection", -Math.PI / 2.0);
        setDoubleField(follower, "smoothTurnEndDirection", -Math.PI / 2.0);
        setDoubleField(follower, "smoothTurnDuration", 0.5);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.1, vehicles, intersections, 1400, 600);

        assertEquals(0.0, follower.getSpeed(), 0.01);
        assertEquals(400.0, follower.getX(), 0.01);
        assertEquals(340.0, follower.getY(), 0.01);
    }

    @Test
    public void diagonalRightTurnVehicleSlowsBehindVehicleOnTurnRoad() throws Exception {
        Vehicle follower = new Car("Follower", 300.0, 340.0, 80.0, Math.PI / 4.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 330.0, 370.0, 0.0, Math.PI / 4.0, 26, 13, false);
        follower.setTurnIntention(2);
        leader.setTurnIntention(2);
        setBooleanField(follower, "isTurningDiagonally", true);
        setBooleanField(leader, "isTurningDiagonally", true);
        setDoubleField(follower, "diagonalTurnCenterX", 400.0);
        setDoubleField(follower, "diagonalTurnCenterY", 300.0);
        setDoubleField(leader, "diagonalTurnCenterX", 400.0);
        setDoubleField(leader, "diagonalTurnCenterY", 300.0);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.2, vehicles, List.of(), 1400, 600);

        assertTrue(follower.getSpeed() > 0.0, "speed=" + follower.getSpeed());
        assertTrue(follower.getSpeed() < 80.0, "speed=" + follower.getSpeed());
        assertTrue(follower.getX() > 300.0, "x=" + follower.getX());
        assertTrue(follower.getY() > 340.0, "y=" + follower.getY());
    }

    @Test
    public void diagonalRightTurnVehicleStopsOnlyWhenTooCloseOnTurnRoad() throws Exception {
        Vehicle follower = new Car("Follower", 300.0, 340.0, 80.0, Math.PI / 4.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 315.0, 355.0, 0.0, Math.PI / 4.0, 26, 13, false);
        follower.setTurnIntention(2);
        leader.setTurnIntention(2);
        setBooleanField(follower, "isTurningDiagonally", true);
        setBooleanField(leader, "isTurningDiagonally", true);
        setDoubleField(follower, "diagonalTurnCenterX", 400.0);
        setDoubleField(follower, "diagonalTurnCenterY", 300.0);
        setDoubleField(leader, "diagonalTurnCenterX", 400.0);
        setDoubleField(leader, "diagonalTurnCenterY", 300.0);
        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.2, vehicles, List.of(), 1400, 600);

        assertEquals(0.0, follower.getSpeed(), 0.01);
        assertEquals(300.0, follower.getX(), 0.01);
        assertEquals(340.0, follower.getY(), 0.01);
    }

    @Test
    public void vehicleLeavingTurnKeepsMovingPastOldIntersectionConflict() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle exiting = new Car("Exit", 390.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle crossTraffic = new Car("Cross", 390.0, 350.0, 0.0, Math.PI / 2.0, 26, 13, false);
        setBooleanField(exiting, "hasTurned", true);
        setDoubleField(exiting, "turnExitClearanceTime", 0.6);
        List<Vehicle> vehicles = new ArrayList<>(List.of(exiting, crossTraffic));

        exiting.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(exiting.getSpeed() > 0.0, "speed=" + exiting.getSpeed());
        assertTrue(exiting.getX() > 390.0, "x=" + exiting.getX());
    }

    @Test
    public void vehicleLeavingTurnKeepsCrawlingBehindStoppedVehicleWithSafeGap() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle exiting = new Car("Exit", 390.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 440.0, 340.0, 0.0, 0.0, 26, 13, false);
        setBooleanField(exiting, "hasTurned", true);
        setDoubleField(exiting, "turnExitClearanceTime", 0.6);
        List<Vehicle> vehicles = new ArrayList<>(List.of(exiting, leader));

        exiting.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(exiting.getSpeed() > 0.0, "speed=" + exiting.getSpeed());
        assertTrue(exiting.getX() > 390.0, "x=" + exiting.getX());
    }

    @Test
    public void vehicleLeavingTurnStillStopsWhenVehicleAheadTooClose() throws Exception {
        List<TrafficLight> lights = greenLights(4);
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle exiting = new Car("Exit", 390.0, 340.0, 80.0, 0.0, 26, 13, false);
        Vehicle leader = new Car("Leader", 415.0, 340.0, 0.0, 0.0, 26, 13, false);
        setBooleanField(exiting, "hasTurned", true);
        setDoubleField(exiting, "turnExitClearanceTime", 0.6);
        List<Vehicle> vehicles = new ArrayList<>(List.of(exiting, leader));

        exiting.update(0.05, vehicles, intersections, 1400, 600);

        assertEquals(0.0, exiting.getSpeed(), 0.01);
        assertEquals(390.0, exiting.getX(), 0.01);
    }

    @Test
    public void roadNetworkVehicleLeavingRoundaboutDoesNotGoStraightThroughThreeWayMissingRoad() throws Exception {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.GREEN, 999);
            light.forceLeftTurnState(TrafficLight.State.GREEN, 999);
            lights.add(light);
        }
        List<Intersection> intersections = List.of(new ThreeWayIntersection("three1", 1200.0, 300.0, lights));
        Vehicle vehicle = new Car("Car1", 1215.0, 120.0, 80.0, Math.PI / 2.0, 26, 13, false);
        vehicle.setTurnIntention(0);
        setBooleanField(vehicle, "hasTurned", true);
        setBooleanField(vehicle, "exitedRoundabout", true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(vehicle));

        for (int i = 0; i < 160 && Math.abs(vehicle.getDirection() - Math.PI / 2.0) < 0.01; i++) {
            vehicle.update(0.05, vehicles, intersections, 1400, 600);
        }

        assertNotEquals(Math.PI / 2.0, vehicle.getDirection(), 0.01);
        assertTrue(vehicle.getY() < 380.0, "x=" + vehicle.getX() + ", y=" + vehicle.getY());
    }

    @Test
    public void fiveWayRoundaboutKeepsCirculatingVehicleInsideRoadBand() throws Exception {
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", 600.0, 300.0, 100.0));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 850.0, 300.0, 80.0, Math.PI, 26, 13, false);
        vehicle.insideRoundabout = true;
        vehicle.roundaboutAngle = 0.0;
        setTargetExitIndex(vehicle, 1);
        vehicles.add(vehicle);

        vehicle.update(0.05, vehicles, intersections, 1400, 600);

        double radius = Math.hypot(vehicle.getX() - 600.0, vehicle.getY() - 300.0);
        assertTrue(radius >= 112.0 && radius <= 166.0, "radius=" + radius);
    }

    @Test
    public void fiveWayRoundaboutStopsForCloseVehicleAhead() throws Exception {
        double cx = 600.0;
        double cy = 300.0;
        double radius = 140.0;
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", cx, cy, 100.0));
        Vehicle follower = new Car("Follower", cx + radius, cy, 80.0, -Math.PI / 2.0, 26, 13, false);
        follower.insideRoundabout = true;
        follower.roundaboutAngle = 0.0;
        setTargetExitIndex(follower, 1);

        double leaderAngle = -0.12;
        Vehicle leader = new Car("Leader",
                cx + radius * Math.cos(leaderAngle),
                cy + radius * Math.sin(leaderAngle),
                0.0,
                -Math.PI / 2.0,
                26,
                13,
                false);
        leader.insideRoundabout = true;
        leader.roundaboutAngle = leaderAngle;
        setTargetExitIndex(leader, 1);

        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.05, vehicles, intersections, 1400, 600);

        assertEquals(0.0, follower.getSpeed(), 0.01);
        assertEquals(0.0, follower.roundaboutAngle, 0.01);
    }

    @Test
    public void fiveWayRoundaboutCrawlsForSmallPositiveGapAhead() throws Exception {
        double cx = 600.0;
        double cy = 300.0;
        double radius = 140.0;
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", cx, cy, 100.0));
        Vehicle follower = new Car("Follower", cx + radius, cy, 80.0, -Math.PI / 2.0, 26, 13, false);
        follower.insideRoundabout = true;
        follower.roundaboutAngle = 0.0;
        setTargetExitIndex(follower, 1);

        double leaderAngle = -0.22;
        Vehicle leader = new Car("Leader",
                cx + radius * Math.cos(leaderAngle),
                cy + radius * Math.sin(leaderAngle),
                0.0,
                -Math.PI / 2.0,
                26,
                13,
                false);
        leader.insideRoundabout = true;
        leader.roundaboutAngle = leaderAngle;
        setTargetExitIndex(leader, 1);

        List<Vehicle> vehicles = new ArrayList<>(List.of(follower, leader));

        follower.update(0.05, vehicles, intersections, 1400, 600);

        assertTrue(follower.getSpeed() > 0.0, "speed=" + follower.getSpeed());
        assertTrue(follower.roundaboutAngle < 0.0, "angle=" + follower.roundaboutAngle);
    }

    @Test
    public void fiveWayRoundaboutCapturesVehicleBeforeItCrossesGreenIsland() throws Exception {
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", 600.0, 300.0, 100.0));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 560.0, 300.0, 80.0, 0.0, 26, 13, false);
        setTargetExitIndex(vehicle, 0);
        vehicles.add(vehicle);

        vehicle.update(0.05, vehicles, intersections, 1400, 600);

        double radius = Math.hypot(vehicle.getX() - 600.0, vehicle.getY() - 300.0);
        assertTrue(vehicle.insideRoundabout);
        assertTrue(radius >= 112.0, "radius=" + radius);
        assertNotEquals(0.0, vehicle.getDirection(), 0.01);
    }

    @Test
    public void fiveWayRoundaboutEntryDoesNotTeleportAtMergePoint() throws Exception {
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", 600.0, 300.0, 100.0));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 790.0, 235.0, 80.0, Math.PI, 26, 13, false);
        setTargetExitIndex(vehicle, 1);
        vehicles.add(vehicle);

        double maxStep = 0.0;
        double previousX = vehicle.getX();
        double previousY = vehicle.getY();
        for (int i = 0; i < 20 && !vehicle.insideRoundabout; i++) {
            vehicle.update(0.05, vehicles, intersections, 1400, 600);
            maxStep = Math.max(maxStep, Math.hypot(vehicle.getX() - previousX, vehicle.getY() - previousY));
            previousX = vehicle.getX();
            previousY = vehicle.getY();
        }

        assertTrue(vehicle.insideRoundabout);
        assertTrue(maxStep < 15.0, "maxStep=" + maxStep);
    }

    @Test
    public void fiveWayRoundaboutExitDoesNotTeleportToRoadLane() throws Exception {
        List<Intersection> intersections = List.of(new RoundaboutIntersection("roundabout1", 600.0, 300.0, 100.0));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 781.0, 260.0, 80.0, Math.PI, 26, 13, false);
        setTargetExitIndex(vehicle, 1);
        vehicles.add(vehicle);

        double maxStep = 0.0;
        double previousX = vehicle.getX();
        double previousY = vehicle.getY();
        for (int i = 0; i < 400 && !vehicle.hasTurned(); i++) {
            vehicle.update(0.05, vehicles, intersections, 1400, 600);
            maxStep = Math.max(maxStep, Math.hypot(vehicle.getX() - previousX, vehicle.getY() - previousY));
            previousX = vehicle.getX();
            previousY = vehicle.getY();
        }

        assertTrue(vehicle.hasTurned());
        assertTrue(maxStep < 20.0, "maxStep=" + maxStep);
    }

    @Test
    public void yieldingToEmergencyVehicleStaysInsideRoadWidth() {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.GREEN, 999);
            light.forceLeftTurnState(TrafficLight.State.GREEN, 999);
            lights.add(light);
        }
        List<Intersection> intersections = List.of(new CrossIntersection("cross1", 400.0, 300.0, lights));
        Vehicle car = new Car("Car1", 120.0, 360.0, 80.0, 0.0, 26, 13, false);
        Vehicle ambulance = new Ambulance("Amb1", 80.0, 360.0, 80.0, 0.0, true);
        List<Vehicle> vehicles = new ArrayList<>(List.of(car, ambulance));

        car.update(1.0, vehicles, intersections, 1400, 600);

        assertTrue(car.getY() <= 300.0 + 80.0 - car.getHeight() / 2.0 + 0.01, "y=" + car.getY());
    }

    @Test
    public void roadNetworkRoundaboutUsesSameEntryAndExitLaneLogicAfterPreviousTurn() throws Exception {
        double[] roadNetworkAngles = new double[] {
                0,
                -Math.PI / 2,
                Math.PI,
                Math.PI / 2,
                -Math.PI / 4
        };
        List<Intersection> intersections = List.of(
                new RoundaboutIntersection("roundabout1", 1200.0, -500.0, 100.0, roadNetworkAngles));
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Car("Car1", 1185.0, -1050.0, 80.0, Math.PI / 2.0, 26, 13, false);
        setTargetExitIndex(vehicle, 0);
        setBooleanField(vehicle, "hasTurned", true);
        vehicles.add(vehicle);

        double dt = 0.05;
        for (int i = 0; i < 500 && vehicle.getDirection() != 0.0; i++) {
            vehicle.update(dt, vehicles, intersections, 1400, 600);
        }

        assertEquals(0.0, vehicle.getDirection(), 0.01);
        assertTrue(vehicle.getX() > 1200.0, "x=" + vehicle.getX() + ", y=" + vehicle.getY());
        assertTrue(vehicle.getY() > -500.0, "x=" + vehicle.getX() + ", y=" + vehicle.getY());
    }

    private int turnIntention(Object spawnPoint) throws Exception {
        Method turnIntention = spawnPoint.getClass().getDeclaredMethod("turnIntention");
        turnIntention.setAccessible(true);
        return (Integer) turnIntention.invoke(spawnPoint);
    }

    private void setTargetExitIndex(Vehicle vehicle, int exitIndex) throws Exception {
        Field targetExitIndex = Vehicle.class.getDeclaredField("targetExitIndex");
        targetExitIndex.setAccessible(true);
        targetExitIndex.setInt(vehicle, exitIndex);
    }

    private void setBooleanField(Vehicle vehicle, String fieldName, boolean value) throws Exception {
        Field field = Vehicle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(vehicle, value);
    }

    private void setDoubleField(Vehicle vehicle, String fieldName, double value) throws Exception {
        Field field = Vehicle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(vehicle, value);
    }

    private void setIntField(Vehicle vehicle, String fieldName, int value) throws Exception {
        Field field = Vehicle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(vehicle, value);
    }

    private void setStringField(Vehicle vehicle, String fieldName, String value) throws Exception {
        Field field = Vehicle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(vehicle, value);
    }

    private List<TrafficLight> greenLights(int count) {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.GREEN, 999);
            light.forceLeftTurnState(TrafficLight.State.GREEN, 999);
            lights.add(light);
        }
        return lights;
    }

    private List<TrafficLight> redLights(int count) {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.RED, 999);
            light.forceLeftTurnState(TrafficLight.State.RED, 999);
            lights.add(light);
        }
        return lights;
    }

    private List<TrafficLight> greenStraightRedLeftLights(int count) {
        List<TrafficLight> lights = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TrafficLight light = new TrafficLight();
            light.forceState(TrafficLight.State.GREEN, 999);
            light.forceLeftTurnState(TrafficLight.State.RED, 999);
            lights.add(light);
        }
        return lights;
    }

    private Vehicle runThreeWayVehicle(String id, double x, double y, double direction, int turnIntention,
            double seconds) {
        List<TrafficLight> lights = greenLights(3);
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
