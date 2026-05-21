package vn.edu.hust.traffic.behavior;

/**
 * Người lái xe khẩn cấp — tốc độ tối đa, vượt đèn đỏ, không nhường ai.
 *
 * Áp dụng cho: Ambulance (isEmergency=true), FireTruck.
 *
 * Hành vi:
 *   - Tốc độ cao nhất (x1.5)
 *   - Khoảng cách an toàn rất ngắn (x0.5) — áp sát để buộc xe khác nhường
 *   - Vượt đèn đỏ (canRunRedLight = true)
 *   - Không nhường xe ưu tiên khác — chính nó là ưu tiên
 *
 * Lưu ý tích hợp vào Vehicle.update():
 *   Chỗ check đèn đỏ hiện tại:
 *     if (!isPriorityVehicle && !isFleeing) { mustStopByLight = true; }
 *   Nên đổi thành:
 *     if (!strategy.canRunRedLight() && !isFleeing) { mustStopByLight = true; }
 *
 *   Chỗ check flee (nhường xe ưu tiên):
 *     if (other.isPriorityVehicle && other != this) { ... isFleeing = true; }
 *   Thêm điều kiện:
 *     if (other.isPriorityVehicle && other != this && strategy.yieldsToEmergency()) { ... }
 */
public class EmergencyDriver implements DrivingStrategy {

    @Override
    public double getSpeedMultiplier() {
        return 1.5;
    }

    @Override
    public double getSafeDistanceMultiplier() {
        return 0.5;
    }

    @Override
    public boolean canRunRedLight() {
        return true;
    }

    @Override
    public boolean yieldsToEmergency() {
        return false;
    }

    @Override
    public String getName() {
        return "Emergency";
    }
}