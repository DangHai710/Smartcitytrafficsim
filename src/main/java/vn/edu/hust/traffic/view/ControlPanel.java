package vn.edu.hust.traffic.view;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
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
        auto.setSelected(true);

        auto.setOnAction(e -> SimulationConfig.autoMode = true);
        manual.setOnAction(e -> SimulationConfig.autoMode = false);

        CheckBox sound = new CheckBox("Sound");
        sound.setSelected(true);
        sound.setOnAction(e ->
                SimulationConfig.soundEnabled = sound.isSelected()
        );

        ComboBox<String> density = new ComboBox<>();
        density.getItems().addAll("LOW", "MEDIUM", "HIGH");
        density.setValue("MEDIUM");

        density.setOnAction(e -> {
            SimulationConfig.density =
                    switch (density.getValue()) {
                        case "LOW" -> 1;
                        case "HIGH" -> 3;
                        default -> 2;
                    };
        });

        getChildren().addAll(title, auto, manual, sound, density);
    }
}