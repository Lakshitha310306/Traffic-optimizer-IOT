package com.trafficoptimizer.ui;

import com.trafficoptimizer.algorithm.Chromosome;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

import java.util.List;

public class ParetoChart extends ScatterChart<Number, Number> {

    private final XYChart.Series<Number, Number> dominatedSeries;
    private final XYChart.Series<Number, Number> paretoSeries;
    private final XYChart.Series<Number, Number> kneeSeries;

    public ParetoChart() {
        super(buildAxis("Vehicle fitness ->", 0, 1),
              buildAxis("Pedestrian fitness ->", 0, 1));
        setTitle("Pareto front");
        setAnimated(false);
        setLegendVisible(true);
        setPrefSize(420, 300);

        dominatedSeries = new XYChart.Series<>();
        paretoSeries    = new XYChart.Series<>();
        kneeSeries      = new XYChart.Series<>();

        dominatedSeries.setName("Dominated");
        paretoSeries.setName("Pareto front");
        kneeSeries.setName("Selected (knee point)");

        getData().addAll(dominatedSeries, paretoSeries, kneeSeries);
    }

    public void update(List<Chromosome> population,
                       List<Chromosome> front,
                       Chromosome       knee) {
        dominatedSeries.getData().clear();
        paretoSeries.getData().clear();
        kneeSeries.getData().clear();

        for (Chromosome c : population)
            if (!front.contains(c))
                dominatedSeries.getData().add(
                    new XYChart.Data<>(r(c.getVehicleFitness()),
                                       r(c.getPedestrianFitness())));

        front.stream()
            .sorted((a, b) -> Double.compare(
                a.getVehicleFitness(), b.getVehicleFitness()))
            .forEach(c -> paretoSeries.getData().add(
                new XYChart.Data<>(r(c.getVehicleFitness()),
                                   r(c.getPedestrianFitness()))));

        kneeSeries.getData().add(
            new XYChart.Data<>(r(knee.getVehicleFitness()),
                               r(knee.getPedestrianFitness())));
    }

    private static NumberAxis buildAxis(String label, double lo, double hi) {
        NumberAxis ax = new NumberAxis(lo, hi, 0.1);
        ax.setLabel(label);
        return ax;
    }

    private double r(double v) { return Math.round(v * 1000.0) / 1000.0; }
}