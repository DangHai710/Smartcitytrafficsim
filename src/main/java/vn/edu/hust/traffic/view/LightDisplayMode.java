package vn.edu.hust.traffic.view;

public enum LightDisplayMode {
    FULL_COUNTDOWN("Dem day du"),
    NO_COUNTDOWN("Khong dem"),
    LAST_10_SECONDS_ONLY("Chi hien 10s cuoi");

    private final String label;

    LightDisplayMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
