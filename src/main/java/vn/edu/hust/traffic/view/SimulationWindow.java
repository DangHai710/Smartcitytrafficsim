package vn.edu.hust.traffic.view;

import java.util.List;
import vn.edu.hust.traffic.controller.TrafficController;
import vn.edu.hust.traffic.config.SimulationConfig;
import vn.edu.hust.traffic.controller.IntersectionPhaseController;
import vn.edu.hust.traffic.model.map.TrafficLight;
import vn.edu.hust.traffic.model.vehicle.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import vn.edu.hust.traffic.utils.ImageLoader;
import vn.edu.hust.traffic.utils.SoundPlayer;

public class SimulationWindow extends Application {
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private Canvas canvas;
	private GraphicsContext gc;

	private TrafficController controller;

	@Override
	public void start(Stage primaryStage) {
		canvas = new Canvas(WIDTH, HEIGHT);
		gc = canvas.getGraphicsContext2D();
		Pane root = new Pane();

		ControlPanel panel = new ControlPanel();
		
		panel.setLayoutX(820);
		panel.setLayoutY(20);
		root.getChildren().addAll(canvas, panel);
		Scene scene = new Scene(root, WIDTH + 200, HEIGHT);

		scene.setOnKeyPressed(event -> {
		    switch (event.getCode()) {
		    case C:
		        controller.spawnVehicleManually("Car");
		        SoundPlayer.playHorn();
		        break;
		    case B:
		        controller.spawnVehicleManually("Bus");
		        SoundPlayer.playHorn();
		        break;
		    case M:
		        controller.spawnVehicleManually("Motorbike");
		        SoundPlayer.playSignal();
		        break;
		    case E:
		        controller.spawnVehicleManually("Emergency");
		        SoundPlayer.playAmbulance();
		        break;
		    case A:
		        controller.spawnVehicleManually("Ambulance");
		        SoundPlayer.playAmbulance();
		        break;
		    case F:
		        controller.spawnVehicleManually("FireTruck");
		        SoundPlayer.playHorn();
		        break;
		    case P:
		        controller.toggleAutoSpawn();
		        break;
		    default:
		        break;
		    }
		});

		setupSimulation();

		primaryStage.setTitle("Traffic Simulation - 6 Lanes Two-Way");
		primaryStage.setScene(scene);
		primaryStage.show();
		SoundPlayer.playBackgroundMusic();
		
		AnimationTimer timer = new AnimationTimer() {
			private long lastUpdate = 0;

			@Override
			public void handle(long now) {
				if (lastUpdate > 0) {
				    double dt = (now - lastUpdate) / 1e9;
				    update(dt);
				    render();

				    gc.setFill(javafx.scene.paint.Color.WHITE);
				    gc.setFont(Font.font("System", FontWeight.BOLD, 14));

				    gc.fillText("Mode: " + (SimulationConfig.autoMode ? "AUTO" : "MANUAL"), 16, 64);

				    gc.fillText("Density: " + switch (SimulationConfig.density) {
				    case 1 -> "LOW";
				    case 2 -> "MEDIUM";
				    case 3 -> "HIGH";
				    default -> "UNKNOWN";
				    }, 16, 84);

				    gc.fillText("View: " + (SimulationConfig.graphicMode ? "GRAPHIC" : "BASIC"), 16, 104);
				}

				lastUpdate = now;
			}
		};
		timer.start();
	}

	private void setupSimulation() {
		controller = new TrafficController();
	}

	private void update(double dt) {
		controller.update(dt);
	}

