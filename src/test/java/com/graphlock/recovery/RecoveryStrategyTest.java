package com.graphlock.recovery;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryStrategyTest {

    @Test
    void terminationStrategyTargetsProcessHoldingFewestResources() {
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 2);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1); // holds 1 total
        state.allocate(p2, r2, 2); // holds 2 total

        DeadlockReport report = new DeadlockReport(true, List.of(p1, p2));

        RecoveryStrategy strategy = new ProcessTerminationStrategy();
        ProcessNode victim = strategy.recover(report, state);

        assertEquals(p1, victim); // fewer resources held
    }

    @Test
    void preemptionStrategyTargetsProcessHoldingMostResources() {
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 2);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 2);

        DeadlockReport report = new DeadlockReport(true, List.of(p1, p2));

        RecoveryStrategy strategy = new ResourcePreemptionStrategy();
        ProcessNode target = strategy.recover(report, state);

        assertEquals(p2, target); // most resources held
    }

    @Test
    void bothStrategiesThrowWhenNoDeadlockDetected() {
        DeadlockReport noDeadlock = new DeadlockReport(false, List.of());
        AllocationState state = new AllocationState(List.of(), List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new ProcessTerminationStrategy().recover(noDeadlock, state));
        assertThrows(IllegalArgumentException.class,
                () -> new ResourcePreemptionStrategy().recover(noDeadlock, state));
    }
}
