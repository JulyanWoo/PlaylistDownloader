"""Run deterministic Music4All balance experiments and write one comparison CSV."""
import argparse, csv, json, subprocess, sys
from collections import Counter
from pathlib import Path

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--music-data", default="data/music-real.csv")
    parser.add_argument("--data", default="datasets/openlid-four-language.csv")
    parser.add_argument("--domain-data", default="datasets/music_titles.csv")
    parser.add_argument("--hard-data", default="")
    parser.add_argument("--music-weight", type=float, default=5.0)
    parser.add_argument("--output", default="build-music-balance-comparison")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    output = Path(args.output); output.mkdir(parents=True, exist_ok=True)
    with open(args.music_data, encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream))
    print("Music4All statistics")
    for language in ("es", "pt", "en", "fr"):
        group = [row for row in rows if row["language"] == language]
        lengths = Counter(len(row["title"].split()) for row in group)
        buckets = {"1": 0, "2": 0, "3-5": 0, "6+": 0}
        for length, count in lengths.items():
            buckets["1" if length == 1 else "2" if length == 2 else "3-5" if length <= 5 else "6+"] += count
        chars = [len(row["title"]) for row in group]
        print(language, {"samples": len(group), "words": buckets,
                         "characters": {"min": min(chars, default=0), "mean": round(sum(chars)/len(chars), 2) if chars else 0,
                                        "max": max(chars, default=0)}})

    experiments = [("BASE", "none", 1.0), ("BALANCED-DOWNSAMPLE", "downsample", 1.0),
                   ("BALANCED-WEIGHTS", "weights", 1.0)]
    experiments.extend((f"ES-BOOST-{boost:g}", "none", boost) for boost in (1.10, 1.25, 1.50, 1.75))
    results = []
    for name, balance, boost in experiments:
        candidate = output / name
        command = [sys.executable, str(root / "train.py"), "--data", args.data,
                   "--domain-data", args.domain_data, "--music-data", args.music_data,
                   "--music-weight", str(args.music_weight), "--music-balance", balance,
                   "--music-es-boost", str(boost), "--output", str(candidate)]
        if args.hard_data: command.extend(["--hard-data", args.hard_data])
        completed = subprocess.run(command, cwd=root, text=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        metrics_path = candidate / "metrics.json"
        if not metrics_path.exists(): raise SystemExit(f"No metrics for {name}: {completed.stderr}")
        metrics = json.loads(metrics_path.read_text(encoding="utf-8"))
        test, golden = metrics["test"], metrics.get("golden_diagnostics", {})
        results.append({"experiment": name, "music_balance": balance, "music_es_boost": boost,
                        "validation_accuracy": metrics["validation"]["accuracy"], "test_accuracy": test["accuracy"],
                        "macro_f1": test["macro_f1"], "es_to_pt": test["es_to_pt_false_positive_rate"],
                        "pt_to_es": test["pt_to_es_false_positive_rate"],
                        "accuracy_es": test["accuracy_by_language"].get("es", 0),
                        "accuracy_pt": test["accuracy_by_language"].get("pt", 0),
                        "accuracy_en": test["accuracy_by_language"].get("en", 0),
                        "accuracy_fr": test["accuracy_by_language"].get("fr", 0),
                        "one_word_accuracy": test["by_length"].get("1_word", {}).get("accuracy", 0),
                        "two_word_accuracy": test["by_length"].get("2_words", {}).get("accuracy", 0),
                        "three_to_five_accuracy": test["by_length"].get("3_5_words", {}).get("accuracy", 0),
                        "golden_exact": golden.get("exact_accuracy", 0), "golden_accepted": golden.get("accepted_accuracy", 0),
                        "golden_coverage": golden.get("coverage", 0), "definitive_accuracy": golden.get("accuracy_on_definitive", 0),
                        "confident_errors": golden.get("confident_wrong_predictions", 0), "process_exit": completed.returncode})
        print(name, results[-1])
    fields = list(results[0])
    with (output / "comparison.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields); writer.writeheader(); writer.writerows(results)
    (output / "comparison.json").write_text(json.dumps(results, indent=2), encoding="utf-8")

if __name__ == "__main__": main()
