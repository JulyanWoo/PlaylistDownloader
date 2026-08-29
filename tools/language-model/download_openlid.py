"""Stream a small OpenLID-v3 subset; never downloads the 118M-row corpus.

Requires: pip install datasets
Usage: python download_openlid.py --per-language 50000
"""
import argparse, csv, hashlib, random
from pathlib import Path
from datasets import load_dataset

REPO = "HPLT/OpenLID-v3"
LANGUAGES = {"spa_Latn": "es", "por_Latn": "pt", "eng_Latn": "en", "fra_Latn": "fr"}

def short_fragment(text, key):
    words = str(text or "").split()
    if not words: return ""
    if len(words) <= 10: return " ".join(words)
    digest = int(hashlib.sha256(key.encode("utf-8")).hexdigest()[:8], 16)
    width = 1 + digest % 10
    start = digest % (len(words) - width + 1)
    return " ".join(words[start:start + width])

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--per-language", type=int, default=50000)
    parser.add_argument("--output", default="datasets/openlid-four-language.csv")
    args = parser.parse_args()
    rows = []
    for source_language, language in LANGUAGES.items():
        url = f"https://huggingface.co/datasets/{REPO}/resolve/main/data/{source_language}.parquet"
        stream = load_dataset("parquet", data_files=url, split="train", streaming=True)
        count = 0
        for index, item in enumerate(stream):
            fragment = short_fragment(item.get("text", ""), f"{source_language}:{index}")
            if not fragment: continue
            rows.append((fragment, language))
            count += 1
            if count >= args.per_language: break
        if count < args.per_language: raise RuntimeError(f"Only found {count} rows for {source_language}")
        print(f"{source_language}: {count} short fragments")
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    with open(output, "w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream); writer.writerow(("title", "language")); writer.writerows(rows)
    print(f"Wrote {len(rows)} rows to {output}")

if __name__ == "__main__": main()
