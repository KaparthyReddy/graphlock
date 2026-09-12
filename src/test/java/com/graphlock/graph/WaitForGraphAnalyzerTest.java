package com.graphlock.graph;

import com.graphlock.core.AllocationState;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaitForGraphAnalyzerTest {

    private final WaitForGraphAnalyzer analyzer = new WaitForGraphAnalyzer();

    @Test
    void detectsSimpleTwoProcessCycle() {
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        state.request(p1, r2, 1);
        state.request(p2, r1, 1);

        List<ProcessNode> cycle = analyzer.findCycle(state);
        assertFalse(cycle.isEmpty());
        assertTrue(cycle.contains(p1));
        assertTrue(cycle.contains(p2));
    }

    @Test
    void returnsEmptyForAcyclicWaitGraph() {
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);

        AllocationState state = new AllocationState(List.of(p1, p2), List.of(r1, r2));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        state.request(p1, r2, 1); // P1 waits on P2, but P2 waits on nothing

        List<ProcessNode> cycle = analyzer.findCycle(state);
        assertTrue(cycle.isEmpty());
    }

    @Test
    void throwsOnMultiInstanceResource() {
        ProcessNode p1 = new ProcessNode("P1");
        ResourceType r1 = new ResourceType("R1", 2); // multi-instance

        AllocationState state = new AllocationState(List.of(p1), List.of(r1));

        assertThrows(IllegalStateException.class, () -> analyzer.findCycle(state));
    }

    @Test
    void detectsThreeProcessCycle() {
        ProcessNode p1 = new ProcessNode("P1");
        ProcessNode p2 = new ProcessNode("P2");
        ProcessNode p3 = new ProcessNode("P3");
        ResourceType r1 = new ResourceType("R1", 1);
        ResourceType r2 = new ResourceType("R2", 1);
        ResourceType r3 = new ResourceType("R3", 1);

        AllocationState state = new AllocationState(List.of(p1, p2, p3), List.of(r1, r2, r3));
        state.allocate(p1, r1, 1);
        state.allocate(p2, r2, 1);
        state.allocate(p3, r3, 1);
        state.request(p1, r2, 1); // P1 -> P2
        state.request(p2, r3, 1); // P2 -> P3
        state.request(p3, r1, 1); // P3 -> P1, closing the cycle

        List<ProcessNode> cycle = analyzer.findCycle(state);
        assertEquals(3, cycle.size());
    }
}
