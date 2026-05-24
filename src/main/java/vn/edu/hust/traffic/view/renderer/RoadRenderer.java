package vn.edu.hust.traffic.view.renderer;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import vn.edu.hust.traffic.view.MapType;
import vn.edu.hust.traffic.view.camera.Camera;

public class RoadRenderer {
    private static final double CROSS_X = 400.0;
    private static final double THREE_WAY_X = 1000.0;
    private static final double CENTER_Y = 300.0;
    private static final double STOP_OFFSET = 120.0;
    private static final Color ROAD = Color.web("#3b3f46");
    private static final Color ROAD_DARK = Color.web("#30343a");
    private static final Color LANE = Color.web("#f1f3f2");
    private static final Color MEDIAN = Color.web("#f1c84b");
    private static final Color SIDEWALK = Color.web("#cdd4d1");

    public void render(GraphicsContext gc, Camera camera, MapType mapType) {
        drawWorldBackground(gc, camera);
        switch (mapType) {
            case T_INTERSECTION -> drawTIntersection(gc, camera);
            case CROSS_INTERSECTION -> drawCrossIntersection(gc, camera);
            case FIVE_WAY_INTERSECTION -> drawFiveWayIntersection(gc, camera);
            case ROAD_NETWORK -> drawRoadNetwork(gc, camera);
        }
    }

    private void drawWorldBackground(GraphicsContext gc, Camera camera) {
        fillWorldRect(gc, camera, camera.getWorldX(), camera.getWorldY(),
                camera.getWorldWidth(), camera.getWorldHeight(), Color.web("#dfe8df"));
    }

    private void drawCrossIntersection(GraphicsContext gc, Camera camera) {
        drawCrossIntersectionAt(gc, camera, CROSS_X, 0, 800);
    }

    private void drawTIntersection(GraphicsContext gc, Camera camera) {
        drawThreeWayIntersectionAt(gc, camera, THREE_WAY_X, 600, 1400);
    }

    private void drawFiveWayIntersection(GraphicsContext gc, Camera camera) {
        drawCrossIntersection(gc, camera);
        drawRoad(gc, camera, CROSS_X + 150, CENTER_Y - 120, 620, 120, -35);
        drawDashedCenterLine(gc, camera, CROSS_X + 150, CENTER_Y - 120, 560, -35, MEDIAN);
        drawRoad(gc, camera, CROSS_X - 15, CENTER_Y + 5, 250, 170, -35);
    }

    private void drawRoadNetwork(GraphicsContext gc, Camera camera) {
        drawRoad(gc, camera, 700, CENTER_Y, 1500, 160, 0);
        drawRoad(gc, camera, CROSS_X, CENTER_Y, 720, 160, 90);
        drawRoad(gc, camera, THREE_WAY_X, CENTER_Y + 160, 440, 160, 90);

        drawNetworkTurnSlipRoads(gc, camera);

        drawHorizontalNetworkLaneMarkings(gc, camera);
        drawVerticalIntersectionLaneMarkings(gc, camera, CROSS_X, true);
        drawVerticalIntersectionLaneMarkings(gc, camera, THREE_WAY_X, false);

        drawCrossIntersectionArrows(gc, camera, CROSS_X);
        drawThreeWayIntersectionArrows(gc, camera, THREE_WAY_X);

        drawStopLines(gc, camera, CROSS_X, true);
        drawStopLines(gc, camera, THREE_WAY_X, false);
        drawCrosswalks(gc, camera, CROSS_X, true);
        drawCrosswalks(gc, camera, THREE_WAY_X, false);
    }

