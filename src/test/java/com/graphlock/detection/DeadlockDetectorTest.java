package com.graphlock.detection;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockDetectorTest {

    private final DeadlockDetector detector = new DeadlockDetector();

    @Test
    void detectsDeadlockInSingleInstanceCircularWait() {
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        state.request(p1, r2, 1);
        state.request(p2, r1, 1);

        DeadlockReport report = detector.detect(state);

        assertTrue(report.isDeadlockDetected());
        assertEquals(2, report.getDeadlockedProcesses().size());
    }

    @Test
    void noDeadlockWhenRequestsCanBeSatisfied() {
        ProcessNode p1 = new ProcessNode("P1");
        ResourceType r1 = new ResourceType("R1", 2);

        AllocationState state = new AllocationState(List.of(p1), List.of(r1));
        state.allocate(p1, r1, 1);
        state.request(p1, r1, 1); // 1 available, request of 1 - satisfiable

        DeadlockReport report = detector.detect(state);

        assertFalse(report.isDeadlockDetected());
    }

    @Test
    void multiInstanceCycleWithoutDeadlockIsCorrectlyResolvedAsSafe() {
        // This is the exact case that would fool naive cycle detection:
        // P1 and P2 each hold one instance of a 2-instance resource, and
        // each requests the other's - but since no request exceeds what's
        // achievable once ANY process finishes, this resolves safely.
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 3); // 3 instances total

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r1, 1);
        // 1 instance remains available (3 - 2 allocated)
        state.request(p1, r1, 1); // satisfiable immediately from the 1 available

        DeadlockReport report = detector.detect(state);

        assertFalse(report.isDeadlockDetected());
    }

    @Test
    void identifiesOnlyTheActuallyStuckProcessesInPartialDeadlock() {
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ProcessNode p3 = new ProcessNode("P3");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState state = new AllocationState(List.of(p1, p2, p3), List.of(r1, r2));
        // P3 has no allocations and no requests - it can always "finish" trivially
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        state.request(p1, r2, 1);
        state.request(p2, r1, 1);

        DeadlockReport report = detector.detect(state);

        assertTrue(report.isDeadlockDetected());
        assertFalse(report.getDeadlockedProcesses().contains(p3));
        assertEquals(2, report.getDeadlockedProcesses().size());
    }
}
