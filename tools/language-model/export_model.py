"""Export train.py JSON weights to the compact Java binary model format."""
import argparse, json, struct
from pathlib import Path

MAGIC = b"PLDMODEL"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", default="build/language-model.json")
    parser.add_argument("--output", default="../../src/main/resources/models/language-chargram-v1.bin")
    args = parser.parse_args()
    with open(args.input, encoding="utf-8") as stream: model = json.load(stream)
    metrics_path = Path(args.input).with_name("metrics.json")
    if metrics_path.exists():
        with open(metrics_path, encoding="utf-8") as stream: all_metrics = json.load(stream)
        metrics = all_metrics["test"]
        short = metrics.get("short_1_to_5_words_accuracy", 0.0)
        golden = all_metrics.get("golden_diagnostics", {})
        if not golden:
            golden = {"exact_accuracy": all_metrics.get("golden", {}).get("accuracy", 0.0),
                      "accepted_accuracy": all_metrics.get("golden", {}).get("accuracy", 0.0),
                      "coverage": 0.0, "accuracy_on_definitive": 0.0, "confident_wrong_predictions": 1}
        distinguishable = all_metrics.get("distinguishable", {"es_to_pt": 0.0, "pt_to_es": 0.0})
        if (all_metrics["validation"]["accuracy"] < 0.95 or metrics["accuracy"] < 0.95
                or short < 0.90 or golden.get("exact_accuracy", 0.0) < 0.80
                or golden.get("coverage", 0.0) < 0.60
                or golden.get("accuracy_on_definitive", 0.0) < 0.98
                or golden.get("confident_wrong_predictions", 1) != 0
                or distinguishable.get("es_to_pt", 1.0) > 0.02
                or distinguishable.get("pt_to_es", 1.0) > 0.03):
            raise SystemExit("Refusing promotion: test thresholds are not met")
    languages = model["languages"]
    weights = model["weights"]
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    with open(output, "wb") as stream:
        stream.write(MAGIC)
        stream.write(struct.pack(">IIII", model["version"], model["feature_count"], model["ngram_min"], model["ngram_max"]))
        stream.write(struct.pack(">I", model["hash_seed"]))
        stream.write(struct.pack(">I", len(languages)))
        for language in languages: stream.write(language.encode("ascii") + b"\0")
        stream.write(struct.pack(">" + "f" * len(languages), *model["bias"]))
        stream.write(struct.pack(">I", len(weights)))
        for index in sorted(weights, key=int):
            stream.write(struct.pack(">I", int(index)))
            stream.write(struct.pack(">" + "f" * len(languages), *weights[index]))
    print(f"Wrote {output} ({output.stat().st_size} bytes)")

if __name__ == "__main__": main()
