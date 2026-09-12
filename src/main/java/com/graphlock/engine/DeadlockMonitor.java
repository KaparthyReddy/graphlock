package com.graphlock.engine;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;
import com.graphlock.detection.DeadlockDetector;
import com.graphlock.observer.DeadlockObserver;
import com.graphlock.recovery.RecoveryStrategy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ties detection and recovery together: runs the general (correct,
 * multi-instance-aware) DeadlockDetector against a given state, notifies
 * observers of the result, and - if a recovery strategy is provided and
 * a deadlock was found - invokes it. Kept separate from
 * WaitForGraphAnalyzer entirely, since that class is a narrower,
 * single-instance-only tool best used for illustrating *why* the general
 * algorithm is necessary, not as the thing production code should rely on.
 */
public class DeadlockMonitor {

    private final DeadlockDetector detector;
    private final List<DeadlockObserver> observers = new CopyOnWriteArrayList<>();

    public DeadlockMonitor(DeadlockDetector detector) {
        this.detector = detector;
    }

    public void addObserver(DeadlockObserver observer) {
        observers.add(observer);
    }

    public DeadlockReport checkState(AllocationState state) {
        DeadlockReport report = detector.detect(state);
        for (DeadlockObserver observer : observers) {
            observer.onDeadlockCheck(report);
        }
        return report;
    }

    /** Runs detection, and if deadlocked, applies the given recovery
     * strategy - returns the chosen recovery target, or null if no
     * deadlock was found (nothing to recover from). */
    public ProcessNode checkAndRecover(AllocationState state, RecoveryStrategy strategy) {
        DeadlockReport report = checkState(state);
        if (!report.isDeadlockDetected()) {
            return null;
        }
        return strategy.recover(report, state);
    }
}
