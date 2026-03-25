package com.trafficoptimizer.algorithm;

import java.util.Random;

public class Chromosome {

    private static final int    MIN_VEHICLE_GREEN     = 10;
    private static final int    MAX_VEHICLE_GREEN     = 60;
    private static final int    MIN_PEDESTRIAN_GREEN  = 10;
    private static final int    MAX_PEDESTRIAN_GREEN  = 45;
    private static final int    GENES_PER_INTERSECTION = 5;

    private final int[][] genes;
    private double vehicleFitness;
    private double pedestrianFitness;
    private double combinedFitness;

    public Chromosome(int numIntersections) {
        this.genes = new int[numIntersections][GENES_PER_INTERSECTION];
        randomize();
    }

    // Copy constructor
    public Chromosome(Chromosome other) {
        this.genes = new int[other.genes.length][GENES_PER_INTERSECTION];
        for (int i = 0; i < genes.length; i++)
            this.genes[i] = other.genes[i].clone();
        this.vehicleFitness    = other.vehicleFitness;
        this.pedestrianFitness = other.pedestrianFitness;
        this.combinedFitness   = other.combinedFitness;
    }

    private void randomize() {
        Random r = new Random();
        for (int[] g : genes) {
            for (int d = 0; d < 4; d++)
                g[d] = MIN_VEHICLE_GREEN
                     + r.nextInt(MAX_VEHICLE_GREEN - MIN_VEHICLE_GREEN);
            g[4] = MIN_PEDESTRIAN_GREEN
                 + r.nextInt(MAX_PEDESTRIAN_GREEN - MIN_PEDESTRIAN_GREEN);
        }
    }

    public Chromosome crossover(Chromosome other) {
        Chromosome child = new Chromosome(genes.length);
        int point = new Random().nextInt(genes.length);
        for (int i = 0; i < genes.length; i++)
            child.genes[i] = (i < point)
                ? genes[i].clone()
                : other.genes[i].clone();
        return child;
    }

    public void mutate(double rate) {
        Random r = new Random();
        for (int[] g : genes) {
            for (int d = 0; d < 4; d++) {
                if (r.nextDouble() < rate) {
                    g[d] += r.nextInt(11) - 5;
                    g[d] = Math.max(MIN_VEHICLE_GREEN,
                           Math.min(MAX_VEHICLE_GREEN, g[d]));
                }
            }
            if (r.nextDouble() < rate) {
                g[4] += r.nextInt(9) - 4;
                g[4] = Math.max(MIN_PEDESTRIAN_GREEN,
                       Math.min(MAX_PEDESTRIAN_GREEN, g[4]));
            }
        }
    }

    public void computeCombinedFitness(double vw, double pw) {
        combinedFitness = vw * vehicleFitness + pw * pedestrianFitness;
    }

    public int[]  getGene(int i)                 { return genes[i]; }
    public double getVehicleFitness()            { return vehicleFitness; }
    public double getPedestrianFitness()         { return pedestrianFitness; }
    public double getCombinedFitness()           { return combinedFitness; }
    public void   setVehicleFitness(double v)    { vehicleFitness = v; }
    public void   setPedestrianFitness(double p) { pedestrianFitness = p; }
    public int    getNumIntersections()          { return genes.length; }
}