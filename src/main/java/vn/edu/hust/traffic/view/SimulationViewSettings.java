package vn.edu.hust.traffic.view;

public class SimulationViewSettings {
    private MapType mapType = MapType.ROAD_NETWORK;
    private RenderMode renderMode = RenderMode.BASIC;
    private ControlMode controlMode = ControlMode.AUTO;
    private LightDisplayMode lightDisplayMode = LightDisplayMode.FULL_COUNTDOWN;
    private int trafficDensity = 2;
    private double simulationSpeed = 1.0;
    private boolean soundEnabled = true;
    private double volume = 0.45;
    private boolean showOverlay = true;

    public MapType getMapType() {
        return mapType;
    }

    public void setMapType(MapType mapType) {
        this.mapType = mapType;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public void setRenderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
    }

    public ControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(ControlMode controlMode) {
        this.controlMode = controlMode;
    }

    public LightDisplayMode getLightDisplayMode() {
        return lightDisplayMode;
    }

    public void setLightDisplayMode(LightDisplayMode lightDisplayMode) {
        this.lightDisplayMode = lightDisplayMode;
    }

    public int getTrafficDensity() {
        return trafficDensity;
    }

    public void setTrafficDensity(int trafficDensity) {
        this.trafficDensity = Math.max(1, Math.min(3, trafficDensity));
    }

    public double getSimulationSpeed() {
        return simulationSpeed;
    }

    public void setSimulationSpeed(double simulationSpeed) {
        this.simulationSpeed = Math.max(0.25, Math.min(3.0, simulationSpeed));
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
    }

    public boolean isShowOverlay() {
        return showOverlay;
    }

    public void setShowOverlay(boolean showOverlay) {
        this.showOverlay = showOverlay;
    }
}
