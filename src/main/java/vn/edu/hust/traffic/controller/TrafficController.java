package vn.edu.hust.traffic.controller;

import vn.edu.hust.traffic.model.vehicle.Vehicle;
import vn.edu.hust.traffic.model.map.Intersection;
import vn.edu.hust.traffic.model.map.CrossIntersection;
import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.vehicle.Car;

import java.util.ArrayList;
import java.util.List;

/**
 * Bộ điều khiển giao thông 
 * Quản lý danh sách các phương tiện và cập nhật mô phỏng.
 */
public class TrafficController {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private double[] carSpawnTimers = new double[] {0, 0, 0, 0};
    private int carCount = 2;
    private CrossIntersection intersection;
    private List<TrafficLight> lights;
    private List<Car> cars;

    // Đổi access modifier thành public để View có thể tạo mới Controller
    public TrafficController() {
        setupSimulation();
    }

    private void setupSimulation() {
        lights = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            lights.add(new TrafficLight());
        }
        intersection = new CrossIntersection("cross1", WIDTH / 2.0, HEIGHT / 2.0, lights);
        cars = new ArrayList<>();
        cars.add(new Car("car1", 100, HEIGHT / 2.0 - 30, 80, 0, 40, 20, false));
        cars.add(new Car("car2", WIDTH / 2.0 + 30, 100, 80, Math.PI / 2, 40, 20, false));
    }

    public void update(double dt) {
        // Cập nhật trạng thái đèn giao thông
        for (TrafficLight light : lights) {
            light.update(dt);
        }

        // Sinh xe mới mỗi 13 giây ở mỗi hướng
        for (int i = 0; i < 4; i++) {
            carSpawnTimers[i] += dt;
            if (carSpawnTimers[i] >= 13.0) {
                carSpawnTimers[i] = 0;
                carCount++;
                // Hướng 0: trái sang phải
                if (i == 0) {
                    cars.add(new Car("carL"+carCount, 100, HEIGHT / 2.0 - 30, 80, 0, 40, 20, false));
                }
                // Hướng 1: phải sang trái
                else if (i == 1) {
                    cars.add(new Car("carR"+carCount, WIDTH - 100, HEIGHT / 2.0 + 30, 80, Math.PI, 40, 20, false));
                }
                // Hướng 2: trên xuống
                else if (i == 2) {
                    cars.add(new Car("carT"+carCount, WIDTH / 2.0 + 30, 100, 80, Math.PI / 2, 40, 20, false));
                }
                // Hướng 3: dưới lên
                else if (i == 3) {
                    cars.add(new Car("carB"+carCount, WIDTH / 2.0 - 30, HEIGHT - 100, 80, -Math.PI / 2, 40, 20, false));
                }
            }
        }

        // Cập nhật trạng thái xe (logic dừng/đi sẽ chuyển sang Car)
        for (Car car : cars) {
            car.update(dt, cars, lights, WIDTH, HEIGHT);
        }
        intersection.update();
    }

    public List<Car> getCars() {
        return cars;
    }

    public List<TrafficLight> getLights() {
        return lights;
    }

    public CrossIntersection getIntersection() {
        return intersection;
    }
}
