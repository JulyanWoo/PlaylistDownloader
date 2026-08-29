"""Classify ES->PT test errors for diagnosis only; it never edits datasets."""
import argparse, csv
from collections import Counter

SHARED = {"amor", "calma", "vida", "solo", "bella", "forma", "momento", "dado", "dada", "mano"}
def classify(title):
    words = title.casefold().split()
    if len(words) == 1 and words[0] in SHARED: return "linguistically_ambiguous"
    if len(words) <= 2 and any(word in SHARED for word in words): return "shared_word"
    if len(words) <= 2: return "very_short"
    if any(word in {"mujer", "encontré", "corazón", "quiero", "contigo", "digas", "para", "que", "yo", "me", "la"} for word in words): return "distinguishable_es"
    return "other"

def main():
    parser = argparse.ArgumentParser(); parser.add_argument("input"); parser.add_argument("--output", required=True); args = parser.parse_args()
    with open(args.input, encoding="utf-8", newline="") as stream: rows = list(csv.DictReader(stream))
    counts = Counter(classify(row["title"]) for row in rows)
    with open(args.output, "w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]) + ["category"] if rows else ["category"]); writer.writeheader()
        for row in rows: row["category"] = classify(row["title"]); writer.writerow(row)
    print(dict(counts)); print(f"analysed={len(rows)}")
if __name__ == "__main__": main()
