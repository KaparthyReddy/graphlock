package com.graphlock.core;

import java.util.Objects;

/** A resource type with a fixed total instance count (e.g. "Printer" with 2 units). */
public final class ResourceType {
    private final String id;
    private final int totalInstances;

    public ResourceType(String id, int totalInstances) {
        if (totalInstances <= 0) {
            throw new IllegalArgumentException("Resource must have at least one instance");
        }
        this.id = Objects.requireNonNull(id);
        this.totalInstances = totalInstances;
    }

    public String getId() { return id; }
    public int getTotalInstances() { return totalInstances; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceType)) return false;
        return id.equals(((ResourceType) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id + "(" + totalInstances + ")"; }
}
