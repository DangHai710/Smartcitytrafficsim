# SmartCityTrafficSim Directory Structure (chuẩn hóa, phân vai rõ ràng)

```
SmartCityTrafficSim/
├── .gitignore
├── pom.xml
├── README.md
├── LICENSE
├── STRUCTURE.md
├── SETUP_ENVIRONMENT.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── vn/edu/hust/traffic/
    │   │       ├── Main.java                  # Điểm khởi chạy (Người 1)
    │   │       ├── base/                      # Interface, abstract class dùng chung (Người 1)
    │   │       ├── controller/                # Bộ điều khiển, quản lý mô phỏng (Người 1)
    │   │       ├── config/                    # Hằng số, thông số hệ thống (Người 1)
    │   │       ├── model/                     # Thực thể, xe, bản đồ, ngã rẽ (Người 2, 4)
    │   │       ├── behavior/                  # Logic lái xe, strategy (Người 3)
    │   │       ├── view/                      # Hiển thị, GUI (Người 5)
    │   │       └── utils/                     # Hàm bổ trợ, load ảnh, âm thanh (Người 6)
    │   └── resources/
    │       ├── assets/
    │       │   ├── images/                    # Ảnh xe, đèn, mặt đường (Người 6)
    │       │   ├── sounds/                    # Âm thanh, còi, động cơ (Người 6)
    │       │   └── fonts/                     # Font chữ riêng (Người 6)
    │       └── config.properties              # File cấu hình ngoài (Người 6)
    └── test/
        └── java/
            └── vn/edu/hust/traffic/
                └── TrafficTest.java           # Test case (Người 6)
```

## Directory Description & Team Assignment
- **base/**: Interface, abstract class dùng chung cho toàn hệ thống (Renderable, Updatable...) — Người 1
- **controller/**: Bộ điều khiển trung tâm, quản lý mô phỏng (TrafficController...) — Người 1
- **config/**: Hằng số, thông số hệ thống (AppConfig...) — Người 1
- **model/**: Thực thể, xe, bản đồ, ngã rẽ (Vehicle, Car, Intersection...) — Người 2, 4
- **behavior/**: Logic lái xe, strategy pattern (DrivingStrategy, NormalDriver...) — Người 3
- **view/**: Hiển thị, GUI, renderer (SimulationWindow, Renderer...) — Người 5
- **utils/**: Hàm bổ trợ, load ảnh, phát âm thanh (ImageLoader, SoundPlayer...) — Người 6
- **resources/assets/**: Ảnh, âm thanh, font phục vụ hiển thị — Người 6
- **test/**: Test case kiểm thử tự động — Người 6

Mỗi thành viên chỉ cần làm đúng phần của mình, không sửa code các phần khác trừ khi có sự thống nhất chung.