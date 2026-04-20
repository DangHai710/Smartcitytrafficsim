package vn.edu.hust.traffic.model.vehicle;

// Lớp xe ô tô (Người 2, 4)
public class Car extends Vehicle {
    // TODO: Thuộc tính và phương thức riêng cho xe ô tô
    public Car(String id, double x, double y, double speed, double direction, double width, double height, boolean isPriorityVehicle) {
        super(id, x, y, speed, direction, width, height, isPriorityVehicle);
    }

    @Override
    public void movePhysically(double dt) {
        // TODO: Logic di chuyển vật lý cho xe ô tô
    }

    @Override
    public void render() {
        // TODO: Vẽ xe ô tô trên màn hình
    }
}
