package com.graphlock.observer;

import com.graphlock.core.DeadlockReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeadlockLogger implements DeadlockObserver {

    private final List<DeadlockReport> history = new ArrayList<>();

    @Override
    public void onDeadlockCheck(DeadlockReport report) {
        history.add(report);
    }

    public List<DeadlockReport> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
