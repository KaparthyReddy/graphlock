package com.graphlock.observer;

import com.graphlock.core.DeadlockReport;

public class DeadlockStatistics implements DeadlockObserver {

    private int totalChecks = 0;
    private int deadlocksFound = 0;

    @Override
    public void onDeadlockCheck(DeadlockReport report) {
        totalChecks++;
        if (report.isDeadlockDetected()) {
            deadlocksFound++;
        }
    }

    public int getTotalChecks() { return totalChecks; }
    public int getDeadlocksFound() { return deadlocksFound; }

    public double getDeadlockRate() {
        return totalChecks == 0 ? 0.0 : (double) deadlocksFound / totalChecks;
    }
}
