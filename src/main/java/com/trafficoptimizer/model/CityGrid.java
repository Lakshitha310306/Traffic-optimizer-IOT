package com.trafficoptimizer.model;

import com.trafficoptimizer.sensor.PedestrianSensor;
import java.util.ArrayList;
import java.util.List;

public class CityGrid {

    private final int size;
    private final Intersection[][] grid;
    private final List<Intersection> allIntersections;
    private final List<int[]> roads;

    public CityGrid(int size) {
        this.size             = size;
        this.grid             = new Intersection[size][size];
        this.allIntersections = new ArrayList<>();
        this.roads            = new ArrayList<>();
        initGrid();
    }

    private void initGrid() {
        int id = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                grid[x][y] = new Intersection(id++, x, y);
                allIntersections.add(grid[x][y]);
            }
        }
        for (int x = 0; x < size - 1; x++)
            for (int y = 0; y < size; y++)
                roads.add(new int[]{x, y, x + 1, y});
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size - 1; y++)
                roads.add(new int[]{x, y, x, y + 1});
    }

    public void tickAll() {
        for (Intersection inter : allIntersections) inter.tick();
    }

    public void resetAll() {
        for (Intersection inter : allIntersections) inter.resetQueues();
    }

    public double getAverageWaitTime() {
        return allIntersections.stream()
            .mapToDouble(Intersection::getAverageWaitTime)
            .average().orElse(0);
    }

    public double getTotalPedestrianUrgency() {
        return allIntersections.stream()
            .mapToDouble(i -> i.getCrosswalkZone().getUrgencyScore())
            .sum();
    }

    public int countCriticalPedestrianWaits() {
        return (int) allIntersections.stream()
            .filter(i -> i.getCrosswalkZone().isWaitCritical())
            .count();
    }

    public Intersection          get(int x, int y)          { return grid[x][y]; }
    public List<Intersection>    getAllIntersections()       { return allIntersections; }
    public List<int[]>           getRoads()                 { return roads; }
    public int                   getSize()                  { return size; }
    public int                   getTotalIntersections()    { return size * size; }
}