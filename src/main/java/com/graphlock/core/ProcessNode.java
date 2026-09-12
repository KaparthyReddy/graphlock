package com.graphlock.core;

import java.util.Objects;

public final class ProcessNode {
    private final String id;

    public ProcessNode(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public String getId() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessNode)) return false;
        return id.equals(((ProcessNode) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id; }
}
