
# SmartCityTrafficSim

## Mô tả đề tài
Ứng dụng mô phỏng giao thông đô thị với nhiều loại ngã rẽ (ngã ba, tư, năm, mạng lưới), đa dạng phương tiện (ô tô, xe máy, xe đạp, xe ưu tiên...), chế độ điều khiển tự động/thủ công, âm thanh, hai chế độ hiển thị (basic/đồ họa). Dễ dàng mở rộng loại xe, ngã rẽ, và "bộ não" lái xe.

---

## 1. Hướng dẫn cài đặt môi trường

### Yêu cầu hệ thống
- **Java JDK 17+**
- **Maven 3.6+**
- **Git**
- **IDE:** IntelliJ IDEA, Eclipse, hoặc VS Code (cài plugin Java & JavaFX)

### Cài đặt
- **Java:** Tải JDK 17 từ https://adoptium.net/ và thiết lập JAVA_HOME.
- **Maven:** Tải từ https://maven.apache.org/download.cgi, thêm vào PATH, kiểm tra bằng `mvn -v`.
- **IDE:** Cài plugin Java, JavaFX (VS Code: Extension Pack for Java, JavaFX Support).

### Clone & Build
```sh
git clone <repo-url>
cd SmartCityTrafficSim
mvn clean install
```

### Chạy ứng dụng
```sh
mvn javafx:run
```
Hoặc chạy Main.java từ IDE (chọn cấu hình JavaFX nếu IDE yêu cầu).

### Thêm tài nguyên
- Đặt ảnh, âm thanh vào `src/main/resources/assets`.

### Chạy kiểm thử
```sh
mvn test
```

### Lưu ý
- Nếu lỗi JavaFX: kiểm tra PATH, JAVA_HOME, hoặc thêm VM options:
    ```
    --module-path <path-to-javafx-lib> --add-modules javafx.controls,javafx.fxml,javafx.media
    ```
- Tham khảo thêm: https://openjfx.io/openjfx-docs/

---

## 2. Cấu trúc dự án & nhiệm vụ từng phần (chuẩn MVC)

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

### Phân công nhiệm vụ & mô tả từng thư mục/file:
- **base/**: Interface, abstract class dùng chung (Renderable, Updatable...) — Người 1
- **controller/**: Bộ điều khiển trung tâm, quản lý mô phỏng (TrafficController...) — Người 1
    - *TrafficController.java*: Quản lý danh sách xe, đèn, intersection, sinh xe mới, cập nhật mô phỏng. Không chứa logic dừng/đi của xe.
- **config/**: Hằng số, thông số hệ thống (AppConfig...) — Người 1
- **model/**: Thực thể, xe, bản đồ, ngã rẽ (Vehicle, Car, Intersection...) — Người 2, 4
    - *Car.java, Vehicle.java*: Chỉ chứa logic trạng thái, hành vi của xe (dừng/đi, tuân thủ đèn, giữ khoảng cách...).
- **behavior/**: Logic lái xe, strategy pattern (DrivingStrategy, NormalDriver...) — Người 3
- **view/**: Hiển thị, GUI, renderer (SimulationWindow, Renderer...) — Người 5
    - *SimulationWindow.java*: Chỉ vẽ giao diện, lấy dữ liệu từ controller, không chứa logic điều khiển.
- **utils/**: Hàm bổ trợ, load ảnh, phát âm thanh (ImageLoader, SoundPlayer...) — Người 6
- **resources/assets/**: Ảnh, âm thanh, font phục vụ hiển thị — Người 6
- **test/**: Test case kiểm thử tự động — Người 6

**Nguyên tắc:**  
- Mỗi thành viên chỉ làm đúng phần của mình, không sửa code phần khác trừ khi có sự thống nhất chung.
- Khi mở rộng loại xe/ngã rẽ: chỉ cần tạo class mới kế thừa Vehicle/Intersection, không sửa TrafficController.

---

## 3. Hướng dẫn mở rộng
- Thêm loại xe mới: tạo class kế thừa Vehicle, cài đặt DrivingStrategy phù hợp.
- Thêm loại ngã rẽ mới: tạo class kế thừa Intersection.
- Không sửa code TrafficController khi mở rộng.

---

## 4. Liên hệ & đóng góp
- Xem STRUCTURE.md để biết phân công và cấu trúc chi tiết.
- Đóng góp, báo lỗi: tạo issue hoặc pull request trên GitHub.
