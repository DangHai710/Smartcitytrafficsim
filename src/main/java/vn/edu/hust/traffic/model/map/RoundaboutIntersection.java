package vn.edu.hust.traffic.model.map;

import java.util.ArrayList;

/**
 * Lớp đại diện cho nút giao vòng xuyến (Roundabout) ngã 5.
 * Vòng xuyến tự điều tiết bằng luật nhường đường cho xe bên trong vòng xuyến,
 * không cần hệ thống đèn tín hiệu.
 */
public class RoundaboutIntersection extends Intersection {
    private final double radius;
    private final double[] roadAngles;

    public RoundaboutIntersection(String id, double x, double y, double radius) {
        super(id, x, y, new ArrayList<>());
        this.radius = radius;
        // Góc của 5 nhánh đường kết nối (Đông, Bắc, Tây, Nam, Đông Bắc)
        this.roadAngles = new double[] {
            0,                  // 0: Đông
            -Math.PI / 2,       // 1: Bắc
            Math.PI,            // 2: Tây
            Math.PI / 2,        // 3: Nam
            -Math.PI / 4        // 4: Đông Bắc
        };
    }

    public RoundaboutIntersection(String id, double x, double y, double radius, double[] customRoadAngles) {
        super(id, x, y, new ArrayList<>());
        this.radius = radius;
        this.roadAngles = customRoadAngles;
    }

    public double getRadius() {
        return radius;
    }

    public double[] getRoadAngles() {
        return roadAngles;
    }

    @Override
    public void update() {
        // Vòng xuyến không có chu kỳ đèn, tự điều tiết
    }
}
