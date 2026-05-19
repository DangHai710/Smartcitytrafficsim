package vn.edu.hust.traffic.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import vn.edu.hust.traffic.model.vehicle.*;

/**
 * Lớp chuyên vẽ phương tiện với hình ảnh chi tiết và animation.
 * Thay thế cách vẽ fillRect đơn giản bằng hình xe thực tế:
 * - Car: Thân xe + kính + bánh xe + đèn pha/hậu
 * - Bus: Xe buýt dài + cửa sổ hành khách
 * - Motorbike: Xe máy nhỏ gọn
 * - Ambulance: Chữ thập đỏ + đèn chớp
 * - FireTruck: Thang cứu hỏa + đèn chớp đỏ/xanh
 */
public class VehicleRenderer {

    private long animationFrame = 0;

    /**
     * Cập nhật frame animation (gọi mỗi render loop).
     */
    public void tick() {
        animationFrame++;
    }

    /**
     * Vẽ một phương tiện lên canvas.
     */
    public void renderVehicle(GraphicsContext gc, Vehicle v) {
        gc.save();

        // Tính góc xoay dựa trên direction
        double angle = Math.toDegrees(v.getDirection());

        // Dịch gốc tọa độ về tâm xe rồi xoay
        gc.translate(v.getX(), v.getY());
        gc.rotate(angle);

        // Vẽ bóng đổ dưới xe
        gc.setEffect(new DropShadow(4, 2, 2, Color.color(0, 0, 0, 0.4)));

        if (v instanceof Ambulance) {
            renderAmbulance(gc, (Ambulance) v);
        } else if (v instanceof FireTruck) {
            renderFireTruck(gc, (FireTruck) v);
        } else if (v instanceof Bus) {
            renderBus(gc, v);
        } else if (v instanceof Motorbike) {
            renderMotorbike(gc, v);
        } else if (v instanceof Car) {
            renderCar(gc, v);
        } else {
            // Fallback: hình chữ nhật đơn giản
            gc.setFill(Color.GRAY);
            gc.fillRect(-v.getWidth() / 2, -v.getHeight() / 2, v.getWidth(), v.getHeight());
        }

        gc.setEffect(null);

        // Vẽ tên xe phía trên
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        gc.setFill(Color.WHITE);
        gc.fillText(v.getId(), -v.getWidth() / 2, -v.getHeight() / 2 - 4);

        gc.restore();
    }

    // ═══════════════════════════════════════════
    //  XE Ô TÔ (Car)
    // ═══════════════════════════════════════════
    private void renderCar(GraphicsContext gc, Vehicle v) {
        double w = v.getWidth();
        double h = v.getHeight();

        // Thân xe - gradient
        LinearGradient bodyGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3498db")),
                new Stop(1, Color.web("#2471a3")));
        gc.setFill(bodyGrad);
        gc.fillRoundRect(-w / 2, -h / 2, w, h, 8, 6);

