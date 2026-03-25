package com.trafficoptimizer.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ParetoOptimizer {

    public List<Chromosome> getParetoFront(List<Chromosome> population) {
        List<Chromosome> front = new ArrayList<>();
        for (Chromosome c : population)
            if (!isDominated(c, population))
                front.add(c);
        return front;
    }

    private boolean isDominated(Chromosome c, List<Chromosome> population) {
        for (Chromosome other : population)
            if (other != c && dominates(other, c)) return true;
        return false;
    }

    private boolean dominates(Chromosome a, Chromosome b) {
        return a.getVehicleFitness()    >= b.getVehicleFitness()
            && a.getPedestrianFitness() >= b.getPedestrianFitness()
            && (a.getVehicleFitness()    > b.getVehicleFitness()
             || a.getPedestrianFitness() > b.getPedestrianFitness());
    }

    public Chromosome selectKneePoint(List<Chromosome> front) {
        return front.stream()
            .min(Comparator.comparingDouble(c ->
                Math.pow(1.0 - c.getVehicleFitness(),    2)
              + Math.pow(1.0 - c.getPedestrianFitness(), 2)))
            .orElseThrow(() -> new IllegalStateException("Empty Pareto front"));
    }

    public Chromosome selectByWeight(List<Chromosome> front,
                                     double vehicleWeight,
                                     double pedestrianWeight) {
        return front.stream()
            .max(Comparator.comparingDouble(c ->
                vehicleWeight    * c.getVehicleFitness()
              + pedestrianWeight * c.getPedestrianFitness()))
            .orElse(front.get(0));
    }
}