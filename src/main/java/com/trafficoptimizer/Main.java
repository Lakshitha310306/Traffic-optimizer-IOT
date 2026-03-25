package com.trafficoptimizer;

import com.trafficoptimizer.ui.Dashboard;

public class Main {

    public static void main(String[] args) throws Exception {

        // If you want console mode
        if (args.length > 0 && args[0].equals("--console")) {
            System.out.println("People-First Smart City Traffic Optimizer");
            System.out.println("IoT Course Project — Multi-Objective GA");
            System.out.println("==========================================\n");

            new TrafficSimulator().run();

        } else {
            // Default → Launch UI
            System.out.println("Launching Dashboard UI...");
            Dashboard.main(args);
        }
    }
}