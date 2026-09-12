package com.graphlock.recovery;

import com.graphlock.core.AllocationState;
import com.graphlock.core.DeadlockReport;
import com.graphlock.core.ProcessNode;

/**
 * Strategy interface: given a deadlock report, decide how to break it.
 * The two classic OS approaches - kill a process, or preempt a resource
 * from one - have genuinely different tradeoffs (data loss vs. rollback
 * complexity), which is exactly the kind of decision that should be
 * swappable rather than hardcoded.
 */
public interface RecoveryStrategy {

    /** @return the process chosen as the recovery target (victim for
     * termination, or the process a resource was preempted from) */
    ProcessNode recover(DeadlockReport report, AllocationState state);

    String getName();
}
