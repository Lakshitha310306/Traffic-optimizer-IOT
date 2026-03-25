package com.trafficoptimizer.sensor;

import java.util.Random;

public class VehicleSensor {

    private final int    intersectionId;
    private final Random rand;

    public VehicleSensor(int intersectionId) {
        this.intersectionId = intersectionId;
        this.rand           = new Random(intersectionId * 31L);
    }

    public int[] readArrivals(int simulationHour) {
        double peak    = getPeakFactor(simulationHour);
        int[]  arrivals = new int[4];
        for (int d = 0; d < 4; d++) {
            int base    = (int)(rand.nextInt(2) * peak);
            arrivals[d] = Math.max(0, Math.min(3, base));
        }
        return arrivals;
    }

    private double getPeakFactor(int hour) {
        if (hour >= 7  && hour <= 9)  return 1.5;
        if (hour >= 17 && hour <= 19) return 1.6;
        if (hour >= 12 && hour <= 13) return 1.0;
        if (hour >= 23 || hour <= 5)  return 0.2;
        return 0.8;
    }

    public int getIntersectionId() { return intersectionId; }
}