    private void drawNetworkTurnSlipRoads(GraphicsContext gc, Camera camera) {
        drawIntersectionCorner(gc, camera, CROSS_X, CENTER_Y, -1, -1);
        drawIntersectionCorner(gc, camera, CROSS_X, CENTER_Y, 1, -1);
        drawIntersectionCorner(gc, camera, CROSS_X, CENTER_Y, -1, 1);
        drawIntersectionCorner(gc, camera, CROSS_X, CENTER_Y, 1, 1);

        drawIntersectionCorner(gc, camera, THREE_WAY_X, CENTER_Y, -1, 1);
        drawIntersectionCorner(gc, camera, THREE_WAY_X, CENTER_Y, 1, 1);
    }

    private void drawCrossIntersectionAt(GraphicsContext gc, Camera camera, double centerX, double fromX, double toX) {
        drawRoad(gc, camera, centerX, CENTER_Y, toX - fromX + 100, 160, 0);
        drawRoad(gc, camera, centerX, CENTER_Y, 720, 160, 90);
        drawIntersectionCorner(gc, camera, centerX, CENTER_Y, -1, -1);
        drawIntersectionCorner(gc, camera, centerX, CENTER_Y, 1, -1);
        drawIntersectionCorner(gc, camera, centerX, CENTER_Y, -1, 1);
        drawIntersectionCorner(gc, camera, centerX, CENTER_Y, 1, 1);
        drawLaneMarkings(gc, camera, true, CENTER_Y, fromX, centerX - STOP_OFFSET);
        drawLaneMarkings(gc, camera, true, CENTER_Y, centerX + STOP_OFFSET, toX);
        drawVerticalIntersectionLaneMarkings(gc, camera, centerX, true);
        drawCrossIntersectionArrows(gc, camera, centerX);
        drawStopLines(gc, camera, centerX, true);
        drawCrosswalks(gc, camera, centerX, true);
    }

    private void drawThreeWayIntersectionAt(GraphicsContext gc, Camera camera, double centerX, double fromX, double toX) {
        drawRoad(gc, camera, centerX, CENTER_Y, toX - fromX + 100, 160, 0);
        drawRoad(gc, camera, centerX, CENTER_Y + 165, 430, 160, 90);
        fillWorldRect(gc, camera, centerX - 80, CENTER_Y - 80, 160, 160, ROAD);
        drawIntersectionCorner(gc, camera, centerX, CENTER_Y, -1, 1);
        drawIntersectionCorner(gc, camera, centerX, CENTER_Y, 1, 1);
        drawLaneMarkings(gc, camera, true, CENTER_Y, fromX, centerX - STOP_OFFSET);
        drawLaneMarkings(gc, camera, true, CENTER_Y, centerX + STOP_OFFSET, toX);
        drawVerticalIntersectionLaneMarkings(gc, camera, centerX, false);
        drawThreeWayIntersectionArrows(gc, camera, centerX);
        drawStopLines(gc, camera, centerX, false);
        drawCrosswalks(gc, camera, centerX, false);
    }

    private void drawHorizontalNetworkLaneMarkings(GraphicsContext gc, Camera camera) {
        drawLaneMarkings(gc, camera, true, CENTER_Y, 0, CROSS_X - STOP_OFFSET);
        drawLaneMarkings(gc, camera, true, CENTER_Y, CROSS_X + STOP_OFFSET, THREE_WAY_X - STOP_OFFSET);
        drawLaneMarkings(gc, camera, true, CENTER_Y, THREE_WAY_X + STOP_OFFSET, 1400);
    }

    private void drawVerticalIntersectionLaneMarkings(GraphicsContext gc, Camera camera,
            double centerX, boolean includeNorthApproach) {
        if (includeNorthApproach) {
            drawLaneMarkings(gc, camera, false, centerX, 0, CENTER_Y - STOP_OFFSET);
        }
        drawLaneMarkings(gc, camera, false, centerX, CENTER_Y + STOP_OFFSET, 600);
    }

