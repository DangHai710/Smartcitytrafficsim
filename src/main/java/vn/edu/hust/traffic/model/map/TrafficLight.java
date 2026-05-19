package vn.edu.hust.traffic.model.map;

/**
 * Đèn giao thông hỗ trợ 2 tín hiệu riêng biệt:
 *   - state / timer         : đèn ĐI THẲNG  (🟢🟡🔴)
 *   - leftTurnState / timer : đèn RẼ TRÁI   (↰ 🟢🟡🔴)
 *   - Rẽ phải: luôn được phép (không cần đèn riêng)
 *
 * Mọi chuyển trạng thái do IntersectionPhaseController quyết định.
 */
public class TrafficLight {
    public enum State { GREEN, YELLOW, RED }

    // === Đèn đi thẳng ===
    private State state;
    private double timer;

    // === Đèn rẽ trái (mũi tên) ===
    private State leftTurnState;
    private double leftTurnTimer;

    public TrafficLight(State initialState) {
        this.state = initialState;
        this.timer = 0;
        this.leftTurnState = State.RED;
        this.leftTurnTimer = 0;
    }

    public TrafficLight() {
        this(State.GREEN);
    }

    // --- Đèn đi thẳng ---
    public void forceState(State newState, double duration) {
        this.state = newState;
        this.timer = duration;
    }

    public State getState() { return state; }
    public int getTimeLeft() { return (int) Math.ceil(timer); }
    public double getTimeLeftDouble() { return timer; }

    // --- Đèn rẽ trái ---
    public void forceLeftTurnState(State newState, double duration) {
        this.leftTurnState = newState;
        this.leftTurnTimer = duration;
    }

    public State getLeftTurnState() { return leftTurnState; }
    public int getLeftTurnTimeLeft() { return (int) Math.ceil(leftTurnTimer); }

    /**
     * Trả về trạng thái đèn phù hợp với ý định rẽ của xe.
     * @param turnIntention 0=thẳng, 1=rẽ trái, 2=rẽ phải
     * @param hasTurned     xe đã rẽ xong chưa
     */
    public State getStateForTurn(int turnIntention, boolean hasTurned) {
        if (hasTurned || turnIntention == 0) return state;       // Đi thẳng hoặc đã rẽ xong
        if (turnIntention == 1) return leftTurnState;             // Rẽ trái → xem đèn mũi tên
        return State.GREEN; // turnIntention == 2: rẽ phải luôn được đi
    }

    /**
     * Cập nhật bộ đếm ngược cả 2 đèn.
     */
    public void update(double dt) {
        if (timer > 0) timer -= dt;
        if (timer < 0) timer = 0;
        if (leftTurnTimer > 0) leftTurnTimer -= dt;
        if (leftTurnTimer < 0) leftTurnTimer = 0;
    }
}
