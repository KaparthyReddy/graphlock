package com.graphlock.observer;

import com.graphlock.core.DeadlockReport;

public interface DeadlockObserver {
    void onDeadlockCheck(DeadlockReport report);
}
