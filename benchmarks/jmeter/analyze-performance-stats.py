#!/usr/bin/env python3
"""Aggregate performance statistics from measured benchmark files."""
import json
import math
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "results"


def load_json(name: str):
    path = ROOT / name
    with open(path, encoding="utf-8-sig") as f:
        return json.load(f)


def percentile(sorted_vals, p):
    if not sorted_vals:
        return None
    k = (len(sorted_vals) - 1) * (p / 100.0)
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return sorted_vals[int(k)]
    return sorted_vals[f] + (sorted_vals[c] - sorted_vals[f]) * (k - f)


def summarize(values):
    vals = sorted(values)
    n = len(vals)
    if n == 0:
        return None
    mean = statistics.mean(vals)
    stdev = statistics.stdev(vals) if n > 1 else 0.0
    # 95% CI for mean (t-distribution)
    if n > 1:
        # approximate t for 95% two-sided; use table for small n
        t_table = {2: 12.706, 3: 4.303, 4: 3.182, 5: 2.776, 6: 2.571, 7: 2.447, 8: 2.365,
                   9: 2.306, 10: 2.262, 11: 2.228, 12: 2.201, 13: 2.179, 14: 2.160, 15: 2.145,
                   20: 2.086, 30: 2.045}
        t = t_table.get(n, 1.96 if n >= 30 else 2.0)
        margin = t * (stdev / math.sqrt(n))
        ci_low, ci_high = mean - margin, mean + margin
    else:
        ci_low = ci_high = mean
    return {
        "n": n,
        "mean": round(mean, 2),
        "median": round(statistics.median(vals), 2),
        "stdev": round(stdev, 2),
        "min": round(min(vals), 2),
        "max": round(max(vals), 2),
        "p95": round(percentile(vals, 95), 2),
        "p99": round(percentile(vals, 99), 2),
        "ci95Low": round(ci_low, 2),
        "ci95High": round(ci_high, 2),
    }


def jmeter_repeated_stats(data):
    labels = ["book-unsafe", "book-synchronized", "book-reentrant-lock", "book-pessimistic", "book-optimistic"]
    out = {}
    for label in labels:
        means = [run["strategies"][label]["meanResTime"] for run in data["runs"] if label in run["strategies"]]
        out[label] = {
            "crossRun_meanResTime": summarize(means),
            "withinRun_example_rep1": data["runs"][0]["strategies"].get(label),
        }
    return out


def race_repeated_stats(data):
    out = {}
    for strategy in sorted({r["strategy"] for r in data["runs"]}):
        subset = [r for r in data["runs"] if r["strategy"] == strategy]
        out[strategy] = {
            "wallClockMs": summarize([r["wallClockMs"] for r in subset]),
            "doubleBookingPairs": summarize([r["doubleBookingPairs"] for r in subset]),
            "successes": summarize([r["successes"] for r in subset]),
        }
    return out


def load_sweep_within_run(manifest):
    """Within-run percentiles from one JMeter execution per thread level."""
    label_map = {
        "book-unsafe": "UNSAFE",
        "book-synchronized": "SYNCHRONIZED",
        "book-reentrant-lock": "REENTRANT_LOCK",
        "book-pessimistic": "PESSIMISTIC",
        "book-optimistic": "OPTIMISTIC",
    }
    out = {}
    for entry in manifest["runs"]:
        t = entry["threads"]
        out[t] = {}
        stats = entry.get("statistics") or {}
        for jlabel, strategy in label_map.items():
            s = stats.get(jlabel)
            if not s:
                continue
            out[t][strategy] = {
                "sampleCount": s["sampleCount"],
                "meanResTime": round(s["meanResTime"], 2),
                "medianResTime": round(s["medianResTime"], 2),
                "minResTime": round(s["minResTime"], 2),
                "maxResTime": round(s["maxResTime"], 2),
                "p90ResTime": round(s["pct1ResTime"], 2),
                "p95ResTime": round(s["pct2ResTime"], 2),
                "p99ResTime": round(s["pct3ResTime"], 2),
                "throughput": round(s["throughput"], 2),
            }
    return out


def main():
    summary = {"sources": []}

    sweep_path = ROOT / "load-sweep" / "load-sweep-manifest.json"
    if sweep_path.exists():
        manifest = json.loads(sweep_path.read_text(encoding="utf-8-sig"))
        summary["jmeterWithinRun_byThreads"] = load_sweep_within_run(manifest)
        summary["sources"].append(str(sweep_path.name))

    repeated_path = ROOT / "jmeter-repeated-t50.json"
    if repeated_path.exists():
        repeated = load_json("jmeter-repeated-t50.json")
        summary["jmeterCrossRun_threads50"] = {
            "repetitions": repeated["repetitions"],
            "strategies": jmeter_repeated_stats(repeated),
        }
        summary["sources"].append(repeated_path.name)

    race_path = ROOT / "race-repeated-c50.json"
    if race_path.exists():
        race = load_json("race-repeated-c50.json")
        summary["harnessCrossRun_concurrency50"] = {
            "repetitions": race["repetitions"],
            "slotStart": race["slotStart"],
            "strategies": race_repeated_stats(race),
        }
        summary["sources"].append(race_path.name)

    out_path = ROOT / "statistical-summary.json"
    out_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(f"Wrote {out_path}")
    return summary


if __name__ == "__main__":
    main()
