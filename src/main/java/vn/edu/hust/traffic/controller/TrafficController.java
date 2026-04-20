package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.vehicle.Vehicle;
import vn.edu.hust.traffic.model.map.Intersection;
import java.util.ArrayList;
import java.util.List;

/**
 * Bộ điều khiển giao thông 
 * Quản lý danh sách các phương tiện và cập nhật mô phỏng.
 */
public class TrafficController {
    private static TrafficController instance;
    private List<Vehicle> vehicles;
    private List<Intersection> intersections;

    private TrafficController() {
        vehicles = new ArrayList<>();
        intersections = new ArrayList<>();
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }
    public void addIntersection(Intersection intersection) {
        intersections.add(intersection);
    }

    public static TrafficController getInstance() {
        if (instance == null) {
            instance = new TrafficController();
        }
        return instance;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }
    public void addVehicle(Vehicle v) {
        vehicles.add(v);
    }

    /**
     * Cập nhật trạng thái mô phỏng (gọi update cho từng phương tiện)
     * @param dt khoảng thời gian cập nhật (delta time)
     */
    public void updateSimulation(double dt) {
        for (Vehicle v : vehicles) {
            v.update(dt);
        }
    }
}
