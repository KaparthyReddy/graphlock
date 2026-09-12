package com.graphlock.engine;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;
import com.graphlock.detection.DeadlockDetector;
import com.graphlock.observer.DeadlockLogger;
import com.graphlock.observer.DeadlockStatistics;
import com.graphlock.recovery.ProcessTerminationStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockMonitorTest {

    @Test
    void notifiesObserversOnEveryCheck() {
        DeadlockMonitor monitor = new DeadlockMonitor(new DeadlockDetector());
        DeadlockLogger logger = new DeadlockLogger();
        DeadlockStatistics stats = new DeadlockStatistics();
        monitor.addObserver(logger);
        monitor.addObserver(stats);

        ProcessNode p1 = new ProcessNode("P1");
        ResourceType r1 = new ResourceType("R1", 1);
        AllocationState safeState = new AllocationState(List.of(p1), List.of(r1));
        safeState.allocate(p1, r1, 1);

        monitor.checkState(safeState);

        assertEquals(1, logger.getHistory().size());
        assertEquals(1, stats.getTotalChecks());
        assertEquals(0, stats.getDeadlocksFound());
    }

    @Test
    void checkAndRecoverReturnsNullWhenNoDeadlock() {
        DeadlockMonitor monitor = new DeadlockMonitor(new DeadlockDetector());
        ProcessNode p1 = new ProcessNode("P1");
        ResourceType r1 = new ResourceType("R1", 1);
        AllocationState safeState = new AllocationState(List.of(p1), List.of(r1));
        safeState.allocate(p1, r1, 1);

        ProcessNode result = monitor.checkAndRecover(safeState, new ProcessTerminationStrategy());

        assertNull(result);
    }

    @Test
    void checkAndRecoverInvokesStrategyWhenDeadlocked() {
        DeadlockMonitor monitor = new DeadlockMonitor(new DeadlockDetector());
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        state.request(p1, r2, 1);
        state.request(p2, r1, 1);

        ProcessNode victim = monitor.checkAndRecover(state, new ProcessTerminationStrategy());

        assertNotNull(victim);
        assertTrue(victim.equals(p1) || victim.equals(p2));
    }

    @Test
    void statisticsAccumulateAcrossMultipleChecks() {
        DeadlockMonitor monitor = new DeadlockMonitor(new DeadlockDetector());
        DeadlockStatistics stats = new DeadlockStatistics();
        monitor.addObserver(stats);

        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState deadlocked = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        deadlocked.allocate(p1, r1, 1);
        deadlocked.allocate(p2, r2, 1);
        deadlocked.request(p1, r2, 1);
        deadlocked.request(p2, r1, 1);

        AllocationState safe = new AllocationState(List.of(p1), List.of(r1));
        safe.allocate(p1, r1, 1);

        monitor.checkState(deadlocked);
        monitor.checkState(safe);
        monitor.checkState(deadlocked);

        assertEquals(3, stats.getTotalChecks());
        assertEquals(2, stats.getDeadlocksFound());
        assertEquals(2.0 / 3.0, stats.getDeadlockRate(), 0.001);
    }
}
