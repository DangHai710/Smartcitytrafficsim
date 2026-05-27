package vn.edu.hust.traffic.model.vehicle;

import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.map.Intersection;
import vn.edu.hust.traffic.model.map.RoundaboutIntersection;
import java.util.List;

/**
 * Lớp cha cho mọi loại phương tiện giao thông.
 */
public abstract class Vehicle implements vn.edu.hust.traffic.base.Renderable, vn.edu.hust.traffic.base.Updatable {
    private static final double ROAD_HALF_WIDTH = 80.0;
    private static final double ROUNDABOUT_ENTRY_RADIUS = 180.0;
    private static final double ROUNDABOUT_CAPTURE_DISTANCE = 260.0;
    private static final double ROUNDABOUT_MIN_DRIVE_RADIUS = 112.0;
    private static final double ROUNDABOUT_MAX_DRIVE_RADIUS = 166.0;
    private static final double ROUNDABOUT_LANE_BLEND_RATE = 8.0;
    private static final double ROUNDABOUT_RADIUS_BLEND_RATE = 1.6;
    private static final double ROUNDABOUT_HEADING_BLEND_RATE = 60.0;
    private static final double ROUNDABOUT_EXIT_TURN_DURATION = 0.55;
    private static final double SMOOTH_TURN_DURATION = 0.38;
    private static final double INTERSECTION_CLEAR_RADIUS = 180.0;
    private static final double CLEARING_MIN_SPEED_FACTOR = 0.45;
    private static final double TURN_EXIT_CLEARANCE_DURATION = 0.85;
    private static final double TURN_EXIT_MIN_SPEED_FACTOR = 0.28;
    private static final double YIELD_LANE_CHANGE_SPEED = 150.0;
    private static final double ROUNDABOUT_CRAWL_MIN_SPEED_FACTOR = 0.22;
    private static long intersectionEntryCounter = 0;
    private static final double[] STANDARD_LANE_OFFSETS = { 15.0, 40.0, 65.0 };
    private static final double[] ROUNDABOUT_LANE_OFFSETS = { 15.0, 40.0, 65.0 };

    protected String id;
    protected double x, y, speed, direction, width, height;
    protected boolean isPriorityVehicle;
    protected double baseSpeed;
    protected boolean passedStopLine = false;
    protected double distToStopLine = Double.MAX_VALUE;
    protected int turnIntention = 0; // 0: Thẳng, 1: Rẽ Trái, 2: Rẽ Phải
    protected boolean hasTurned = false;
    protected final int originalLightIdx;
    protected String activeIntersectionId = null;
    protected int activeIntersectionEntryLightIdx = -1;
    protected long activeIntersectionEntryOrder = Long.MAX_VALUE;
    protected boolean isTurningDiagonally = false;
    protected double diagonalTurnCenterX = 0.0;
    protected double diagonalTurnCenterY = 0.0;
    protected boolean isTurningSmoothly = false;
    protected double smoothTurnStartX = 0.0;
    protected double smoothTurnStartY = 0.0;
    protected double smoothTurnEndX = 0.0;
    protected double smoothTurnEndY = 0.0;
    protected double smoothTurnStartDirection = 0.0;
    protected double smoothTurnEndDirection = 0.0;
    protected double smoothTurnElapsed = 0.0;
    protected double smoothTurnDuration = SMOOTH_TURN_DURATION;
    protected boolean smoothTurnCompletesTurn = true;
    protected double turnExitClearanceTime = 0.0;
    protected boolean overtakingSlowVehicle = false;
    protected double overtakeOriginalLaneOffset = 0.0;
    protected double overtakeTargetLaneOffset = 0.0;
    protected int overtakeLightIdx = -1;
    protected String overtakeIntersectionId = null;
    protected String overtakeVehicleId = null;
    protected boolean yieldingToPriorityVehicle = false;
    protected double yieldTargetLaneOffset = 0.0;
    protected int yieldLightIdx = -1;
    protected String yieldIntersectionId = null;
    protected String yieldPriorityVehicleId = null;
    protected boolean bypassingTurningVehicle = false;
    protected double bypassTargetLaneOffset = 0.0;
    protected int bypassLightIdx = -1;
    protected String bypassIntersectionId = null;
    protected String bypassVehicleId = null;

    // Roundabout routing and state fields
    protected int targetExitIndex = -1;
    public boolean insideRoundabout = false;
    public double roundaboutAngle = 0.0;
    protected boolean exitedRoundabout = false;
    protected int spawnSourceIndex = -1;
    protected double laneOffsetVal = 40.0;

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


    private Intersection getTargetIntersection(List<Intersection> intersections) {
        Intersection target = null;
        double minPositiveDist = Double.MAX_VALUE;
        double hl = getHalfLength();

        for (Intersection inter : intersections) {
            if (inter instanceof RoundaboutIntersection) {
                RoundaboutIntersection roundabout = (RoundaboutIntersection) inter;
                if (!exitedRoundabout
                        && (insideRoundabout
                                || isInsideRoundaboutBody(roundabout)
                                || isOnRoundaboutApproach(roundabout))) {
                    return inter;
                }
                continue;
            }
            double stopX_LTR = inter.getX() - 120;
            double stopX_RTL = inter.getX() + 120;
            double stopY_TTB = inter.getY() - 120;
            double stopY_BTT = inter.getY() + 120;

            int lightIdx = isTurningDiagonally ? originalLightIdx : getLightIdx(direction);
            if (!isTurningDiagonally && !isAlignedWithIntersectionRoad(inter, lightIdx)) {
                continue;
            }

            double dist = Double.MAX_VALUE;

            switch (lightIdx) {
                case 0: dist = stopX_LTR - (x + hl); break;
                case 1: dist = (x - hl) - stopX_RTL; break;
                case 2: dist = stopY_TTB - (y + hl); break;
                case 3: dist = (y - hl) - stopY_BTT; break;
            }

            // Đang ở trong ngã tư (đã qua vạch dừng nhưng chưa thoát hẳn, bán kính ngã tư ~200)
            if (dist <= 0 && dist > -200) {
                return inter;
            }
            // Đang tiến tới ngã tư
            if (dist > 0 && dist < minPositiveDist) {
                minPositiveDist = dist;
                target = inter;
            }
        }
        return target;
    }

