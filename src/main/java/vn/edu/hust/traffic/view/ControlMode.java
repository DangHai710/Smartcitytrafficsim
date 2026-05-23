package vn.edu.hust.traffic.view;

public enum ControlMode {
    AUTO("Tu dong"),
    MANUAL("Thu cong");

    private final String label;

    ControlMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
