package com.graphlock.recovery;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;

/**
 * Kills the deadlocked process holding the fewest total resource
 * instances - a common real heuristic, since it releases the deadlock
 * while sacrificing the "cheapest" process in terms of resources
 * invested. Termination is simple and guaranteed to break the cycle
 * immediately, at the cost of losing that process's work entirely.
 */
public class ProcessTerminationStrategy implements RecoveryStrategy {

    @Override
    public ProcessNode recover(DeadlockReport report, AllocationState state) {
        if (!report.isDeadlockDetected()) {
            throw new IllegalArgumentException("Cannot recover - no deadlock was detected");
        }

        ProcessNode victim = null;
        int lowestResourceCount = Integer.MAX_VALUE;

        for (ProcessNode process : report.getDeadlockedProcesses()) {
            int totalHeld = 0;
            for (ResourceType resource : state.getResources()) {
                totalHeld += state.getAllocated(process, resource);
            }
            if (totalHeld < lowestResourceCount) {
                lowestResourceCount = totalHeld;
                victim = process;
            }
        }

        return victim;
    }

    @Override
    public String getName() { return "Process Termination"; }
}
