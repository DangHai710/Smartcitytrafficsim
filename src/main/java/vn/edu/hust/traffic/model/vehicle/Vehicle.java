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
    protected final int originalLightIdx;

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
        this.originalLightIdx = getLightIdx(direction);
    }

    public abstract void movePhysically(double dt);

    public void setTurnIntention(int turn) {
        this.turnIntention = turn;
    }

    /**
     * Lấy nửa chiều dài xe theo trục di chuyển.
     * width = chiều dài xe (luôn là chiều dài lớn nhất).
     */
    private double getHalfLength() {
        return width / 2.0;
    }


    public void update(double dt, List<Vehicle> allVehicles, List<TrafficLight> lights, int screenWidth, int screenHeight) {
        double safeDistance = (width > 30) ? 50 : 30;
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
        double hl = getHalfLength();

        switch (lightIdx) {
            case 0: 
                distToStopLine = stopX_LTR - (x + hl);
                passedStopLine = (x + hl) >= stopX_LTR;
                break;
            case 1: 
                distToStopLine = (x - hl) - stopX_RTL;
                passedStopLine = (x - hl) <= stopX_RTL;
                break;
            case 2: 
                distToStopLine = stopY_TTB - (y + hl);
                passedStopLine = (y + hl) >= stopY_TTB;
                break;
            case 3: 
                distToStopLine = (y - hl) - stopY_BTT;
                passedStopLine = (y - hl) <= stopY_BTT;
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
                    // Mở rộng threshold theo kích thước xe — xe lớn cần threshold rộng hơn
                    double fleeThreshold = Math.max(16, (this.height + other.height) / 2.0);
                    boolean sameLane = (lightIdx < 2)
                            ? Math.abs(other.y - this.y) < fleeThreshold
                            : Math.abs(other.x - this.x) < fleeThreshold;
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

        // BƯỚC 2: Check đèn — dùng đèn PHÙ HỢP với ý định rẽ của xe
        //   turnIntention==0 (thẳng): xem đèn thẳng
        //   turnIntention==1 (rẽ trái): xem đèn mũi tên rẽ trái
        //   turnIntention==2 (rẽ phải): luôn GREEN (Right Turn on Red)
        TrafficLight.State myEffectiveLight = light.getStateForTurn(turnIntention, hasTurned);
        boolean isRightTurnOnRed = (turnIntention == 2 && !hasTurned);
        boolean mustStopByLight = false;
        if (!isPriorityVehicle && !isFleeing) {
            if (myEffectiveLight == TrafficLight.State.RED || 
                (myEffectiveLight == TrafficLight.State.YELLOW && !passedStopLine)) {
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

        // BƯỚC 3.5: Xe rẽ phải khi đèn đỏ — giảm tốc cẩn thận trước ngã tư, không dừng hẳn
        if (isRightTurnOnRed && !passedStopLine && light.getState() != TrafficLight.State.GREEN) {
            double cautionSpeed = baseSpeed * 0.4;
            if (distToStopLine < SLOW_ZONE && distToStopLine > 0) {
                double ratio = distToStopLine / SLOW_ZONE;
                currentTargetSpeed = Math.min(currentTargetSpeed, cautionSpeed * ratio + cautionSpeed * 0.3);
            } else if (distToStopLine <= 0) {
                currentTargetSpeed = Math.min(currentTargetSpeed, cautionSpeed);
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

                // Clearance phụ thuộc kích thước xe — xe lớn cần vùng lớn hơn
                double CLEARANCE = Math.max(40.0, Math.max(this.width, other.width) * 0.7);

                // 1. Phá băng giao thông: Ai ĐÃ qua rồi thì thoát ra khỏi vùng ảnh hưởng tuyệt đối!
                if (myDistToIntersect < -CLEARANCE || otherDistToIntersect < -CLEARANCE) {
                    continue; 
                }

                // KHÔNG BAO GIỜ xung đột với xe xuất phát từ cùng một nhánh đường
                if (this.originalLightIdx == other.originalLightIdx) {
                    continue;
                }

                // 2. Chống lác (Deadlock anti-freeze): Bỏ qua xe đỗ chờ đèn đỏ
                //    - Xe đứng yên VÀ còn xa ngã tư (>50px) → chắc chắn đang chờ đèn
                //    - Xe đứng yên VÀ đèn của nó đang đỏ → đang tuân thủ đèn, không phải mối đe dọa
                if (other.speed < 0.5 && otherDistToIntersect > 50) {
                    continue;
                }
                TrafficLight otherLight = lights.get(otherLightIdx);
                TrafficLight.State otherEffState = otherLight.getStateForTurn(other.turnIntention, other.hasTurned);
                if (other.speed < 0.5 && otherEffState == TrafficLight.State.RED && otherDistToIntersect > 0) {
                    continue; // Xe đang dừng đèn đỏ đúng luật → không cần nhường
                }

                // So sánh phân nhánh ưu tiên
                boolean iMustYield = false;
                boolean myPri = this.isPriorityVehicle || isFleeing;
                boolean otherPri = other.isPriorityVehicle;
                
                // Trạng thái đè mặt ngã tư (Giải phóng ngã tư):
                // LUẬT MỚI: Xe ĐÃ VÀO ngã tư (vượt qua vạch dừng) được ưu tiên TUYỆT ĐỐI để dọn đường
                // Xe vừa có đèn xanh PHẢI CHỜ xe vừa dính đèn đỏ đi nốt qua ngã tư.
                boolean iAmClearing = (this.passedStopLine && myDistToIntersect > -CLEARANCE);
                boolean otherIsClearing = (other.passedStopLine && otherDistToIntersect > -CLEARANCE);

                // Ưu tiên hiện trạng trường vật lý:
                // Nếu xe kia đang "dọn đường", ta chưa vào ngã tư thì phải nhường tuyệt đối!
                if (iAmClearing && !otherIsClearing) {
                    iMustYield = false;
                } else if (!iAmClearing && otherIsClearing) {
                    iMustYield = true;
                } else {
                    // Cả 2 cùng chưa vào hoặc cùng vào rồi (hiếm): Đấu độ ưu tiên dựa trên cự ly tiếp cận
                    double myEffective = myDistToIntersect - (myPri ? 120 : 0);
                    double otherEffective = otherDistToIntersect - (otherPri ? 120 : 0);

                    // Đèn xanh ưu tiên qua trước — dùng myEffectiveLight thay vì light.getState()
                    if (myEffectiveLight == TrafficLight.State.GREEN && !otherPri) {
                        myEffective -= 1000;
                    }
                    TrafficLight otherTL = lights.get(otherLightIdx);
                    TrafficLight.State otherEffLight = otherTL.getStateForTurn(other.turnIntention, other.hasTurned);
                    if (otherEffLight == TrafficLight.State.GREEN && !myPri) {
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

            // Xử lý L-shaped following: Hai xe cùng nguồn, cùng hướng rẽ, nhưng xe kia đã rẽ
            boolean isLShapedFollow = false;
            if (this.originalLightIdx == other.originalLightIdx && this.turnIntention == other.turnIntention && this.turnIntention != 0) {
                if (!this.hasTurned && other.hasTurned) {
                    isLShapedFollow = true;
                }
            }

            int otherLightIdx = getLightIdx(other.direction);
            boolean sameAxis = (lightIdx < 2 && otherLightIdx < 2) || (lightIdx >= 2 && otherLightIdx >= 2);
            
            if (!sameAxis && !isLShapedFollow) continue;

            // sameLane threshold mở rộng theo kích thước xe — tránh miss khi xe lớn hoặc flee
            double laneThreshold = Math.max(16, (this.height + other.height) / 2.0);
            boolean sameLane = true;
            if (sameAxis) {
                sameLane = (lightIdx < 2)
                        ? Math.abs(other.y - this.y) < laneThreshold
                        : Math.abs(other.x - this.x) < laneThreshold;
            }
            if (!sameLane) continue;

            double gap = Double.MAX_VALUE;
            double myHL = this.getHalfLength();
            double otherHL = other.getHalfLength();

            if (isLShapedFollow) {
                // L-shape gap = khoảng cách của tôi đến điểm rẽ + khoảng cách của xe kia tính từ điểm rẽ
                // Điểm rẽ của cả 2 xe là như nhau!
                double myDistToTurn = 0;
                double otherDistFromTurn = 0;
                double targetCoord = 0;
                
                if (turnIntention == 1) { // Left
                    if (originalLightIdx == 0) targetCoord = cx + 15;
                    else if (originalLightIdx == 1) targetCoord = cx - 15;
                    else if (originalLightIdx == 2) targetCoord = cy + 15;
                    else if (originalLightIdx == 3) targetCoord = cy - 15;
                } else if (turnIntention == 2) { // Right
                    if (originalLightIdx == 0) targetCoord = cx - 65;
                    else if (originalLightIdx == 1) targetCoord = cx + 65;
                    else if (originalLightIdx == 2) targetCoord = cy - 65;
                    else if (originalLightIdx == 3) targetCoord = cy + 65;
                }
                
                // My distance TO turn point (chưa rẽ nên myDistToTurn phải > 0)
                if (originalLightIdx == 0) myDistToTurn = targetCoord - (this.x + myHL);
                else if (originalLightIdx == 1) myDistToTurn = (this.x - myHL) - targetCoord;
                else if (originalLightIdx == 2) myDistToTurn = targetCoord - (this.y + myHL);
                else if (originalLightIdx == 3) myDistToTurn = (this.y - myHL) - targetCoord;

                // Other distance FROM turn point (đã rẽ nên dist phải > 0)
                // Lấy tọa độ xuất phát trên trục mới của xe đã rẽ
                double otherOriginPathCoord = 0;
                if (originalLightIdx == 0) otherOriginPathCoord = cy + (turnIntention == 1 ? 15 : 65);
                else if (originalLightIdx == 1) otherOriginPathCoord = cy - (turnIntention == 1 ? 15 : 65);
                else if (originalLightIdx == 2) otherOriginPathCoord = cx - (turnIntention == 1 ? 15 : 65);
                else if (originalLightIdx == 3) otherOriginPathCoord = cx + (turnIntention == 1 ? 15 : 65);

                if (otherLightIdx == 0) otherDistFromTurn = (other.x - otherHL) - otherOriginPathCoord;
                else if (otherLightIdx == 1) otherDistFromTurn = otherOriginPathCoord - (other.x + otherHL);
                else if (otherLightIdx == 2) otherDistFromTurn = (other.y - otherHL) - otherOriginPathCoord;
                else if (otherLightIdx == 3) otherDistFromTurn = otherOriginPathCoord - (other.y + otherHL);

                if (myDistToTurn >= -this.getHalfLength() && otherDistFromTurn >= 0) {
                    gap = myDistToTurn + otherDistFromTurn;
                }

            } else {
                // Straight follow
                if (lightIdx == 0 && other.x > x)
                    gap = (other.x - otherHL)  - (x + myHL);
                else if (lightIdx == 1 && other.x < x)
                    gap = (x - myHL) - (other.x + otherHL);
                else if (lightIdx == 2 && other.y > y)
                    gap = (other.y - otherHL) - (y + myHL);
                else if (lightIdx == 3 && other.y < y)
                    gap = (y - myHL) - (other.y + otherHL);
            }

            if (gap < safeDistance) {
                double minGap = 8.0; 
                if (gap <= minGap) {
                    shouldStop = true;
                } else {
                    double ratio = (gap - minGap) / (safeDistance - minGap);
                    ratio = Math.max(0, Math.min(1, ratio));
                    double followSpeed = other.speed * ratio;
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

    public int getTurnIntention() { return turnIntention; }
    public boolean hasTurned() { return hasTurned; }
    public double getDistToStopLine() { return distToStopLine; }

    public int getLightIdx(double dir) {
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
