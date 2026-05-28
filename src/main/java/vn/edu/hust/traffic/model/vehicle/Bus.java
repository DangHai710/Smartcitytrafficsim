package vn.edu.hust.traffic.model.vehicle;

/**
 * Lớp Bus đại diện cho xe buýt.
 * Kích thước lớn, đi ở làn ô tô.
 */
public class Bus extends Vehicle {
    public Bus(String id, double x, double y, double speed, double direction) {
        // Kích thước thu nhỏ cho vừa làn: 52x18
        super(id, x, y, speed, direction, 52, 18, false);
    }

    @Override
    public void movePhysically(double dt) {
        // Tốc độ đã được Vehicle.update() tính toán chính xác — không nhân hệ số thêm
        setX(getX() + Math.cos(getDirection()) * getSpeed() * dt);
        setY(getY() + Math.sin(getDirection()) * getSpeed() * dt);
    }

    @Override
    public void render() {
    }
}
