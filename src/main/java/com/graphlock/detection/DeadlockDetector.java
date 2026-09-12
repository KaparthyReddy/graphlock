package com.graphlock.detection;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;
import com.graphlock.core.ResourceType;

import java.util.*;

/**
 * General-purpose deadlock detection for multi-instance resources -
 * structurally the same algorithm as Banker's Safety Algorithm, just
 * applied to detect deadlock among *already-allocated* processes rather
 * than validating a hypothetical future request.
 *
 * Repeatedly finds any process whose outstanding request can be fully
 * satisfied by what's currently available, "finishes" it (releasing its
 * resources back to Available), and repeats. Any process never finished
 * this way is genuinely deadlocked - unlike simple cycle detection, this
 * correctly handles the case where a cycle exists in the wait-for graph
 * but the system can still resolve it because enough instances exist
 * elsewhere.
 */
public class DeadlockDetector {

    public DeadlockReport detect(AllocationState state) {
        List<ProcessNode> processes = state.getProcesses();
        List<ResourceType> resources = state.getResources();

        Map<ResourceType, Integer> work = new HashMap<>();
        for (ResourceType resource : resources) {
            work.put(resource, state.getAvailable(resource));
        }

        Set<ProcessNode> finished = new HashSet<>();
        boolean progress = true;

        while (progress) {
            progress = false;
            for (ProcessNode process : processes) {
                if (finished.contains(process)) continue;

                if (canBeSatisfied(process, resources, state, work)) {
                    for (ResourceType resource : resources) {
                        work.merge(resource, state.getAllocated(process, resource), Integer::sum);
                    }
                    finished.add(process);
                    progress = true;
                }
            }
        }

        List<ProcessNode> deadlocked = processes.stream()
                .filter(p -> !finished.contains(p))
                .toList();

        return new DeadlockReport(!deadlocked.isEmpty(), deadlocked);
    }

    private boolean canBeSatisfied(ProcessNode process, List<ResourceType> resources,
                                    AllocationState state, Map<ResourceType, Integer> work) {
        for (ResourceType resource : resources) {
            if (state.getRequested(process, resource) > work.get(resource)) {
                return false;
            }
        }
        return true;
    }
}
