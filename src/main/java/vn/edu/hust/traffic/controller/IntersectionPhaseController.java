package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.map.TrafficLight.State;
import vn.edu.hust.traffic.model.vehicle.Vehicle;

import java.util.List;

/**
 * Kịch bản B: Pha lệch giờ (Xanh sớm / Đỏ muộn) - 12 phases.
 * Chiều đi LTR Xanh cả thẳng và rẽ trái trước (Xanh sớm). Sau đó đèn trái LTR đỏ, dòng thẳng 2 chiều LTR và RTL cùng Xanh.
 * Sau đó LTR đỏ, RTL được Xanh rẽ trái (Đỏ muộn). Tương tự cho trục Dọc.
 *
 *   Phase 0: LTR Thẳng + Trái XANH      | (Base 5s, max 15s)
 *   Phase 1: LTR Trái VÀNG
 *   Phase 2: LTR & RTL Thẳng XANH       | (Base 10s, max 30s)
 *   Phase 3: LTR Thẳng VÀNG
 *   Phase 4: RTL Thẳng + Trái XANH      | (Base 5s, max 15s)
 *   Phase 5: RTL Thẳng + Trái VÀNG
 *   Phase 6: TTB Thẳng + Trái XANH      | (Base 5s, max 15s)
 *   Phase 7: TTB Trái VÀNG
 *   Phase 8: TTB & BTT Thẳng XANH       | (Base 10s, max 30s)
 *   Phase 9: TTB Thẳng VÀNG
 *   Phase 10: BTT Thẳng + Trái XANH     | (Base 5s, max 15s)
 *   Phase 11: BTT Thẳng + Trái VÀNG
 *
 *   Rẽ phải: LUÔN được phép.
 *
 * Index đèn: [0]=LTR, [1]=RTL, [2]=TTB, [3]=BTT
 */
public class IntersectionPhaseController {

    // Base và max duration cho đèn xanh (giây)
    private static final double BASE_STRAIGHT = 10.0;
    private static final double MAX_STRAIGHT  = 30.0;
    private static final double BASE_LEFT     =  5.0;
    private static final double MAX_LEFT      = 15.0;
    private static final double DUR_YELLOW    =  3.0;

    private int currentPhase;
    private double phaseTimer;
    private final List<TrafficLight> lights;

    public IntersectionPhaseController(List<TrafficLight> lights) {
        this.lights = lights;
        this.currentPhase = -1;
        advancePhase(null); // Init with default
    }

    public void update(double dt, List<Vehicle> vehicles) {
        for (TrafficLight light : lights) {
            light.update(dt);
        }
        phaseTimer -= dt;
        if (phaseTimer <= 0) {
            advancePhase(vehicles);
        }
    }

