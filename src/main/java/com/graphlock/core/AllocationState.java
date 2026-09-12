package com.graphlock.core;

import java.util.*;

/**
 * The full snapshot a detection algorithm operates on: which processes
 * currently hold which resource instances (Allocation), what each
 * process is additionally requesting (Request), and what's left
 * unallocated (Available) - the same three-vector/matrix shape used by
 * Banker's Algorithm, since deadlock detection and safe-state checking
 * are structurally the same kind of problem.
 */
public class AllocationState {

    private final List<ProcessNode> processes;
    private final List<ResourceType> resources;
    private final Map<ProcessNode, Map<ResourceType, Integer>> allocation = new HashMap<>();
    private final Map<ProcessNode, Map<ResourceType, Integer>> request = new HashMap<>();

    public AllocationState(List<ProcessNode> processes, List<ResourceType> resources) {
        this.processes = List.copyOf(processes);
        this.resources = List.copyOf(resources);
        for (ProcessNode p : processes) {
            allocation.put(p, new HashMap<>());
            request.put(p, new HashMap<>());
        }
    }

    public void allocate(ProcessNode process, ResourceType resource, int instances) {
        validateWithinTotal(resource, instances);
        allocation.get(process).merge(resource, instances, Integer::sum);
    }

    public void request(ProcessNode process, ResourceType resource, int instances) {
        validateWithinTotal(resource, instances);
        request.get(process).merge(resource, instances, Integer::sum);
    }

    private void validateWithinTotal(ResourceType resource, int instances) {
        if (instances <= 0) {
            throw new IllegalArgumentException("Instance count must be positive");
        }
        if (instances > resource.getTotalInstances()) {
            throw new IllegalArgumentException(
                    "Requested " + instances + " instances of " + resource + " but only "
                            + resource.getTotalInstances() + " exist");
        }
    }

    public int getAllocated(ProcessNode process, ResourceType resource) {
        return allocation.get(process).getOrDefault(resource, 0);
    }

    public int getRequested(ProcessNode process, ResourceType resource) {
        return request.get(process).getOrDefault(resource, 0);
    }

    public int getAvailable(ResourceType resource) {
        int totalAllocated = processes.stream()
                .mapToInt(p -> getAllocated(p, resource))
                .sum();
        return resource.getTotalInstances() - totalAllocated;
    }

    public List<ProcessNode> getProcesses() { return processes; }
    public List<ResourceType> getResources() { return resources; }

    /** True if this process holds at least one instance of the given resource. */
    public boolean holds(ProcessNode process, ResourceType resource) {
        return getAllocated(process, resource) > 0;
    }

    /** True if this process has an outstanding request for the given resource. */
    public boolean isWaitingFor(ProcessNode process, ResourceType resource) {
        return getRequested(process, resource) > 0;
    }
}
