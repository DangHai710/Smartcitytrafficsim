package vn.edu.hust.traffic.model.vehicle;

import vn.edu.hust.traffic.model.map.TrafficLight;
import java.util.List;

/**
 * Lớp cha cho mọi loại phương tiện giao thông.
 */
public abstract class Vehicle implements vn.edu.hust.traffic.base.Renderable, vn.edu.hust.traffic.base.Updatable {
    protected String id;
    protected double x, y, speed, direction, width, height;
    protected boolean isPriorityVehicle;
    protected double baseSpeed;
    protected boolean passedStopLine = false;
    protected double distToStopLine = Double.MAX_VALUE;
    protected int turnIntention = 0; // 0: Thẳng, 1: Rẽ Trái, 2: Rẽ Phải
    protected boolean hasTurned = false;

    public Vehicle(String id, double x, double y, double speed, double direction, double width, double height, boolean isPriorityVehicle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.baseSpeed = speed;
        this.direction = direction;
        this.width = width;
        this.height = height;
        this.isPriorityVehicle = isPriorityVehicle;
    }

    public abstract void movePhysically(double dt);

    public void setTurnIntention(int turn) {
        this.turnIntention = turn;
    }

    public void update(double dt, List<Vehicle> allVehicles, List<TrafficLight> lights, int screenWidth, int screenHeight) {
        double safeDistance = (width > 30) ? 45 : 25;
        final double SLOW_ZONE = 80.0;
        boolean shouldStop = false;
        double currentTargetSpeed = baseSpeed;

        int lightIdx = getLightIdx(direction);
        TrafficLight light = lights.get(lightIdx);

        double stopX_LTR = screenWidth  / 2.0 - 100;
        double stopX_RTL = screenWidth  / 2.0 + 100;
        double stopY_TTB = screenHeight / 2.0 - 100;
        double stopY_BTT = screenHeight / 2.0 + 100;

        distToStopLine = Double.MAX_VALUE;

        switch (lightIdx) {
            case 0: 
                distToStopLine = stopX_LTR - (x + width / 2.0);
                passedStopLine = (x + width / 2.0) >= stopX_LTR;
                break;
            case 1: 
                distToStopLine = (x - width / 2.0) - stopX_RTL;
                passedStopLine = (x - width / 2.0) <= stopX_RTL;
                break;
            case 2: 
                distToStopLine = stopY_TTB - (y + width / 2.0);
                passedStopLine = (y + width / 2.0) >= stopY_TTB;
                break;
            case 3: 
                distToStopLine = (y - width / 2.0) - stopY_BTT;
                passedStopLine = (y - width / 2.0) <= stopY_BTT;
                break;
        }

        // BƯỚC 0.5: Kiểm tra và thực hiện rẽ nếu xe đang ở giữa ngã tư
        double cx = screenWidth / 2.0;
        double cy = screenHeight / 2.0;
        
        if (!hasTurned && turnIntention != 0 && passedStopLine) {
            boolean readyToTurn = false;
            double targetCoord = 0;
            
            // Tính toán tọa độ chính xác để sau khi bẻ lái, xe nằm đúng boong giữa làn
            if (turnIntention == 1) { // Rẽ trái (vào làn priority offset 15)
                if (lightIdx == 0) { targetCoord = cx + 15; readyToTurn = (x >= targetCoord); }
                else if (lightIdx == 1) { targetCoord = cx - 15; readyToTurn = (x <= targetCoord); }
                else if (lightIdx == 2) { targetCoord = cy + 15; readyToTurn = (y >= targetCoord); }
                else if (lightIdx == 3) { targetCoord = cy - 15; readyToTurn = (y <= targetCoord); }
            } else if (turnIntention == 2) { // Rẽ phải (vào làn bike offset 65)
                if (lightIdx == 0) { targetCoord = cx - 65; readyToTurn = (x >= targetCoord); }
                else if (lightIdx == 1) { targetCoord = cx + 65; readyToTurn = (x <= targetCoord); }
                else if (lightIdx == 2) { targetCoord = cy - 65; readyToTurn = (y >= targetCoord); }
                else if (lightIdx == 3) { targetCoord = cy + 65; readyToTurn = (y <= targetCoord); }
            }
            
            if (readyToTurn) {
                // Chỉnh thẳng góc tọa độ trục cũ vào đúng quỹ đạo trục mới
                if (lightIdx == 0 || lightIdx == 1) this.x = targetCoord;
                else this.y = targetCoord;

                if (turnIntention == 1) { // Rẽ trái
                    if (lightIdx == 0) direction = -Math.PI/2;
                    else if (lightIdx == 1) direction = Math.PI/2;
                    else if (lightIdx == 2) direction = 0;
                    else if (lightIdx == 3) direction = Math.PI;
                } else if (turnIntention == 2) { // Rẽ phải
                    if (lightIdx == 0) direction = Math.PI/2;
                    else if (lightIdx == 1) direction = -Math.PI/2;
                    else if (lightIdx == 2) direction = Math.PI;
                    else if (lightIdx == 3) direction = 0;
                }
                hasTurned = true;
                lightIdx = getLightIdx(direction); // Cập nhật ngay lightIdx mới
                light = lights.get(lightIdx);
            }
        }

        // BƯỚC 1: Quét tìm cứu thương khẩn cấp (Emergency Ambulance) để tiến hành Flee Mode
        boolean isFleeing = false;

        for (Vehicle other : allVehicles) {
            if (other.isPriorityVehicle && other != this) {
                int otherLightIdx = getLightIdx(other.direction);
                
                // 1. Phân tích bỏ chạy (FLEE) nếu xe cấp cứu ở NGAY SAU LƯNG trong cùng làn 
                if (otherLightIdx == lightIdx) {
                    boolean sameLane = (lightIdx < 2)
                            ? Math.abs(other.y - this.y) < 12
                            : Math.abs(other.x - this.x) < 12;
                    if (sameLane) {
                        double behindDist = -1;
                        if (lightIdx == 0) behindDist = this.x - other.x;
                        else if (lightIdx == 1) behindDist = other.x - this.x;
                        else if (lightIdx == 2) behindDist = this.y - other.y;
                        else if (lightIdx == 3) behindDist = other.y - this.y;
                        
                        // Cứu thương đang sát đít (từ 0 đến 400px) -> Lách sang lề phải để nhường đường!
                        if (behindDist > 0 && behindDist < 400) {
                            isFleeing = true;
                            
                            // Logic lách nhường đường (dạt ra lề phải của chiều đi)
                            double shiftSpeed = 40.0 * dt;
                            if (lightIdx == 0) {
                                if (this.y < cy + 85) this.y += shiftSpeed;
                            } else if (lightIdx == 1) {
                                if (this.y > cy - 85) this.y -= shiftSpeed;
                            } else if (lightIdx == 2) {
                                if (this.x > cx - 85) this.x -= shiftSpeed;
                            } else if (lightIdx == 3) {
                                if (this.x < cx + 85) this.x += shiftSpeed;
                            }
                        }
                    }
                }
            }
        }

        // BƯỚC 2: Check đèn (Chỉ kiểm tra đèn bình thường, bỏ đi cơ chế ép dừng máy móc)
        boolean mustStopByLight = false;
        if (!isPriorityVehicle && !isFleeing) {
            if (light.getState() == TrafficLight.State.RED || (light.getState() == TrafficLight.State.YELLOW && !passedStopLine)) {
                mustStopByLight = true;
            }
        }

        // BƯỚC 3: Dừng mềm trước vạch theo đèn tín hiệu
        if (mustStopByLight && !passedStopLine) {
            if (distToStopLine <= 0) {
                shouldStop = true;
            } else if (distToStopLine < SLOW_ZONE) {
                double ratio = distToStopLine / SLOW_ZONE;
                currentTargetSpeed = baseSpeed * ratio;
                if (distToStopLine < 5) shouldStop = true;
            }
        }

        // BƯỚC 4: Rà phanh động (Dynamic Right of Way) - Thuật toán giao tuyến quang học
        for (Vehicle other : allVehicles) {
            if (other == this) continue;
            
            int otherLightIdx = getLightIdx(other.direction);
            boolean sameAxis = (lightIdx < 2 && otherLightIdx < 2) || (lightIdx >= 2 && otherLightIdx >= 2);
            
            // Chỉ xét 2 xe có quỹ đạo chéo góc (cross-traffic)
            if (!sameAxis) {
                // Xác định tọa độ giao cắt của 2 quỹ đạo
                double intersectX, intersectY;
                if (lightIdx < 2) {
                    intersectY = this.y;
                    intersectX = other.x;
                } else {
                    intersectX = this.x;
                    intersectY = other.y;
                }

                // Khoảng cách từ mũi xe ĐẾN điểm giao cắt (dương = chưa tới, âm = đi lố qua rồi)
                double myDistToIntersect = 0;
                if (lightIdx == 0) myDistToIntersect = intersectX - this.x;
                else if (lightIdx == 1) myDistToIntersect = this.x - intersectX;
                else if (lightIdx == 2) myDistToIntersect = intersectY - this.y;
                else if (lightIdx == 3) myDistToIntersect = this.y - intersectY;

                double otherDistToIntersect = 0;
                if (otherLightIdx == 0) otherDistToIntersect = intersectX - other.x;
                else if (otherLightIdx == 1) otherDistToIntersect = other.x - intersectX;
                else if (otherLightIdx == 2) otherDistToIntersect = intersectY - other.y;
                else if (otherLightIdx == 3) otherDistToIntersect = other.y - intersectY;

                double CLEARANCE = 40.0;

                // 1. Phá băng giao thông: Ai ĐÃ qua rồi thì thoát ra khỏi vùng ảnh hưởng tuyệt đối!
                // Giải quyết yêu cầu: "đối với các xe mà đã đi qua rồi k cần phải dừng hay giảm tốc độ với nó"
                if (myDistToIntersect < -CLEARANCE || otherDistToIntersect < -CLEARANCE) {
                    continue; 
                }

                // 2. Chống lác (Deadlock anti-freeze): Bỏ qua xe ngoan ngoãn đỗ bên đường chờ đèn đỏ
                if (other.speed < 0.5 && otherDistToIntersect > 80) {
                    continue;
                }

                // So sánh phân nhánh ưu tiên
                boolean iMustYield = false;
                boolean myPri = this.isPriorityVehicle || isFleeing;
                boolean otherPri = other.isPriorityVehicle;
                
                // Trạng thái đè mặt ngã tư:
                // Nếu mình đã rúc sâu vào điểm giao cắt (đang chắn đường ngang)
                boolean iAmBlocking = (myDistToIntersect > -CLEARANCE && myDistToIntersect < 30);
                boolean otherIsBlocking = (otherDistToIntersect > -CLEARANCE && otherDistToIntersect < 30);

                // Ưu tiên hiện trạng trường vật lý: (Xe nào chắn giữa đường thì luôn đi trước, kể cả xe đang chắn là xe thường gặp cấp cứu)
                // "xe buýt đã đi ngang qua đầu xe khẩn cấp thì không cần dừng lại, khẩn cấp phải nhường"
                if (iAmBlocking && !otherIsBlocking) {
                    iMustYield = false;
                } else if (!iAmBlocking && otherIsBlocking) {
                    iMustYield = true;
                } else {
                    // Chưa xe nào đè vạch giao cắt: Đấu độ ưu tiên dựa trên cự ly tiếp cận
                    double myEffective = myDistToIntersect - (myPri ? 120 : 0);
                    double otherEffective = otherDistToIntersect - (otherPri ? 120 : 0);

                    // Đèn xanh ưu tiên qua trước so với đèn đỏ (trừ xe ưu tiên)
                    if (light.getState() == TrafficLight.State.GREEN && !otherPri) {
                        myEffective -= 1000;
                    }
                    if (lights.get(otherLightIdx).getState() == TrafficLight.State.GREEN && !myPri) {
                        otherEffective -= 1000;
                    }

                    // Ai còn cách xa (hoặc kém ưu tiên) thì sẽ "tự cảm thấy" cần nhường
                    if (myEffective > otherEffective) {
                        iMustYield = true;
                    } else if (Math.abs(myEffective - otherEffective) < 1.0) {
                        iMustYield = (this.id.compareTo(other.id) > 0);
                    }
                }
                
                // Tuân lệnh giảm tốc
                if (iMustYield) {
                    // Tránh xe xa tít chân trời cũng phanh, chỉ phanh khi xe khẩn cấp đe doạ tiến vào 
                    if (otherDistToIntersect < 200) {
                        if (myDistToIntersect < 45) {
                            shouldStop = true; // Chạm chân đến ngã tư thì lết bánh hẳn
                        } else {
                            // "vẫn có thể di chuyển nhưng với tốc độ an toàn và mức khoảng cách hợp lý"
                            double ratio = (myDistToIntersect - 45) / 100.0;
                            currentTargetSpeed = Math.min(currentTargetSpeed, baseSpeed * Math.max(0, ratio));
                        }
                    }
                }
            }
        }

        // BƯỚC 5: Giữ khoảng cách
        for (Vehicle other : allVehicles) {
            if (other == this) continue;

            int otherLightIdx = getLightIdx(other.direction);
            boolean sameAxis = (lightIdx < 2 && otherLightIdx < 2) || (lightIdx >= 2 && otherLightIdx >= 2);
            if (!sameAxis) continue;

            boolean sameLane = (lightIdx < 2)
                    ? Math.abs(other.y - this.y) < 12
                    : Math.abs(other.x - this.x) < 12;
            if (!sameLane) continue;

            double gap = Double.MAX_VALUE;
            if (lightIdx == 0 && other.x > x)
                gap = (other.x - other.width / 2.0)  - (x + width / 2.0);
            else if (lightIdx == 1 && other.x < x)
                gap = (x - width / 2.0) - (other.x + other.width / 2.0);
            else if (lightIdx == 2 && other.y > y)
                gap = (other.y - other.width / 2.0) - (y + width / 2.0);
            else if (lightIdx == 3 && other.y < y)
                gap = (y - width / 2.0) - (other.y + other.width / 2.0);

            if (gap < safeDistance) {
                double minGap = 5.0; 
                if (gap <= minGap) {
                    shouldStop = true;
                } else {
                    double ratio = (gap - minGap) / (safeDistance - minGap);
                    ratio = Math.max(0, Math.min(1, ratio));
                    double followSpeed = other.speed + (baseSpeed - other.speed) * ratio;
                    currentTargetSpeed = Math.min(currentTargetSpeed, followSpeed);
                }
            }
        }

        // BƯỚC 6: Áp tốc độ vật lý
        if (shouldStop) {
            this.speed = 0;
        } else {
            boolean inIntersection = Math.hypot(x - cx, y - cy) < 180;
            boolean isClear = (Math.abs(currentTargetSpeed - baseSpeed) < 1.0);

            if (isPriorityVehicle) {
                // Tăng bứt tốc ngã tư (Intersection Clear Burst)
                if (inIntersection && isClear) {
                    // Không có chướng ngại vật -> Xe khẩn cấp rít ga phóng 1.5x tốc độ qua ngã tư
                    this.speed = currentTargetSpeed * 1.5; 
                } else if (inIntersection && !isClear) {
                    // Đang vướng xe phải nhường -> Chay chuẩn theo biểu đồ rà phanh
                    this.speed = currentTargetSpeed; 
                } else {
                    // Trên đường thẳng ngoài ngã tư -> Duy trì tốc độ tuần tra 1.3x
                    this.speed = currentTargetSpeed * 1.3;
                }
            } else if (isFleeing) {
                // Xe dân sự đang hoảng loạn lách đường, vọt lẹ hơn tí nếu trống
                this.speed = isClear ? currentTargetSpeed * 1.2 : currentTargetSpeed;
            } else {
                // Xe dân sự đang đèn xanh đi qua ngã tư thì tăng tốc để thoát nhanh, tránh bị đì
                if (inIntersection && isClear && light.getState() == TrafficLight.State.GREEN) {
                    this.speed = currentTargetSpeed * 1.4; // Tăng 40% tốc độ
                } else if (inIntersection && isClear && passedStopLine) {
                    this.speed = currentTargetSpeed * 1.2; // Lỡ dở đèn vàng thì rít nhanh cho qua
                } else {
                    this.speed = currentTargetSpeed;
                }
            }
            movePhysically(dt);
        }
    }

    private int getLightIdx(double dir) {
        if      (Math.abs(dir - 0)           < 0.1) return 0;
        else if (Math.abs(dir - Math.PI)     < 0.1) return 1;
        else if (Math.abs(dir - Math.PI / 2) < 0.1) return 2;
        else if (Math.abs(dir + Math.PI / 2) < 0.1) return 3;
        return 0;
    }

    @Override public void update() {}
    public String getId() { return id; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public double getDirection() { return direction; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isPriorityVehicle() { return isPriorityVehicle; }
}
