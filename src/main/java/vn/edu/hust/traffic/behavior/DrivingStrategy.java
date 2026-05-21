package vn.edu.hust.traffic.behavior;

/**
 * Interface định nghĩa "bộ não lái xe" cho từng loại phương tiện.
 *
 * Thay vì hardcode hành vi vào từng class xe, mỗi xe giữ một DrivingStrategy.
 * Vehicle.update() đọc các giá trị từ strategy này để điều chỉnh hành vi.
 *
 * Các class implement: NormalDriver, AggressiveDriver, EmergencyDriver.
 *
 * Cách dùng trong Vehicle.update():
 *   double safeDistance = BASE_SAFE_DISTANCE * strategy.getSafeDistanceMultiplier();
 *   double currentTargetSpeed = baseSpeed * strategy.getSpeedMultiplier();
 *   if (!strategy.canRunRedLight()) { ... kiểm tra đèn đỏ ... }
 *   if (strategy.yieldsToEmergency()) { ... nhường xe ưu tiên ... }
 */
public interface DrivingStrategy {

    /**
     * Hệ số nhân tốc độ cơ bản của xe.
     * Ví dụ: 1.0 = bình thường, 1.3 = hung hăng, 1.5 = khẩn cấp.
     */
    double getSpeedMultiplier();

    /**
     * Hệ số nhân khoảng cách an toàn giữ với xe trước.
     * Ví dụ: 1.0 = chuẩn, 0.6 = bám sát, 0.5 = rất sát.
     */
    double getSafeDistanceMultiplier();

    /**
     * Xe có được phép vượt đèn đỏ không.
     * Chỉ EmergencyDriver trả về true.
     */
    boolean canRunRedLight();

    /**
     * Xe có nhường đường khi xe ưu tiên (ambulance, firetruck) đến gần không.
     * EmergencyDriver không nhường vì chính nó là xe ưu tiên.
     */
    boolean yieldsToEmergency();

    /**
     * Tên chiến lược, dùng để log / debug / hiển thị tooltip trong GUI.
     */
    String getName();
}