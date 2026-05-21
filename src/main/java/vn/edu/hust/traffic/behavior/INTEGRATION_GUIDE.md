# Hướng dẫn tích hợp DrivingStrategy vào Vehicle.java
# (Dành cho Người 1 hoặc ai đang giữ nhánh dev)

## 1. Thêm field strategy vào Vehicle

```java
// Thêm vào danh sách field của Vehicle:
protected DrivingStrategy strategy;

// Thêm constructor set strategy mặc định:
public Vehicle(...) {
    ...
    this.strategy = new NormalDriver(); // mặc định
}

// Thêm setter để các lớp con hoặc TrafficController có thể đổi:
public void setStrategy(DrivingStrategy strategy) {
    this.strategy = strategy;
}
public DrivingStrategy getStrategy() {
    return strategy;
}
```

---

## 2. Gán strategy mặc định trong từng lớp con

```java
// Motorbike.java — constructor:
public Motorbike(...) {
    super(...);
    this.strategy = new AggressiveDriver();
}

// Ambulance.java — constructor (khi isEmergency = true):
public Ambulance(..., boolean isEmergency) {
    super(..., isEmergency);
    this.strategy = isEmergency ? new EmergencyDriver() : new NormalDriver();
}

// FireTruck.java — constructor:
public FireTruck(...) {
    super(..., true);
    this.strategy = new EmergencyDriver();
}

// Car.java, Bus.java — giữ mặc định NormalDriver, không cần đổi gì.
```

---

## 3. Sửa 3 chỗ trong Vehicle.update()

### Chỗ 1 — khoảng cách an toàn (dòng ~đầu method update):
```java
// TRƯỚC:
double safeDistance = (width > 30) ? 50 : 30;

// SAU:
double BASE_SAFE = (width > 30) ? 50 : 30;
double safeDistance = BASE_SAFE * strategy.getSafeDistanceMultiplier();
```

### Chỗ 2 — check đèn đỏ (Bước 2 → Bước 3):
```java
// TRƯỚC:
if (!isPriorityVehicle && !isFleeing) {

// SAU:
if (!strategy.canRunRedLight() && !isFleeing) {
```

### Chỗ 3 — logic flee / nhường xe ưu tiên (Bước 1):
```java
// TRƯỚC:
if (other.isPriorityVehicle && other != this) {

// SAU:
if (other.isPriorityVehicle && other != this && strategy.yieldsToEmergency()) {
```

---

## 4. Kết quả sau khi tích hợp

| Xe            | Strategy          | Chạy đèn đỏ | Nhường ưu tiên | Tốc độ | Khoảng cách |
|---------------|-------------------|-------------|----------------|--------|-------------|
| Car           | NormalDriver      | Không       | Có             | x1.0   | x1.0        |
| Bus           | NormalDriver      | Không       | Có             | x1.0   | x1.0        |
| Motorbike     | AggressiveDriver  | Không       | Có             | x1.3   | x0.6        |
| Ambulance (!)| EmergencyDriver   | **Có**      | Không          | x1.5   | x0.5        |
| FireTruck     | EmergencyDriver   | **Có**      | Không          | x1.5   | x0.5        |

---

## Lưu ý về nhánh

- 4 file behavior đặt vào: `src/main/java/vn/edu/hust/traffic/behavior/`
- Không xóa file DrivingStrategy.java cũ — ghi đè (replace) toàn bộ nội dung
- Không xóa file NormalDriver.java cũ — ghi đè toàn bộ nội dung
- Nên làm trên nhánh dev, không phải main cũ
