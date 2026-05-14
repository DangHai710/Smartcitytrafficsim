package vn.edu.hust.traffic.model.map;

/**
 * Đèn giao thông với chế độ điều khiển từ bên ngoài (forceState).
 * Mọi chuyển trạng thái thực tế đều do IntersectionPhaseController quyết định,
 * đảm bảo 4 đèn luôn đồng bộ theo phase.
 */
public class TrafficLight {
    public enum State { GREEN, YELLOW, RED }

    private State state;
    private double timer;  // Đếm ngược thời gian phase hiện tại

    public TrafficLight(State initialState) {
        this.state = initialState;
        this.timer = 0;
    }

    public TrafficLight() {
        this(State.GREEN);
    }

    /**
     * Được gọi bởi IntersectionPhaseController để đồng bộ trạng thái.
     * Thay thế hoàn toàn cơ chế tự chuyển trạng thái cũ.
     *
     * @param newState  Trạng thái mới cần chuyển sang
     * @param duration  Thời gian (giây) đèn giữ trạng thái này
     */
    public void forceState(State newState, double duration) {
        this.state = newState;
        this.timer = duration;
    }

    /**
     * Cập nhật bộ đếm ngược (chỉ giảm timer, không tự chuyển state).
     * IntersectionPhaseController sẽ gọi forceState() khi đúng lúc.
     */
    public void update(double dt) {
        if (timer > 0) timer -= dt;
        if (timer < 0) timer = 0;
    }

    public State getState() { return state; }

    /** Trả về giây nguyên để hiển thị countdown. */
    public int getTimeLeft() { return (int) Math.ceil(timer); }

    /** Trả về giây thực (double) để tính toán giảm tốc mượt. */
    public double getTimeLeftDouble() { return timer; }
}