    private void drawCrossIntersectionArrows(GraphicsContext gc, Camera camera, double centerX) {
        drawLaneArrows(gc, camera, centerX - STOP_OFFSET, CENTER_Y, 0, "left", "straight", "straight_right");
        drawLaneArrows(gc, camera, centerX + STOP_OFFSET, CENTER_Y, 180, "left", "straight", "straight_right");
        drawLaneArrows(gc, camera, centerX, CENTER_Y - STOP_OFFSET, 90, "left", "straight", "straight_right");
        drawLaneArrows(gc, camera, centerX, CENTER_Y + STOP_OFFSET, 270, "left", "straight", "straight_right");
    }

    private void drawThreeWayIntersectionArrows(GraphicsContext gc, Camera camera, double centerX) {
        drawLaneArrows(gc, camera, centerX - STOP_OFFSET, CENTER_Y, 0, "straight", "straight", "straight_right");
        drawLaneArrows(gc, camera, centerX + STOP_OFFSET, CENTER_Y, 180, "left", "straight", "straight");
        drawLaneArrows(gc, camera, centerX, CENTER_Y + STOP_OFFSET, 270, "left", "left_right", "right");
    }

    private void drawRoad(GraphicsContext gc, Camera camera, double centerX, double centerY,
            double length, double width, double angleDegrees) {
        Point2D p = camera.worldToScreen(centerX, centerY);
        double scale = camera.getScale();

        gc.save();
        gc.translate(p.getX(), p.getY());
        gc.rotate(angleDegrees);
        gc.setFill(SIDEWALK);
        gc.fillRect(-length * scale / 2.0, -width * scale / 2.0 - 10 * scale, length * scale, (width + 20) * scale);
        gc.setFill(ROAD_DARK);
        gc.fillRect(-length * scale / 2.0, -width * scale / 2.0, length * scale, width * scale);
        gc.setFill(ROAD);
        gc.fillRect(-length * scale / 2.0, -width * scale / 2.0 + 3 * scale, length * scale, (width - 6) * scale);
        gc.restore();
    }

    private void drawLaneMarkings(GraphicsContext gc, Camera camera, boolean horizontal,
            double center, double from, double to) {
        double[] offsets = { -53, -27, 27, 53 };
        for (double offset : offsets) {
            if (horizontal) {
                strokeWorldLine(gc, camera, from, center + offset, to, center + offset, LANE, 1.0, true);
            } else {
                strokeWorldLine(gc, camera, center + offset, from, center + offset, to, LANE, 1.0, true);
            }
        }

        if (horizontal) {
            strokeWorldLine(gc, camera, from, center - 2, to, center - 2, MEDIAN, 2.0, false);
            strokeWorldLine(gc, camera, from, center + 2, to, center + 2, MEDIAN, 2.0, false);
        } else {
            strokeWorldLine(gc, camera, center - 2, from, center - 2, to, MEDIAN, 2.0, false);
            strokeWorldLine(gc, camera, center + 2, from, center + 2, to, MEDIAN, 2.0, false);
        }
    }

    private void drawStopLines(GraphicsContext gc, Camera camera, double centerX, boolean includeNorthApproach) {
        strokeWorldLine(gc, camera, centerX - STOP_OFFSET, CENTER_Y + 5, centerX - STOP_OFFSET, CENTER_Y + 75, LANE, 4.0, false);
        strokeWorldLine(gc, camera, centerX + STOP_OFFSET, CENTER_Y - 5, centerX + STOP_OFFSET, CENTER_Y - 75, LANE, 4.0, false);
        if (includeNorthApproach) {
            strokeWorldLine(gc, camera, centerX - 75, CENTER_Y - STOP_OFFSET, centerX - 5, CENTER_Y - STOP_OFFSET, LANE, 4.0, false);
        }
        strokeWorldLine(gc, camera, centerX + 75, CENTER_Y + STOP_OFFSET, centerX + 5, CENTER_Y + STOP_OFFSET, LANE, 4.0, false);
    }

