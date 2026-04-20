package vn.edu.hust.traffic.model.map;

public class TrafficLight {
    public enum State { GREEN, RED }

    private State state;
    private double timer;
    private double durationGreen;
    private double durationRed;

    public TrafficLight() {
        this.durationGreen = 30; // giây
        this.durationRed = 30;   // giây
        this.state = State.GREEN;
        this.timer = durationGreen;
    }

    public void update(double dt) {
        timer -= dt;
        if (timer <= 0) {
            if (state == State.GREEN) {
                state = State.RED;
                timer = durationRed;
            } else {
                state = State.GREEN;
                timer = durationGreen;
            }
        }
    }

    public State getState() {
        return state;
    }

    public int getTimeLeft() {
        return (int)Math.ceil(timer);
    }

    public void setDurationGreen(double durationGreen) {
        this.durationGreen = durationGreen;
    }
    public void setDurationRed(double durationRed) {
        this.durationRed = durationRed;
    }
}