    private void advancePhase(List<Vehicle> vehicles) {
        currentPhase = (currentPhase + 1) % 12;

        State ltrS = State.RED, ltrL = State.RED;
        State rtlS = State.RED, rtlL = State.RED;
        State ttbS = State.RED, ttbL = State.RED;
        State bttS = State.RED, bttL = State.RED;
        double duration = DUR_YELLOW;

        switch (currentPhase) {
            case 0: // LTR Xanh sớm
                ltrS = State.GREEN; ltrL = State.GREEN;
                duration = calculateDynamicDuration(vehicles, 0, 1, BASE_LEFT, MAX_LEFT);
                break;
            case 1: // LTR Trái vàng
                ltrS = State.GREEN; ltrL = State.YELLOW; duration = DUR_YELLOW;
                break;
            case 2: // Hai dòng đi thẳng ngang
                ltrS = State.GREEN; rtlS = State.GREEN;
                duration = calculateDynamicDuration(vehicles, -1, 0, BASE_STRAIGHT, MAX_STRAIGHT);
                break;
            case 3: // LTR Thẳng vàng
                ltrS = State.YELLOW; rtlS = State.GREEN; duration = DUR_YELLOW;
                break;
            case 4: // RTL Xanh toàn bộ (Đỏ muộn)
                rtlS = State.GREEN; rtlL = State.GREEN;
                duration = calculateDynamicDuration(vehicles, 1, 1, BASE_LEFT, MAX_LEFT);
                break;
            case 5: // RTL Thẳng + Trái vàng
                rtlS = State.YELLOW; rtlL = State.YELLOW; duration = DUR_YELLOW;
                break;
            case 6: // TTB Xanh sớm
                ttbS = State.GREEN; ttbL = State.GREEN;
                duration = calculateDynamicDuration(vehicles, 2, 1, BASE_LEFT, MAX_LEFT);
                break;
            case 7: // TTB Trái vàng
                ttbS = State.GREEN; ttbL = State.YELLOW; duration = DUR_YELLOW;
                break;
            case 8: // Hai dòng đi thẳng dọc
                ttbS = State.GREEN; bttS = State.GREEN;
                duration = calculateDynamicDuration(vehicles, -2, 0, BASE_STRAIGHT, MAX_STRAIGHT);
                break;
            case 9: // TTB Thẳng vàng
                ttbS = State.YELLOW; bttS = State.GREEN; duration = DUR_YELLOW;
                break;
            case 10: // BTT Xanh toàn bộ (Đỏ muộn)
                bttS = State.GREEN; bttL = State.GREEN;
                duration = calculateDynamicDuration(vehicles, 3, 1, BASE_LEFT, MAX_LEFT);
                break;
            case 11: // BTT Thẳng + Trái vàng
                bttS = State.YELLOW; bttL = State.YELLOW; duration = DUR_YELLOW;
                break;
        }

        phaseTimer = duration;

        // Áp trạng thái đồng bộ cho 4 đèn
        lights.get(0).forceState(ltrS, duration);          // LTR thẳng
        lights.get(0).forceLeftTurnState(ltrL, duration);   // LTR rẽ trái
        lights.get(1).forceState(rtlS, duration);          // RTL thẳng
        lights.get(1).forceLeftTurnState(rtlL, duration);   // RTL rẽ trái
        lights.get(2).forceState(ttbS, duration);          // TTB thẳng
        lights.get(2).forceLeftTurnState(ttbL, duration);   // TTB rẽ trái
        lights.get(3).forceState(bttS, duration);          // BTT thẳng
        lights.get(3).forceLeftTurnState(bttL, duration);   // BTT rẽ trái
    }

    /**
     * Tính toán thời gian đèn xanh dựa trên số lượng xe đang chờ.
     * @param targetLightIdx -1: Ngang (0,1), -2: Dọc (2,3), hoặc 0/1/2/3 cho làn cụ thể
     * @param intention 0: Đi thẳng, 1: Rẽ trái
     */
    private double calculateDynamicDuration(List<Vehicle> vehicles, int targetLightIdx, int intention, double baseDur, double maxDur) {
        if (vehicles == null || vehicles.isEmpty()) return baseDur;

        int waitingCount = 0;
        for (Vehicle v : vehicles) {
            if (v.hasTurned()) continue;
            
            int vLightIdx = v.getLightIdx(v.getDirection());
            
            // Check lightIdx matches requirement
            if (targetLightIdx == -1 && vLightIdx >= 2) continue; // Phải là ngang
            if (targetLightIdx == -2 && vLightIdx < 2) continue;  // Phải là dọc
            if (targetLightIdx >= 0 && vLightIdx != targetLightIdx) continue;

            // Check intention
            if (v.getTurnIntention() != intention) continue;

            // Check if vehicle is approaching or waiting at the stop line
            // We only count vehicles that are near the intersection (within 300px)
            double dist = v.getDistToStopLine();
            if (dist > -10 && dist < 300) {
                waitingCount++;
            }
        }

        // Mỗi xe chờ cộng thêm 1.5 giây
        double calculatedDur = baseDur + (waitingCount * 1.5);
        return Math.min(calculatedDur, maxDur);
    }

    public int getCurrentPhase() { return currentPhase; }
    public double getPhaseTimeLeft() { return phaseTimer; }
}
