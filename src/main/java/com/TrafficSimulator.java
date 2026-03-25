package com.trafficoptimizer;

import com.trafficoptimizer.algorithm.*;
import com.trafficoptimizer.export.CSVExporter;
import com.trafficoptimizer.model.CityGrid;
import com.trafficoptimizer.sensor.PedestrianSensor;
import com.trafficoptimizer.sensor.VehicleSensor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrafficSimulator {

    private static final int GRID_SIZE = 4;
    private static final int SIM_TICKS = 500;
    private static final int SIM_HOUR  = 8;

    private final CityGrid               grid;
    private final List<VehicleSensor>    vehicleSensors;
    private final List<PedestrianSensor> pedestrianSensors;
    private final FitnessEvaluator       evaluator;
    private final GeneticAlgorithm       ga;
    private final ParetoOptimizer        pareto;
    private CSVExporter                  exporter;

    private double baselineAvgWait;
    private double optimizedAvgWait;
    private double baselinePedUrgency;
    private double optimizedPedUrgency;

    public TrafficSimulator() {
        this.grid              = new CityGrid(GRID_SIZE);
        this.vehicleSensors    = new ArrayList<>();
        this.pedestrianSensors = new ArrayList<>();

        PedestrianSensor.Location[] locations = {
            PedestrianSensor.Location.SCHOOL_ZONE,
            PedestrianSensor.Location.OFFICE_ZONE,
            PedestrianSensor.Location.MARKET_AREA,
            PedestrianSensor.Location.RESIDENTIAL,
            PedestrianSensor.Location.HOSPITAL_ZONE,
            PedestrianSensor.Location.OFFICE_ZONE,
            PedestrianSensor.Location.RESIDENTIAL,
            PedestrianSensor.Location.MARKET_AREA,
            PedestrianSensor.Location.SCHOOL_ZONE,
            PedestrianSensor.Location.RESIDENTIAL,
            PedestrianSensor.Location.OFFICE_ZONE,
            PedestrianSensor.Location.HOSPITAL_ZONE,
            PedestrianSensor.Location.MARKET_AREA,
            PedestrianSensor.Location.RESIDENTIAL,
            PedestrianSensor.Location.SCHOOL_ZONE,
            PedestrianSensor.Location.OFFICE_ZONE
        };

        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            vehicleSensors.add(new VehicleSensor(i));
            pedestrianSensors.add(new PedestrianSensor(i, locations[i]));
        }

        this.evaluator = new FitnessEvaluator(vehicleSensors, pedestrianSensors, SIM_HOUR);
        this.pareto    = new ParetoOptimizer();
        this.ga        = new GeneticAlgorithm(evaluator, pareto, grid);
    }

    public void run() throws IOException {
        exporter = new CSVExporter();

        System.out.println("=== People-First Smart City Traffic Optimizer ===");
        System.out.println("Grid: " + GRID_SIZE + "x" + GRID_SIZE
            + " | Hour: " + SIM_HOUR + ":00 | Ticks: " + SIM_TICKS);
        System.out.println();

        System.out.println("--- Phase 1: Baseline simulation (fixed 30s timers) ---");
        grid.resetAll();
        double[] bm = runSimulationPhase("BASELINE");
        baselineAvgWait    = bm[0];
        baselinePedUrgency = bm[1];

        System.out.println("\n--- Phase 2: Genetic Algorithm optimisation ---");
        Chromosome best  = ga.evolve();
        List<Chromosome> front = ga.getParetoFront();
        exporter.exportParetoFront(front);
        System.out.printf("Pareto front size: %d solutions%n", front.size());

        for (int i = 0; i < grid.getTotalIntersections(); i++)
            grid.getAllIntersections().get(i).setGreenTimes(best.getGene(i));

        System.out.println("\n--- Phase 3: Optimized simulation (GA solution) ---");
        grid.resetAll();
        double[] om = runSimulationPhase("OPTIMIZED");
        optimizedAvgWait    = om[0];
        optimizedPedUrgency = om[1];

        printSummary();
        exporter.close();
    }

    private double[] runSimulationPhase(String mode) {
        double totalWait   = 0;
        double totalPedUrg = 0;

        for (int t = 0; t < SIM_TICKS; t++) {
            for (int i = 0; i < grid.getTotalIntersections(); i++) {
                var inter    = grid.getAllIntersections().get(i);
                int[] arr    = vehicleSensors.get(i).readArrivals(SIM_HOUR);
                for (int d = 0; d < 4; d++) inter.addVehicles(d, arr[d]);
                int crowd    = pedestrianSensors.get(i).readCrowdDensity(SIM_HOUR);
                boolean vuln = pedestrianSensors.get(i)
                                   .detectVulnerablePedestrian(crowd);
                inter.getCrosswalkZone().update(crowd, vuln);
            }
            grid.tickAll();

            double avgWait = grid.getAverageWaitTime();
            double pedUrg  = grid.getTotalPedestrianUrgency();
            int    crit    = grid.countCriticalPedestrianWaits();

            totalWait   += avgWait;
            totalPedUrg += pedUrg;

            exporter.logTick(t, avgWait, (int) avgWait, pedUrg, crit, mode);

            if (t % 100 == 0)
                System.out.printf(
                    "  [%s] Tick %3d | AvgWait=%.2f | PedUrgency=%.3f | CritWaits=%d%n",
                    mode, t, avgWait, pedUrg, crit);
        }
        return new double[]{ totalWait / SIM_TICKS, totalPedUrg / SIM_TICKS };
    }

    private void printSummary() {
        double wi = ((baselineAvgWait    - optimizedAvgWait)    / baselineAvgWait)    * 100;
        double pi = ((baselinePedUrgency - optimizedPedUrgency) / baselinePedUrgency) * 100;

        System.out.println("\n=================== RESULTS ===================");
        System.out.printf("Vehicle avg wait   : %.2f -> %.2f  (%.1f%% improvement)%n",
            baselineAvgWait, optimizedAvgWait, wi);
        System.out.printf("Pedestrian urgency : %.3f -> %.3f  (%.1f%% improvement)%n",
            baselinePedUrgency, optimizedPedUrgency, pi);
        System.out.println("Data  -> data/simulation_results.csv");
        System.out.println("Pareto -> data/pareto_front.csv");
        System.out.println("================================================");
    }

    // ── Getters used by Dashboard.java ───────────────────────────────────────
    public CityGrid                  getGrid()              { return grid; }
    public GeneticAlgorithm          getGa()                { return ga; }
    public FitnessEvaluator          getEvaluator()         { return evaluator; }
    public List<VehicleSensor>       getVehicleSensors()    { return vehicleSensors; }
    public List<PedestrianSensor>    getPedestrianSensors() { return pedestrianSensors; }
    public double                    getBaselineAvgWait()   { return baselineAvgWait; }
    public double                    getOptimizedAvgWait()  { return optimizedAvgWait; }
}