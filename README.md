# GraphLock

A deadlock detection and recovery system implementing both the classic resource-allocation-graph cycle detection (for single-instance resources) and the general matrix-based detection algorithm (for multi-instance resources) — structurally the same shape as Banker's Safety Algorithm. Pluggable recovery via Strategy (process termination vs. resource preemption), Observer-based event tracking. Completes an OS trilogy alongside PageVault (memory management) and ShellCraft (process control).

## What this is — and the core insight it's built around

A cycle in a wait-for graph only *guarantees* deadlock when every resource type involved has exactly one instance. With multiple instances of a resource, a cycle can exist while the system is still perfectly safe — another process might be able to finish and release what's needed, breaking the "cycle" without anyone actually being stuck. Naive cycle detection alone gives a **false positive** in that case.

This project implements both approaches deliberately, side by side:
- `WaitForGraphAnalyzer` — DFS-based cycle detection, correctly scoped to single-instance resources only (it throws rather than give a wrong answer if handed multi-instance data)
- `DeadlockDetector` — the general, correct algorithm: repeatedly finds any process whose request can currently be satisfied, "finishes" it, releases its resources, and repeats. Anything never finished this way is genuinely deadlocked.

## Design patterns

- **Strategy** (`RecoveryStrategy`) — `ProcessTerminationStrategy` (kill the deadlocked process holding the fewest resources) and `ResourcePreemptionStrategy` (identify the process most able to spare a resource) are fully interchangeable.
- **Observer** (`DeadlockObserver`) — `DeadlockLogger` and `DeadlockStatistics` both react to every detection check independently, same architecture as PageVault's fault observers.

## Core components

| Layer | What it does |
|---|---|
| `core/` | `ProcessNode`, `ResourceType`, `AllocationState` (Allocation/Request/Available, same shape as Banker's Algorithm), `DeadlockReport` |
| `graph/` | `WaitForGraphAnalyzer` — DFS cycle detection, single-instance only |
| `detection/` | `DeadlockDetector` — the general, correct multi-instance-aware algorithm |
| `recovery/` | `ProcessTerminationStrategy`, `ResourcePreemptionStrategy` |
| `observer/` | `DeadlockLogger`, `DeadlockStatistics` |
| `engine/` | `DeadlockMonitor` — ties detection, observers, and recovery together |

## Tech stack

- Java 17, Maven
- JUnit 5
- GitHub Actions CI

## Project structure

```text
graphlock/
├── src/main/java/com/graphlock/
│ ├── core/
│ ├── graph/
│ ├── detection/
│ ├── recovery/
│ ├── observer/
│ ├── engine/
│ └── Main.java
└── src/test/java/com/graphlock/
├── graph/
├── detection/
├── recovery/
└── engine/
```


## Running it

```bash
mvn clean compile   # build
mvn test             # run the test suite (15 tests)
mvn exec:java         # run the three-scenario demo
```

## Verified working example — the false-positive demonstration

```bash
=== Scenario 2: Classic single-instance circular wait ===
Wait-for graph cycle found: [P1, P2]
General detector confirms: DEADLOCK detected among: [P1, P2]

-- Recovery options --
Process Termination would target: P1
Resource Preemption would target: P1

=== Scenario 3: Cycle exists, but system is actually SAFE (multi-instance) ===
Note: R1 has 2 instances; a naive single-instance cycle check on this wait-for graph
would flag P1<->P2 as circular, but the system is actually fine.
General (correct) detector result: No deadlock - system is in a safe state
(WaitForGraphAnalyzer is intentionally NOT run here - it would throw, since it
correctly refuses to analyze multi-instance resources)
```


In Scenario 3, R1 has 2 total instances, both allocated (1 each to P1 and P2), leaving 0 available. P1 requests one more instance — unsatisfiable immediately. But P2 has no outstanding request, so it can trivially finish and release its instance, which then satisfies P1's request. The general algorithm correctly walks through this resolution path and reports **no deadlock** — exactly the case naive cycle detection would get wrong.

## Test coverage

15 tests across cycle detection, the general detection algorithm, recovery strategies, and the monitoring engine — all passing.

```bash
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```


## License

MIT
