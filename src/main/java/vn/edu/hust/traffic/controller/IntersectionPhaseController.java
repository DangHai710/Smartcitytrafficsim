package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.map.TrafficLight.State;

import java.util.List;

/**
 * Bộ điều khiển phase giao lộ — đồng bộ hóa 4 đèn giao thông.
 *
 * Chu kỳ 43 giây (4 phase):
 *   Phase 0 — 17s: Ngang XANH  | Dọc ĐỎ
 *   Phase 1 —  3s: Ngang VÀNG  | Dọc ĐỎ
 *   Phase 2 — 20s: Ngang ĐỎ   | Dọc XANH
 *   Phase 3 —  3s: Ngang ĐỎ   | Dọc VÀNG
 *
 * Index đèn trong danh sách lights:
 *   [0] Hướng Trái→Phải (ngang)
 *   [1] Hướng Phải→Trái (ngang)
 *   [2] Hướng Trên→Dưới (dọc)
 *   [3] Hướng Dưới→Trên (dọc)
 */
public class IntersectionPhaseController {

    // Độ dài từng phase (giây)
    private static final double DURATION_H_GREEN  = 17.0; // Phase 0: Ngang xanh
    private static final double DURATION_H_YELLOW =  3.0; // Phase 1: Ngang vàng
    private static final double DURATION_V_GREEN  = 20.0; // Phase 2: Dọc xanh
    private static final double DURATION_V_YELLOW =  3.0; // Phase 3: Dọc vàng

    private int currentPhase;     // Phase hiện tại (0..3)
    private double phaseTimer;    // Thời gian còn lại của phase hiện tại
    private final List<TrafficLight> lights;

    public IntersectionPhaseController(List<TrafficLight> lights) {
        this.lights = lights;
        this.currentPhase = -1; // Chưa khởi tạo
        advancePhase();          // Nhảy sang phase 0 ngay lập tức
    }

    /**
     * Cập nhật bộ đếm, chuyển phase khi timer hết.
     */
    public void update(double dt) {
        // Cập nhật timer của từng đèn (chỉ để hiển thị countdown đúng)
        for (TrafficLight light : lights) {
            light.update(dt);
        }

        phaseTimer -= dt;
        if (phaseTimer <= 0) {
            advancePhase();
        }
    }

    /**
     * Chuyển sang phase tiếp theo và cưỡng bức trạng thái tất cả đèn.
     */
    private void advancePhase() {
        currentPhase = (currentPhase + 1) % 4;

        State hState, vState;
        double hDuration, vDuration;

        switch (currentPhase) {
            case 0: // Ngang XANH — Dọc ĐỎ
                hState = State.GREEN;  hDuration = DURATION_H_GREEN;
                vState = State.RED;    vDuration = DURATION_H_GREEN;
                phaseTimer = DURATION_H_GREEN;
                break;
            case 1: // Ngang VÀNG — Dọc ĐỎ (chờ)
                hState = State.YELLOW; hDuration = DURATION_H_YELLOW;
                vState = State.RED;    vDuration = DURATION_H_YELLOW;
                phaseTimer = DURATION_H_YELLOW;
                break;
            case 2: // Ngang ĐỎ — Dọc XANH
                hState = State.RED;    hDuration = DURATION_V_GREEN;
                vState = State.GREEN;  vDuration = DURATION_V_GREEN;
                phaseTimer = DURATION_V_GREEN;
                break;
            case 3: // Ngang ĐỎ — Dọc VÀNG
                hState = State.RED;    hDuration = DURATION_V_YELLOW;
                vState = State.YELLOW; vDuration = DURATION_V_YELLOW;
                phaseTimer = DURATION_V_YELLOW;
                break;
            default:
                return;
        }

        // Áp trạng thái đồng bộ cho cả 4 đèn
        lights.get(0).forceState(hState, hDuration); // Trái→Phải
        lights.get(1).forceState(hState, hDuration); // Phải→Trái
        lights.get(2).forceState(vState, vDuration); // Trên→Dưới
        lights.get(3).forceState(vState, vDuration); // Dưới→Trên
    }

    /** Phase hiện tại (0..3) — để view vẽ chú thích nếu cần. */
    public int getCurrentPhase() { return currentPhase; }

    /** Thời gian còn lại của phase hiện tại. */
    public double getPhaseTimeLeft() { return phaseTimer; }
}
