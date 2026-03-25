package com.trafficoptimizer.ui;

import com.trafficoptimizer.model.CityGrid;
import com.trafficoptimizer.model.CrosswalkZone;
import com.trafficoptimizer.model.Intersection;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class CityGridPanel extends Canvas {

    private static final int CELL_SIZE   = 140;
    private static final int MARGIN      = 60;
    private static final int NODE_RADIUS = 28;

    private CityGrid grid;

    public CityGridPanel(CityGrid grid) {
        this.grid = grid;
        int size = MARGIN * 2 + CELL_SIZE * (grid.getSize() - 1);
        setWidth(size);
        setHeight(size);
    }

    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.web("#F8F7F4"));
        gc.fillRect(0, 0, getWidth(), getHeight());
        drawRoads(gc);
        drawCrosswalks(gc);
        drawIntersections(gc);
    }

    private void drawRoads(GraphicsContext gc) {
        int size = grid.getSize();
        gc.setStroke(Color.web("#D3D1C7"));
        gc.setLineWidth(10);
        gc.setLineDashes(0);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int px = MARGIN + x * CELL_SIZE;
                int py = MARGIN + y * CELL_SIZE;
                if (x < size - 1)
                    gc.strokeLine(px + NODE_RADIUS, py,
                                  px + CELL_SIZE - NODE_RADIUS, py);
                if (y < size - 1)
                    gc.strokeLine(px, py + NODE_RADIUS,
                                  px, py + CELL_SIZE - NODE_RADIUS);
            }
        }
        gc.setStroke(Color.web("#FFFFFF", 0.5));
        gc.setLineWidth(1.5);
        gc.setLineDashes(8, 6);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int px = MARGIN + x * CELL_SIZE;
                int py = MARGIN + y * CELL_SIZE;
                if (x < size - 1)
                    gc.strokeLine(px + NODE_RADIUS, py,
                                  px + CELL_SIZE - NODE_RADIUS, py);
                if (y < size - 1)
                    gc.strokeLine(px, py + NODE_RADIUS,
                                  px, py + CELL_SIZE - NODE_RADIUS);
            }
        }
        gc.setLineDashes(0);
    }

    private void drawCrosswalks(GraphicsContext gc) {
        int size = grid.getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                CrosswalkZone cz = grid.get(x, y).getCrosswalkZone();
                int px = MARGIN + x * CELL_SIZE;
                int py = MARGIN + y * CELL_SIZE;

                Color c = cz.isCrossingActive()  ? Color.web("#1D9E75", 0.7)
                        : cz.isWaitCritical()     ? Color.web("#E24B4A", 0.5)
                        :                           Color.web("#888780", 0.3);
                gc.setFill(c);
                for (int s = 0; s < 3; s++)
                    gc.fillRect(px + NODE_RADIUS + 4 + s * 5, py - 10, 3, 20);

                if (cz.getWaitingPedestrians() > 0) {
                    gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                    gc.setFill(cz.isVulnerablePresent()
                        ? Color.web("#D85A30") : Color.web("#444441"));
                    gc.setTextAlign(TextAlignment.LEFT);
                    gc.fillText("P:" + cz.getWaitingPedestrians(),
                        px + NODE_RADIUS + 20, py - 14);
                }
            }
        }
    }

    private void drawIntersections(GraphicsContext gc) {
        int size = grid.getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Intersection inter = grid.get(x, y);
                int px = MARGIN + x * CELL_SIZE;
                int py = MARGIN + y * CELL_SIZE;

                Color nodeColor = getNodeColor(inter);
                gc.setFill(nodeColor.deriveColor(0, 1, 1, 0.2));
                gc.fillOval(px - NODE_RADIUS - 6, py - NODE_RADIUS - 6,
                            (NODE_RADIUS + 6) * 2, (NODE_RADIUS + 6) * 2);

                gc.setFill(nodeColor);
                gc.fillOval(px - NODE_RADIUS, py - NODE_RADIUS,
                            NODE_RADIUS * 2, NODE_RADIUS * 2);

                gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                gc.setFill(Color.WHITE);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.format("%.0f", inter.getAverageWaitTime()),
                    px, py + 4);

                gc.setFont(Font.font("Arial", FontWeight.NORMAL, 9));
                gc.setFill(Color.web("#5F5E5A"));
                gc.fillText("INT " + inter.getId(), px, py + NODE_RADIUS + 14);
            }
        }
    }

    private Color getNodeColor(Intersection inter) {
        if (inter.isPedestrianPhase()) return Color.web("#1D9E75");
        double vehicleScore = Math.min(1.0, inter.getAverageWaitTime() / 20.0);
        double pedScore     = inter.getCrosswalkZone().getUrgencyScore();
        double combined     = vehicleScore * 0.6 + pedScore * 0.4;
        if (combined < 0.25) return Color.web("#639922");
        if (combined < 0.50) return Color.web("#BA7517");
        if (combined < 0.75) return Color.web("#D85A30");
        return Color.web("#E24B4A");
    }

    public void setGrid(CityGrid grid) { this.grid = grid; }
}