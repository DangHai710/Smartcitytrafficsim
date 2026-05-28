package vn.edu.hust.traffic.model.vehicle;
import vn.edu.hust.traffic.behavior.AggressiveDriver;
/**
 * Lớp Motorbike đại diện cho xe máy.
 * Di chuyển ở làn sát lề đường.
 */
public class Motorbike extends Vehicle {
    public Motorbike(String id, double x, double y, double speed, double direction, boolean isPriorityVehicle) {
        // Kích thước thu nhỏ: 16x8
        super(id, x, y, speed, direction, 16, 8, isPriorityVehicle);
        this.strategy = new AggressiveDriver();
    }

    @Override
    public void movePhysically(double dt) {
        setX(getX() + Math.cos(getDirection()) * getSpeed() * dt);
        setY(getY() + Math.sin(getDirection()) * getSpeed() * dt);
    }

    @Override
    public void render() {
        // Xử lý vẽ riêng nếu cần
    }
}
