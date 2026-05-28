package vn.edu.hust.traffic.behavior;

/**
 * Người lái xe hung hăng — đi nhanh, bám sát xe trước, nhưng vẫn dừng đèn đỏ.
 *
 * Áp dụng cho: Motorbike (mặc định), hoặc bất kỳ xe nào được gán strategy này.
 *
 * Hành vi:
 *   - Tốc độ cao hơn bình thường 30% (x1.3)
 *   - Khoảng cách an toàn thu ngắn còn 60% (x0.6) — bám sát xe trước hơn
 *   - Vẫn dừng đèn đỏ (không liều đến mức đó)
 *   - Vẫn nhường xe ưu tiên
 *
 * Lưu ý tích hợp vào Vehicle.update():
 *   double safeDistance = BASE_SAFE_DIST * strategy.getSafeDistanceMultiplier();
 *   // AggressiveDriver sẽ cho safeDistance nhỏ hơn → bám sát hơn, nhưng không đụng
 */
public class AggressiveDriver implements DrivingStrategy {

    @Override
    public double getSpeedMultiplier() {
        return 1.3;
    }

    @Override
    public double getSafeDistanceMultiplier() {
        return 0.6;
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
        return "Aggressive";
    }
}