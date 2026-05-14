package vn.edu.hust.traffic.model.vehicle;

/**
 * Lớp Motorbike đại diện cho xe máy.
 * Di chuyển ở làn sát lề đường.
 */
public class Motorbike extends Vehicle {
    public Motorbike(String id, double x, double y, double speed, double direction, boolean isPriorityVehicle) {
        // Kích thước nhỏ: 25x12
        super(id, x, y, speed, direction, 25, 12, isPriorityVehicle);
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
