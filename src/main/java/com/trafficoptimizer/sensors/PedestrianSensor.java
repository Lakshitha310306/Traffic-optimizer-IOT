package com.trafficoptimizer.sensor;

import java.util.Random;

public class PedestrianSensor {

    public enum Location {
        SCHOOL_ZONE, MARKET_AREA, OFFICE_ZONE, HOSPITAL_ZONE, RESIDENTIAL
    }

    private final int      intersectionId;
    private final Location location;
    private final Random   rand;

    private static final double VULNERABLE_PROBABILITY = 0.15;

    public PedestrianSensor(int intersectionId, Location location) {
        this.intersectionId = intersectionId;
        this.location       = location;
        this.rand           = new Random(intersectionId * 17L + location.ordinal());
    }

    public int readCrowdDensity(int simulationHour) {
        double factor = getLocationFactor(simulationHour);
        int base = (int)(rand.nextInt(8) * factor);
        return Math.min(30, Math.max(0, base + rand.nextInt(3)));
    }

    public boolean detectVulnerablePedestrian(int crowdDensity) {
        if (crowdDensity == 0) return false;
        return rand.nextDouble() < VULNERABLE_PROBABILITY;
    }

    private double getLocationFactor(int hour) {
        switch (location) {
            case SCHOOL_ZONE:
                if ((hour >= 7 && hour <= 9) || (hour >= 14 && hour <= 16)) return 3.5;
                return 0.2;
            case MARKET_AREA:
                if (hour >= 10 && hour <= 20) return 2.2;
                return 0.4;
            case OFFICE_ZONE:
                if ((hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19)) return 2.8;
                return 0.5;
            case HOSPITAL_ZONE:
                return 1.4;
            default:
                return 0.6;
        }
    }

    public int      getIntersectionId() { return intersectionId; }
    public Location getLocation()       { return location; }
}