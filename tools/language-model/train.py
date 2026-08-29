"""Train the small offline title model without external ML dependencies.

Usage: python train.py [--data datasets/openlid-four-language.csv]
                       [--domain-data datasets/music_titles.csv]
                       [--music-data data/music-real.csv] [--output build]
It writes model.json and metrics.json. The binary is produced by export_model.py.
"""
import argparse, csv, hashlib, json, math
from collections import Counter
from pathlib import Path
import numpy as np

LANGUAGES = ["es", "en", "pt", "fr"]
MIN_N, MAX_N, FEATURES, SEED = 2, 5, 65536, 42
VOCABULARY = {
    "es": ("amor mujer mejor corazón vida noche contigo quiero volver cielo".split(), ("yo", "mi", "la", "por", "para", "no")),
    "en": ("love woman better heart life night together forever waiting beautiful dream".split(), ("i", "my", "the", "with", "for", "don't")),
    "pt": ("amor mulher melhor coração vida noite contigo quero voltar saudade coração".split(), ("eu", "meu", "a", "por", "para", "não")),
    "fr": ("amour femme meilleur cœur vie nuit ensemble toujours attendre belle rêve".split(), ("je", "mon", "la", "avec", "pour", "ne")),
}

def feature_hash(ngram):
    value = SEED
    for char in ngram:
        value = ((value * 31) + ord(char)) & 0xffffffff
    return (value & 0x7fffffff) % FEATURES

def vectorize(text):
    value = " " + text.lower() + " "
    result = {}
    for start in range(len(value)):
        for size in range(MIN_N, MAX_N + 1):
            if start + size <= len(value):
                index = feature_hash(value[start:start + size])
                result[index] = result.get(index, 0.0) + 1.0
    total = sum(result.values()) or 1.0
    return {index: count / total for index, count in result.items()}

def split(rows):
    groups = {language: [] for language in LANGUAGES}
    for title, language in rows:
        groups[language].append((title, language))
    train, validation, test = [], [], []
    for language in LANGUAGES:
        items = sorted(groups[language], key=lambda item: hashlib.sha256(item[0].lower().encode("utf-8")).hexdigest())
        for index, item in enumerate(items):
            (test if index % 20 in (0, 1, 2) else validation if index % 20 in (3, 4, 5) else train).append(item)
    return train, validation, test

def expand_rows(rows):
    """Add deterministic short title-like samples; source CSV remains the auditable seed set."""
    expanded = list(rows)
    for language in LANGUAGES:
        words, function_words = VOCABULARY[language]
        for index in range(250):
            first = words[index % len(words)]
            second = words[(index * 3 + 2) % len(words)]
            connector = function_words[index % len(function_words)]
            if index % 4 == 0:
                title = first
            elif index % 4 == 1:
                title = f"{connector} {first}"
            elif index % 4 == 2:
                title = f"{first} {second}"
            else:
                title = f"{connector} {first} {second}"
            expanded.append((title, language))
    return expanded

def softmax(scores):
    scores = scores - np.max(scores)
    values = np.exp(scores)
    return values / np.sum(values)

def train_model(rows, epochs=160, learning_rate=1.2, l2=0.0008):
    weights = np.zeros((FEATURES, len(LANGUAGES)), dtype=np.float64)
    bias = np.zeros(len(LANGUAGES), dtype=np.float64)
    for epoch in range(epochs):
        order = np.arange(len(rows))
        np.random.default_rng(SEED + epoch).shuffle(order)
        for position in order:
            title, language = rows[position]
            features = vectorize(title)
            scores = bias.copy()
            for index, value in features.items(): scores += weights[index] * value
            probabilities = softmax(scores)
            target = LANGUAGES.index(language)
            probabilities[target] -= 1.0
            scale = learning_rate / max(1, len(rows))
            for index, value in features.items():
                weights[index] -= scale * (probabilities * value + l2 * weights[index])
            bias -= scale * probabilities
    return weights, bias

def matrix_for(rows):
    """Efficient sparse logistic training for the OpenLID-sized corpus.

    The sparse matrix is still built by this file's vectorize()/feature_hash(),
    never by sklearn's hashing implementation.
    """
    from scipy.sparse import csr_matrix
    vectors = [vectorize(title) for title, _ in rows]
    data, indices, indptr = [], [], [0]
    for vector in vectors:
        for index, value in vector.items():
            indices.append(index); data.append(value)
        indptr.append(len(data))
    matrix = csr_matrix((data, indices, indptr), shape=(len(rows), FEATURES), dtype=np.float64)
    labels = np.array([LANGUAGES.index(language) for _, language in rows])
    return matrix, labels

