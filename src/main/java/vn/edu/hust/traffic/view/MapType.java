package vn.edu.hust.traffic.view;

public enum MapType {
    T_INTERSECTION("Nga 3"),
    CROSS_INTERSECTION("Nga 4"),
    FIVE_WAY_INTERSECTION("Nga 5"),
    ROAD_NETWORK("Mang luoi");

    private final String label;

    MapType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
