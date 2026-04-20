# SmartCityTrafficSim

## Mô tả đề tài
Ứng dụng mô phỏng giao thông đô thị với các loại ngã rẽ (ngã ba, tư, năm, mạng lưới), nhiều loại phương tiện (ô tô, xe máy, xe đạp, xe cứu thương, cứu hỏa...), chế độ điều khiển tự động/thủ công, âm thanh, và hai chế độ hiển thị (basic/đồ họa). Dễ dàng mở rộng loại xe, ngã rẽ, và "bộ não" lái xe.


## Cấu trúc dự án (chuẩn MVC, phân vai rõ ràng)
Xem chi tiết trong STRUCTURE.md

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

### Vai trò từng thư mục:
- **base/**: Interface, abstract class dùng chung cho toàn hệ thống (Renderable, Updatable...)
- **controller/**: Bộ điều khiển trung tâm, quản lý mô phỏng (TrafficController...)
- **config/**: Hằng số, thông số hệ thống (AppConfig...)
- **model/**: Thực thể, xe, bản đồ, ngã rẽ (Vehicle, Car, Intersection...)
- **behavior/**: Logic lái xe, strategy pattern (DrivingStrategy, NormalDriver...)
- **view/**: Hiển thị, GUI, renderer (SimulationWindow, Renderer...)
- **utils/**: Hàm bổ trợ, load ảnh, phát âm thanh (ImageLoader, SoundPlayer...)
- **resources/assets/**: Ảnh, âm thanh, font phục vụ hiển thị
- **test/**: Test case kiểm thử tự động

Mỗi thành viên chỉ cần làm đúng phần của mình, không sửa code các phần khác trừ khi có sự thống nhất chung.

## Thiết lập môi trường
Xem [SETUP_ENVIRONMENT.md](SETUP_ENVIRONMENT.md) để biết chi tiết cài đặt Java, Maven, IDE, build và chạy dự án.

## Build & Run
```sh
mvn clean install
mvn javafx:run
```
Hoặc chạy Main.java từ IDE.

## Kiểm thử
```sh
mvn test
```

## Mở rộng
- Để thêm loại xe mới: tạo class kế thừa Vehicle, cài đặt DrivingStrategy phù hợp.
- Để thêm loại ngã rẽ mới: tạo class kế thừa Intersection.
- Không sửa code TrafficController khi mở rộng.

## Liên hệ
- Xem STRUCTURE.md để biết phân công và cấu trúc chi tiết.
>>>>>>> ce52dc9 (Build cấu trúc)
