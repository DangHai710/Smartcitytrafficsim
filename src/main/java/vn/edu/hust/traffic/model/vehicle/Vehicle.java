package vn.edu.hust.traffic.model.vehicle;

import vn.edu.hust.traffic.behavior.DrivingStrategy;
import vn.edu.hust.traffic.base.Renderable;
import vn.edu.hust.traffic.base.Updatable;

/**
 * Lớp cha cho mọi loại phương tiện giao thông.
 * Chứa vị trí, tốc độ, hướng đi, kích thước, cờ ưu tiên, strategy lái xe.
 */
public abstract class Vehicle implements Renderable, Updatable {
    protected String id;
    protected double x;
    protected double y;
    protected double speed;
    protected double direction; // góc hướng di chuyển (độ hoặc radian)
    protected double width;
    protected double height;
    protected boolean isPriorityVehicle;
    protected DrivingStrategy strategy;

    /**
     * @param id Mã định danh
     * @param x  Tọa độ x
     * @param y  Tọa độ y
     * @param speed Tốc độ
     * @param direction Hướng di chuyển
     * @param width Chiều rộng xe
     * @param height Chiều cao xe
     * @param isPriorityVehicle Có phải xe ưu tiên không
     */
    public Vehicle(String id, double x, double y, double speed, double direction, double width, double height, boolean isPriorityVehicle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.direction = direction;
        this.width = width;
        this.height = height;
        this.isPriorityVehicle = isPriorityVehicle;
    }

    public abstract void movePhysically(double dt);

    public void setStrategy(DrivingStrategy strategy) {
        this.strategy = strategy;
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public double getDirection() { return direction; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isPriorityVehicle() { return isPriorityVehicle; }
    public DrivingStrategy getStrategy() { return strategy; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    // Hỗ trợ update vật lý theo thời gian
    public void update(double dt) {
        movePhysically(dt);
    }

    // Override phương thức Updatable (không có tham số)
    @Override
    public void update() {
        // Có thể gọi update với dt mặc định hoặc bỏ trống
        // update(1.0); // ví dụ: dt = 1.0
    }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setDirection(double direction) { this.direction = direction; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public void setPriorityVehicle(boolean isPriorityVehicle) { this.isPriorityVehicle = isPriorityVehicle; }
}
