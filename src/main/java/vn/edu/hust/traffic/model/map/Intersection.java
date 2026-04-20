package vn.edu.hust.traffic.model.map;

import vn.edu.hust.traffic.base.Updatable;
import java.util.List;

public abstract class Intersection implements Updatable {
    protected String id;
    protected double x;
    protected double y;
    protected List<TrafficLight> lights;

    public Intersection(String id, double x, double y, List<TrafficLight> lights) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.lights = lights;
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public List<TrafficLight> getLights() { return lights; }
}