def train_model_sklearn(rows, sample_weights):
    from sklearn.linear_model import SGDClassifier
    matrix, labels = matrix_for(rows)
    classifier = SGDClassifier(loss="log_loss", alpha=1e-6, max_iter=80, tol=1e-4,
                               random_state=SEED, average=True)
    classifier.fit(matrix, labels, sample_weight=np.array(sample_weights))
    return classifier.coef_.T.astype(np.float64), classifier.intercept_.astype(np.float64)

def evaluate(rows, weights, bias):
    confusion = np.zeros((len(LANGUAGES), len(LANGUAGES)), dtype=int)
    for title, language in rows:
        scores = bias.copy()
        for index, value in vectorize(title).items(): scores += weights[index] * value
        predicted = int(np.argmax(scores))
        confusion[LANGUAGES.index(language), predicted] += 1
    total = int(np.sum(confusion))
    accuracy = float(np.trace(confusion) / total) if total else 0.0
    es_total = int(np.sum(confusion[0]))
    es_to_pt = int(confusion[0, 2])
    pt_total = int(np.sum(confusion[2]))
    pt_to_es = int(confusion[2, 0])
    buckets = {"1_word": [], "2_words": [], "3_5_words": [], "6_10_words": []}
    for item in rows:
        count = len(item[0].split())
        bucket = "1_word" if count == 1 else "2_words" if count == 2 else "3_5_words" if count <= 5 else "6_10_words"
        buckets[bucket].append(item)
    by_length = {name: evaluate_basic(items, weights, bias) for name, items in buckets.items() if items}
    macro_f1 = []
    accuracy_by_language = {}
    for language in range(len(LANGUAGES)):
        language_total = int(np.sum(confusion[language, :]))
        accuracy_by_language[LANGUAGES[language]] = confusion[language, language] / language_total if language_total else 0.0
        precision = confusion[language, language] / max(1, int(np.sum(confusion[:, language])))
        recall = confusion[language, language] / max(1, int(np.sum(confusion[language, :])))
        macro_f1.append(2 * precision * recall / (precision + recall) if precision + recall else 0.0)
    return {"accuracy": accuracy, "macro_f1": sum(macro_f1) / len(macro_f1),
            "es_to_pt_false_positive_rate": es_to_pt / es_total if es_total else 0.0,
            "pt_to_es_false_positive_rate": pt_to_es / pt_total if pt_total else 0.0,
            "confusion_matrix": confusion.tolist(), "accuracy_by_language": accuracy_by_language,
            "samples": total, "by_length": by_length}

def evaluate_basic(rows, weights, bias):
    correct = 0
    for title, language in rows:
        scores = bias.copy()
        for index, value in vectorize(title).items(): scores += weights[index] * value
        correct += int(int(np.argmax(scores)) == LANGUAGES.index(language))
    return {"accuracy": correct / len(rows), "samples": len(rows)}

def evaluate_coverage(rows, weights, bias):
    thresholds = {1: (0.93, 0.35), 2: (0.88, 0.25), 3: (0.82, 0.18)}
    decided = correct = 0
    for title, language in rows:
        scores = bias.copy()
        for index, value in vectorize(title).items(): scores += weights[index] * value
        probabilities = softmax(scores)
        order = np.argsort(probabilities)
        best, second = int(order[-1]), int(order[-2])
        words = len(title.split())
        confidence_limit, margin_limit = thresholds[1 if words == 1 else 2 if words == 2 else 3]
        if probabilities[best] >= confidence_limit and probabilities[best] - probabilities[second] >= margin_limit:
            decided += 1
            correct += int(best == LANGUAGES.index(language))
    return {"coverage": decided / len(rows) if rows else 0.0,
            "accuracy_on_decided": correct / decided if decided else 0.0,
            "ambiguous": len(rows) - decided, "samples": len(rows)}

def classify_prediction(title, weights, bias):
    scores = bias.copy()
    for index, value in vectorize(title).items(): scores += weights[index] * value
    probabilities = softmax(scores)
    order = np.argsort(probabilities)
    best, second = int(order[-1]), int(order[-2])
    words = len(title.split())
    confidence_limit, margin_limit = {1: (0.93, 0.35), 2: (0.88, 0.25)}.get(words, (0.82, 0.18))
    decided = probabilities[best] >= confidence_limit and probabilities[best] - probabilities[second] >= margin_limit
    return (LANGUAGES[best] if decided else "ambiguous", float(probabilities[best]),
            float(probabilities[best] - probabilities[second]), LANGUAGES[second], decided)

