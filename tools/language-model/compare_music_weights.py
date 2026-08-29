"""Train music-domain candidates and compare them without promoting any model."""
import argparse
import csv
import json
import subprocess
import sys
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--weights", default="2,3,4,5")
    parser.add_argument("--data", default="datasets/openlid-four-language.csv")
    parser.add_argument("--domain-data", default="datasets/music_titles.csv")
    parser.add_argument("--music-data", default="data/music-real.csv")
    parser.add_argument("--hard-data", default="")
    parser.add_argument("--output", default="build-music-comparison")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    results = []
    for value in (float(item.strip()) for item in args.weights.split(",")):
        name = f"build-music-weight-{value:g}"
        candidate = output / name
        command = [sys.executable, str(root / "train.py"), "--data", args.data,
                   "--domain-data", args.domain_data, "--music-data", args.music_data,
                   "--music-weight", str(value), "--output", str(candidate)]
        if args.hard_data:
            command.extend(["--hard-data", args.hard_data])
        completed = subprocess.run(command, cwd=root, text=True, capture_output=True)
        metrics_path = candidate / "metrics.json"
        if not metrics_path.exists():
            raise SystemExit(f"Candidate {value:g} did not produce metrics.json\n{completed.stderr}")
        metrics = json.loads(metrics_path.read_text(encoding="utf-8"))
        test = metrics["test"]
        golden = metrics.get("golden_diagnostics", {})
        results.append({
            "music_weight": value,
            "validation_accuracy": metrics["validation"]["accuracy"],
            "test_accuracy": test["accuracy"],
            "macro_f1": test["macro_f1"],
            "es_to_pt": test["es_to_pt_false_positive_rate"],
            "pt_to_es": test["pt_to_es_false_positive_rate"],
            "short_1_to_5_accuracy": test.get("short_1_to_5_words_accuracy", 0),
            "golden_exact": golden.get("exact_accuracy", 0),
            "golden_accepted": golden.get("accepted_accuracy", 0),
            "golden_coverage": golden.get("coverage", 0),
            "golden_definitive_accuracy": golden.get("accuracy_on_definitive", 0),
            "golden_confident_errors": golden.get("confident_wrong_predictions", 0),
            "process_exit": completed.returncode,
        })
        print(json.dumps(results[-1], indent=2))

    fields = list(results[0]) if results else []
    with (output / "comparison.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(results)
    (output / "comparison.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    print(f"Wrote {output / 'comparison.csv'}")


if __name__ == "__main__":
    main()
