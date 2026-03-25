package com.trafficoptimizer.algorithm;

import com.trafficoptimizer.model.CityGrid;
import com.trafficoptimizer.model.CrosswalkZone;
import com.trafficoptimizer.model.Intersection;
import com.trafficoptimizer.sensor.VehicleSensor;
import com.trafficoptimizer.sensor.PedestrianSensor;

import java.util.List;

public class FitnessEvaluator {

    private static final int    SIM_TICKS          = 120;
    private static final double VEHICLE_SCALE      = 3.0;
    private static final double PEDESTRIAN_SCALE   = 1.5;
    private static final double URGENCY_PENALTY    = 1.0;
    private static final double CRITICAL_PENALTY   = 4.0;
    private static final double VULNERABLE_PENALTY = 2.5;

    private double vehicleWeight    = 0.6;
    private double pedestrianWeight = 0.4;

    private final List<VehicleSensor>    vehicleSensors;
    private final List<PedestrianSensor> pedestrianSensors;
    private final int                    simulationHour;

    // Pre-generated deterministic arrivals — same traffic for every chromosome
    private final int[][] deterministicArrivals;

    public FitnessEvaluator(List<VehicleSensor>    vehicleSensors,
                            List<PedestrianSensor> pedestrianSensors,
                            int                    simulationHour) {
        this.vehicleSensors    = vehicleSensors;
        this.pedestrianSensors = pedestrianSensors;
        this.simulationHour    = simulationHour;
        this.deterministicArrivals = preGenerateArrivals();
    }

    private int[][] preGenerateArrivals() {
        int n   = vehicleSensors.size();
        int[][] arr = new int[n][SIM_TICKS * 4];
        for (int i = 0; i < n; i++) {
            for (int t = 0; t < SIM_TICKS; t++) {
                int[] a = vehicleSensors.get(i).readArrivals(simulationHour);
                for (int d = 0; d < 4; d++)
                    arr[i][t * 4 + d] = a[d];
            }
        }
        return arr;
    }

    public void evaluate(Chromosome c, CityGrid grid) {
        applyChromosome(c, grid);
        grid.resetAll();

        double totalVehicleQueue = 0;
        double totalPedPenalty   = 0;
        int    n                 = grid.getTotalIntersections();

        for (int t = 0; t < SIM_TICKS; t++) {
            for (int i = 0; i < n; i++) {
                Intersection inter = grid.getAllIntersections().get(i);
                for (int d = 0; d < 4; d++)
                    inter.addVehicles(d, deterministicArrivals[i][t * 4 + d]);
                int     crowd = pedestrianSensors.get(i).readCrowdDensity(simulationHour);
                boolean vuln  = pedestrianSensors.get(i)
                                    .detectVulnerablePedestrian(crowd);
                inter.getCrosswalkZone().update(crowd, vuln);
            }

            grid.tickAll();

            for (Intersection inter : grid.getAllIntersections()) {
                totalVehicleQueue += inter.getTotalQueueLoad();
                CrosswalkZone cz   = inter.getCrosswalkZone();
                if (cz.getWaitingPedestrians() > 0 && !cz.isCrossingActive())
                    totalPedPenalty += cz.getUrgencyScore() * URGENCY_PENALTY;
                if (cz.isVulnerablePresent() && !cz.isCrossingActive())
                    totalPedPenalty += VULNERABLE_PENALTY;
                if (cz.isWaitCritical())
                    totalPedPenalty += CRITICAL_PENALTY;
            }
        }

        double avgVQ = totalVehicleQueue / (double)(SIM_TICKS * n);
        double avgPP = totalPedPenalty   / (double)(SIM_TICKS * n);

        c.setVehicleFitness(   1.0 / (1.0 + avgVQ / VEHICLE_SCALE));
        c.setPedestrianFitness(1.0 / (1.0 + avgPP / PEDESTRIAN_SCALE));
        c.computeCombinedFitness(vehicleWeight, pedestrianWeight);
    }

    private void applyChromosome(Chromosome c, CityGrid grid) {
        List<Intersection> all = grid.getAllIntersections();
        for (int i = 0; i < all.size(); i++)
            all.get(i).setGreenTimes(c.getGene(i));
    }

    public void setWeights(double vw, double pw) {
        double total        = vw + pw;
        this.vehicleWeight  = vw / total;
        this.pedestrianWeight = pw / total;
    }

    public double getVehicleWeight()    { return vehicleWeight; }
    public double getPedestrianWeight() { return pedestrianWeight; }
}