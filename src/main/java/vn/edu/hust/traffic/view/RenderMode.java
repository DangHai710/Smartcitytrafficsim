package vn.edu.hust.traffic.view;

public enum RenderMode {
    BASIC("Basic"),
    GRAPHIC("Graphic");

    private final String label;

    RenderMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