    private void drawLaneArrows(GraphicsContext gc, Camera camera, double x, double y, double rotationDegree,
            String lane1, String lane2, String lane3) {
        Point2D p = camera.worldToScreen(x, y);
        double scale = camera.getScale();

        gc.save();
        gc.translate(p.getX(), p.getY());
        gc.rotate(rotationDegree);
        gc.scale(scale, scale);
        gc.setStroke(LANE);
        gc.setLineWidth(2.0);
        gc.setLineDashes(new double[0]);

        double arrowX = -25;
        drawSingleArrow(gc, arrowX, 15, lane1);
        drawSingleArrow(gc, arrowX, 40, lane2);
        drawSingleArrow(gc, arrowX, 65, lane3);

        gc.restore();
    }

    private void drawSingleArrow(GraphicsContext gc, double x, double y, String type) {
        if ("none".equals(type)) {
            return;
        }

        gc.save();
        gc.translate(x, y);

        if ("straight".equals(type)) {
            gc.strokeLine(-10, 0, 10, 0);
            gc.strokeLine(10, 0, 5, -4);
            gc.strokeLine(10, 0, 5, 4);
        } else if ("left".equals(type)) {
            gc.strokeLine(-10, 0, 3, 0);
            gc.strokeLine(3, 0, 3, -10);
            gc.strokeLine(3, -10, 0, -7);
            gc.strokeLine(3, -10, 6, -7);
        } else if ("right".equals(type)) {
            gc.strokeLine(-10, 0, 3, 0);
            gc.strokeLine(3, 0, 3, 10);
            gc.strokeLine(3, 10, 0, 7);
            gc.strokeLine(3, 10, 6, 7);
        } else if ("left_right".equals(type)) {
            gc.strokeLine(-10, 0, 3, 0);
            gc.strokeLine(3, 0, 3, -10);
            gc.strokeLine(3, -10, 0, -7);
            gc.strokeLine(3, -10, 6, -7);
            gc.strokeLine(3, 0, 3, 10);
            gc.strokeLine(3, 10, 0, 7);
            gc.strokeLine(3, 10, 6, 7);
        } else if ("straight_right".equals(type)) {
            gc.strokeLine(-10, 0, 10, 0);
            gc.strokeLine(10, 0, 5, -4);
            gc.strokeLine(10, 0, 5, 4);
            gc.strokeLine(3, 0, 3, 10);
            gc.strokeLine(3, 10, 0, 7);
            gc.strokeLine(3, 10, 6, 7);
        }

        gc.restore();
    }

    private void drawCrosswalks(GraphicsContext gc, Camera camera, double centerX, boolean includeNorthApproach) {
        drawZebraCrossing(gc, camera, centerX - 95, CENTER_Y - 75, centerX - 85, CENTER_Y + 75, true);
        drawZebraCrossing(gc, camera, centerX + 85, CENTER_Y - 75, centerX + 95, CENTER_Y + 75, true);
        if (includeNorthApproach) {
            drawZebraCrossing(gc, camera, centerX - 75, CENTER_Y - 95, centerX + 75, CENTER_Y - 85, false);
        }
        drawZebraCrossing(gc, camera, centerX - 75, CENTER_Y + 85, centerX + 75, CENTER_Y + 95, false);
    }

    private void drawZebraCrossing(GraphicsContext gc, Camera camera, double startX, double startY,
            double endX, double endY, boolean horizontalRoad) {
        if (horizontalRoad) {
            for (double y = startY; y <= endY; y += 12) {
                strokeWorldLine(gc, camera, startX, y, endX, y, LANE, 5.0, false);
            }
        } else {
            for (double x = startX; x <= endX; x += 12) {
                strokeWorldLine(gc, camera, x, startY, x, endY, LANE, 5.0, false);
            }
        }
    }

