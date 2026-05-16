package vn.edu.hust.traffic.view;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import vn.edu.hust.traffic.config.SimulationConfig;
import vn.edu.hust.traffic.utils.SoundPlayer;

public class ControlPanel extends VBox {

	public ControlPanel() {

		setStyle("-fx-padding: 10; -fx-background-color: #2b2b2b;");

		Label title = new Label("CONTROL PANEL");
		title.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

		ToggleButton auto = new ToggleButton("AUTO");
		ToggleButton manual = new ToggleButton("MANUAL");

		ToggleGroup group = new ToggleGroup();
		auto.setToggleGroup(group);
		manual.setToggleGroup(group);
		auto.setSelected(SimulationConfig.autoMode);
		manual.setSelected(!SimulationConfig.autoMode);

		auto.setOnAction(e -> SimulationConfig.autoMode = true);
		manual.setOnAction(e -> SimulationConfig.autoMode = false);
		ToggleButton basic = new ToggleButton("BASIC");
		ToggleButton graphic = new ToggleButton("GRAPHIC");

		ToggleGroup renderGroup = new ToggleGroup();
		basic.setToggleGroup(renderGroup);
		graphic.setToggleGroup(renderGroup);

		basic.setSelected(!SimulationConfig.graphicMode);
		graphic.setSelected(SimulationConfig.graphicMode);

		basic.setOnAction(e -> SimulationConfig.graphicMode = false);
		graphic.setOnAction(e -> SimulationConfig.graphicMode = true);

		CheckBox sound = new CheckBox("Sound");
		sound.setSelected(SimulationConfig.soundEnabled);
		sound.setOnAction(e -> {
			SimulationConfig.soundEnabled = sound.isSelected();
			SoundPlayer.updateBackgroundMusicState();
		});
		ComboBox<String> density = new ComboBox<>();
		density.getItems().addAll("LOW", "MEDIUM", "HIGH");
		density.setValue("MEDIUM");

		density.setOnAction(e -> {
			SimulationConfig.density = switch (density.getValue()) {
			case "LOW" -> 1;
			case "HIGH" -> 3;
			default -> 2;
			};
		});

		getChildren().addAll(title, auto, manual, basic, graphic, sound, density);
	}
}