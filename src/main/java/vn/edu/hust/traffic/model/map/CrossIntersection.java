// Giao lộ chữ thập (Người 2, 4)
package vn.edu.hust.traffic.model.map;

import java.util.List;

/**
 * Giao lộ chữ thập 4 hướng.
 * Cung cấp API truy vấn trạng thái đèn theo hướng di chuyển,
 * giúp Vehicle không cần biết chi tiết layout index của danh sách lights.
 *
 * Index lights: [0]=Trái→Phải, [1]=Phải→Trái, [2]=Trên→Dưới, [3]=Dưới→Trên
 */
public class CrossIntersection extends Intersection {

    public CrossIntersection(String id, double x, double y, List<TrafficLight> lights) {
        super(id, x, y, lights);
    }

    /**
     * Trả về trạng thái đèn tương ứng với hướng di chuyển của xe.
     *
     * @param direction góc hướng (radian): 0=→, PI=←, PI/2=↓, -PI/2=↑
     * @return trạng thái đèn (GREEN / YELLOW / RED)
     */
    public TrafficLight.State getLightStateForDirection(double direction) {
        int idx = resolveIndex(direction);
        return lights.get(idx).getState();
    }

    /**
     * Trả về số giây còn lại của đèn theo hướng di chuyển.
     */
    public int getTimeLeftForDirection(double direction) {
        int idx = resolveIndex(direction);
        return lights.get(idx).getTimeLeft();
    }

    /** Map hướng → index trong danh sách lights. */
    private int resolveIndex(double direction) {
        if      (Math.abs(direction - 0)            < 0.1) return 0;
        else if (Math.abs(direction - Math.PI)      < 0.1) return 1;
        else if (Math.abs(direction - Math.PI / 2)  < 0.1) return 2;
        else if (Math.abs(direction + Math.PI / 2)  < 0.1) return 3;
        return 0; // fallback
    }

    @Override
    public void update() {
        // Phase do IntersectionPhaseController quản lý — không cần logic ở đây
    }
}

