package vn.edu.hust.traffic.model.vehicle;

/**
 * Lớp Ambulance đại diện cho xe ưu tiên.
 * Nếu là khẩn cấp (isEmergency = true), vượt đèn đỏ và xin ngã tư trống.
 * Nếu là loại bình thường, tuân thủ mọi luật giao thông như một xe thường.
 */
public class Ambulance extends Vehicle {
    public Ambulance(String id, double x, double y, double speed, double direction, boolean isEmergency) {
        // Kích thước thu nhỏ: 30x14, isEmergency true -> isPriorityVehicle=true
        super(id, x, y, speed, direction, 30, 14, isEmergency);
    }

    @Override
    public void movePhysically(double dt) {
        // Logic vận tốc giờ đã được quản lý tập trung hoàn toàn ở Vehicle.java
        setX(getX() + Math.cos(getDirection()) * getSpeed() * dt);
        setY(getY() + Math.sin(getDirection()) * getSpeed() * dt);
    }

    @Override
    public void render() {}
}


