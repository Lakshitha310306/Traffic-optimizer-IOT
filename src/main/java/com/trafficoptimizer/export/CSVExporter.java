package com.trafficoptimizer.export;

import com.trafficoptimizer.algorithm.Chromosome;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CSVExporter {

    private static final String RESULTS_FILE = "data/simulation_results.csv";
    private static final String PARETO_FILE  = "data/pareto_front.csv";

    private PrintWriter resultsWriter;

    public CSVExporter() throws IOException {
        new java.io.File("data").mkdirs();
        resultsWriter = new PrintWriter(new FileWriter(RESULTS_FILE));
        resultsWriter.println(
            "tick,avg_vehicle_wait,total_queue," +
            "ped_urgency,critical_ped_waits,mode,timestamp");
    }

    public void logTick(int tick, double avgVehicleWait, int totalQueue,
                        double pedUrgency, int criticalPedWaits, String mode) {
        resultsWriter.printf("%d,%.3f,%d,%.3f,%d,%s,%s%n",
            tick, avgVehicleWait, totalQueue, pedUrgency, criticalPedWaits,
            mode,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        resultsWriter.flush();
    }

    public void exportParetoFront(List<Chromosome> front) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(PARETO_FILE))) {
            pw.println("solution_id,vehicle_fitness,pedestrian_fitness,combined_fitness");
            for (int i = 0; i < front.size(); i++) {
                Chromosome c = front.get(i);
                pw.printf("%d,%.6f,%.6f,%.6f%n",
                    i, c.getVehicleFitness(),
                    c.getPedestrianFitness(), c.getCombinedFitness());
            }
            System.out.println("Pareto front exported to " + PARETO_FILE
                + " (" + front.size() + " solutions)");
        } catch (IOException e) {
            System.err.println("Failed to export Pareto front: " + e.getMessage());
        }
    }

    public void close() {
        if (resultsWriter != null) {
            resultsWriter.flush();
            resultsWriter.close();
            System.out.println("Results exported to " + RESULTS_FILE);
        }
    }
}