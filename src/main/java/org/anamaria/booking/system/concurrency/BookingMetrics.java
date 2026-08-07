package org.anamaria.booking.system.concurrency;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component
public class BookingMetrics {

    private final Map<StrategyName, StrategyStats> stats = new ConcurrentHashMap<>();

    public void recordSuccess(StrategyName strategy, long latencyNanos) {
        statsFor(strategy).recordSuccess(latencyNanos);
    }

    public void recordConflict(StrategyName strategy, long latencyNanos) {
        statsFor(strategy).recordConflict(latencyNanos);
    }

    public void recordError(StrategyName strategy, long latencyNanos) {
        statsFor(strategy).recordError(latencyNanos);
    }

    public <T> T timed(StrategyName strategy, BookingCallable<T> action) {
        long start = System.nanoTime();
        try {
            T result = action.call();
            recordSuccess(strategy, System.nanoTime() - start);
            return result;
        } catch (BookingConflictException ex) {
            recordConflict(strategy, System.nanoTime() - start);
            throw ex;
        } catch (RuntimeException ex) {
            recordError(strategy, System.nanoTime() - start);
            throw ex;
        } catch (Exception ex) {
            recordError(strategy, System.nanoTime() - start);
            throw new RuntimeException(ex);
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (StrategyName name : StrategyName.values()) {
            StrategyStats s = stats.get(name);
            if (s != null && s.attempts() > 0) {
                out.put(name.name(), s.toMap());
            }
        }
        return out;
    }

    public void reset() {
        stats.clear();
    }

    private StrategyStats statsFor(StrategyName strategy) {
        return stats.computeIfAbsent(strategy, ignored -> new StrategyStats());
    }

    @FunctionalInterface
    public interface BookingCallable<T> {
        T call() throws Exception;
    }

    private static final class StrategyStats {
        private final LongAdder successes = new LongAdder();
        private final LongAdder conflicts = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder totalLatencyNanos = new LongAdder();
        private final AtomicLong maxLatencyNanos = new AtomicLong();
        private final ConcurrentHashMap.KeySetView<Long, Boolean> latencySamples =
                ConcurrentHashMap.newKeySet();

        void recordSuccess(long latencyNanos) {
            successes.increment();
            trackLatency(latencyNanos);
        }

        void recordConflict(long latencyNanos) {
            conflicts.increment();
            trackLatency(latencyNanos);
        }

        void recordError(long latencyNanos) {
            errors.increment();
            trackLatency(latencyNanos);
        }

        private void trackLatency(long latencyNanos) {
            totalLatencyNanos.add(latencyNanos);
            maxLatencyNanos.accumulateAndGet(latencyNanos, Math::max);
            if (latencySamples.size() < 10_000) {
                latencySamples.add(latencyNanos);
            }
        }

        long attempts() {
            return successes.sum() + conflicts.sum() + errors.sum();
        }

        Map<String, Object> toMap() {
            long attempts = attempts();
            long totalNanos = totalLatencyNanos.sum();
            double avgMs = attempts == 0 ? 0 : (totalNanos / (double) attempts) / 1_000_000.0;
            double p95Ms = percentileMs(0.95);
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("attempts", attempts);
            map.put("successes", successes.sum());
            map.put("conflicts", conflicts.sum());
            map.put("errors", errors.sum());
            map.put("avgLatencyMs", round(avgMs));
            map.put("p95LatencyMs", round(p95Ms));
            map.put("maxLatencyMs", round(maxLatencyNanos.get() / 1_000_000.0));
            return map;
        }

        private double percentileMs(double percentile) {
            long[] samples = latencySamples.stream().mapToLong(Long::longValue).sorted().toArray();
            if (samples.length == 0) {
                return 0;
            }
            int index = Math.min(samples.length - 1, (int) Math.ceil(percentile * samples.length) - 1);
            return samples[Math.max(0, index)] / 1_000_000.0;
        }

        private static double round(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }
}
