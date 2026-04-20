# Hướng dẫn Thiết lập Môi trường cho SmartCityTrafficSim

## 1. Yêu cầu hệ thống
- Java JDK 17 trở lên
- Maven 3.6+
- Git
- IDE: IntelliJ IDEA, Eclipse, hoặc VS Code (cài plugin Java & JavaFX)

## 2. Cài đặt Java
- Tải và cài đặt JDK 17 từ https://adoptium.net/
- Thiết lập biến môi trường JAVA_HOME trỏ tới thư mục cài đặt JDK

## 3. Cài đặt Maven
- Tải Maven: https://maven.apache.org/download.cgi
- Giải nén và thêm vào PATH
- Kiểm tra bằng lệnh: `mvn -v`

## 4. Cài đặt IDE
- Cài đặt IntelliJ IDEA/Eclipse/VS Code
- Cài plugin Java, JavaFX (nếu dùng VS Code: Extension Pack for Java, JavaFX Support)

## 5. Clone và build dự án
```sh
git clone <repo-url>
cd SmartCityTrafficSim
mvn clean install
```

## 6. Chạy ứng dụng
```sh
mvn javafx:run
```
Hoặc chạy Main.java từ IDE (chọn cấu hình JavaFX nếu IDE yêu cầu)

## 7. Thêm tài nguyên
- Đặt ảnh, âm thanh vào thư mục `src/main/resources/assets`

## 8. Chạy kiểm thử
```sh
mvn test
```

## 9. Ghi chú
- Nếu gặp lỗi JavaFX, kiểm tra biến môi trường PATH và JAVA_HOME.
- Có thể cần cấu hình VM options cho JavaFX:
  - `--module-path <path-to-javafx-lib> --add-modules javafx.controls,javafx.fxml,javafx.media`
- Đọc thêm tại: https://openjfx.io/openjfx-docs/
