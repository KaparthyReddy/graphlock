package com.graphlock.core;

import java.util.List;

/** Result of running detection: which processes (if any) are deadlocked. */
public final class DeadlockReport {
    private final boolean deadlockDetected;
    private final List<ProcessNode> deadlockedProcesses;

    public DeadlockReport(boolean deadlockDetected, List<ProcessNode> deadlockedProcesses) {
        this.deadlockDetected = deadlockDetected;
        this.deadlockedProcesses = List.copyOf(deadlockedProcesses);
    }

    public boolean isDeadlockDetected() { return deadlockDetected; }
    public List<ProcessNode> getDeadlockedProcesses() { return deadlockedProcesses; }

    @Override
    public String toString() {
        return deadlockDetected
                ? "DEADLOCK detected among: " + deadlockedProcesses
                : "No deadlock - system is in a safe state";
    }
}
