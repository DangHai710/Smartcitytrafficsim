package vn.edu.hust.traffic.behavior;

/**
 * Người lái xe bình thường — tuân thủ đầy đủ luật giao thông.
 *
 * Áp dụng cho: Car, Bus, Bicycle.
 *
 * Hành vi:
 *   - Tốc độ chuẩn (x1.0)
 *   - Giữ khoảng cách an toàn đầy đủ (x1.0)
 *   - Dừng đèn đỏ
 *   - Nhường xe ưu tiên
 */
public class NormalDriver implements DrivingStrategy {

    @Override
    public double getSpeedMultiplier() {
        return 1.0;
    }

    @Override
    public double getSafeDistanceMultiplier() {
        return 1.0;
    }

    @Override
    public boolean canRunRedLight() {
        return false;
    }

    @Override
    public boolean yieldsToEmergency() {
        return true;
    }

    @Override
    public String getName() {
        return "Normal";
    }
}