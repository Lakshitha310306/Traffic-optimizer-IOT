package com.trafficoptimizer.algorithm;

import com.trafficoptimizer.model.CityGrid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {

    private static final int    POPULATION_SIZE = 80;
    private static final int    GENERATIONS     = 150;
    private static final double MUTATION_RATE   = 0.08;
    private static final double ELITE_RATIO     = 0.10;
    private static final int    TOURNAMENT_K    = 5;

    private final FitnessEvaluator evaluator;
    private final ParetoOptimizer  paretoOptimizer;
    private final CityGrid         grid;
    private final Random           rand = new Random();

    private List<Chromosome> population;
    private List<Chromosome> paretoFront;

    // ── Progress listener — 4 params: gen, total, vehicleFit, pedFit ────────
    public interface ProgressListener {
        void onGeneration(int generation, int total,
                          double vehicleFitness, double pedestrianFitness);
    }
    private ProgressListener progressListener;

    public GeneticAlgorithm(FitnessEvaluator evaluator,
                            ParetoOptimizer  paretoOptimizer,
                            CityGrid         grid) {
        this.evaluator       = evaluator;
        this.paretoOptimizer = paretoOptimizer;
        this.grid            = grid;
    }

    public Chromosome evolve() {
        initPopulation();

        for (int gen = 0; gen < GENERATIONS; gen++) {

            for (Chromosome c : population)
                evaluator.evaluate(c, grid);

            population.sort(
                Comparator.comparingDouble(Chromosome::getCombinedFitness).reversed());

            List<Chromosome> nextGen   = new ArrayList<>();
            int              eliteCount = (int)(POPULATION_SIZE * ELITE_RATIO);

            for (int i = 0; i < eliteCount; i++)
                nextGen.add(new Chromosome(population.get(i)));

            while (nextGen.size() < POPULATION_SIZE) {
                Chromosome child = tournamentSelect().crossover(tournamentSelect());
                child.mutate(MUTATION_RATE);
                nextGen.add(child);
            }

            population = nextGen;

            Chromosome best = population.get(0);

            // Notify UI — exactly 4 arguments matching the interface above
            if (progressListener != null)
                progressListener.onGeneration(
                    gen + 1, GENERATIONS,
                    best.getVehicleFitness(),
                    best.getPedestrianFitness());

            System.out.printf(
                "Gen %3d/%d | VehicleFit=%.4f | PedFit=%.4f | Combined=%.4f%n",
                gen + 1, GENERATIONS,
                best.getVehicleFitness(),
                best.getPedestrianFitness(),
                best.getCombinedFitness());
        }

        // Final evaluation
        for (Chromosome c : population)
            evaluator.evaluate(c, grid);

        paretoFront = paretoOptimizer.getParetoFront(population);
        return paretoOptimizer.selectKneePoint(paretoFront);
    }

    private void initPopulation() {
        population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++)
            population.add(new Chromosome(grid.getTotalIntersections()));
    }

    private Chromosome tournamentSelect() {
        Chromosome best = null;
        for (int i = 0; i < TOURNAMENT_K; i++) {
            Chromosome c = population.get(rand.nextInt(population.size()));
            if (best == null || c.getCombinedFitness() > best.getCombinedFitness())
                best = c;
        }
        return best;
    }

    public List<Chromosome> getParetoFront()            { return paretoFront; }
    public List<Chromosome> getPopulation()             { return population; }
    public void setProgressListener(ProgressListener l) { progressListener = l; }
}