    private void drawIntersectionCorner(GraphicsContext gc, Camera camera, double centerX, double centerY,
            double signX, double signY) {
        fillWorldPolygon(gc, camera,
                new double[] { centerX + signX * 250, centerX + signX * 80, centerX + signX * 80 },
                new double[] { centerY + signY * 80, centerY + signY * 250, centerY + signY * 80 },
                ROAD_DARK);
        strokeWorldLine(gc, camera,
                centerX + signX * 250, centerY + signY * 80,
                centerX + signX * 80, centerY + signY * 250,
                Color.web("#8f969d"), 2.0, false);

        fillWorldPolygon(gc, camera,
                new double[] { centerX + signX * 186, centerX + signX * 80, centerX + signX * 80 },
                new double[] { centerY + signY * 80, centerY + signY * 186, centerY + signY * 80 },
                Color.web("#a06050"));
        strokeWorldPolygon(gc, camera,
                new double[] { centerX + signX * 186, centerX + signX * 80, centerX + signX * 80 },
                new double[] { centerY + signY * 80, centerY + signY * 186, centerY + signY * 80 },
                Color.web("#dddddd"), 2.0);

        strokeWorldLine(gc, camera,
                centerX + signX * 218, centerY + signY * 80,
                centerX + signX * 80, centerY + signY * 218,
                LANE, 2.0, true);
    }

    private void drawDashedCenterLine(GraphicsContext gc, Camera camera, double centerX, double centerY,
            double length, double angleDegrees, Color color) {
        Point2D p = camera.worldToScreen(centerX, centerY);
        double scale = camera.getScale();

        gc.save();
        gc.translate(p.getX(), p.getY());
        gc.rotate(angleDegrees);
        gc.setStroke(color);
        gc.setLineWidth(Math.max(1.0, 2.0 * scale));
        gc.setLineDashes(18.0 * scale, 14.0 * scale);
        gc.strokeLine(-length * scale / 2.0, 0, length * scale / 2.0, 0);
        gc.setLineDashes(new double[0]);
        gc.restore();
    }

    private void fillWorldRect(GraphicsContext gc, Camera camera, double x, double y,
            double width, double height, Color color) {
        Point2D p = camera.worldToScreen(x, y);
        gc.setFill(color);
        gc.fillRect(p.getX(), p.getY(), width * camera.getScale(), height * camera.getScale());
    }

    private void fillWorldPolygon(GraphicsContext gc, Camera camera, double[] worldX, double[] worldY, Color color) {
        double[] screenX = new double[worldX.length];
        double[] screenY = new double[worldY.length];
        for (int i = 0; i < worldX.length; i++) {
            Point2D p = camera.worldToScreen(worldX[i], worldY[i]);
            screenX[i] = p.getX();
            screenY[i] = p.getY();
        }
        gc.setFill(color);
        gc.fillPolygon(screenX, screenY, worldX.length);
    }

    private void strokeWorldPolygon(GraphicsContext gc, Camera camera, double[] worldX, double[] worldY,
            Color color, double width) {
        double[] screenX = new double[worldX.length];
        double[] screenY = new double[worldY.length];
        for (int i = 0; i < worldX.length; i++) {
            Point2D p = camera.worldToScreen(worldX[i], worldY[i]);
            screenX[i] = p.getX();
            screenY[i] = p.getY();
        }
        gc.setStroke(color);
        gc.setLineWidth(Math.max(1.0, width * camera.getScale()));
        gc.strokePolygon(screenX, screenY, worldX.length);
    }

    private void strokeWorldLine(GraphicsContext gc, Camera camera, double x1, double y1,
            double x2, double y2, Color color, double width, boolean dashed) {
        Point2D p1 = camera.worldToScreen(x1, y1);
        Point2D p2 = camera.worldToScreen(x2, y2);
        gc.setStroke(color);
        gc.setLineWidth(Math.max(1.0, width * camera.getScale()));
        if (dashed) {
            gc.setLineDashes(14.0 * camera.getScale(), 12.0 * camera.getScale());
        } else {
            gc.setLineDashes(new double[0]);
        }
        gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        gc.setLineDashes(new double[0]);
    }
}
