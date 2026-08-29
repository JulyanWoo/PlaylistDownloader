"""Mine short ES/PT examples from training/domain data only; golden is never read."""
import argparse, csv, json
from pathlib import Path
import numpy as np
from train import LANGUAGES, vectorize, softmax

def load_model(path):
    with open(path, encoding="utf-8") as stream: model = json.load(stream)
    weights = np.zeros((model["feature_count"], 4), dtype=np.float64)
    for index, values in model["weights"].items(): weights[int(index)] = values
    return weights, np.array(model["bias"], dtype=np.float64)

def rows(path):
    with open(path, encoding="utf-8", newline="") as stream:
        return [(row["title"].strip(), row["language"].strip()) for row in csv.DictReader(stream)]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="build-domain-diagnostic/language-model.json")
    parser.add_argument("--input", action="append", required=True)
    parser.add_argument("--output", default="data/hard-es-pt.csv")
    parser.add_argument("--margin", type=float, default=0.35)
    parser.add_argument("--limit", type=int, default=20000)
    args = parser.parse_args()
    weights, bias = load_model(args.model)
    candidates = {}
    for path in args.input:
        for title, expected in rows(path):
            if expected not in ("es", "pt") or not 1 <= len(title.split()) <= 5: continue
            scores = bias.copy()
            for index, value in vectorize(title).items(): scores += weights[index] * value
            probabilities = softmax(scores)
            order = np.argsort(probabilities)
            best, second = int(order[-1]), int(order[-2])
            margin = float(probabilities[best] - probabilities[second])
            pair = {LANGUAGES[best], LANGUAGES[second]}
            if pair == {"es", "pt"} and (LANGUAGES[best] != expected or margin <= args.margin):
                candidates.setdefault(title.lower(), (title, expected, margin, LANGUAGES[best]))
    ranked = sorted(candidates.values(), key=lambda item: (item[2], item[0].lower()))
    by_language = {language: [item for item in ranked if item[1] == language] for language in ("es", "pt")}
    per_language = min(args.limit // 2, len(by_language["es"]), len(by_language["pt"]))
    selected = by_language["es"][:per_language] + by_language["pt"][:per_language]
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    with open(output, "w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream); writer.writerow(("title", "language")); writer.writerows((title, language) for title, language, _, _ in selected)
    print(json.dumps({"candidates": len(candidates), "selected": len(selected), "es": sum(language == "es" for _, language, _, _ in selected), "pt": sum(language == "pt" for _, language, _, _ in selected), "output": str(output)}, indent=2))

if __name__ == "__main__": main()
