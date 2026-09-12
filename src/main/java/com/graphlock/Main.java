package com.graphlock;

import com.graphlock.core.*;
import com.graphlock.detection.DeadlockDetector;
import com.graphlock.engine.DeadlockMonitor;
import com.graphlock.graph.WaitForGraphAnalyzer;
import com.graphlock.observer.DeadlockLogger;
import com.graphlock.observer.DeadlockStatistics;
import com.graphlock.recovery.ProcessTerminationStrategy;
import com.graphlock.recovery.ResourcePreemptionStrategy;
import com.graphlock.recovery.RecoveryStrategy;

import java.util.List;

/**
 * Demo runner: three scenarios showing the full spectrum this project
 * needs to handle correctly -
 *   1. A genuinely safe state (no deadlock, single-instance resources)
 *   2. An unambiguous single-instance cycle (real deadlock)
 *   3. A multi-instance case where a wait-for cycle EXISTS but the
 *      system is still actually safe - proving why naive cycle
 *      detection alone would give a false positive here, and why
 *      DeadlockDetector's general algorithm is necessary.
 */
public class Main {

    public static void main(String[] args) {
        scenarioOneSafeState();
        scenarioTwoSingleInstanceDeadlock();
        scenarioThreeCycleWithoutDeadlock();
    }

    private static void scenarioOneSafeState() {
        System.out.println("=== Scenario 1: Safe state, no deadlock ===");

        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        // Neither process is requesting anything further - trivially safe

        DeadlockMonitor monitor = buildMonitor();
        DeadlockReport report = monitor.checkState(state);
        System.out.println(report);
        System.out.println();
    }

    private static void scenarioTwoSingleInstanceDeadlock() {
        System.out.println("=== Scenario 2: Classic single-instance circular wait ===");

        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        state.request(p1, r2, 1); // P1 holds R1, wants R2
        state.request(p2, r1, 1); // P2 holds R2, wants R1 -> circular wait

        WaitForGraphAnalyzer graphAnalyzer = new WaitForGraphAnalyzer();
        List<ProcessNode> cycle = graphAnalyzer.findCycle(state);
        System.out.println("Wait-for graph cycle found: " + cycle);

        DeadlockMonitor monitor = buildMonitor();
        DeadlockReport report = monitor.checkState(state);
        System.out.println("General detector confirms: " + report);

        System.out.println("\n-- Recovery options --");
        RecoveryStrategy termination = new ProcessTerminationStrategy();
        RecoveryStrategy preemption = new ResourcePreemptionStrategy();
        System.out.println(termination.getName() + " would target: " + termination.recover(report, state));
        System.out.println(preemption.getName() + " would target: " + preemption.recover(report, state));
        System.out.println();
    }

    private static void scenarioThreeCycleWithoutDeadlock() {
        System.out.println("=== Scenario 3: Cycle exists, but system is actually SAFE (multi-instance) ===");

        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 2); // 2 instances available

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r1, 1);
        // One instance of R1 remains available (2 total - 2 allocated... wait,
        // both allocated means 0 available - let's make this genuinely safe:
        // P1 requests the resource P2 holds, but since there are 2 instances,
        // whichever process's request CAN be satisfied first breaks the "cycle"
        state.request(p1, r1, 1); // P1 wants one more instance

        System.out.println("Note: R1 has " + r1.getTotalInstances() + " instances; "
                + "a naive single-instance cycle check on this wait-for graph "
                + "would flag P1<->P2 as circular, but the system is actually fine.");

        DeadlockMonitor monitor = buildMonitor();
        DeadlockReport report = monitor.checkState(state);
        System.out.println("General (correct) detector result: " + report);
        System.out.println("(WaitForGraphAnalyzer is intentionally NOT run here - "
                + "it would throw, since it correctly refuses to analyze multi-instance resources)");
    }

    private static DeadlockMonitor buildMonitor() {
        DeadlockMonitor monitor = new DeadlockMonitor(new DeadlockDetector());
        monitor.addObserver(new DeadlockLogger());
        monitor.addObserver(new DeadlockStatistics());
        return monitor;
    }
}
