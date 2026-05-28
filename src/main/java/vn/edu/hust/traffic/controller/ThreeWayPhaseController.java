package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.map.TrafficLight.State;
import vn.edu.hust.traffic.model.vehicle.Vehicle;

import java.util.List;

/**
 * Điều khiển pha đèn cho Ngã 3 (T-junction).
 * 3 Đèn: [0]=LTR, [1]=RTL, [2]=TTB.
 * 
 * Phase 0: LTR Thẳng XANH, RTL Thẳng/Phải XANH
 * Phase 1: LTR & RTL VÀNG
 * Phase 2: LTR Trái XANH (RTL Đỏ)
 * Phase 3: LTR Trái VÀNG
 * Phase 4: TTB Trái/Phải XANH
 * Phase 5: TTB VÀNG
 */
public class ThreeWayPhaseController {
    private static final double BASE_STRAIGHT = 10.0;
    private static final double MAX_STRAIGHT  = 20.0;
    private static final double BASE_LEFT     = 6.0;
    private static final double MAX_LEFT      = 12.0;
    private static final double DUR_YELLOW    = 3.0;

    private int currentPhase;
    private double phaseTimer;
    private final List<TrafficLight> lights;

    public ThreeWayPhaseController(List<TrafficLight> lights) {
        this.lights = lights;
        this.currentPhase = -1;
        advancePhase(null);
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
        currentPhase = (currentPhase + 1) % 6;

        State ltrS = State.RED, ltrL = State.RED;
        State rtlS = State.RED, rtlL = State.RED;
        State ttbS = State.RED, ttbL = State.RED;
        double duration = DUR_YELLOW;

        switch (currentPhase) {
            case 0: // LTR Thẳng XANH, RTL Thẳng/Phải XANH
                ltrS = State.GREEN; rtlS = State.GREEN;
                duration = BASE_STRAIGHT; 
                break;
            case 1:
                ltrS = State.YELLOW; rtlS = State.YELLOW; duration = DUR_YELLOW;
                break;
            case 2: // LTR Trái XANH
                ltrL = State.GREEN; duration = BASE_LEFT;
                break;
            case 3:
                ltrL = State.YELLOW; duration = DUR_YELLOW;
                break;
            case 4: // TTB (Trên xuống) Trái/Phải XANH
                ttbS = State.GREEN; ttbL = State.GREEN; duration = BASE_LEFT;
                break;
            case 5:
                ttbS = State.YELLOW; ttbL = State.YELLOW; duration = DUR_YELLOW;
                break;
        }

        phaseTimer = duration;

        lights.get(0).forceState(ltrS, duration);
        lights.get(0).forceLeftTurnState(ltrL, duration);
        lights.get(1).forceState(rtlS, duration);
        lights.get(1).forceLeftTurnState(rtlL, duration);
        lights.get(2).forceState(ttbS, duration);
        lights.get(2).forceLeftTurnState(ttbL, duration);
    }

    public int getCurrentPhase() { return currentPhase; }
    public double getPhaseTimeLeft() { return phaseTimer; }
}