        // Viền xe
        gc.setStroke(Color.web("#1a5276"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(-w / 2, -h / 2, w, h, 8, 6);

        // Kính chắn gió trước (phía phải = mũi xe khi dir=0)
        gc.setFill(Color.web("#aed6f1", 0.8));
        gc.fillRoundRect(w / 2 - 10, -h / 2 + 3, 7, h - 6, 3, 3);

        // Kính chắn gió sau
        gc.setFill(Color.web("#85c1e9", 0.6));
        gc.fillRoundRect(-w / 2 + 3, -h / 2 + 3, 6, h - 6, 3, 3);

        // Nóc xe (cabin)
        gc.setFill(Color.web("#5dade2", 0.5));
        gc.fillRoundRect(-w / 2 + 10, -h / 2 + 3, w - 22, h - 6, 4, 4);

        // Bánh xe (4 bánh)
        gc.setFill(Color.web("#2c3e50"));
        gc.fillRoundRect(-w / 2 + 3, -h / 2 - 2, 7, 4, 2, 2);   // Trước-trái
        gc.fillRoundRect(-w / 2 + 3, h / 2 - 2, 7, 4, 2, 2);     // Trước-phải
        gc.fillRoundRect(w / 2 - 10, -h / 2 - 2, 7, 4, 2, 2);    // Sau-trái
        gc.fillRoundRect(w / 2 - 10, h / 2 - 2, 7, 4, 2, 2);     // Sau-phải

        // Đèn pha (mũi xe) - sáng nhấp nháy nhẹ
        double brightness = 0.7 + 0.3 * Math.sin(animationFrame * 0.05);
        gc.setFill(Color.color(1, 1, 0.7, brightness));
        gc.fillOval(w / 2 - 3, -h / 2 + 2, 4, 4);   // Đèn trái
        gc.fillOval(w / 2 - 3, h / 2 - 6, 4, 4);     // Đèn phải

        // Đèn hậu (đuôi xe) — đỏ
        gc.setFill(Color.web("#e74c3c", 0.9));
        gc.fillOval(-w / 2 - 1, -h / 2 + 2, 3, 3);
        gc.fillOval(-w / 2 - 1, h / 2 - 5, 3, 3);
    }

    // ═══════════════════════════════════════════
    //  XE BUÝT (Bus)
    // ═══════════════════════════════════════════
    private void renderBus(GraphicsContext gc, Vehicle v) {
        double w = v.getWidth();
        double h = v.getHeight();

        // Thân xe buýt - gradient xanh lá đậm
        LinearGradient bodyGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#27ae60")),
                new Stop(1, Color.web("#1e8449")));
        gc.setFill(bodyGrad);
        gc.fillRoundRect(-w / 2, -h / 2, w, h, 6, 6);

        // Viền
        gc.setStroke(Color.web("#145a32"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(-w / 2, -h / 2, w, h, 6, 6);

        // Dải trang trí vàng ở giữa thân xe
        gc.setFill(Color.web("#f1c40f"));
        gc.fillRect(-w / 2 + 3, -1, w - 6, 2);

        // Cửa sổ hành khách (dãy cửa sổ hai bên)
        gc.setFill(Color.web("#d5f5e3", 0.85));
        int numWindows = (int) (w / 12);
        for (int i = 1; i < numWindows; i++) {
            double wx = -w / 2 + i * 10 + 2;
            gc.fillRoundRect(wx, -h / 2 + 3, 6, h / 2 - 5, 2, 2);  // Bên trên
            gc.fillRoundRect(wx, 2, 6, h / 2 - 5, 2, 2);            // Bên dưới
        }

        // Kính chắn gió mũi xe
        gc.setFill(Color.web("#abebc6", 0.9));
        gc.fillRoundRect(w / 2 - 8, -h / 2 + 2, 6, h - 4, 3, 3);

        // Bánh xe (6 bánh)
        gc.setFill(Color.web("#1c2833"));
        gc.fillRoundRect(-w / 2 + 5, -h / 2 - 3, 8, 5, 2, 2);
        gc.fillRoundRect(-w / 2 + 5, h / 2 - 2, 8, 5, 2, 2);
        gc.fillRoundRect(0, -h / 2 - 3, 8, 5, 2, 2);
        gc.fillRoundRect(0, h / 2 - 2, 8, 5, 2, 2);
        gc.fillRoundRect(w / 2 - 13, -h / 2 - 3, 8, 5, 2, 2);
        gc.fillRoundRect(w / 2 - 13, h / 2 - 2, 8, 5, 2, 2);

        // Đèn pha
        gc.setFill(Color.web("#f9e79f"));
        gc.fillOval(w / 2 - 3, -h / 2 + 2, 4, 5);
        gc.fillOval(w / 2 - 3, h / 2 - 7, 4, 5);

        // Đèn hậu đỏ
        gc.setFill(Color.web("#e74c3c"));
        gc.fillOval(-w / 2 - 1, -h / 2 + 3, 3, 4);
        gc.fillOval(-w / 2 - 1, h / 2 - 7, 3, 4);
    }

    // ═══════════════════════════════════════════
    //  XE MÁY (Motorbike)
    // ═══════════════════════════════════════════
    private void renderMotorbike(GraphicsContext gc, Vehicle v) {
        double w = v.getWidth();
        double h = v.getHeight();

        // Thân xe máy
        LinearGradient bodyGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#e67e22")),
                new Stop(1, Color.web("#d35400")));
        gc.setFill(bodyGrad);
        gc.fillRoundRect(-w / 2, -h / 2 + 1, w, h - 2, 10, 6);

        // Yên xe
        gc.setFill(Color.web("#2c3e50"));
        gc.fillRoundRect(-w / 2 + 4, -h / 2 + 2, w / 2, h - 4, 4, 4);

        // Tay lái (phía trước)
        gc.setStroke(Color.web("#7f8c8d"));
        gc.setLineWidth(2);
        gc.strokeLine(w / 2 - 5, -h / 2 - 1, w / 2 - 5, h / 2 + 1);

        // Bánh xe (2 bánh, lớn hơn tỉ lệ)
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillOval(-w / 2 - 1, -h / 2 + 1, 7, h - 2);    // Bánh sau
        gc.fillOval(w / 2 - 6, -h / 2 + 1, 7, h - 2);      // Bánh trước

        // Đèn pha nhỏ
        double blink = 0.6 + 0.4 * Math.sin(animationFrame * 0.08);
        gc.setFill(Color.color(1, 1, 0.8, blink));
        gc.fillOval(w / 2 - 2, h / 2 - 5, 3, 3);

        // Đèn hậu đỏ
        gc.setFill(Color.web("#c0392b", 0.9));
        gc.fillOval(-w / 2 - 1, h / 2 - 5, 3, 3);
    }

    // ═══════════════════════════════════════════
    //  XE CỨU THƯƠNG (Ambulance)
    // ═══════════════════════════════════════════
    private void renderAmbulance(GraphicsContext gc, Ambulance v) {
        double w = v.getWidth();
        double h = v.getHeight();
        boolean isEmergency = v.isPriorityVehicle();

        // Thân xe trắng
        LinearGradient bodyGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ecf0f1")),
                new Stop(1, Color.web("#bdc3c7")));
        gc.setFill(bodyGrad);
        gc.fillRoundRect(-w / 2, -h / 2, w, h, 6, 6);

        // Viền
        gc.setStroke(Color.web("#95a5a6"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(-w / 2, -h / 2, w, h, 6, 6);

        // Sọc đỏ ngang thân xe
        gc.setFill(Color.web("#e74c3c"));
        gc.fillRect(-w / 2 + 2, -2, w - 4, 4);

        // Chữ thập đỏ trên nóc xe
        gc.setFill(Color.web("#c0392b"));
        double crossX = -4, crossY = -h / 2 + 3;
        gc.fillRect(crossX - 1, crossY, 3, 8);    // Dọc
        gc.fillRect(crossX - 4, crossY + 2.5, 9, 3);  // Ngang

        // Kính chắn gió
        gc.setFill(Color.web("#aed6f1", 0.8));
        gc.fillRoundRect(w / 2 - 8, -h / 2 + 2, 6, h - 4, 3, 3);

        // Bánh xe
        gc.setFill(Color.web("#2c3e50"));
        gc.fillRoundRect(-w / 2 + 3, -h / 2 - 2, 7, 4, 2, 2);
        gc.fillRoundRect(-w / 2 + 3, h / 2 - 2, 7, 4, 2, 2);
        gc.fillRoundRect(w / 2 - 10, -h / 2 - 2, 7, 4, 2, 2);
        gc.fillRoundRect(w / 2 - 10, h / 2 - 2, 7, 4, 2, 2);

        // ĐÈN CHỚP — Animation nhấp nháy nếu là xe khẩn cấp
        if (isEmergency) {
            boolean flashOn = (animationFrame % 12) < 6;
            // Đèn xanh + đỏ xen kẽ trên nóc xe
            gc.setEffect(new Glow(0.8));
            gc.setFill(flashOn ? Color.RED : Color.color(1, 0, 0, 0.2));
            gc.fillOval(-w / 2 + 5, -h / 2 - 4, 6, 6);
            gc.setFill(!flashOn ? Color.DEEPSKYBLUE : Color.color(0, 0.5, 1, 0.2));
            gc.fillOval(w / 2 - 11, -h / 2 - 4, 6, 6);

            // Đèn đối diện
            gc.setFill(flashOn ? Color.RED : Color.color(1, 0, 0, 0.2));
            gc.fillOval(-w / 2 + 5, h / 2 - 2, 6, 6);
            gc.setFill(!flashOn ? Color.DEEPSKYBLUE : Color.color(0, 0.5, 1, 0.2));
            gc.fillOval(w / 2 - 11, h / 2 - 2, 6, 6);
            gc.setEffect(null);
        }

        // Đèn pha
        gc.setFill(Color.web("#f9e79f"));
        gc.fillOval(w / 2 - 2, -h / 2 + 2, 3, 4);
        gc.fillOval(w / 2 - 2, h / 2 - 6, 3, 4);
    }

    // ═══════════════════════════════════════════
    //  XE CỨU HỎA (FireTruck)
    // ═══════════════════════════════════════════
    private void renderFireTruck(GraphicsContext gc, Vehicle v) {
        double w = v.getWidth();
        double h = v.getHeight();

        // Thân xe đỏ
        LinearGradient bodyGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#e74c3c")),
                new Stop(1, Color.web("#c0392b")));
        gc.setFill(bodyGrad);
        gc.fillRoundRect(-w / 2, -h / 2, w, h, 6, 6);

        // Viền
        gc.setStroke(Color.web("#922b21"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(-w / 2, -h / 2, w, h, 6, 6);

        // Sọc trắng phản quang
        gc.setFill(Color.web("#ecf0f1", 0.7));
        gc.fillRect(-w / 2 + 2, -1.5, w - 4, 3);

        // Cabin phía trước
        gc.setFill(Color.web("#f5b7b1", 0.7));
        gc.fillRoundRect(w / 2 - 15, -h / 2 + 2, 13, h - 4, 4, 4);

        // Kính cabin
        gc.setFill(Color.web("#aed6f1", 0.85));
        gc.fillRoundRect(w / 2 - 8, -h / 2 + 3, 6, h - 6, 3, 3);

        // Thang cứu hỏa trên nóc (sọc chéo vàng-bạc)
        gc.setStroke(Color.web("#f1c40f"));
        gc.setLineWidth(1.5);
        gc.strokeLine(-w / 2 + 5, -h / 2 + 5, w / 2 - 18, -h / 2 + 5);
        gc.strokeLine(-w / 2 + 5, h / 2 - 5, w / 2 - 18, h / 2 - 5);
        gc.setStroke(Color.web("#bdc3c7"));
        gc.setLineWidth(0.8);
        for (double lx = -w / 2 + 8; lx < w / 2 - 18; lx += 6) {
            gc.strokeLine(lx, -h / 2 + 4, lx, h / 2 - 4);
        }

        // Bánh xe (6 bánh)
        gc.setFill(Color.web("#1c2833"));
        gc.fillRoundRect(-w / 2 + 3, -h / 2 - 3, 8, 5, 2, 2);
        gc.fillRoundRect(-w / 2 + 3, h / 2 - 2, 8, 5, 2, 2);
        gc.fillRoundRect(5, -h / 2 - 3, 8, 5, 2, 2);
        gc.fillRoundRect(5, h / 2 - 2, 8, 5, 2, 2);
        gc.fillRoundRect(w / 2 - 13, -h / 2 - 3, 8, 5, 2, 2);
        gc.fillRoundRect(w / 2 - 13, h / 2 - 2, 8, 5, 2, 2);

        // ĐÈN CHỚP — Xen kẽ đỏ/xanh dương chớp nhanh
        boolean flashOn = (animationFrame % 10) < 5;
        gc.setEffect(new Glow(0.9));
        gc.setFill(flashOn ? Color.RED : Color.color(1, 0, 0, 0.15));
        gc.fillOval(-w / 2 + 3, -h / 2 - 5, 7, 7);
        gc.setFill(!flashOn ? Color.DODGERBLUE : Color.color(0.1, 0.4, 1, 0.15));
        gc.fillOval(w / 2 - 10, -h / 2 - 5, 7, 7);

        gc.setFill(flashOn ? Color.RED : Color.color(1, 0, 0, 0.15));
        gc.fillOval(-w / 2 + 3, h / 2 - 2, 7, 7);
        gc.setFill(!flashOn ? Color.DODGERBLUE : Color.color(0.1, 0.4, 1, 0.15));
        gc.fillOval(w / 2 - 10, h / 2 - 2, 7, 7);
        gc.setEffect(null);

        // Đèn pha
        gc.setFill(Color.web("#f9e79f"));
        gc.fillOval(w / 2 - 2, -h / 2 + 3, 4, 5);
        gc.fillOval(w / 2 - 2, h / 2 - 8, 4, 5);
    }
}
