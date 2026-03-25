package com.trafficoptimizer.model;

public class CrosswalkZone {

    private final int intersectionId;
    private int     waitingPedestrians;
    private boolean vulnerablePersonPresent;
    private int     pedestrianWaitTicks;
    private int     allocatedGreenTime;
    private boolean crossingActive;

    private static final int BASE_CROSSING_TIME    = 15;
    private static final int VULNERABLE_EXTRA_TIME = 8;
    private static final int LARGE_CROWD_THRESHOLD = 15;
    private static final int LARGE_CROWD_EXTRA     = 5;
    public  static final int CRITICAL_WAIT_TICKS   = 60;

    public CrosswalkZone(int intersectionId) {
        this.intersectionId  = intersectionId;
        this.allocatedGreenTime = BASE_CROSSING_TIME;
    }

    public void update(int crowdDensity, boolean vulnerable) {
        this.waitingPedestrians      = crowdDensity;
        this.vulnerablePersonPresent = vulnerable;
        allocatedGreenTime = BASE_CROSSING_TIME;
        if (vulnerable)                           allocatedGreenTime += VULNERABLE_EXTRA_TIME;
        if (crowdDensity > LARGE_CROWD_THRESHOLD) allocatedGreenTime += LARGE_CROWD_EXTRA;
        if (crowdDensity > 0) pedestrianWaitTicks++;
    }

    public void activateCrossing() {
        crossingActive          = true;
        pedestrianWaitTicks     = 0;
        waitingPedestrians      = 0;
        vulnerablePersonPresent = false;
    }

    public void deactivateCrossing() { crossingActive = false; }

    public boolean isWaitCritical() {
        return pedestrianWaitTicks > CRITICAL_WAIT_TICKS && waitingPedestrians > 0;
    }

    public double getUrgencyScore() {
        if (waitingPedestrians == 0) return 0.0;
        double crowdFactor = Math.min(1.0, waitingPedestrians / 20.0);
        double waitFactor  = Math.min(1.0, pedestrianWaitTicks / 60.0);
        double vulnFactor  = vulnerablePersonPresent ? 0.3 : 0.0;
        return Math.min(1.0, crowdFactor * 0.4 + waitFactor * 0.4 + vulnFactor);
    }

    public int     getIntersectionId()       { return intersectionId; }
    public int     getWaitingPedestrians()   { return waitingPedestrians; }
    public boolean isVulnerablePresent()     { return vulnerablePersonPresent; }
    public int     getAllocatedGreenTime()   { return allocatedGreenTime; }
    public boolean isCrossingActive()       { return crossingActive; }
    public int     getPedestrianWaitTicks() { return pedestrianWaitTicks; }
    public void    setAllocatedGreenTime(int t) { this.allocatedGreenTime = t; }
}