package vn.edu.hust.traffic.behavior;


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