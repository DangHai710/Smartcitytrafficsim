package vn.edu.hust.traffic.model.map;

import java.util.List;

/**
 * Giao lộ ngã 3 (T-junction lật ngược).
 * Nhánh ngang: Trái -> Phải (0) và Phải -> Trái (PI)
 * Nhánh dọc: Dưới -> Trên (-PI/2)
 *
 * Index lights: [0]=Trái→Phải, [1]=Phải→Trái, [2]=Dưới→Trên
 */
public class ThreeWayIntersection extends Intersection {

    public ThreeWayIntersection(String id, double x, double y, List<TrafficLight> lights) {
        super(id, x, y, lights);
    }

    public TrafficLight getLightForDirection(double direction) {
        int idx = resolveIndex(direction);
        return lights.get(idx);
    }

    public int resolveIndex(double direction) {
        if      (Math.abs(direction - 0)            < 0.1) return 0; // LTR
        else if (Math.abs(direction - Math.PI)      < 0.1) return 1; // RTL
        else if (Math.abs(direction + Math.PI / 2)  < 0.1) return 2; // BTT
        return 0; // fallback
    }

    @Override
    public void update() {
        // Phase do controller quản lý
    }
}
