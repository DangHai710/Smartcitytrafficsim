package vn.edu.hust.traffic.model.vehicle;

import vn.edu.hust.traffic.model.map.TrafficLight;
import java.util.List;

/**
 * Lớp Car mô phỏng xe ô tô trong hệ thống giao thông thông minh.
 * Xe có thể di chuyển, thay đổi tốc độ, hướng và tuân thủ luật giao thông.
 *
 * @author Người 1
 */
public class Car extends Vehicle {
    public Car(String id, double x, double y, double speed, double direction, double width, double height, boolean isPriorityVehicle) {
        super(id, x, y, speed, direction, width, height, isPriorityVehicle);
    }

    /**
     * Cập nhật trạng thái xe: dừng/đi theo đèn, giảm tốc, giữ khoảng cách an toàn.
     * @param dt delta time
     * @param cars danh sách xe cùng làn
     * @param lights danh sách đèn giao thông
     * @param WIDTH chiều rộng màn hình
     * @param HEIGHT chiều cao màn hình
     */
    public void update(double dt, List<Car> cars, List<TrafficLight> lights, int WIDTH, int HEIGHT) {
        double stopX = WIDTH / 2.0 - 60;
        double stopY = HEIGHT / 2.0 - 60;
        double safeDistance = 41;
        boolean shouldStop = false;
        double targetSpeed = 80;

        int lightIdx = 0;
        if (Math.abs(getDirection() - 0) < 0.01) lightIdx = 0;
        else if (Math.abs(getDirection() - Math.PI) < 0.01) lightIdx = 1;
        else if (Math.abs(getDirection() - Math.PI/2) < 0.01) lightIdx = 2;
        else if (Math.abs(getDirection() + Math.PI/2) < 0.01) lightIdx = 3;
        TrafficLight light = lights.get(lightIdx);

        boolean nearStopLine = false;
        boolean passedStopLine = false;
        if (lightIdx == 0) {
            nearStopLine = getX() + getWidth() + 2 >= stopX && getX() < stopX;
            passedStopLine = getX() >= stopX;
        } else if (lightIdx == 1) {
            nearStopLine = getX() <= WIDTH / 2.0 + 60 && getX() > WIDTH / 2.0 + 60 - getWidth();
            passedStopLine = getX() <= WIDTH / 2.0 + 60 - getWidth();
        } else if (lightIdx == 2) {
            nearStopLine = getY() + getHeight() + 2 >= stopY && getY() < stopY;
            passedStopLine = getY() >= stopY;
        } else if (lightIdx == 3) {
            nearStopLine = getY() <= HEIGHT / 2.0 + 60 && getY() > HEIGHT / 2.0 + 60 - getHeight();
            passedStopLine = getY() <= HEIGHT / 2.0 + 60 - getHeight();
        }

        Car carAhead = null;
        for (Car other : cars) {
            if (other == this) continue;
            if (Math.abs(other.getDirection() - getDirection()) < 0.01) {
                if ((lightIdx == 0 && other.getX() > getX()) ||
                    (lightIdx == 1 && other.getX() < getX()) ||
                    (lightIdx == 2 && other.getY() > getY()) ||
                    (lightIdx == 3 && other.getY() < getY())) {
                    if (carAhead == null ||
                        (lightIdx == 0 && other.getX() < carAhead.getX()) ||
                        (lightIdx == 1 && other.getX() > carAhead.getX()) ||
                        (lightIdx == 2 && other.getY() < carAhead.getY()) ||
                        (lightIdx == 3 && other.getY() > carAhead.getY())) {
                        carAhead = other;
                    }
                }
            }
        }

        if (light.getState() == TrafficLight.State.RED) {
            if (passedStopLine) {
                shouldStop = false;
            } else if (nearStopLine) {
                shouldStop = true;
            } else {
                shouldStop = false;
            }
        } else {
            if (light.getTimeLeft() <= 4) {
                targetSpeed = 30;
            }
            if (light.getTimeLeft() <= 5 && nearStopLine) {
                shouldStop = false;
            }
        }

        if (carAhead != null) {
            double dist = 9999;
            if (lightIdx == 0) dist = carAhead.getX() - (getX() + getWidth());
            else if (lightIdx == 1) dist = getX() - (carAhead.getX() + carAhead.getWidth());
            else if (lightIdx == 2) dist = carAhead.getY() - (getY() + getHeight());
            else if (lightIdx == 3) dist = getY() - (carAhead.getY() + carAhead.getHeight());
            if (dist < safeDistance) {
                shouldStop = true;
            }
        }

        if (shouldStop) {
            // Không di chuyển
        } else {
            setSpeed(targetSpeed);
            movePhysically(dt);
        }
    }

    @Override
    public void movePhysically(double dt) {
        // Di chuyển theo hướng và tốc độ
        setX(getX() + Math.cos(getDirection()) * getSpeed() * dt);
        setY(getY() + Math.sin(getDirection()) * getSpeed() * dt);
    }
    @Override
    public void render() {
        // TODO: Implement rendering logic for Car (handled in View/Renderer)
    }
}
