package com.graphlock.recovery;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;

/**
 * Instead of killing a process outright, preempts one resource instance
 * from the deadlocked process holding the MOST total resources (the
 * process most likely to have something spare to give up), and rolls
 * that process back to a checkpoint - in a real OS this requires
 * checkpoint/rollback support, which is genuinely more complex than
 * termination but avoids losing an entire process's work outright.
 *
 * This simulator identifies which process and which resource would be
 * preempted, rather than implementing actual rollback machinery - that
 * would require a full checkpointing subsystem, which is out of scope
 * for a deadlock-recovery-focused project.
 */
public class ResourcePreemptionStrategy implements RecoveryStrategy {

    @Override
    public ProcessNode recover(DeadlockReport report, AllocationState state) {
        if (!report.isDeadlockDetected()) {
            throw new IllegalArgumentException("Cannot recover - no deadlock was detected");
        }

        ProcessNode target = null;
        int highestResourceCount = -1;

        for (ProcessNode process : report.getDeadlockedProcesses()) {
            int totalHeld = 0;
            for (ResourceType resource : state.getResources()) {
                totalHeld += state.getAllocated(process, resource);
            }
            if (totalHeld > highestResourceCount) {
                highestResourceCount = totalHeld;
                target = process;
            }
        }

        return target;
    }

    @Override
    public String getName() { return "Resource Preemption"; }
}
