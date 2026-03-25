package com.trafficoptimizer.model;

public class Intersection {

    public static final int NORTH = 0, SOUTH = 1, EAST = 2, WEST = 3;
    private static final String[] DIR_NAMES = {"N", "S", "E", "W"};

    private final int id;
    private final int x, y;

    private int[] vehicleGreenTimes;
    private int   pedestrianGreenTime;
    private int[] queueLengths;
    private int   currentPhase;
    private int   phaseTimer;

    private static final int DEFAULT_GREEN               = 30;
    private static final int MIN_GREEN                   = 10;
    private static final int MAX_QUEUE                   = 20;
    private static final int MAX_VEHICLES_CLEARED_PER_PHASE = 12;

    private CrosswalkZone crosswalkZone;

    public Intersection(int id, int x, int y) {
        this.id                = id;
        this.x                 = x;
        this.y                 = y;
        this.vehicleGreenTimes = new int[]{DEFAULT_GREEN, DEFAULT_GREEN,
                                           DEFAULT_GREEN, DEFAULT_GREEN};
        this.pedestrianGreenTime = 15;
        this.queueLengths      = new int[4];
        this.currentPhase      = 0;
        this.phaseTimer        = DEFAULT_GREEN;
        this.crosswalkZone     = new CrosswalkZone(id);
    }

    public void tick() {
        phaseTimer--;
        if (phaseTimer <= 0) advancePhase();
        for (int d = 0; d < 4; d++) {
            if (currentPhase != d)
                queueLengths[d] = Math.min(queueLengths[d] + 1, MAX_QUEUE);
        }
    }

    private void advancePhase() {
        if (currentPhase < 4) {
            int greenTime = vehicleGreenTimes[currentPhase];
            int cleared   = (int)(MAX_VEHICLES_CLEARED_PER_PHASE
                            * (greenTime / (double) DEFAULT_GREEN));
            queueLengths[currentPhase] = Math.max(0,
                queueLengths[currentPhase] - cleared);
        } else {
            crosswalkZone.deactivateCrossing();
        }
        currentPhase = (currentPhase + 1) % 5;
        if (currentPhase < 4) {
            phaseTimer = Math.max(MIN_GREEN, vehicleGreenTimes[currentPhase]);
        } else {
            phaseTimer = pedestrianGreenTime;
            crosswalkZone.activateCrossing();
        }
    }

    public void addVehicles(int direction, int count) {
        queueLengths[direction] = Math.min(MAX_QUEUE,
                                           queueLengths[direction] + count);
    }

    public double getTotalQueueLoad() {
        double total = 0;
        for (int q : queueLengths) total += q;
        return total;
    }

    public double getAverageWaitTime() { return getTotalQueueLoad() / 4.0; }

    public void setGreenTimes(int[] genes) {
        System.arraycopy(genes, 0, vehicleGreenTimes, 0, 4);
        pedestrianGreenTime = genes[4];
        crosswalkZone.setAllocatedGreenTime(genes[4]);
    }

    public void resetQueues() {
        queueLengths = new int[4];
        currentPhase = 0;
        phaseTimer   = vehicleGreenTimes[0];
    }

    public int           getId()                  { return id; }
    public int           getX()                   { return x; }
    public int           getY()                   { return y; }
    public int[]         getQueueLengths()        { return queueLengths; }
    public int           getCurrentPhase()        { return currentPhase; }
    public int[]         getVehicleGreenTimes()   { return vehicleGreenTimes; }
    public int           getPedestrianGreenTime() { return pedestrianGreenTime; }
    public CrosswalkZone getCrosswalkZone()       { return crosswalkZone; }
    public boolean       isPedestrianPhase()      { return currentPhase == 4; }

    @Override
    public String toString() {
        return String.format(
            "INT[%d](%d,%d) phase=%s queues=[N:%d S:%d E:%d W:%d] pedWait=%d",
            id, x, y,
            currentPhase < 4 ? DIR_NAMES[currentPhase] : "PED",
            queueLengths[0], queueLengths[1], queueLengths[2], queueLengths[3],
            crosswalkZone.getWaitingPedestrians());
    }
}