def write_es_pt_errors(rows, weights, bias, output_path):
    errors = []
    for title, expected in rows:
        predicted, confidence, margin, second, _ = classify_prediction(title, weights, bias)
        if expected == "es" and predicted == "pt":
            errors.append((confidence, title, predicted, margin, len(title.split()), second))
    errors.sort(reverse=True)
    with open(output_path, "w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(("title", "expected", "predicted", "confidence", "margin", "word_count", "second_alternative"))
        for confidence, title, predicted, margin, word_count, second in errors[:50]:
            writer.writerow((title, "es", predicted, round(confidence, 6), round(margin, 6), word_count, second))

def failure_category(title, expected, predicted):
    lower = title.lower()
    if any(marker in lower for marker in ("official", "remix", "feat", " ft ", "video", "audio")):
        return "parser_related"
    if len(title.split()) <= 2:
        return "very_short"
    if expected in ("es", "pt") and predicted in ("es", "pt") and expected != predicted:
        return "ES_PT_confusion"
    if expected in ("en", "fr") and predicted in ("en", "fr") and expected != predicted:
        return "EN_FR_confusion"
    if any(word in lower.split() for word in ("amor", "vida", "calma", "solo", "bella", "love", "forever", "tonight")):
        return "shared_word"
    return "wrong_language" if predicted != "ambiguous" else "ambiguous_but_should_be_detectable"

def diagnose_golden(rows, weights, bias, output_path):
    details = []
    exact = accepted = decided = correct_decided = confident_wrong = 0
    with open(output_path, "w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(("title", "expected", "ambiguous_acceptable", "predicted", "confidence", "margin", "second_alternative", "word_count", "status", "category"))
        for title, expected, acceptable in rows:
            predicted, confidence, margin, second, is_decided = classify_prediction(title, weights, bias)
            word_count = len(title.split())
            exact_ok = predicted == expected
            accepted_ok = exact_ok or (predicted == "ambiguous" and acceptable)
            status = "correct" if exact_ok else "accepted_ambiguous" if accepted_ok else "incorrect"
            category = "correct" if accepted_ok else failure_category(title, expected, predicted)
            if exact_ok: exact += 1
            if accepted_ok: accepted += 1
            if is_decided:
                decided += 1
                correct_decided += int(predicted == expected)
            if is_decided and not exact_ok: confident_wrong += 1
            row = [title, expected, int(acceptable), predicted, round(confidence, 6), round(margin, 6), second, word_count, status, category]
            writer.writerow(row)
            details.append((confidence if not exact_ok else -1, row))
    total = len(rows)
    return {"exact_accuracy": exact / total if total else 0.0,
            "accepted_accuracy": accepted / total if total else 0.0,
            "coverage": decided / total if total else 0.0,
            "accuracy_on_definitive": correct_decided / decided if decided else 0.0,
            "confident_wrong_predictions": confident_wrong,
            "samples": total,
            "top_confident_wrong": [row for _, row in sorted(details, reverse=True) if row[8] == "incorrect"][:30]}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="datasets/openlid-four-language.csv")
    parser.add_argument("--domain-data", default="datasets/music_titles.csv")
    parser.add_argument("--music-data", default="", help="Optional real music title dataset; never golden")
    parser.add_argument("--music-weight", type=float, default=3.0)
    parser.add_argument("--music-balance", choices=("none", "downsample", "weights"), default="none")
    parser.add_argument("--music-es-boost", type=float, default=1.0)
    parser.add_argument("--hard-data", default="", help="Optional mined ES/PT examples; never golden")
    parser.add_argument("--output", default="build")
    args = parser.parse_args()
    with open(args.data, encoding="utf-8", newline="") as stream:
        base_rows = [(row["title"].strip(), row["language"].strip()) for row in csv.DictReader(stream)]
    with open(args.domain_data, encoding="utf-8", newline="") as stream:
        domain_rows = [(row["title"].strip(), row["language"].strip()) for row in csv.DictReader(stream)]
    music_rows = []
    if args.music_data and Path(args.music_data).exists():
        with open(args.music_data, encoding="utf-8", newline="") as stream:
            music_rows = [(row["title"].strip(), row["language"].strip())
                          for row in csv.DictReader(stream)
                          if row.get("language", "").strip() in LANGUAGES]
    if args.music_balance == "downsample" and music_rows:
        music_groups = {language: [] for language in LANGUAGES}
        for row in music_rows:
            music_groups[row[1]].append(row)
        target = min(len(rows) for rows in music_groups.values())
        music_rows = []
        for language in LANGUAGES:
            ordered = sorted(music_groups[language], key=lambda item: hashlib.sha256(
                f"{language}\0{item[0].casefold()}".encode("utf-8")).hexdigest())
            music_rows.extend(ordered[:target])
    hard_rows = []
    if args.hard_data and Path(args.hard_data).exists():
        with open(args.hard_data, encoding="utf-8", newline="") as stream:
            hard_rows = [(row["title"].strip(), row["language"].strip()) for row in csv.DictReader(stream)]
    rows = base_rows + domain_rows
    if sorted({language for _, language in rows}) != sorted(LANGUAGES): raise SystemExit("Dataset must contain ES/EN/PT/FR")
    base_train, validation, test = split(base_rows)
    domain_train, _, _ = split(domain_rows)
    music_train, _, _ = split(music_rows)
    hard_train, _, _ = split(hard_rows) if hard_rows else ([], [], [])
    train = base_train + domain_train + music_train + hard_train
    sample_weights = [1.0] * len(base_train)
    for title, language in domain_train:
        sample_weights.append(5.0 if language in ("es", "pt") and len(title.split()) <= 3 else 3.0)
    for title, language in music_train:
        balance_factor = 1.0
        if args.music_balance == "weights" and language == "pt":
            counts = Counter(item[1] for item in music_rows)
            balance_factor = counts.get("es", 1) / max(1, counts.get("pt", 1))
        es_factor = args.music_es_boost if language == "es" else 1.0
        short_factor = 1.5 if language in ("es", "pt") and len(title.split()) <= 3 else 1.0
        sample_weights.append(args.music_weight * balance_factor * es_factor * short_factor)
    sample_weights.extend([8.0] * len(hard_train))
    weights, bias = train_model_sklearn(train, sample_weights) 
    output = Path(args.output); output.mkdir(parents=True, exist_ok=True)
    metrics = {"train": evaluate(train, weights, bias), "validation": evaluate(validation, weights, bias), "test": evaluate(test, weights, bias),
               "languages": LANGUAGES, "feature_count": FEATURES, "ngram_min": MIN_N, "ngram_max": MAX_N, "hash_seed": SEED,
               "dataset_sizes": {"base": len(base_rows), "domain": len(domain_rows), "music": len(music_rows), "hard": len(hard_rows)},
               "training_weights": {"music": args.music_weight, "music_balance": args.music_balance,
                                    "music_es_boost": args.music_es_boost}}
    metrics["validation"]["coverage"] = evaluate_coverage(validation, weights, bias)
    metrics["test"]["coverage"] = evaluate_coverage(test, weights, bias)
    write_es_pt_errors(test, weights, bias, output / "test-es-to-pt-errors.csv")
    golden_path = Path(__file__).resolve().parents[2] / "src" / "test" / "resources" / "language" / "golden-dataset.csv"
    if golden_path.exists():
        with open(golden_path, encoding="utf-8", newline="") as stream:
            golden = [(row["title"].strip(), row["language"].strip(), row.get("ambiguous_acceptable", "0").strip() == "1")
                      for row in csv.DictReader(stream)]
        golden_language_rows = [(title, language) for title, language, _ in golden if language in LANGUAGES]
        metrics["golden"] = evaluate(golden_language_rows, weights, bias)
        metrics["golden_diagnostics"] = diagnose_golden(golden, weights, bias, output / "golden-diagnostics.csv")
    sparse = {str(index): [float(value) for value in weights[index]] for index in range(FEATURES) if np.any(np.abs(weights[index]) > 1e-12)}
    with open(output / "language-model.json", "w", encoding="utf-8") as stream:
        json.dump({"version": 1, "feature_count": FEATURES, "ngram_min": MIN_N, "ngram_max": MAX_N, "hash_seed": SEED,
                   "languages": LANGUAGES, "bias": bias.tolist(), "weights": sparse}, stream, ensure_ascii=False)
    with open(output / "metrics.json", "w", encoding="utf-8") as stream: json.dump(metrics, stream, indent=2)
    print(json.dumps(metrics, indent=2))
    short = [metrics["test"]["by_length"].get(name) for name in ("1_word", "2_words", "3_5_words")]
    short_accuracy = sum(item["accuracy"] * item["samples"] for item in short if item) / sum(item["samples"] for item in short if item)
    metrics["test"]["short_1_to_5_words_accuracy"] = short_accuracy
    with open(output / "metrics.json", "w", encoding="utf-8") as stream: json.dump(metrics, stream, indent=2)
    if (metrics["validation"]["accuracy"] < 0.95 or metrics["test"]["accuracy"] < 0.95
            or short_accuracy < 0.90
            or metrics["test"]["es_to_pt_false_positive_rate"] > 0.02
            or metrics["test"]["pt_to_es_false_positive_rate"] > 0.03):
        raise SystemExit("Threshold not met; existing runtime model was not replaced")

if __name__ == "__main__": main()