	private void render() {
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		List<TrafficLight> lights = controller.getLights();
		List<Vehicle> vehicles = controller.getVehicles();
		IntersectionPhaseController phase = controller.getPhaseController();

		// 1. Vẽ đường ngang (Rộng 160px)
		gc.setFill(javafx.scene.paint.Color.web("#333333"));
		gc.fillRect(0, HEIGHT / 2.0 - 80, WIDTH, 160);

		// Vạch trắng phân làn ngang
		gc.setStroke(javafx.scene.paint.Color.WHITE);
		gc.setLineWidth(1);
		gc.setLineDashes(15);
		gc.strokeLine(0, HEIGHT / 2.0 - 53, WIDTH, HEIGHT / 2.0 - 53);
		gc.strokeLine(0, HEIGHT / 2.0 - 27, WIDTH, HEIGHT / 2.0 - 27);
		gc.strokeLine(0, HEIGHT / 2.0 + 27, WIDTH, HEIGHT / 2.0 + 27);
		gc.strokeLine(0, HEIGHT / 2.0 + 53, WIDTH, HEIGHT / 2.0 + 53);

		// Dải phân cách vàng đôi ngang
		gc.setLineDashes(0);
		gc.setStroke(javafx.scene.paint.Color.YELLOW);
		gc.setLineWidth(2);
		gc.strokeLine(0, HEIGHT / 2.0 - 2, WIDTH, HEIGHT / 2.0 - 2);
		gc.strokeLine(0, HEIGHT / 2.0 + 2, WIDTH, HEIGHT / 2.0 + 2);

		// 2. Vẽ đường dọc (Rộng 160px)
		gc.setFill(javafx.scene.paint.Color.web("#333333"));
		gc.fillRect(WIDTH / 2.0 - 80, 0, 160, HEIGHT);

		// Vạch trắng phân làn dọc
		gc.setStroke(javafx.scene.paint.Color.WHITE);
		gc.setLineWidth(1);
		gc.setLineDashes(15);
		gc.strokeLine(WIDTH / 2.0 - 53, 0, WIDTH / 2.0 - 53, HEIGHT);
		gc.strokeLine(WIDTH / 2.0 - 27, 0, WIDTH / 2.0 - 27, HEIGHT);
		gc.strokeLine(WIDTH / 2.0 + 27, 0, WIDTH / 2.0 + 27, HEIGHT);
		gc.strokeLine(WIDTH / 2.0 + 53, 0, WIDTH / 2.0 + 53, HEIGHT);

		// Dải phân cách vàng đôi dọc
		gc.setLineDashes(0);
		gc.setStroke(javafx.scene.paint.Color.YELLOW);
		gc.strokeLine(WIDTH / 2.0 - 2, 0, WIDTH / 2.0 - 2, HEIGHT);
		gc.strokeLine(WIDTH / 2.0 + 2, 0, WIDTH / 2.0 + 2, HEIGHT);

		// 3. Vạch dừng (Stop Lines)
		gc.setStroke(javafx.scene.paint.Color.WHITE);
		gc.setLineWidth(4);
		gc.strokeLine(WIDTH / 2.0 - 100, HEIGHT / 2.0 + 5, WIDTH / 2.0 - 100, HEIGHT / 2.0 + 75); // Hướng LTR
		gc.strokeLine(WIDTH / 2.0 + 100, HEIGHT / 2.0 - 5, WIDTH / 2.0 + 100, HEIGHT / 2.0 - 75); // Hướng RTL
		gc.strokeLine(WIDTH / 2.0 - 75, HEIGHT / 2.0 - 100, WIDTH / 2.0 - 5, HEIGHT / 2.0 - 100); // Hướng TTB
		gc.strokeLine(WIDTH / 2.0 + 75, HEIGHT / 2.0 + 100, WIDTH / 2.0 + 5, HEIGHT / 2.0 + 100); // Hướng BTT

		
		// 4. Ve phuong tien
		for (Vehicle v : vehicles) {
		    drawVehicle(v);
		}

		// 5. Đèn giao thông (Cụm 3 đèn Đỏ - Vàng - Xanh) - Vẽ sau xe để nổi lên trên
		for (int i = 0; i < 4; i++) {
			TrafficLight light = lights.get(i);
			double lx = 0, ly = 0;
			switch (i) {
			case 0:
				lx = WIDTH / 2.0 - 140;
				ly = HEIGHT / 2.0 - 205;
				break; // Kéo hẳn lên trên, không lẹm vào đường ngang
			case 1:
				lx = WIDTH / 2.0 + 115;
				ly = HEIGHT / 2.0 - 205;
				break; // Kéo hẳn lên trên
			case 2:
				lx = WIDTH / 2.0 - 140;
				ly = HEIGHT / 2.0 + 115;
				break;
			case 3:
				lx = WIDTH / 2.0 + 115;
				ly = HEIGHT / 2.0 + 115;
				break;
			}

			// Vẽ hộp đựng đèn
			gc.setFill(javafx.scene.paint.Color.web("#222222"));
			gc.fillRoundRect(lx - 5, ly - 5, 35, 95, 10, 10);

			// Đèn Đỏ
			gc.setFill(light.getState() == TrafficLight.State.RED ? javafx.scene.paint.Color.RED
					: javafx.scene.paint.Color.web("#440000"));
			gc.fillOval(lx + 2, ly + 2, 21, 21);

			// Đèn Vàng
			gc.setFill(light.getState() == TrafficLight.State.YELLOW ? javafx.scene.paint.Color.YELLOW
					: javafx.scene.paint.Color.web("#444400"));
			gc.fillOval(lx + 2, ly + 32, 21, 21);

			// Đèn Xanh
			gc.setFill(light.getState() == TrafficLight.State.GREEN ? javafx.scene.paint.Color.LIME
					: javafx.scene.paint.Color.web("#004400"));
			gc.fillOval(lx + 2, ly + 62, 21, 21);

			// Số giây hiển thị (vẽ ở dưới cụm đèn)
			gc.setFill(javafx.scene.paint.Color.WHITE);
			gc.setFont(Font.font("System", FontWeight.BOLD, 12));
			gc.fillText(String.valueOf(light.getTimeLeft()), lx + 7, ly + 105);
		}

		// 5. Phase HUD — hiển thị trạng thái phase hiện tại
		String[] phaseLabels = { "Phase 0: Ngang XANH | Doc DO", "Phase 1: Ngang VANG | Doc DO",
				"Phase 2: Ngang DO  | Doc XANH", "Phase 3: Ngang DO  | Doc VANG" };
		javafx.scene.paint.Color[] phaseColors = { javafx.scene.paint.Color.LIMEGREEN, javafx.scene.paint.Color.YELLOW,
				javafx.scene.paint.Color.LIMEGREEN, javafx.scene.paint.Color.YELLOW };
		int currentPhase = phase.getCurrentPhase();
		int phaseTimeLeft = (int) Math.ceil(phase.getPhaseTimeLeft());

		// Nền HUD bên trái
		gc.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.6));
		gc.fillRoundRect(8, 8, 280, 62, 10, 10);
		gc.setFont(Font.font("System", FontWeight.BOLD, 13));
		gc.setFill(phaseColors[currentPhase]);
		gc.fillText(phaseLabels[currentPhase], 16, 28);
		gc.setFill(javafx.scene.paint.Color.WHITE);
		gc.fillText("Con lai: " + phaseTimeLeft + "s   |   Xe: " + vehicles.size(), 16, 46);

	}
	
	private void drawVehicle(Vehicle v) {
	    if (SimulationConfig.graphicMode) {
	        drawGraphicVehicle(v);
	    } else {
	        drawBasicVehicle(v);
	    }

	    gc.setFont(Font.font("System", 10));
	    gc.setFill(javafx.scene.paint.Color.BLACK);
	    gc.fillText(v.getId(), v.getX() - 10, v.getY() - 15);
	}

	private void drawBasicVehicle(Vehicle v) {
	    if (v instanceof Ambulance) {
	        gc.setFill(v.isPriorityVehicle() ? javafx.scene.paint.Color.RED : javafx.scene.paint.Color.PINK);
	    } else if (v instanceof FireTruck) {
	        gc.setFill(javafx.scene.paint.Color.DARKRED);
	    } else if (v instanceof Bus) {
	        gc.setFill(javafx.scene.paint.Color.DARKGREEN);
	    } else if (v instanceof Motorbike) {
	        gc.setFill(javafx.scene.paint.Color.ORANGE);
	    } else {
	        gc.setFill(javafx.scene.paint.Color.BLUE);
	    }

	    drawVehicleRectangle(v);
	}

	
	private void drawGraphicVehicle(Vehicle v) {
	    boolean saved = false;

	    try {
	        Image image = ImageLoader.loadImage(getVehicleImageName(v));

	        gc.save();
	        saved = true;

	        gc.translate(v.getX(), v.getY());
	        gc.rotate(Math.toDegrees(v.getDirection()));
	        gc.drawImage(image, -v.getWidth() / 2, -v.getHeight() / 2, v.getWidth(), v.getHeight());
	    } catch (Exception e) {
	        drawBasicVehicle(v);
	    } finally {
	        if (saved) {
	            gc.restore();
	        }
	    }
	}

	private void drawVehicleRectangle(Vehicle v) {
	    boolean vertical = Math.abs(v.getDirection() - Math.PI / 2) < 0.1
	            || Math.abs(v.getDirection() + Math.PI / 2) < 0.1;

	    if (vertical) {
	        gc.fillRect(v.getX() - v.getHeight() / 2, v.getY() - v.getWidth() / 2, v.getHeight(), v.getWidth());
	    } else {
	        gc.fillRect(v.getX() - v.getWidth() / 2, v.getY() - v.getHeight() / 2, v.getWidth(), v.getHeight());
	    }
	}

	private String getVehicleImageName(Vehicle v) {
	    if (v instanceof FireTruck) {
	        return "firetruck.png";
	    }

	    if (v instanceof Ambulance) {
	        return "ambulance.png";
	    }

	    if (v instanceof Bus) {
	        return "bus.png";
	    }

	    if (v instanceof Motorbike) {
	        return "motorbike.png";
	    }

	    return "car.png";
	}
	public static void main(String[] args) {
		launch(args);
	}
}
