"""Search decision thresholds without retraining or changing the datasets."""
import argparse, csv, itertools, json
from pathlib import Path
import numpy as np
from train import LANGUAGES, vectorize, softmax

BANDS = ("1_word", "2_words", "3_5_words", "6_plus_words")
GRID = ((0.70, 0.75, 0.80, 0.85, 0.90, 0.93, 0.96),
        (0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40))

def band(title):
    count = len(title.split())
    return "1_word" if count == 1 else "2_words" if count == 2 else "3_5_words" if count <= 5 else "6_plus_words"

def load_rows(path, allow_column=False):
    with open(path, encoding="utf-8", newline="") as stream:
        return [(row["title"].strip(), row["language"].strip(),
                 allow_column and row.get("ambiguous_acceptable", "0").strip() == "1")
                for row in csv.DictReader(stream)]

def load_model(path):
    with open(path, encoding="utf-8") as stream: model = json.load(stream)
    weights = np.zeros((model["feature_count"], len(LANGUAGES)), dtype=np.float64)
    for index, values in model["weights"].items(): weights[int(index)] = values
    return weights, np.array(model["bias"], dtype=np.float64)

def score_rows(rows, weights, bias):
    scored = []
    for title, expected, acceptable in rows:
        scores = bias.copy()
        for index, value in vectorize(title).items(): scores += weights[index] * value
        probabilities = softmax(scores)
        order = np.argsort(probabilities)
        best, second = int(order[-1]), int(order[-2])
        scored.append((title, expected, acceptable, band(title), LANGUAGES[best],
                       float(probabilities[best]), float(probabilities[best] - probabilities[second])))
    return scored

def evaluate(scored, thresholds):
    exact = accepted = decided = correct_decided = confident_wrong = 0
    confusion = np.zeros((4, 4), dtype=int)
    for title, expected, acceptable, length_band, predicted, confidence, margin in scored:
        confidence_limit, margin_limit = thresholds[length_band]
        is_decided = confidence >= confidence_limit and margin >= margin_limit
        actual_prediction = predicted if is_decided else "ambiguous"
        if is_decided:
            decided += 1
            predicted_index = LANGUAGES.index(predicted)
            if expected in LANGUAGES: confusion[LANGUAGES.index(expected), predicted_index] += 1
        exact_ok = expected == actual_prediction if expected in LANGUAGES else actual_prediction == "ambiguous"
        accepted_ok = exact_ok or (actual_prediction == "ambiguous" and acceptable)
        exact += int(exact_ok)
        accepted += int(accepted_ok)
        if is_decided:
            correct_decided += int(expected in LANGUAGES and predicted == expected)
            confident_wrong += int(expected in LANGUAGES and predicted != expected)
    total = len(scored)
    es_total, pt_total = int(np.sum(confusion[0])), int(np.sum(confusion[2]))
    return {"exact_accuracy": exact / total if total else 0.0,
            "accepted_accuracy": accepted / total if total else 0.0,
            "coverage": decided / total if total else 0.0,
            "definitive_accuracy": correct_decided / decided if decided else 0.0,
            "confident_wrong_predictions": confident_wrong,
            "es_to_pt": int(confusion[0, 2]) / es_total if es_total else 0.0,
            "pt_to_es": int(confusion[2, 0]) / pt_total if pt_total else 0.0,
            "samples": total}

def meets(metrics):
    return (metrics["accepted_accuracy"] >= 0.95 and metrics["definitive_accuracy"] >= 0.98
            and metrics["confident_wrong_predictions"] / max(1, metrics["samples"]) <= 0.01
            and metrics["es_to_pt"] <= 0.02 and metrics["pt_to_es"] <= 0.03)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="build-domain-diagnostic/language-model.json")
    parser.add_argument("--domain", default="datasets/music_titles.csv")
    parser.add_argument("--golden", default="../../src/test/resources/language/golden-dataset.csv")
    parser.add_argument("--output", default="build-domain-diagnostic")
    args = parser.parse_args()
    weights, bias = load_model(args.model)
    scored = score_rows(load_rows(args.domain), weights, bias) + score_rows(load_rows(args.golden, True), weights, bias)
    current = {"1_word": (0.93, 0.35), "2_words": (0.88, 0.25), "3_5_words": (0.82, 0.18), "6_plus_words": (0.82, 0.18)}
    evaluated = []
    for _ in range(4):
        changed = False
        for length_band in BANDS:
            candidates = []
            for confidence, margin in itertools.product(*GRID):
                candidate = dict(current); candidate[length_band] = (confidence, margin)
                metrics = evaluate(scored, candidate)
                candidates.append((metrics["coverage"] if meets(metrics) else -1.0, metrics["accepted_accuracy"], candidate, metrics))
                evaluated.append({"thresholds": candidate, **metrics})
            best = max(candidates, key=lambda item: (item[0], item[1]))
            if best[0] >= 0 and best[2] != current:
                current = best[2]; changed = True
        if not changed: break
    final_metrics = evaluate(scored, current)
    output = Path(args.output); output.mkdir(parents=True, exist_ok=True)
    with open(output / "threshold-grid.csv", "w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["thresholds", "exact_accuracy", "accepted_accuracy", "coverage", "definitive_accuracy", "confident_wrong_predictions", "es_to_pt", "pt_to_es", "samples"])
        writer.writeheader(); writer.writerows(evaluated)
    with open(output / "best-thresholds.json", "w", encoding="utf-8") as stream:
        json.dump({"thresholds": current, "metrics": final_metrics, "meets_promotion_constraints": meets(final_metrics)}, stream, indent=2)
    print(json.dumps({"thresholds": current, "metrics": final_metrics, "evaluated_configurations": len(evaluated), "meets_promotion_constraints": meets(final_metrics)}, indent=2))

if __name__ == "__main__": main()
