package com.trafficoptimizer.ui;

import com.trafficoptimizer.TrafficSimulator;
import com.trafficoptimizer.algorithm.Chromosome;
import com.trafficoptimizer.model.CityGrid;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Dashboard extends Application {

    private TrafficSimulator simulator;
    private CityGridPanel    gridPanel;
    private ParetoChart      paretoChart;
    private AnimationTimer   renderLoop;

    private Label       lblAvgWait;
    private Label       lblPedUrgency;
    private Label       lblCritWaits;
    private Label       lblGeneration;
    private Label       lblStatus;
    private ProgressBar progressBar;
    private Slider      weightSlider;
    private Label       weightLabel;
    private Button      btnRun;
    private Button      btnReset;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start(Stage stage) {
        simulator = new TrafficSimulator();
        gridPanel = new CityGridPanel(simulator.getGrid());

        // ── Header ──────────────────────────────────────────────────────────
        Label title = new Label("People-First Smart City Traffic Optimizer");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#2C2C2A"));

        Label subtitle = new Label("IoT Multi-Objective Pareto GA  |  4x4 City Grid");
        subtitle.setFont(Font.font("Arial", 12));
        subtitle.setTextFill(Color.web("#5F5E5A"));

        lblStatus = new Label("Ready — press Run to start");
        lblStatus.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblStatus.setTextFill(Color.web("#185FA5"));

        VBox header = new VBox(2, title, subtitle, lblStatus);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color:#FFFFFF;"
            + "-fx-border-color:#D3D1C7;-fx-border-width:0 0 1 0;");

        // ── Metric cards ─────────────────────────────────────────────────────
        lblAvgWait    = metricValue("--");
        lblPedUrgency = metricValue("--");
        lblCritWaits  = metricValue("--");
        lblGeneration = metricValue("0/150");

        HBox metrics = new HBox(10,
            metricCard("Avg vehicle wait",   lblAvgWait,    "#E6F1FB", "#185FA5"),
            metricCard("Pedestrian urgency", lblPedUrgency, "#E1F5EE", "#0F6E56"),
            metricCard("Critical waits",     lblCritWaits,  "#FCEBEB", "#A32D2D"),
            metricCard("GA generation",      lblGeneration, "#FAEEDA", "#854F0B")
        );
        metrics.setPadding(new Insets(12, 0, 8, 0));

        // ── Weight slider ─────────────────────────────────────────────────────
        Label sliderTitle = new Label("Priority balance");
        sliderTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        HBox sliderLabels = new HBox();
        sliderLabels.setAlignment(Pos.CENTER);
        Label lPed = new Label("Pedestrian");
        Label lVeh = new Label("Vehicle");
        lPed.setFont(Font.font("Arial", 11));
        lVeh.setFont(Font.font("Arial", 11));
        lPed.setTextFill(Color.web("#0F6E56"));
        lVeh.setTextFill(Color.web("#185FA5"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        sliderLabels.getChildren().addAll(lPed, sp, lVeh);

        weightSlider = new Slider(0.1, 0.9, 0.6);
        weightSlider.setPrefWidth(300);
        weightLabel  = new Label("Vehicles 60%  /  Pedestrians 40%");
        weightLabel.setFont(Font.font("Arial", 11));
        weightLabel.setTextFill(Color.web("#5F5E5A"));

        weightSlider.valueProperty().addListener((obs, o, n) -> {
            double vw = n.doubleValue(), pw = 1.0 - vw;
            weightLabel.setText(String.format(
                "Vehicles %.0f%%  /  Pedestrians %.0f%%", vw * 100, pw * 100));
            simulator.getEvaluator().setWeights(vw, pw);
        });

        VBox sliderBox = new VBox(4, sliderTitle, sliderLabels, weightSlider, weightLabel);
        sliderBox.setPadding(new Insets(10, 0, 10, 0));

        // ── Pareto chart ──────────────────────────────────────────────────────
        paretoChart = new ParetoChart();

        // ── Buttons ───────────────────────────────────────────────────────────
        btnRun = new Button("Run Optimization");
        btnRun.setStyle("-fx-background-color:#185FA5;-fx-text-fill:white;"
            + "-fx-font-weight:bold;-fx-padding:8 20;-fx-background-radius:6;");
        btnRun.setPrefWidth(200);
        btnRun.setOnAction(e -> startOptimization());

        btnReset = new Button("Reset");
        btnReset.setStyle("-fx-background-color:transparent;-fx-text-fill:#5F5E5A;"
            + "-fx-border-color:#B4B2A9;-fx-border-radius:6;"
            + "-fx-background-radius:6;-fx-padding:8 20;");
        btnReset.setPrefWidth(100);
        btnReset.setOnAction(e -> resetSimulation());

        HBox buttons = new HBox(10, btnRun, btnReset);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(6, 0, 0, 0));

        // ── Right panel ───────────────────────────────────────────────────────
        VBox rightPanel = new VBox(10, metrics, sliderBox, paretoChart, buttons);
        rightPanel.setPadding(new Insets(16));
        rightPanel.setMinWidth(440);
        rightPanel.setStyle("-fx-background-color:#F8F7F4;");

        // ── Left panel ────────────────────────────────────────────────────────
        VBox leftPanel = new VBox(gridPanel);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color:#FFFFFF;");
        leftPanel.setAlignment(Pos.CENTER);

        VBox leftFull = new VBox(10, leftPanel, buildLegend());
        leftFull.setStyle("-fx-background-color:#FFFFFF;"
            + "-fx-border-color:#D3D1C7;-fx-border-width:0 1 0 0;");

        // ── Progress + status bar ─────────────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);

        HBox statusBar = new HBox(8, new Label("Status:"), lblStatus);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(6, 16, 6, 16));
        statusBar.setStyle("-fx-background-color:#F1EFE8;");

        // ── Root ──────────────────────────────────────────────────────────────
        HBox centre = new HBox(leftFull, rightPanel);
        HBox.setHgrow(leftFull,   Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.NEVER);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(centre);
        root.setBottom(new VBox(progressBar, statusBar));

        stage.setTitle("Smart City Traffic Optimizer");
        stage.setScene(new Scene(root, 980, 700));
        stage.setResizable(true);
        stage.show();

        startRenderLoop();
    }

    // ── Render loop ──────────────────────────────────────────────────────────
    private void startRenderLoop() {
        renderLoop = new AnimationTimer() {
            @Override public void handle(long now) {
                if (running.get()) {
                    gridPanel.render();
                    updateMetrics();
                }
            }
        };
        renderLoop.start();
        gridPanel.render();
    }

    private void updateMetrics() {
        CityGrid g = simulator.getGrid();
        lblAvgWait.setText(   String.format("%.1f", g.getAverageWaitTime()));
        lblPedUrgency.setText(String.format("%.3f", g.getTotalPedestrianUrgency()));
        lblCritWaits.setText( String.valueOf(g.countCriticalPedestrianWaits()));
    }

    // ── Run GA in background thread ──────────────────────────────────────────
    private void startOptimization() {
        if (running.get()) return;
        btnRun.setDisable(true);
        btnReset.setDisable(true);
        running.set(true);

        // ── ProgressListener lambda — 4 params matching interface exactly ────
        simulator.getGa().setProgressListener(
            (gen, total, vFit, pFit) ->
                Platform.runLater(() -> {
                    lblGeneration.setText(gen + "/" + total);
                    progressBar.setProgress((double) gen / total);
                    lblStatus.setText(String.format(
                        "Gen %d — VehicleFit=%.4f  PedFit=%.4f",
                        gen, vFit, pFit));
                })
        );

        new Thread(() -> {
            try {
                Platform.runLater(() ->
                    lblStatus.setText("Phase 2: Genetic Algorithm running..."));

                Chromosome       best  = simulator.getGa().evolve();
                List<Chromosome> front = simulator.getGa().getParetoFront();

                Platform.runLater(() -> {
                    paretoChart.update(simulator.getGa().getPopulation(), front, best);
                    lblGeneration.setText("Done");
                    progressBar.setProgress(1.0);
                    lblStatus.setText("Optimization complete — GA solution applied");
                    btnRun.setDisable(false);
                    btnReset.setDisable(false);
                    btnRun.setText("Re-Optimize");
                });

                // Apply best chromosome
                CityGrid g = simulator.getGrid();
                for (int i = 0; i < g.getTotalIntersections(); i++)
                    g.getAllIntersections().get(i).setGreenTimes(best.getGene(i));

                // Live simulation ticks for visual display
                for (int t = 0; t < 500 && running.get(); t++) {
                    for (int i = 0; i < g.getTotalIntersections(); i++) {
                        var inter    = g.getAllIntersections().get(i);
                        int[] arr    = simulator.getVehicleSensors().get(i)
                                           .readArrivals(8);
                        for (int d = 0; d < 4; d++) inter.addVehicles(d, arr[d]);
                        int crowd    = simulator.getPedestrianSensors().get(i)
                                           .readCrowdDensity(8);
                        boolean vuln = simulator.getPedestrianSensors().get(i)
                                           .detectVulnerablePedestrian(crowd);
                        inter.getCrosswalkZone().update(crowd, vuln);
                    }
                    g.tickAll();
                    Thread.sleep(30);
                }

                Platform.runLater(() ->
                    lblStatus.setText("Done. Results saved to data/"));

            } catch (Exception e) {
                Platform.runLater(() ->
                    lblStatus.setText("Error: " + e.getMessage()));
                e.printStackTrace();
            } finally {
                running.set(false);
            }
        }, "GA-Thread").start();
    }

    private void resetSimulation() {
        running.set(false);
        simulator.getGrid().resetAll();
        gridPanel.render();
        progressBar.setProgress(0);
        lblStatus.setText("Reset — press Run to start again");
        lblAvgWait.setText("--");
        lblPedUrgency.setText("--");
        lblCritWaits.setText("--");
        lblGeneration.setText("0/150");
        btnRun.setText("Run Optimization");
        btnRun.setDisable(false);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private VBox metricCard(String title, Label val, String bg, String fg) {
        Label lbl = new Label(title);
        lbl.setFont(Font.font("Arial", 10));
        lbl.setTextFill(Color.web(fg));
        val.setTextFill(Color.web(fg));
        VBox card = new VBox(2, lbl, val);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:8;");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(100);
        return card;
    }

    private Label metricValue(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        return l;
    }

    private HBox buildLegend() {
        HBox box = new HBox(14,
            dot("#639922", "Low"),
            dot("#BA7517", "Moderate"),
            dot("#D85A30", "Busy"),
            dot("#E24B4A", "Congested"),
            dot("#1D9E75", "Pedestrian crossing"));
        box.setPadding(new Insets(6, 20, 10, 20));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private HBox dot(String color, String label) {
        javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(6);
        c.setFill(Color.web(color));
        Label l = new Label(label);
        l.setFont(Font.font("Arial", 10));
        l.setTextFill(Color.web("#5F5E5A"));
        HBox b = new HBox(5, c, l);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    public static void main(String[] args) { launch(args); }
}