    public void update(double dt, List<Vehicle> allVehicles, List<Intersection> intersections, int screenWidth, int screenHeight) {
        double safeDistance = (width > 30) ? 50 : 30;
        final double SLOW_ZONE = 80.0;
        boolean shouldStop = false;
        boolean hardSameLaneBlockAhead = false;
        double currentTargetSpeed = baseSpeed;
        boolean forcingTurnExit = hasTurned && turnExitClearanceTime > 0.0;
        turnExitClearanceTime = Math.max(0.0, turnExitClearanceTime - Math.max(0.0, dt));

        if (continueSmoothTurn(dt, allVehicles, intersections)) {
            return;
        }

        if (continueDiagonalRightTurn(dt, allVehicles)) {
            return;
        }

        Intersection targetInter = getTargetIntersection(intersections);
        if (targetInter == null) {
            activeIntersectionId = null;
            activeIntersectionEntryLightIdx = -1;
            activeIntersectionEntryOrder = Long.MAX_VALUE;
            resetOvertakeState();
            resetYieldState();
            resetTurningBypassState();
            this.speed = baseSpeed;
            movePhysically(dt);
            if (isTurningDiagonally) {
                finishDiagonalRightTurnIfNeeded(diagonalTurnCenterX, diagonalTurnCenterY);
            }
            return;
        }

        if (targetInter instanceof RoundaboutIntersection) {
            resetOvertakeState();
            resetYieldState();
            resetTurningBypassState();
            updateRoundabout(dt, allVehicles, (RoundaboutIntersection) targetInter);
            return;
        }

        beginIntersectionIfNeeded(targetInter);
        double cx = targetInter.getX();
        double cy = targetInter.getY();
        int lightIdx = getLightIdx(direction);
        int entryLightIdx = getIntersectionEntryLightIdx();

        // BẢO VỆ NGÃ 3: RTL không rẽ trái xuống Nam, LTR không rẽ phải xuống Nam, TTB không đi thẳng xuống Nam
        if (targetInter instanceof vn.edu.hust.traffic.model.map.ThreeWayIntersection
                && !hasTurned && !isTurningDiagonally) {
            if (entryLightIdx == 0 && turnIntention == 2) { // LTR (đi Đông) không thể rẽ phải (Nam)
                turnIntention = Math.random() < 0.5 ? 0 : 1;
            } else if (entryLightIdx == 1 && turnIntention == 1) { // RTL (đi Tây) không thể rẽ trái (Nam)
                turnIntention = Math.random() < 0.5 ? 0 : 2;
            } else if (entryLightIdx == 2 && turnIntention == 0) { // TTB (đi Nam) không thể đi thẳng (Nam)
                turnIntention = Math.random() < 0.5 ? 1 : 2;
            }
        }

        TrafficLight light = null;
        if (targetInter instanceof vn.edu.hust.traffic.model.map.CrossIntersection) {
            light = targetInter.getLights().get(lightIdx);
        } else if (targetInter instanceof vn.edu.hust.traffic.model.map.ThreeWayIntersection
                && !hasTurned && !isTurningDiagonally) {
            light = ((vn.edu.hust.traffic.model.map.ThreeWayIntersection)targetInter).getLightForDirection(direction);
        }
        if (light == null) {
            this.speed = baseSpeed;
            movePhysically(dt);
            return;
        }

        double stopX_LTR = cx - 120;
        double stopX_RTL = cx + 120;
        double stopY_TTB = cy - 120;
        double stopY_BTT = cy + 120;

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
        if (forcingTurnExit) {
            passedStopLine = true;
            distToStopLine = -1.0;
        }
        markIntersectionEntryIfNeeded(passedStopLine);

        // BƯỚC 0.5: Kiểm tra và thực hiện rẽ nếu xe đang ở giữa ngã tư

        // THÊM MỚI: QUỸ ĐẠO RẼ PHẢI CHÉO GÓC (VÀO ĐƯỜNG RẼ TẮT)
        if (!hasTurned && turnIntention == 2) {
            double TURN_DIST = 233.0;
            double END_LANE = 65.0;

            if (!isTurningDiagonally) {
                boolean readyToDiagonal = false;
                if (entryLightIdx == 0) readyToDiagonal = (x >= cx - TURN_DIST);
                else if (entryLightIdx == 1) readyToDiagonal = (x <= cx + TURN_DIST);
                else if (entryLightIdx == 2) readyToDiagonal = (y >= cy - TURN_DIST);
                else if (entryLightIdx == 3) readyToDiagonal = (y <= cy + TURN_DIST);

                if (readyToDiagonal) {
                    isTurningDiagonally = true;
                    diagonalTurnCenterX = cx;
                    diagonalTurnCenterY = cy;
                    passedStopLine = true; // Bỏ qua đèn đỏ vì làn rẽ phải luôn thông
                    double targetX = x;
                    double targetY = y;
                    double targetDirection = direction;
                    double advance = Math.min(32.0, Math.max(14.0, baseSpeed * 0.22));
                    if (entryLightIdx == 0) {
                        targetX = Math.max(x, cx - TURN_DIST) + Math.cos(Math.PI / 4) * advance;
                        targetY = y + Math.sin(Math.PI / 4) * advance;
                        targetDirection = Math.PI / 4;
                    } else if (entryLightIdx == 1) {
                        targetX = Math.min(x, cx + TURN_DIST) + Math.cos(-Math.PI * 3 / 4) * advance;
                        targetY = y + Math.sin(-Math.PI * 3 / 4) * advance;
                        targetDirection = -Math.PI * 3 / 4;
                    } else if (entryLightIdx == 2) {
                        targetX = x + Math.cos(Math.PI * 3 / 4) * advance;
                        targetY = Math.max(y, cy - TURN_DIST) + Math.sin(Math.PI * 3 / 4) * advance;
                        targetDirection = Math.PI * 3 / 4;
                    } else if (entryLightIdx == 3) {
                        targetX = x + Math.cos(-Math.PI / 4) * advance;
                        targetY = Math.min(y, cy + TURN_DIST) + Math.sin(-Math.PI / 4) * advance;
                        targetDirection = -Math.PI / 4;
                    }
                    startSmoothTurn(targetX, targetY, targetDirection, false, 0.22);
                    return;
                }
            }

            if (isTurningDiagonally) {
                boolean endDiagonal = false;
                if (entryLightIdx == 0) endDiagonal = (x >= cx - END_LANE);
                else if (entryLightIdx == 1) endDiagonal = (x <= cx + END_LANE);
                else if (entryLightIdx == 2) endDiagonal = (y >= cy - END_LANE);
                else if (entryLightIdx == 3) endDiagonal = (y <= cy + END_LANE);

                if (endDiagonal) {
                    finishDiagonalRightTurnIfNeeded(cx, cy);
                    if (isTurningSmoothly) {
                        return;
                    }

                    lightIdx = getLightIdx(direction); // Cập nhật lại tín hiệu đèn sau khi nắn thẳng trục
                }
            }
        }

        // RẼ TRÁI Ở GIỮA NGÃ TƯ
        if (!hasTurned && turnIntention == 1 && passedStopLine) {
            boolean readyToTurn = false;
            double targetCoord = 0;

            // Tính toán tọa độ chính xác để sau khi bẻ lái, xe nằm đúng boong giữa làn
            if (turnIntention == 1) { // Rẽ trái (vào làn priority offset 15)
                if (entryLightIdx == 0) { targetCoord = cx + 15; readyToTurn = (x >= targetCoord); }
                else if (entryLightIdx == 1) { targetCoord = cx - 15; readyToTurn = (x <= targetCoord); }
                else if (entryLightIdx == 2) { targetCoord = cy + 15; readyToTurn = (y >= targetCoord); }
                else if (entryLightIdx == 3) { targetCoord = cy - 15; readyToTurn = (y <= targetCoord); }
            }

            if (readyToTurn) {
                // Chỉnh thẳng góc tọa độ trục cũ vào đúng quỹ đạo trục mới
                double targetX = x;
                double targetY = y;
                double targetDirection = direction;
                if (entryLightIdx == 0 || entryLightIdx == 1) targetX = targetCoord;
                else targetY = targetCoord;

                if (turnIntention == 1) { // Rẽ trái
                    if (entryLightIdx == 0) targetDirection = -Math.PI/2;
                    else if (entryLightIdx == 1) targetDirection = Math.PI/2;
                    else if (entryLightIdx == 2) targetDirection = 0;
                    else if (entryLightIdx == 3) targetDirection = Math.PI;
                }
                startSmoothTurn(targetX, targetY, targetDirection, true, SMOOTH_TURN_DURATION);
                return;
            }
        }

        movePriorityToLeastBusyLaneIfRedQueueAhead(allVehicles, intersections, targetInter, lightIdx, dt);

        // BƯỚC 1: Quét tìm cứu thương khẩn cấp (Emergency Ambulance) để tiến hành Flee Mode
        boolean isFleeing = false;
        boolean yieldingThisUpdate = false;

        for (Vehicle other : allVehicles) {
            if (other.isPriorityVehicle && other != this) {
                if (!this.isPriorityVehicle
                        && !this.passedStopLine
                        && isPriorityApproachingSameIntersection(other, targetInter, intersections)) {
                    if (distToStopLine <= 45.0) {
                        shouldStop = true;
                    } else if (distToStopLine < 160.0) {
                        currentTargetSpeed = Math.min(currentTargetSpeed,
                                baseSpeed * Math.max(0.15, distToStopLine / 160.0));
                    }
                }

                int otherLightIdx = getLightIdx(other.direction);

                // 1. Nhường đường nếu xe ưu tiên đang áp sát phía sau trên cùng hướng tiếp cận.
                if (!this.isPriorityVehicle
                        && !this.passedStopLine
                        && otherLightIdx == lightIdx
                        && isSameApproachToIntersection(other, intersections, targetInter, lightIdx)) {
                    double behindDist = -longitudinalDistanceAhead(lightIdx, other.x, other.y);
                    if (behindDist > 0.0 && behindDist < 420.0
                            && (isContinuingYieldForPriority(targetInter, lightIdx, other)
                                    || isBlockingPriorityLane(targetInter, lightIdx, other))) {
                        isFleeing = true;
                        yieldingThisUpdate = true;
                        shouldStop = false;
                        currentTargetSpeed = Math.max(currentTargetSpeed, baseSpeed * 0.65);

                        double targetOffset = stableYieldLaneOffset(allVehicles, intersections, targetInter,
                                lightIdx, other);
                        moveTowardStandardLane(targetInter, lightIdx, targetOffset, dt, YIELD_LANE_CHANGE_SPEED);
                    }
                }
            }
        }
        if (!yieldingThisUpdate && continueYieldLaneChangeToTargetIfNeeded(
                allVehicles, intersections, targetInter, lightIdx, dt)) {
            isFleeing = true;
            yieldingThisUpdate = true;
            shouldStop = false;
            currentTargetSpeed = Math.max(currentTargetSpeed, baseSpeed * 0.65);
        }
        if (!yieldingThisUpdate) {
            resetYieldState();
        }

        // BƯỚC 2: Check đèn — dùng đèn PHÙ HỢP với ý định rẽ của xe
        //   turnIntention==0 (thẳng): xem đèn thẳng
        //   turnIntention==1 (rẽ trái): xem đèn mũi tên rẽ trái
        //   turnIntention==2 (rẽ phải): luôn GREEN (Right Turn on Red)
        TrafficLight.State myEffectiveLight = light.getStateForTurn(turnIntention, hasTurned);
        boolean isRightTurnOnRed = (turnIntention == 2 && !hasTurned);
        boolean mustStopByLight = false;
        if (!isPriorityVehicle && !isFleeing && !forcingTurnExit) {
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

        if (updateNormalOvertakeIfNeeded(allVehicles, intersections, targetInter, lightIdx, dt,
                mustStopByLight, isFleeing)) {
            currentTargetSpeed = Math.max(currentTargetSpeed, baseSpeed * 0.95);
        }
        boolean bypassingTurningBlocker = updateTurningVehicleBypassIfNeeded(
                allVehicles, intersections, targetInter, lightIdx, dt);
        if (bypassingTurningBlocker) {
            shouldStop = false;
            currentTargetSpeed = Math.max(currentTargetSpeed, baseSpeed * (isPriorityVehicle ? 0.95 : 0.70));
        }

        // BƯỚC 4: Rà phanh động (Dynamic Right of Way) - Thuật toán giao tuyến quang học
        for (Vehicle other : allVehicles) {
            if (other == this) continue;
            if (forcingTurnExit) continue;
            if (bypassingTurningBlocker && isActiveTurningBypassBlocker(other, targetInter, lightIdx)) {
                continue;
            }

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
                TrafficLight otherLight = null;
                Intersection otherTarget = other.getTargetIntersection(intersections);
                if (otherTarget instanceof vn.edu.hust.traffic.model.map.CrossIntersection) {
                    otherLight = otherTarget.getLights().get(otherLightIdx);
                } else if (otherTarget instanceof vn.edu.hust.traffic.model.map.ThreeWayIntersection) {
                    otherLight = ((vn.edu.hust.traffic.model.map.ThreeWayIntersection)otherTarget).getLightForDirection(other.direction);
                }

                if (otherLight != null) {
                    TrafficLight.State otherEffState = otherLight.getStateForTurn(other.turnIntention, other.hasTurned);
                    if (other.speed < 0.5 && otherEffState == TrafficLight.State.RED && otherDistToIntersect > 0) {
                        continue; // Xe đang dừng đèn đỏ đúng luật → không cần nhường
                    }
                }

                // So sánh phân nhánh ưu tiên
                boolean iMustYield = false;
                boolean myPri = this.isPriorityVehicle;
                boolean otherPri = other.isPriorityVehicle;

                // Trạng thái đè mặt ngã tư (Giải phóng ngã tư):
                // LUẬT MỚI: Xe ĐÃ VÀO ngã tư (vượt qua vạch dừng) được ưu tiên TUYỆT ĐỐI để dọn đường
                // Xe vừa có đèn xanh PHẢI CHỜ xe vừa dính đèn đỏ đi nốt qua ngã tư.
                boolean iAmClearing = (this.passedStopLine && myDistToIntersect > -CLEARANCE);
                boolean otherIsClearing = (other.passedStopLine && otherDistToIntersect > -CLEARANCE);

                // Ưu tiên hiện trạng trường vật lý:
                // Nếu xe kia đang "dọn đường", ta chưa vào ngã tư thì phải nhường tuyệt đối!
                if (!myPri && otherPri) {
                    iMustYield = true;
                } else if (myPri && !otherPri) {
                    iMustYield = false;
                } else if (!myPri && !otherPri && iAmClearing && otherIsClearing
                        && this.activeIntersectionEntryOrder != Long.MAX_VALUE
                        && other.activeIntersectionEntryOrder != Long.MAX_VALUE) {
                    if (this.activeIntersectionEntryOrder != other.activeIntersectionEntryOrder) {
                        iMustYield = this.activeIntersectionEntryOrder > other.activeIntersectionEntryOrder;
                    } else {
                        iMustYield = (this.id.compareTo(other.id) > 0);
                    }
                } else if (iAmClearing && !otherIsClearing) {
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
                    if (otherLight != null) {
                        TrafficLight.State otherEffLight = otherLight.getStateForTurn(other.turnIntention, other.hasTurned);
                        if (otherEffLight == TrafficLight.State.GREEN && !myPri) {
                            otherEffective -= 1000;
                        }
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
                    double yieldDistance = otherPri ? 320.0 : 200.0;
                    if (otherDistToIntersect < yieldDistance) {
                        if (myDistToIntersect < 45) {
                            currentTargetSpeed = Math.min(currentTargetSpeed,
                                    baseSpeed * (otherPri ? 0.2 : CLEARING_MIN_SPEED_FACTOR));
                            shouldStop = true; // Chạm chân đến ngã tư thì lết bánh hẳn
                        } else {
                            // "vẫn có thể di chuyển nhưng với tốc độ an toàn và mức khoảng cách hợp lý"
                            double ratio = (myDistToIntersect - 45) / (otherPri ? 160.0 : 100.0);
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
            if (bypassingTurningBlocker && isActiveTurningBypassBlocker(other, targetInter, lightIdx)) {
                continue;
            }
            boolean isLShapedFollow = false;
            if (this.originalLightIdx == other.originalLightIdx && this.turnIntention == other.turnIntention && this.turnIntention != 0) {
                if (!this.hasTurned && (other.hasTurned || other.isTurningSmoothly || other.isTurningDiagonally)) {
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

            // Xử lý collision khi cả 2 xe cùng đang đi trên đường chéo
            if (this.isTurningDiagonally && other.isTurningDiagonally && this.originalLightIdx == other.originalLightIdx) {
                sameAxis = true;
                sameLane = true;
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

                if (other.isTurningSmoothly || other.isTurningDiagonally) {
                    otherDistFromTurn = 0.0;
                } else if (otherLightIdx == 0) otherDistFromTurn = (other.x - otherHL) - otherOriginPathCoord;
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
                    hardSameLaneBlockAhead = true;
                } else {
                    double ratio = (gap - minGap) / (safeDistance - minGap);
                    ratio = Math.max(0, Math.min(1, ratio));
                    double followSpeed = other.speed * ratio;
                    if (forcingTurnExit) {
                        followSpeed = Math.max(followSpeed, baseSpeed * TURN_EXIT_MIN_SPEED_FACTOR);
                    }
                    currentTargetSpeed = Math.min(currentTargetSpeed, followSpeed);
                }
            }
        }

        // BƯỚC 6: Áp tốc độ vật lý
        boolean inIntersection = Math.hypot(x - cx, y - cy) < INTERSECTION_CLEAR_RADIUS;
        boolean clearingIntersection = passedStopLine && inIntersection;
        if (clearingIntersection && shouldStop && !(forcingTurnExit && hardSameLaneBlockAhead)) {
            shouldStop = false;
            currentTargetSpeed = Math.max(currentTargetSpeed, baseSpeed * CLEARING_MIN_SPEED_FACTOR);
        }

        if (shouldStop) {
            this.speed = 0;
        } else {
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
            if (!forcingTurnExit) {
                this.speed = limitSpeedForPredictedIntersectionCollision(dt, this.speed, allVehicles, targetInter);
            } else {
                this.speed = Math.max(this.speed, baseSpeed * TURN_EXIT_MIN_SPEED_FACTOR);
            }
            movePhysically(dt);
            finishDiagonalRightTurnIfNeeded(cx, cy);
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

    private void updateRoundabout(double dt, List<Vehicle> allVehicles, RoundaboutIntersection roundabout) {
        double cx = roundabout.getX();
        double cy = roundabout.getY();
        double[] roadAngles = roundabout.getRoadAngles();
        if (roadAngles.length == 0) {
            movePhysically(dt);
            return;
        }

        // 1. Initialize roundabout target exit and source index if not set
        if (spawnSourceIndex == -1) {
            spawnSourceIndex = getApproachRoadIndex(x, y, direction, cx, cy, roadAngles);

            // Calculate our offset from the road axis to know which lane we spawned in
            double dx = x - cx;
            double dy = y - cy;
            double roadTheta = roadAngles[spawnSourceIndex];
            double calculatedOffset = dx * Math.sin(roadTheta) - dy * Math.cos(roadTheta);
            laneOffsetVal = normalizeRoundaboutLaneOffset(Math.abs(calculatedOffset));

            if (targetExitIndex < 0 || targetExitIndex >= roadAngles.length) {
                java.util.Random rand = new java.util.Random();
                int exitCount = roadAngles.length;
                if (exitCount <= 1) {
                    targetExitIndex = spawnSourceIndex;
                } else {
                    do {
                        targetExitIndex = rand.nextInt(exitCount);
                    } while (targetExitIndex == spawnSourceIndex);
                }
            }
        }

        double thetaSource = roadAngles[spawnSourceIndex];
        double thetaTarget = roadAngles[targetExitIndex];
        double entryMergeDistance = roundaboutLaneMergeDistance();
        double targetExitAngle = getRoundaboutExitLaneAngle(thetaTarget);

        if (!insideRoundabout && isInsideRoundaboutBody(roundabout)) {
            insideRoundabout = true;
            roundaboutAngle = Math.atan2(y - cy, x - cx);
            double currentR = Math.hypot(x - cx, y - cy);
            double targetR = clampRoundaboutRadius(currentR);
            double newR = (currentR < ROUNDABOUT_MIN_DRIVE_RADIUS || speed <= 0.1)
                    ? targetR
                    : currentR + (targetR - currentR) * blendAlpha(dt, ROUNDABOUT_RADIUS_BLEND_RATE);
            x = cx + newR * Math.cos(roundaboutAngle);
            y = cy + newR * Math.sin(roundaboutAngle);
        }

        double safeDistance = (width > 30) ? 50 : 30;
        double currentTargetSpeed = baseSpeed;
        boolean shouldStop = false;
        boolean hardRoundaboutBlock = false;
        boolean speedAlreadyApplied = false;

        if (!insideRoundabout) {
            // APPROACHING THE ROUNDABOUT (yielding at distance 180)
            double dx = x - cx;
            double dy = y - cy;
            double d = dx * Math.cos(thetaSource) + dy * Math.sin(thetaSource);

            double stopDist = d - ROUNDABOUT_ENTRY_RADIUS;
            passedStopLine = d <= ROUNDABOUT_ENTRY_RADIUS;

            // Yield to circulating vehicles that will reach this entry while moving counter-clockwise.
            boolean yieldRequired = false;
            for (Vehicle other : allVehicles) {
                if (other != this && other.insideRoundabout) {
                    double diff = counterClockwiseDistance(other.roundaboutAngle, thetaSource);
                    double priorityWindow = (!isPriorityVehicle && other.isPriorityVehicle) ? 1.0 : 0.6;
                    if (diff > 0 && diff < priorityWindow) {
                        yieldRequired = true;
                        break;
                    }
                }
                if (!isPriorityVehicle
                        && other != this
                        && other.isPriorityVehicle
                        && !other.insideRoundabout
                        && !other.exitedRoundabout
                        && other.isOnRoundaboutApproach(roundabout)) {
                    yieldRequired = true;
                    break;
                }
            }

            if (yieldRequired && !passedStopLine) {
                if (stopDist <= 0) {
                    shouldStop = true;
                } else if (stopDist < 80.0) {
                    double ratio = stopDist / 80.0;
                    currentTargetSpeed = baseSpeed * ratio;
                    if (stopDist < 5) shouldStop = true;
                }
            }

            for (Vehicle other : allVehicles) {
                if (other == this || other.insideRoundabout || other.exitedRoundabout) {
                    continue;
                }

                double otherDx = other.x - cx;
                double otherDy = other.y - cy;
                double otherD = otherDx * Math.cos(thetaSource) + otherDy * Math.sin(thetaSource);
                double otherLateral = Math.abs(otherDx * Math.sin(thetaSource) - otherDy * Math.cos(thetaSource));
                double otherHeadingDiff = Math.abs(normalizeAngle(other.direction - (thetaSource + Math.PI)));
                if (otherD >= d || otherLateral > ROAD_HALF_WIDTH || otherHeadingDiff > 0.65) {
                    continue;
                }

                double gap = (d - otherD) - getHalfLength() - other.getHalfLength();
                if (gap < safeDistance) {
                    if (gap <= 8.0) {
                        shouldStop = true;
                    } else {
                        double ratio = (gap - 8.0) / (safeDistance - 8.0);
                        currentTargetSpeed = Math.min(currentTargetSpeed, other.speed * Math.max(0.0, ratio));
                    }
                }
            }

            if (d <= entryMergeDistance) {
                moveTowardRoundaboutEntryLane(cx, cy, thetaSource, Math.max(0.0, d), dt);
                insideRoundabout = true;
                roundaboutAngle = Math.atan2(y - cy, x - cx);
            } else {
                direction = interpolateAngle(direction, thetaSource + Math.PI, blendAlpha(dt, ROUNDABOUT_HEADING_BLEND_RATE));
                if (d <= ROUNDABOUT_ENTRY_RADIUS) {
                    moveTowardRoundaboutEntryLane(cx, cy, thetaSource, d, dt);
                }
            }
        }

        if (insideRoundabout) {
            // INSIDE THE ROUNDABOUT (3 concentric lanes: Outer=165, Middle=140, Inner=115)
            int exitsRemaining = getExitsRemaining(roundaboutAngle, thetaTarget, roadAngles);
            double targetR = 165.0;
            if (exitsRemaining > 2) {
                targetR = 115.0; // Inner lane
            } else if (exitsRemaining == 2) {
                targetR = 140.0; // Middle lane
            } else {
                targetR = 165.0; // Outer lane
            }

            double currentR = clampRoundaboutRadius(Math.hypot(x - cx, y - cy));
            double newR = clampRoundaboutRadius(
                    currentR + (targetR - currentR) * blendAlpha(dt, ROUNDABOUT_RADIUS_BLEND_RATE));

            for (Vehicle other : allVehicles) {
                if (other != this && other.insideRoundabout) {
                    double angleDiff = counterClockwiseDistance(roundaboutAngle, other.roundaboutAngle);
                    double otherR = clampRoundaboutRadius(Math.hypot(other.x - cx, other.y - cy));
                    double laneDistance = Math.abs(otherR - currentR);
                    if (angleDiff > 0 && angleDiff < 0.55 && laneDistance < 32.0) {
                        double gap = Math.min(newR, otherR) * angleDiff - getHalfLength() - other.getHalfLength();
                        if (gap < safeDistance) {
                            double minGap = 8.0;
                            if (gap <= minGap) {
                                if (gap <= 0.0) {
                                    hardRoundaboutBlock = true;
                                    shouldStop = true;
                                } else {
                                    currentTargetSpeed = Math.min(currentTargetSpeed,
                                            Math.max(other.speed, baseSpeed * ROUNDABOUT_CRAWL_MIN_SPEED_FACTOR));
                                }
                            } else {
                                double ratio = (gap - minGap) / (safeDistance - minGap);
                                currentTargetSpeed = Math.min(currentTargetSpeed, other.speed * ratio);
                            }
                        }
                    }
                }
            }

            if (shouldStop && hardRoundaboutBlock) {
                speed = 0;
                newR = currentR;
            } else {
                double adjustedTargetSpeed = shouldStop
                        ? Math.max(currentTargetSpeed, baseSpeed * ROUNDABOUT_CRAWL_MIN_SPEED_FACTOR)
                        : currentTargetSpeed;
                speed = speed + (adjustedTargetSpeed - speed) * 0.1;
                if (shouldStop) {
                    speed = Math.max(speed, baseSpeed * ROUNDABOUT_CRAWL_MIN_SPEED_FACTOR);
                }
            }
            speedAlreadyApplied = true;

            double previousAngle = roundaboutAngle;
            double omega = speed / newR;
            roundaboutAngle = normalizeAngle(roundaboutAngle - omega * dt);

            x = cx + newR * Math.cos(roundaboutAngle);
            y = cy + newR * Math.sin(roundaboutAngle);
            direction = interpolateAngle(direction, roundaboutAngle - Math.PI / 2.0,
                    blendAlpha(dt, ROUNDABOUT_HEADING_BLEND_RATE));

            // Exit condition
            if (hasReachedCounterClockwiseExit(previousAngle, roundaboutAngle, targetExitAngle)) {
                insideRoundabout = false;
                exitedRoundabout = true;
                roundaboutAngle = thetaTarget;
                double targetX = cx + entryMergeDistance * Math.cos(thetaTarget) - laneOffsetVal * Math.sin(thetaTarget);
                double targetY = cy + entryMergeDistance * Math.sin(thetaTarget) + laneOffsetVal * Math.cos(thetaTarget);
                startSmoothTurn(targetX, targetY, thetaTarget, true, ROUNDABOUT_EXIT_TURN_DURATION);
                return;
            }
        } else if (exitedRoundabout) {
            // EXITING THE ROUNDABOUT
            double dx = x - cx;
            double dy = y - cy;
            double d = dx * Math.cos(thetaTarget) + dy * Math.sin(thetaTarget);

            direction = thetaTarget;
            placeOnRoundaboutExitLane(cx, cy, thetaTarget, d + speed * dt);
        }

        if (!speedAlreadyApplied) {
            if (shouldStop) {
                speed = 0;
            } else {
                speed = speed + (currentTargetSpeed - speed) * 0.1;
            }
        }

        if (!insideRoundabout && !exitedRoundabout) {
            movePhysically(dt);
            double dx = x - cx;
            double dy = y - cy;
            double d = dx * Math.cos(thetaSource) + dy * Math.sin(thetaSource);
            if (d <= ROUNDABOUT_ENTRY_RADIUS) {
                placeOnRoundaboutEntryLane(cx, cy, thetaSource, Math.max(0.0, d));
            }
        }
    }

    private void placeOnRoundaboutEntryLane(double cx, double cy, double theta, double distanceFromCenter) {
        x = cx + distanceFromCenter * Math.cos(theta) + laneOffsetVal * Math.sin(theta);
        y = cy + distanceFromCenter * Math.sin(theta) - laneOffsetVal * Math.cos(theta);
    }

    private void moveTowardRoundaboutEntryLane(double cx, double cy, double theta, double distanceFromCenter,
            double dt) {
        double targetX = cx + distanceFromCenter * Math.cos(theta) + laneOffsetVal * Math.sin(theta);
        double targetY = cy + distanceFromCenter * Math.sin(theta) - laneOffsetVal * Math.cos(theta);
        if (speed <= 0.1) {
            x = targetX;
            y = targetY;
            return;
        }

        double alpha = blendAlpha(dt, ROUNDABOUT_LANE_BLEND_RATE);
        x += (targetX - x) * alpha;
        y += (targetY - y) * alpha;
    }

    private void placeOnRoundaboutExitLane(double cx, double cy, double theta, double distanceFromCenter) {
        x = cx + distanceFromCenter * Math.cos(theta) - laneOffsetVal * Math.sin(theta);
        y = cy + distanceFromCenter * Math.sin(theta) + laneOffsetVal * Math.cos(theta);
    }

    private double normalizeRoundaboutLaneOffset(double offset) {
        double closest = ROUNDABOUT_LANE_OFFSETS[0];
        double minDistance = Math.abs(offset - closest);
        for (double laneOffset : ROUNDABOUT_LANE_OFFSETS) {
            double distance = Math.abs(offset - laneOffset);
            if (distance < minDistance) {
                closest = laneOffset;
                minDistance = distance;
            }
        }
        return closest;
    }

    private double clampRoundaboutRadius(double radius) {
        return Math.max(ROUNDABOUT_MIN_DRIVE_RADIUS, Math.min(ROUNDABOUT_MAX_DRIVE_RADIUS, radius));
    }

    private double roadCenterOffsetLimit() {
        return Math.max(0.0, ROAD_HALF_WIDTH - height / 2.0);
    }

    private void movePriorityToLeastBusyLaneIfRedQueueAhead(List<Vehicle> allVehicles,
            List<Intersection> intersections, Intersection intersection, int lightIdx, double dt) {
        if (!isPriorityVehicle || passedStopLine || hasTurned || isTurningDiagonally || isTurningSmoothly) {
            return;
        }
        if (!hasStoppedQueueAhead(allVehicles, intersections, intersection, lightIdx)) {
            return;
        }

        double targetOffset = leastBusyLaneOffset(allVehicles, intersections, intersection, lightIdx);
        moveTowardStandardLane(intersection, lightIdx, targetOffset, dt, 90.0);
    }

    private boolean updateNormalOvertakeIfNeeded(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, double dt, boolean mustStopByLight, boolean isFleeing) {
        if (isPriorityVehicle || isFleeing || mustStopByLight || passedStopLine || hasTurned
                || isTurningDiagonally || isTurningSmoothly || distToStopLine < 120.0
                || hasRedLightStoppedVehicleAhead(allVehicles, intersections, intersection, lightIdx)
                || hasPriorityVehicleNearSameIntersection(allVehicles, intersection, intersections)) {
            if (overtakingSlowVehicle) {
                tryReturnToOriginalLane(allVehicles, intersections, intersection, lightIdx, dt);
            }
            return overtakingSlowVehicle;
        }

        if (!overtakingSlowVehicle) {
            Vehicle slowVehicle = findSlowVehicleAheadForOvertake(allVehicles, intersections, intersection, lightIdx);
            if (slowVehicle == null) {
                return false;
            }

            double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
            int currentLaneIndex = nearestStandardLaneIndex(currentOffset);
            int targetLaneIndex = chooseSafeOvertakeLane(allVehicles, intersections, intersection, lightIdx,
                    currentLaneIndex);
            if (targetLaneIndex < 0) {
                return false;
            }

            overtakingSlowVehicle = true;
            overtakeOriginalLaneOffset = Math.min(STANDARD_LANE_OFFSETS[currentLaneIndex], roadCenterOffsetLimit());
            overtakeTargetLaneOffset = Math.min(STANDARD_LANE_OFFSETS[targetLaneIndex], roadCenterOffsetLimit());
            overtakeLightIdx = lightIdx;
            overtakeIntersectionId = intersection.getId();
            overtakeVehicleId = slowVehicle.id;
        }

        if (overtakeLightIdx != lightIdx || !intersection.getId().equals(overtakeIntersectionId)) {
            resetOvertakeState();
            return false;
        }

        if (hasPassedOvertakeTarget(allVehicles, lightIdx)) {
            tryReturnToOriginalLane(allVehicles, intersections, intersection, lightIdx, dt);
        } else {
            if (isLaneSafeForChange(allVehicles, intersections, intersection, lightIdx, overtakeTargetLaneOffset)) {
                moveTowardStandardLane(intersection, lightIdx, overtakeTargetLaneOffset, dt, 75.0);
            }
        }

        return overtakingSlowVehicle;
    }

    private Vehicle findSlowVehicleAheadForOvertake(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx) {
        Vehicle closest = null;
        double closestAhead = Double.MAX_VALUE;
        double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
        int currentLaneIndex = nearestStandardLaneIndex(currentOffset);

        for (Vehicle other : allVehicles) {
            if (other == this || other.isPriorityVehicle) {
                continue;
            }
            if (!isSameApproachToIntersection(other, intersections, intersection, lightIdx)) {
                continue;
            }
            double otherOffset = Math.abs(standardLaneOffset(intersection, lightIdx, other.x, other.y));
            if (nearestStandardLaneIndex(otherOffset) != currentLaneIndex) {
                continue;
            }

            double ahead = longitudinalDistanceAhead(lightIdx, other.x, other.y);
            if (ahead <= 0.0 || ahead > 170.0) {
                continue;
            }
            if (other.speed > baseSpeed * 0.72 && other.speed > speed - 12.0) {
                continue;
            }
            if (ahead < closestAhead) {
                closestAhead = ahead;
                closest = other;
            }
        }
        return closest;
    }

    private boolean hasRedLightStoppedVehicleAhead(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx) {
        for (Vehicle other : allVehicles) {
            if (other == this || other.isPriorityVehicle) {
                continue;
            }
            if (!isSameApproachToIntersection(other, intersections, intersection, lightIdx)) {
                continue;
            }

            double ahead = longitudinalDistanceAhead(lightIdx, other.x, other.y);
            if (ahead > 0.0 && ahead < 500.0 && isVehicleStoppedByRedLight(other, intersections)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVehicleStoppedByRedLight(Vehicle vehicle, List<Intersection> intersections) {
        if (vehicle.speed >= 2.0 || vehicle.passedStopLine) {
            return false;
        }

        Intersection target = vehicle.getTargetIntersection(intersections);
        if (target == null || target instanceof RoundaboutIntersection) {
            return false;
        }

        int lightIdx = vehicle.getLightIdx(vehicle.direction);
        TrafficLight targetLight = null;
        if (target instanceof vn.edu.hust.traffic.model.map.CrossIntersection) {
            targetLight = target.getLights().get(lightIdx);
        } else if (target instanceof vn.edu.hust.traffic.model.map.ThreeWayIntersection) {
            targetLight = ((vn.edu.hust.traffic.model.map.ThreeWayIntersection) target)
                    .getLightForDirection(vehicle.direction);
        }
        if (targetLight == null) {
            return false;
        }

        TrafficLight.State state = targetLight.getStateForTurn(vehicle.turnIntention, vehicle.hasTurned);
        return state == TrafficLight.State.RED || state == TrafficLight.State.YELLOW;
    }

    private int chooseSafeOvertakeLane(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, int currentLaneIndex) {
        int bestIndex = -1;
        int bestCount = Integer.MAX_VALUE;
        double bestMove = Double.MAX_VALUE;
        for (int i = 0; i < STANDARD_LANE_OFFSETS.length; i++) {
            if (i == currentLaneIndex || Math.abs(i - currentLaneIndex) != 1) {
                continue;
            }
            double offset = Math.min(STANDARD_LANE_OFFSETS[i], roadCenterOffsetLimit());
            if (!isLaneSafeForChange(allVehicles, intersections, intersection, lightIdx, offset)) {
                continue;
            }

            int count = countVehiclesInLaneWindow(allVehicles, intersections, intersection, lightIdx, offset);
            double move = Math.abs(i - currentLaneIndex);
            if (count < bestCount || (count == bestCount && move < bestMove)) {
                bestIndex = i;
                bestCount = count;
                bestMove = move;
            }
        }
        return bestIndex;
    }

    private boolean isLaneSafeForChange(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, double targetOffset) {
        double myProjectedLaneCoord = standardLaneCoordinate(intersection, lightIdx, targetOffset);
        double myLongitudinal = longitudinalCoordinate(lightIdx, x, y);
        for (Vehicle other : allVehicles) {
            if (other == this) {
                continue;
            }
            if (!isSameApproachToIntersection(other, intersections, intersection, lightIdx)) {
                continue;
            }

            double otherOffset = Math.abs(standardLaneOffset(intersection, lightIdx, other.x, other.y));
            if (Math.abs(otherOffset - targetOffset) > 14.0) {
                continue;
            }

            double otherLongitudinal = longitudinalCoordinate(lightIdx, other.x, other.y);
            double relative = signedLongitudinalDelta(lightIdx, myLongitudinal, otherLongitudinal);
            double requiredGap = Math.max(52.0, getHalfLength() + other.getHalfLength() + 24.0);
            if (other.isPriorityVehicle) {
                requiredGap += 90.0;
            }
            if (relative > -requiredGap && relative < requiredGap * 1.35) {
                return false;
            }

            double lateralDistance = lightIdx < 2
                    ? Math.abs(other.y - myProjectedLaneCoord)
                    : Math.abs(other.x - myProjectedLaneCoord);
            if (Math.abs(relative) < requiredGap * 1.8 && lateralDistance < Math.max(18.0, height + other.height)) {
                return false;
            }
        }
        return true;
    }

    private int countVehiclesInLaneWindow(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, double targetOffset) {
        int count = 0;
        for (Vehicle other : allVehicles) {
            if (other == this || !isSameApproachToIntersection(other, intersections, intersection, lightIdx)) {
                continue;
            }
            double offset = Math.abs(standardLaneOffset(intersection, lightIdx, other.x, other.y));
            if (Math.abs(offset - targetOffset) > 14.0) {
                continue;
            }
            double ahead = longitudinalDistanceAhead(lightIdx, other.x, other.y);
            if (ahead > -80.0 && ahead < 220.0) {
                count++;
            }
        }
        return count;
    }

    private boolean hasPassedOvertakeTarget(List<Vehicle> allVehicles, int lightIdx) {
        if (overtakeVehicleId == null) {
            return true;
        }
        for (Vehicle other : allVehicles) {
            if (!overtakeVehicleId.equals(other.id)) {
                continue;
            }
            double ahead = longitudinalDistanceAhead(lightIdx, other.x, other.y);
            return ahead < -(getHalfLength() + other.getHalfLength() + 30.0);
        }
        return true;
    }

    private void tryReturnToOriginalLane(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, double dt) {
        if (!isLaneSafeForChange(allVehicles, intersections, intersection, lightIdx, overtakeOriginalLaneOffset)) {
            return;
        }
        moveTowardStandardLane(intersection, lightIdx, overtakeOriginalLaneOffset, dt, 70.0);
        double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
        if (Math.abs(currentOffset - overtakeOriginalLaneOffset) < 2.0) {
            resetOvertakeState();
        }
    }

    private boolean hasPriorityVehicleNearSameIntersection(List<Vehicle> allVehicles, Intersection intersection,
            List<Intersection> intersections) {
        for (Vehicle other : allVehicles) {
            if (other != this && other.isPriorityVehicle
                    && isPriorityApproachingSameIntersection(other, intersection, intersections)) {
                return true;
            }
        }
        return false;
    }

    private double longitudinalCoordinate(int lightIdx, double px, double py) {
        return lightIdx < 2 ? px : py;
    }

    private double signedLongitudinalDelta(int lightIdx, double myLongitudinal, double otherLongitudinal) {
        if (lightIdx == 0 || lightIdx == 2) {
            return otherLongitudinal - myLongitudinal;
        }
        return myLongitudinal - otherLongitudinal;
    }

    private void resetOvertakeState() {
        overtakingSlowVehicle = false;
        overtakeOriginalLaneOffset = 0.0;
        overtakeTargetLaneOffset = 0.0;
        overtakeLightIdx = -1;
        overtakeIntersectionId = null;
        overtakeVehicleId = null;
    }

    private void resetYieldState() {
        yieldingToPriorityVehicle = false;
        yieldTargetLaneOffset = 0.0;
        yieldLightIdx = -1;
        yieldIntersectionId = null;
        yieldPriorityVehicleId = null;
    }

    private void resetTurningBypassState() {
        bypassingTurningVehicle = false;
        bypassTargetLaneOffset = 0.0;
        bypassLightIdx = -1;
        bypassIntersectionId = null;
        bypassVehicleId = null;
    }

    private boolean updateTurningVehicleBypassIfNeeded(List<Vehicle> allVehicles,
            List<Intersection> intersections, Intersection intersection, int lightIdx, double dt) {
        boolean insideIntersection = intersection != null && isInsideStandardIntersection(intersection, x, y);
        boolean canBypassInCurrentPosition = isPriorityVehicle
                ? (passedStopLine || insideIntersection)
                : (passedStopLine && insideIntersection);
        if (intersection == null || intersection instanceof RoundaboutIntersection
                || isTurningDiagonally || isTurningSmoothly || !canBypassInCurrentPosition) {
            resetTurningBypassState();
            return false;
        }

        Vehicle blocker = findTurningVehicleBlockingCurrentLane(allVehicles, intersection, lightIdx);
        if (blocker == null && bypassingTurningVehicle
                && lightIdx == bypassLightIdx
                && intersection.getId().equals(bypassIntersectionId)) {
            moveTowardStandardLane(intersection, lightIdx, bypassTargetLaneOffset, dt, 115.0);
            if (Math.abs(Math.abs(standardLaneOffset(intersection, lightIdx, x, y))
                    - bypassTargetLaneOffset) < 2.0) {
                resetTurningBypassState();
            }
            return true;
        }
        if (blocker == null) {
            resetTurningBypassState();
            return false;
        }

        if (!bypassingTurningVehicle
                || lightIdx != bypassLightIdx
                || !intersection.getId().equals(bypassIntersectionId)
                || !blocker.id.equals(bypassVehicleId)) {
            double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
            int currentLaneIndex = nearestStandardLaneIndex(currentOffset);
            int targetLaneIndex = chooseTurningBypassLaneIndex(allVehicles, intersections, intersection,
                    lightIdx, currentLaneIndex, blocker);
            if (targetLaneIndex < 0) {
                resetTurningBypassState();
                return false;
            }

            bypassingTurningVehicle = true;
            bypassTargetLaneOffset = Math.min(STANDARD_LANE_OFFSETS[targetLaneIndex],
                    roadCenterOffsetLimit());
            bypassLightIdx = lightIdx;
            bypassIntersectionId = intersection.getId();
            bypassVehicleId = blocker.id;
        }

        moveTowardStandardLane(intersection, lightIdx, bypassTargetLaneOffset, dt, 115.0);
        return true;
    }

    private Vehicle findTurningVehicleBlockingCurrentLane(List<Vehicle> allVehicles,
            Intersection intersection, int lightIdx) {
        Vehicle closest = null;
        double closestAhead = Double.MAX_VALUE;
        double dirX = Math.cos(direction);
        double dirY = Math.sin(direction);
        double laneThreshold = Math.max(20.0, height + 10.0);
        for (Vehicle other : allVehicles) {
            if (other == this || other.isPriorityVehicle) {
                continue;
            }
            if (!isPriorityVehicle
                    && other.originalLightIdx == originalLightIdx
                    && other.turnIntention == turnIntention) {
                continue;
            }
            boolean turningInIntersection = (other.isTurningSmoothly || other.isTurningDiagonally || other.hasTurned)
                    && isInsideStandardIntersection(intersection, other.x, other.y);
            if (!turningInIntersection) {
                continue;
            }

            double relX = other.x - x;
            double relY = other.y - y;
            double ahead = relX * dirX + relY * dirY;
            if (ahead <= 0.0 || ahead > 150.0) {
                continue;
            }
            double lateral = Math.abs(relX * dirY - relY * dirX);
            if (lateral > laneThreshold) {
                continue;
            }
            double offset = Math.abs(standardLaneOffset(intersection, lightIdx, other.x, other.y));
            double myOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
            if (nearestStandardLaneIndex(offset) != nearestStandardLaneIndex(myOffset)) {
                continue;
            }
            if (ahead < closestAhead) {
                closestAhead = ahead;
                closest = other;
            }
        }
        return closest;
    }

    private int chooseTurningBypassLaneIndex(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, int currentLaneIndex, Vehicle blocker) {
        int bestIndex = -1;
        int bestCount = Integer.MAX_VALUE;
        for (int candidate = 0; candidate < STANDARD_LANE_OFFSETS.length; candidate++) {
            if (candidate == currentLaneIndex || Math.abs(candidate - currentLaneIndex) != 1) {
                continue;
            }
            double offset = Math.min(STANDARD_LANE_OFFSETS[candidate], roadCenterOffsetLimit());
            if (!isTurningBypassLaneSafe(allVehicles, intersections, intersection, lightIdx, offset, blocker)) {
                continue;
            }

            int count = countVehiclesInLaneWindow(allVehicles, intersections, intersection, lightIdx, offset);
            if (count < bestCount) {
                bestCount = count;
                bestIndex = candidate;
            }
        }
        return bestIndex;
    }

    private boolean isTurningBypassLaneSafe(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, double targetOffset, Vehicle blocker) {
        if (!isLaneSafeForChange(allVehicles, intersections, intersection, lightIdx, targetOffset)) {
            return false;
        }

        double targetLaneCoord = standardLaneCoordinate(intersection, lightIdx, targetOffset);
        double myLongitudinal = longitudinalCoordinate(lightIdx, x, y);
        for (Vehicle other : allVehicles) {
            if (other == this || other == blocker) {
                continue;
            }
            if (!isInsideStandardIntersectionGuardZone(intersection, other.x, other.y, other.x, other.y)
                    && !isSameApproachToIntersection(other, intersections, intersection, lightIdx)) {
                continue;
            }

            double otherLongitudinal = longitudinalCoordinate(lightIdx, other.x, other.y);
            double relative = signedLongitudinalDelta(lightIdx, myLongitudinal, otherLongitudinal);
            if (relative < -70.0 || relative > 180.0) {
                continue;
            }

            double lateral = lightIdx < 2
                    ? Math.abs(other.y - targetLaneCoord)
                    : Math.abs(other.x - targetLaneCoord);
            if (lateral < Math.max(18.0, (height + other.height) * 0.65)) {
                return false;
            }
        }
        return true;
    }

    private boolean isActiveTurningBypassBlocker(Vehicle other, Intersection intersection, int lightIdx) {
        return bypassingTurningVehicle
                && lightIdx == bypassLightIdx
                && intersection.getId().equals(bypassIntersectionId)
                && other.id.equals(bypassVehicleId);
    }

    private boolean hasStoppedQueueAhead(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx) {
        for (Vehicle other : allVehicles) {
            if (other == this || other.isPriorityVehicle) {
                continue;
            }
            if (!isSameApproachToIntersection(other, intersections, intersection, lightIdx)) {
                continue;
            }

            double ahead = longitudinalDistanceAhead(lightIdx, other.x, other.y);
            if (ahead > 0.0 && ahead < 420.0 && other.speed < 2.0 && !other.passedStopLine) {
                return true;
            }
        }
        return false;
    }

    private double leastBusyLaneOffset(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx) {
        return leastBusyLaneOffset(allVehicles, intersections, intersection, lightIdx, -1);
    }

    private double stableYieldLaneOffset(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, Vehicle priorityVehicle) {
        if (yieldingToPriorityVehicle
                && lightIdx == yieldLightIdx
                && intersection.getId().equals(yieldIntersectionId)
                && priorityVehicle.id.equals(yieldPriorityVehicleId)) {
            return yieldTargetLaneOffset;
        }

        int targetLaneIndex = chooseAdjacentYieldLaneIndex(allVehicles, intersections, intersection, lightIdx,
                priorityVehicle);
        if (targetLaneIndex < 0) {
            resetYieldState();
            return Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
        }

        yieldingToPriorityVehicle = true;
        yieldTargetLaneOffset = Math.min(STANDARD_LANE_OFFSETS[targetLaneIndex], roadCenterOffsetLimit());
        yieldLightIdx = lightIdx;
        yieldIntersectionId = intersection.getId();
        yieldPriorityVehicleId = priorityVehicle.id;
        return yieldTargetLaneOffset;
    }

    private boolean continueYieldLaneChangeToTargetIfNeeded(List<Vehicle> allVehicles,
            List<Intersection> intersections, Intersection intersection, int lightIdx, double dt) {
        if (!yieldingToPriorityVehicle || intersection == null || intersection instanceof RoundaboutIntersection
                || lightIdx != yieldLightIdx || !intersection.getId().equals(yieldIntersectionId)) {
            return false;
        }

        double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
        if (Math.abs(currentOffset - yieldTargetLaneOffset) <= 1.5) {
            resetYieldState();
            return false;
        }

        if (isLaneSafeForChange(allVehicles, intersections, intersection, lightIdx, yieldTargetLaneOffset)) {
            moveTowardStandardLane(intersection, lightIdx, yieldTargetLaneOffset, dt, YIELD_LANE_CHANGE_SPEED);
        }
        return true;
    }

    private int chooseAdjacentYieldLaneIndex(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, Vehicle priorityVehicle) {
        double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
        int currentLaneIndex = nearestStandardLaneIndex(currentOffset);
        double priorityOffset = Math.abs(standardLaneOffset(intersection, lightIdx,
                priorityVehicle.x, priorityVehicle.y));
        int priorityLaneIndex = nearestStandardLaneIndex(priorityOffset);

        int bestIndex = -1;
        int bestCount = Integer.MAX_VALUE;
        for (int candidate = 0; candidate < STANDARD_LANE_OFFSETS.length; candidate++) {
            if (candidate == currentLaneIndex
                    || candidate == priorityLaneIndex
                    || Math.abs(candidate - currentLaneIndex) != 1) {
                continue;
            }

            double offset = Math.min(STANDARD_LANE_OFFSETS[candidate], roadCenterOffsetLimit());
            if (!isLaneSafeForChange(allVehicles, intersections, intersection, lightIdx, offset)) {
                continue;
            }

            int count = countVehiclesInLaneWindow(allVehicles, intersections, intersection, lightIdx, offset);
            if (count < bestCount) {
                bestCount = count;
                bestIndex = candidate;
            }
        }
        return bestIndex;
    }

    private double leastBusyLaneOffset(List<Vehicle> allVehicles, List<Intersection> intersections,
            Intersection intersection, int lightIdx, int avoidLaneIndex) {
        int[] counts = new int[STANDARD_LANE_OFFSETS.length];
        double[] nearestDistances = new double[STANDARD_LANE_OFFSETS.length];
        for (int i = 0; i < nearestDistances.length; i++) {
            nearestDistances[i] = Double.MAX_VALUE;
        }
        if (avoidLaneIndex >= 0 && avoidLaneIndex < counts.length) {
            counts[avoidLaneIndex] += 1000;
        }

        for (Vehicle other : allVehicles) {
            if (other == this || !isSameApproachToIntersection(other, intersections, intersection, lightIdx)) {
                continue;
            }

            double ahead = longitudinalDistanceAhead(lightIdx, other.x, other.y);
            if (ahead < -20.0 || ahead > 500.0) {
                continue;
            }

            double offset = standardLaneOffset(intersection, lightIdx, other.x, other.y);
            if (Math.abs(offset) > ROAD_HALF_WIDTH + other.width / 2.0) {
                continue;
            }

            int laneIndex = nearestStandardLaneIndex(Math.abs(offset));
            counts[laneIndex]++;
            nearestDistances[laneIndex] = Math.min(nearestDistances[laneIndex], Math.max(0.0, ahead));
        }

        double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
        int bestIndex = 0;
        for (int i = 1; i < STANDARD_LANE_OFFSETS.length; i++) {
            if (counts[i] < counts[bestIndex]) {
                bestIndex = i;
            } else if (counts[i] == counts[bestIndex]) {
                double currentMove = Math.abs(STANDARD_LANE_OFFSETS[i] - currentOffset);
                double bestMove = Math.abs(STANDARD_LANE_OFFSETS[bestIndex] - currentOffset);
                if (nearestDistances[i] > nearestDistances[bestIndex] + 1.0
                        || (Math.abs(nearestDistances[i] - nearestDistances[bestIndex]) <= 1.0
                                && currentMove < bestMove)) {
                    bestIndex = i;
                }
            }
        }
        return Math.min(STANDARD_LANE_OFFSETS[bestIndex], roadCenterOffsetLimit());
    }

    private boolean isSameApproachToIntersection(Vehicle other, List<Intersection> intersections,
            Intersection intersection, int lightIdx) {
        if (getLightIdx(other.direction) != lightIdx) {
            return false;
        }
        Intersection otherTarget = other.getTargetIntersection(intersections);
        if (otherTarget != intersection && !intersection.getId().equals(other.activeIntersectionId)) {
            return false;
        }
        double offset = standardLaneOffset(intersection, lightIdx, other.x, other.y);
        return Math.abs(offset) <= ROAD_HALF_WIDTH + other.width / 2.0;
    }

    private boolean isBlockingPriorityLane(Intersection intersection, int lightIdx, Vehicle priorityVehicle) {
        double currentOffset = Math.abs(standardLaneOffset(intersection, lightIdx, x, y));
        double priorityOffset = Math.abs(standardLaneOffset(intersection, lightIdx,
                priorityVehicle.x, priorityVehicle.y));
        return nearestStandardLaneIndex(currentOffset) == nearestStandardLaneIndex(priorityOffset);
    }

    private boolean isContinuingYieldForPriority(Intersection intersection, int lightIdx, Vehicle priorityVehicle) {
        return yieldingToPriorityVehicle
                && lightIdx == yieldLightIdx
                && intersection.getId().equals(yieldIntersectionId)
                && priorityVehicle.id.equals(yieldPriorityVehicleId);
    }

    private double standardLaneOffset(Intersection intersection, int lightIdx, double px, double py) {
        double cx = intersection.getX();
        double cy = intersection.getY();
        if (lightIdx == 0) {
            return py - cy;
        }
        if (lightIdx == 1) {
            return cy - py;
        }
        if (lightIdx == 2) {
            return cx - px;
        }
        return px - cx;
    }

    private int nearestStandardLaneIndex(double offset) {
        int nearestIndex = 0;
        double nearestDistance = Math.abs(offset - STANDARD_LANE_OFFSETS[0]);
        for (int i = 1; i < STANDARD_LANE_OFFSETS.length; i++) {
            double distance = Math.abs(offset - STANDARD_LANE_OFFSETS[i]);
            if (distance < nearestDistance) {
                nearestIndex = i;
                nearestDistance = distance;
            }
        }
        return nearestIndex;
    }

    private double longitudinalDistanceAhead(int lightIdx, double otherX, double otherY) {
        if (lightIdx == 0) {
            return otherX - x;
        }
        if (lightIdx == 1) {
            return x - otherX;
        }
        if (lightIdx == 2) {
            return otherY - y;
        }
        return y - otherY;
    }

    private void moveTowardStandardLane(Intersection intersection, int lightIdx, double offset,
            double dt, double lateralSpeed) {
        double target = standardLaneCoordinate(intersection, lightIdx, offset);
        double maxStep = Math.max(0.0, lateralSpeed * dt);
        if (lightIdx < 2) {
            y = moveToward(y, target, maxStep);
        } else {
            x = moveToward(x, target, maxStep);
        }
    }

    private double standardLaneCoordinate(Intersection intersection, int lightIdx, double offset) {
        if (lightIdx == 0) {
            return intersection.getY() + offset;
        }
        if (lightIdx == 1) {
            return intersection.getY() - offset;
        }
        if (lightIdx == 2) {
            return intersection.getX() - offset;
        }
        return intersection.getX() + offset;
    }

    private double moveToward(double current, double target, double maxStep) {
        if (Math.abs(target - current) <= maxStep) {
            return target;
        }
        return current + Math.signum(target - current) * maxStep;
    }

    private void beginIntersectionIfNeeded(Intersection intersection) {
        String id = intersection.getId();
        if (id.equals(activeIntersectionId)) {
            return;
        }

        activeIntersectionId = id;
        activeIntersectionEntryLightIdx = getLightIdx(direction);
        hasTurned = false;
        passedStopLine = false;
        activeIntersectionEntryOrder = Long.MAX_VALUE;
        resetTurningBypassState();
    }

    private void markIntersectionEntryIfNeeded(boolean entered) {
        if (entered && activeIntersectionEntryOrder == Long.MAX_VALUE) {
            activeIntersectionEntryOrder = nextIntersectionEntryOrder();
        }
    }

    private static synchronized long nextIntersectionEntryOrder() {
        return ++intersectionEntryCounter;
    }

    private boolean isPriorityApproachingSameIntersection(Vehicle priorityVehicle,
            Intersection intersection, List<Intersection> intersections) {
        if (priorityVehicle == this || !priorityVehicle.isPriorityVehicle) {
            return false;
        }
        if (Math.hypot(priorityVehicle.x - intersection.getX(), priorityVehicle.y - intersection.getY()) < 360.0) {
            return true;
        }
        return priorityVehicle.getTargetIntersection(intersections) == intersection;
    }

    private double limitSpeedForPredictedIntersectionCollision(double dt, double proposedSpeed,
            List<Vehicle> allVehicles, Intersection intersection) {
        if (proposedSpeed <= 0.0 || intersection instanceof RoundaboutIntersection) {
            return proposedSpeed;
        }

        double nextX = x + Math.cos(direction) * proposedSpeed * dt;
        double nextY = y + Math.sin(direction) * proposedSpeed * dt;
        if (!isInsideStandardIntersectionGuardZone(intersection, x, y, nextX, nextY)) {
            return proposedSpeed;
        }

        double limitedSpeed = proposedSpeed;
        for (Vehicle other : allVehicles) {
            if (other == this) {
                continue;
            }
            if (isActiveTurningBypassBlocker(other, intersection, getLightIdx(direction))) {
                continue;
            }

            double otherNextX = other.x + Math.cos(other.direction) * Math.max(0.0, other.speed) * dt;
            double otherNextY = other.y + Math.sin(other.direction) * Math.max(0.0, other.speed) * dt;
            if (!isInsideStandardIntersectionGuardZone(intersection, other.x, other.y, otherNextX, otherNextY)) {
                continue;
            }

            int lightIdx = getLightIdx(direction);
            int otherLightIdx = getLightIdx(other.direction);
            boolean sameAxis = (lightIdx < 2 && otherLightIdx < 2) || (lightIdx >= 2 && otherLightIdx >= 2);
            if (sameAxis && !isSameCollisionLane(other, lightIdx)) {
                continue;
            }

            double collisionGap = Math.max(24.0,
                    (getHalfLength() + other.getHalfLength()) * 0.9 + Math.max(height, other.height) * 0.25);
            double currentDistance = Math.hypot(x - other.x, y - other.y);
            double nextDistance = Math.hypot(nextX - otherNextX, nextY - otherNextY);
            double pathDistance = segmentDistance(x, y, nextX, nextY, other.x, other.y, otherNextX, otherNextY);

            if (currentDistance > collisionGap * 1.35
                    && nextDistance > collisionGap * 1.25
                    && pathDistance > collisionGap) {
                continue;
            }

            boolean mustYield = shouldYieldForIntersectionCollision(other);
            boolean immediateCollision = currentDistance < collisionGap || nextDistance < collisionGap;
            boolean hardCollision = immediateCollision || pathDistance < collisionGap * 0.85;
            if (!mustYield && !hardCollision) {
                continue;
            }

            boolean insideIntersection = Math.hypot(x - intersection.getX(), y - intersection.getY())
                    < INTERSECTION_CLEAR_RADIUS;
            boolean clearingIntersection = passedStopLine && insideIntersection;
            if (!clearingIntersection) {
                limitedSpeed = 0.0;
            } else {
                boolean clearFirst = hasIntersectionClearPriorityOver(other, intersection);
                if (!clearFirst) {
                    if (immediateCollision) {
                        limitedSpeed = 0.0;
                        continue;
                    }
                    limitedSpeed = Math.min(limitedSpeed, baseSpeed * CLEARING_MIN_SPEED_FACTOR);
                    continue;
                }
                double crawlFactor = CLEARING_MIN_SPEED_FACTOR;
                limitedSpeed = Math.min(limitedSpeed, baseSpeed * crawlFactor);
            }
        }
        return limitedSpeed;
    }

    private boolean hasIntersectionClearPriorityOver(Vehicle other, Intersection intersection) {
        if (isPriorityVehicle != other.isPriorityVehicle) {
            return isPriorityVehicle;
        }

        boolean thisClearing = passedStopLine || isInsideStandardIntersection(intersection, x, y);
        boolean otherClearing = other.passedStopLine || isInsideStandardIntersection(intersection, other.x, other.y);
        if (thisClearing != otherClearing) {
            return thisClearing;
        }

        if (activeIntersectionEntryOrder != Long.MAX_VALUE
                && other.activeIntersectionEntryOrder != Long.MAX_VALUE
                && activeIntersectionEntryOrder != other.activeIntersectionEntryOrder) {
            return activeIntersectionEntryOrder < other.activeIntersectionEntryOrder;
        }
        return id.compareTo(other.id) <= 0;
    }

    private boolean shouldYieldForIntersectionCollision(Vehicle other) {
        if (!isPriorityVehicle && other.isPriorityVehicle) {
            return true;
        }
        if (isPriorityVehicle && !other.isPriorityVehicle) {
            return false;
        }
        if (passedStopLine != other.passedStopLine) {
            return !passedStopLine && other.passedStopLine;
        }
        if (activeIntersectionEntryOrder != Long.MAX_VALUE
                && other.activeIntersectionEntryOrder != Long.MAX_VALUE
                && activeIntersectionEntryOrder != other.activeIntersectionEntryOrder) {
            return activeIntersectionEntryOrder > other.activeIntersectionEntryOrder;
        }
        return id.compareTo(other.id) > 0;
    }

    private boolean isSameCollisionLane(Vehicle other, int lightIdx) {
        double laneThreshold = Math.max(16.0, (height + other.height) * 0.55);
        if (lightIdx < 2) {
            return Math.abs(other.y - y) <= laneThreshold;
        }
        return Math.abs(other.x - x) <= laneThreshold;
    }

    private boolean isInsideStandardIntersectionGuardZone(Intersection intersection,
            double currentX, double currentY, double nextX, double nextY) {
        double guardRadius = INTERSECTION_CLEAR_RADIUS + 55.0;
        return Math.hypot(currentX - intersection.getX(), currentY - intersection.getY()) < guardRadius
                || Math.hypot(nextX - intersection.getX(), nextY - intersection.getY()) < guardRadius;
    }

    private boolean isInsideStandardIntersection(Intersection intersection, double px, double py) {
        return Math.hypot(px - intersection.getX(), py - intersection.getY()) < INTERSECTION_CLEAR_RADIUS;
    }

    private double segmentDistance(double ax, double ay, double bx, double by,
            double cx, double cy, double dx, double dy) {
        if (segmentsIntersect(ax, ay, bx, by, cx, cy, dx, dy)) {
            return 0.0;
        }
        return Math.min(
                Math.min(pointToSegmentDistance(ax, ay, cx, cy, dx, dy),
                        pointToSegmentDistance(bx, by, cx, cy, dx, dy)),
                Math.min(pointToSegmentDistance(cx, cy, ax, ay, bx, by),
                        pointToSegmentDistance(dx, dy, ax, ay, bx, by)));
    }

    private double pointToSegmentDistance(double px, double py, double ax, double ay, double bx, double by) {
        double vx = bx - ax;
        double vy = by - ay;
        double lengthSq = vx * vx + vy * vy;
        if (lengthSq < 0.0001) {
            return Math.hypot(px - ax, py - ay);
        }

        double t = ((px - ax) * vx + (py - ay) * vy) / lengthSq;
        t = Math.max(0.0, Math.min(1.0, t));
        double closestX = ax + vx * t;
        double closestY = ay + vy * t;
        return Math.hypot(px - closestX, py - closestY);
    }

    private boolean segmentsIntersect(double ax, double ay, double bx, double by,
            double cx, double cy, double dx, double dy) {
        double o1 = orientation(ax, ay, bx, by, cx, cy);
        double o2 = orientation(ax, ay, bx, by, dx, dy);
        double o3 = orientation(cx, cy, dx, dy, ax, ay);
        double o4 = orientation(cx, cy, dx, dy, bx, by);
        return o1 * o2 < 0.0 && o3 * o4 < 0.0;
    }

    private double orientation(double ax, double ay, double bx, double by, double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private int getIntersectionEntryLightIdx() {
        return activeIntersectionEntryLightIdx >= 0 ? activeIntersectionEntryLightIdx : originalLightIdx;
    }

    private boolean isAlignedWithIntersectionRoad(Intersection inter, int lightIdx) {
        double lateralLimit = ROAD_HALF_WIDTH + width / 2.0;
        if (lightIdx < 2) {
            return Math.abs(y - inter.getY()) <= lateralLimit;
        }
        return Math.abs(x - inter.getX()) <= lateralLimit;
    }

    private boolean isOnRoundaboutApproach(RoundaboutIntersection roundabout) {
        if (insideRoundabout) {
            return true;
        }

        double[] roadAngles = roundabout.getRoadAngles();
        if (roadAngles.length == 0) {
            return false;
        }

        double cx = roundabout.getX();
        double cy = roundabout.getY();
        int roadIndex = getApproachRoadIndex(x, y, direction, cx, cy, roadAngles);
        double theta = roadAngles[roadIndex];
        double dx = x - cx;
        double dy = y - cy;
        double forwardDistance = dx * Math.cos(theta) + dy * Math.sin(theta);
        double lateralDistance = Math.abs(dx * Math.sin(theta) - dy * Math.cos(theta));
        double headingDiff = Math.abs(normalizeAngle(direction - (theta + Math.PI)));

        return forwardDistance > 0
                && forwardDistance < ROUNDABOUT_CAPTURE_DISTANCE
                && lateralDistance <= ROAD_HALF_WIDTH + width / 2.0
                && headingDiff < 0.65;
    }

    private boolean isInsideRoundaboutBody(RoundaboutIntersection roundabout) {
        return Math.hypot(x - roundabout.getX(), y - roundabout.getY())
                <= ROUNDABOUT_MAX_DRIVE_RADIUS + getHalfLength();
    }

    private double roundaboutLaneMergeDistance() {
        double r = ROUNDABOUT_MAX_DRIVE_RADIUS;
        return Math.sqrt(Math.max(0.0, r * r - laneOffsetVal * laneOffsetVal));
    }

    private double getRoundaboutExitLaneAngle(double theta) {
        double d = roundaboutLaneMergeDistance();
        double localX = d * Math.cos(theta) - laneOffsetVal * Math.sin(theta);
        double localY = d * Math.sin(theta) + laneOffsetVal * Math.cos(theta);
        return Math.atan2(localY, localX);
    }

    private int getExitsRemaining(double currentAngle, double targetExitAngle, double[] roadAngles) {
        double targetNorm = normalizeAngle(targetExitAngle);
        double currentNorm = normalizeAngle(currentAngle);
        double diff = counterClockwiseDistance(currentNorm, targetNorm);

        int count = 0;
        for (double roadAngle : roadAngles) {
            double d = counterClockwiseDistance(currentNorm, roadAngle);
            if (d <= diff) {
                count++;
            }
        }
        return count;
    }

    private int getClosestRoadIndex(double x, double y, double cx, double cy, double[] roadAngles) {
        double dx = x - cx;
        double dy = y - cy;
        double currentAngle = Math.atan2(dy, dx);
        int closestIdx = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < roadAngles.length; i++) {
            double diff = Math.abs(normalizeAngle(currentAngle - roadAngles[i]));
            if (diff < minDist) {
                minDist = diff;
                closestIdx = i;
            }
        }
        return closestIdx;
    }

    private int getApproachRoadIndex(double x, double y, double direction, double cx, double cy, double[] roadAngles) {
        int bestHeadingIdx = 0;
        double bestHeadingDiff = Double.MAX_VALUE;
        for (int i = 0; i < roadAngles.length; i++) {
            double incomingDirection = normalizeAngle(roadAngles[i] + Math.PI);
            double diff = Math.abs(normalizeAngle(direction - incomingDirection));
            if (diff < bestHeadingDiff) {
                bestHeadingDiff = diff;
                bestHeadingIdx = i;
            }
        }

        if (bestHeadingDiff < 0.65) {
            return bestHeadingIdx;
        }

        return getClosestRoadIndex(x, y, cx, cy, roadAngles);
    }

    private void startSmoothTurn(double targetX, double targetY, double targetDirection,
            boolean completesTurn, double duration) {
        isTurningSmoothly = true;
        smoothTurnStartX = x;
        smoothTurnStartY = y;
        smoothTurnEndX = targetX;
        smoothTurnEndY = targetY;
        smoothTurnStartDirection = direction;
        smoothTurnEndDirection = normalizeAngle(targetDirection);
        smoothTurnElapsed = 0.0;
        smoothTurnDuration = Math.max(0.12, duration);
        smoothTurnCompletesTurn = completesTurn;
        speed = Math.max(speed, baseSpeed * 0.75);
    }

    private boolean continueSmoothTurn(double dt, List<Vehicle> allVehicles, List<Intersection> intersections) {
        if (!isTurningSmoothly) {
            return false;
        }

        double turnSpeedFactor = smoothTurnSpeedFactor(allVehicles, dt);
        if (turnSpeedFactor <= 0.0) {
            speed = 0;
            return true;
        }

        smoothTurnElapsed += Math.max(0.0, dt) * turnSpeedFactor;
        double rawT = smoothTurnElapsed / smoothTurnDuration;
        double t = Math.min(1.0, rawT);
        double eased = smoothStep(t);

        x = smoothTurnStartX + (smoothTurnEndX - smoothTurnStartX) * eased;
        y = smoothTurnStartY + (smoothTurnEndY - smoothTurnStartY) * eased;
        direction = interpolateAngle(smoothTurnStartDirection, smoothTurnEndDirection, eased);
        speed = baseSpeed * turnSpeedFactor;

        if (t >= 1.0) {
            x = smoothTurnEndX;
            y = smoothTurnEndY;
            direction = smoothTurnEndDirection;
            isTurningSmoothly = false;
            if (smoothTurnCompletesTurn) {
                hasTurned = true;
                passedStopLine = true;
                turnExitClearanceTime = TURN_EXIT_CLEARANCE_DURATION;
            }
        }

        return true;
    }

    private double smoothTurnSpeedFactor(List<Vehicle> allVehicles, double dt) {
        double pathX = smoothTurnEndX - smoothTurnStartX;
        double pathY = smoothTurnEndY - smoothTurnStartY;
        double pathLength = Math.hypot(pathX, pathY);
        if (pathLength < 1.0) {
            return 1.0;
        }

        double dirX = pathX / pathLength;
        double dirY = pathY / pathLength;
        double nextElapsed = Math.min(smoothTurnDuration, smoothTurnElapsed + Math.max(0.0, dt));
        double nextT = nextElapsed / smoothTurnDuration;
        double nextEased = smoothStep(nextT);
        double nextX = smoothTurnStartX + pathX * nextEased;
        double nextY = smoothTurnStartY + pathY * nextEased;
        double safeGap = Math.max(34.0, getHalfLength() + 24.0);
        double factor = 1.0;

        for (Vehicle other : allVehicles) {
            if (other == this) {
                continue;
            }

            double relX = other.x - x;
            double relY = other.y - y;
            double ahead = relX * dirX + relY * dirY;
            double lateral = Math.abs(relX * dirY - relY * dirX);
            double laneThreshold = Math.max(18.0, (height + other.height) * 0.7);
            double combinedGap = safeGap + other.getHalfLength();

            if (ahead > -other.getHalfLength() && ahead < combinedGap && lateral < laneThreshold) {
                double hardGap = getHalfLength() + other.getHalfLength() + 6.0;
                if (ahead <= hardGap) {
                    return 0.0;
                }
                double localFactor = (ahead - hardGap) / Math.max(1.0, combinedGap - hardGap);
                factor = Math.min(factor, Math.max(0.18, Math.min(1.0, localFactor)));
            }

            double nextDistance = Math.hypot(other.x - nextX, other.y - nextY);
            double overlapDistance = Math.max(18.0, (getHalfLength() + other.getHalfLength()) * 0.55);
            if (nextDistance < overlapDistance
                    && (other.isTurningSmoothly || other.isTurningDiagonally || other.insideRoundabout || other.exitedRoundabout)) {
                if (nextDistance < overlapDistance * 0.75) {
                    return 0.0;
                }
                factor = Math.min(factor, 0.18);
            }
        }

        return factor;
    }

    private double smoothStep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private double blendAlpha(double dt, double rate) {
        return Math.max(0.0, Math.min(1.0, 1.0 - Math.exp(-Math.max(0.0, dt) * rate)));
    }

    private double interpolateAngle(double fromAngle, double toAngle, double t) {
        return normalizeAngle(fromAngle + normalizeAngle(toAngle - fromAngle) * t);
    }

    private void finishDiagonalRightTurnIfNeeded(double cx, double cy) {
        if (!isTurningDiagonally || hasTurned || turnIntention != 2) {
            return;
        }

        int entryLightIdx = getIntersectionEntryLightIdx();
        double endLane = 65.0;
        boolean endDiagonal = false;
        if (entryLightIdx == 0) endDiagonal = (x >= cx - endLane);
        else if (entryLightIdx == 1) endDiagonal = (x <= cx + endLane);
        else if (entryLightIdx == 2) endDiagonal = (y >= cy - endLane);
        else if (entryLightIdx == 3) endDiagonal = (y <= cy + endLane);

        if (!endDiagonal) {
            return;
        }

        isTurningDiagonally = false;
        double targetX = x;
        double targetY = y;
        double targetDirection = direction;
        if (entryLightIdx == 0) { targetX = cx - endLane; targetDirection = Math.PI / 2; }
        else if (entryLightIdx == 1) { targetX = cx + endLane; targetDirection = -Math.PI / 2; }
        else if (entryLightIdx == 2) { targetY = cy - endLane; targetDirection = Math.PI; }
        else if (entryLightIdx == 3) { targetY = cy + endLane; targetDirection = 0; }
        startSmoothTurn(targetX, targetY, targetDirection, true, SMOOTH_TURN_DURATION);
    }

    private boolean continueDiagonalRightTurn(double dt, List<Vehicle> allVehicles) {
        if (!isTurningDiagonally) {
            return false;
        }

        double turnSpeedFactor = diagonalRightTurnSpeedFactor(allVehicles, dt);
        if (turnSpeedFactor <= 0.0) {
            speed = 0;
            return true;
        }

        speed = baseSpeed * turnSpeedFactor;
        movePhysically(dt);
        finishDiagonalRightTurnIfNeeded(diagonalTurnCenterX, diagonalTurnCenterY);
        return true;
    }

    private double diagonalRightTurnSpeedFactor(List<Vehicle> allVehicles, double dt) {
        double dirX = Math.cos(direction);
        double dirY = Math.sin(direction);
        double nextX = x + dirX * baseSpeed * dt;
        double nextY = y + dirY * baseSpeed * dt;
        double safeGap = Math.max(34.0, getHalfLength() + 24.0);
        double factor = 1.0;

        for (Vehicle other : allVehicles) {
            if (other == this) {
                continue;
            }

            double relX = other.x - x;
            double relY = other.y - y;
            double ahead = relX * dirX + relY * dirY;
            double lateral = Math.abs(relX * dirY - relY * dirX);
            double laneThreshold = Math.max(18.0, (height + other.height) * 0.8);
            double combinedGap = safeGap + other.getHalfLength();
            boolean sameTurnStream = originalLightIdx == other.originalLightIdx
                    && turnIntention == other.turnIntention
                    && turnIntention != 0;

            if (ahead > -other.getHalfLength() && ahead < combinedGap && lateral < laneThreshold) {
                double hardGap = getHalfLength() + other.getHalfLength() + 6.0;
                if (ahead <= hardGap) {
                    return 0.0;
                }
                double localFactor = (ahead - hardGap) / Math.max(1.0, combinedGap - hardGap);
                factor = Math.min(factor, Math.max(0.18, Math.min(1.0, localFactor)));
            }

            double nextDistance = Math.hypot(other.x - nextX, other.y - nextY);
            double overlapDistance = Math.max(18.0, (getHalfLength() + other.getHalfLength()) * 0.55);
            if (sameTurnStream
                    && nextDistance < overlapDistance
                    && (other.isTurningDiagonally || other.isTurningSmoothly || other.hasTurned)) {
                if (nextDistance < overlapDistance * 0.75) {
                    return 0.0;
                }
                factor = Math.min(factor, 0.18);
            }
        }

        return factor;
    }

    private boolean isNextExit(double currentAngle, double targetExitAngle, double[] roadAngles) {
        double minCCWDiff = Double.MAX_VALUE;
        int nextExitIdx = -1;
        for (int i = 0; i < roadAngles.length; i++) {
            double diff = counterClockwiseDistance(currentAngle, roadAngles[i]);
            if (diff > 0 && diff < minCCWDiff) {
                minCCWDiff = diff;
                nextExitIdx = i;
            }
        }
        if (nextExitIdx != -1) {
            return Math.abs(normalizeAngle(roadAngles[nextExitIdx] - targetExitAngle)) < 0.05;
        }
        return false;
    }

    private double normalizeAngle(double angle) {
        while (angle <= -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    private double normalizePositiveAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    private double counterClockwiseDistance(double fromAngle, double toAngle) {
        return normalizePositiveAngle(fromAngle - toAngle);
    }

    private boolean hasReachedCounterClockwiseExit(double previousAngle, double currentAngle, double targetAngle) {
        double travelled = counterClockwiseDistance(previousAngle, currentAngle);
        double distanceToTarget = counterClockwiseDistance(previousAngle, targetAngle);
        return distanceToTarget <= travelled + 0.03
                || Math.abs(normalizeAngle(currentAngle - targetAngle)) < 0.08;
    }
}
