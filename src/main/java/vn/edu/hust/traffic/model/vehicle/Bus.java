package vn.edu.hust.traffic.model.vehicle;

/**
 * Lớp Bus đại diện cho xe buýt.
 * Kích thước lớn, đi ở làn ô tô.
 */
public class Bus extends Vehicle {
    public Bus(String id, double x, double y, double speed, double direction) {
        // Kích thước lớn: 80x28
        super(id, x, y, speed, direction, 80, 28, false);
    }

    @Override
    public void movePhysically(double dt) {
        // Xe buýt đi chậm và ổn định
        setX(getX() + Math.cos(getDirection()) * getSpeed() * 0.8 * dt);
        setY(getY() + Math.sin(getDirection()) * getSpeed() * 0.8 * dt);
    }

    @Override
    public void render() {
    }
}
