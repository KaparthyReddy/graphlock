package com.graphlock.graph;

import com.graphlock.core.AllocationState;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;

import java.util.*;

/**
 * Builds a wait-for graph from an AllocationState and detects cycles via
 * DFS. IMPORTANT correctness note: a cycle in this graph only *guarantees*
 * deadlock when every resource type involved has exactly one instance.
 * With multiple instances, a cycle can exist without deadlock (see
 * DeadlockDetector for the general case that handles this correctly).
 * This class is deliberately scoped to the single-instance case and
 * documents that limitation rather than silently giving wrong answers
 * on multi-instance systems.
 */
public class WaitForGraphAnalyzer {

    /**
     * @throws IllegalStateException if any resource in the state has more
     * than one instance - cycle detection alone is not a valid deadlock
     * test in that case, and this class refuses to give a misleading answer.
     */
    public List<ProcessNode> findCycle(AllocationState state) {
        for (ResourceType resource : state.getResources()) {
            if (resource.getTotalInstances() > 1) {
                throw new IllegalStateException(
                        "WaitForGraphAnalyzer only supports single-instance resources; "
                                + resource + " has " + resource.getTotalInstances()
                                + " instances - use DeadlockDetector instead");
            }
        }

        Map<ProcessNode, List<ProcessNode>> waitForEdges = buildWaitForEdges(state);

        Set<ProcessNode> visited = new HashSet<>();
        Set<ProcessNode> inStack = new HashSet<>();
        Deque<ProcessNode> pathStack = new ArrayDeque<>();

        for (ProcessNode process : state.getProcesses()) {
            if (!visited.contains(process)) {
                List<ProcessNode> cycle = dfs(process, waitForEdges, visited, inStack, pathStack);
                if (cycle != null) return cycle;
            }
        }
        return List.of();
    }

    /** Pi waits-for Pj if Pi is requesting a resource currently held by Pj. */
    private Map<ProcessNode, List<ProcessNode>> buildWaitForEdges(AllocationState state) {
        Map<ProcessNode, List<ProcessNode>> edges = new HashMap<>();
        for (ProcessNode requester : state.getProcesses()) {
            edges.put(requester, new ArrayList<>());
            for (ResourceType resource : state.getResources()) {
                if (!state.isWaitingFor(requester, resource)) continue;

                for (ProcessNode holder : state.getProcesses()) {
                    if (!holder.equals(requester) && state.holds(holder, resource)) {
                        edges.get(requester).add(holder);
                    }
                }
            }
        }
        return edges;
    }

    private List<ProcessNode> dfs(ProcessNode current, Map<ProcessNode, List<ProcessNode>> edges,
                                   Set<ProcessNode> visited, Set<ProcessNode> inStack,
                                   Deque<ProcessNode> pathStack) {
        visited.add(current);
        inStack.add(current);
        pathStack.push(current);

        for (ProcessNode neighbor : edges.getOrDefault(current, List.of())) {
            if (inStack.contains(neighbor)) {
                // Found a cycle - extract just the cyclic portion of the path
                List<ProcessNode> cycle = new ArrayList<>();
                for (ProcessNode p : pathStack) {
                    cycle.add(p);
                    if (p.equals(neighbor)) break;
                }
                Collections.reverse(cycle);
                return cycle;
            }
            if (!visited.contains(neighbor)) {
                List<ProcessNode> found = dfs(neighbor, edges, visited, inStack, pathStack);
                if (found != null) return found;
            }
        }

        inStack.remove(current);
        pathStack.pop();
        return null;
    }
}
