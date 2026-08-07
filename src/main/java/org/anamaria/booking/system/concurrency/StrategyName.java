package org.anamaria.booking.system.concurrency;

public enum StrategyName {
    UNSAFE,
    SYNCHRONIZED,
    REENTRANT_LOCK,
    PESSIMISTIC,
    OPTIMISTIC,
    BLOCKING,
    EXECUTOR,
    COMPLETABLE_FUTURE,
    VIRTUAL_THREAD
}
