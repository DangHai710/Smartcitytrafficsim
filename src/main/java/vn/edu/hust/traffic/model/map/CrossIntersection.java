// Giao lộ chữ thập (Người 2, 4)
package vn.edu.hust.traffic.model.map;

import java.util.List;

public class CrossIntersection extends Intersection {
    // TODO: Thuộc tính và phương thức riêng cho giao lộ chữ thập
    public CrossIntersection(String id, double x, double y, List<TrafficLight> lights) {
        super(id, x, y, lights);
    }

    @Override
    public void update() {
        // TODO: Cập nhật trạng thái giao lộ chữ thập
    }
}
