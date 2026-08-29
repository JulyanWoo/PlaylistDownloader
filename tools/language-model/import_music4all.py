"""Import Music4All title/language metadata into the domain training set.

Only metadata is used: no audio and no lyrics are downloaded.  The output is
deterministic so a future dataset refresh can be reproduced exactly.
"""
import argparse
import csv
import hashlib
import re
from collections import Counter, defaultdict
from pathlib import Path

LANGUAGES = ("es", "pt", "en", "fr")
DEFAULT_TARGETS = {"es": 8000, "pt": 8000, "en": 5000, "fr": 5000}

ANNOTATION = re.compile(
    r"\s*[\[(](?=[^\])]*(?:official|video|audio|lyrics?|remaster(?:ed)?|radio\s*edit|"
    r"extended\s*mix|original\s*mix|remix|version|versi[oó]n|hd|4k|karaoke|instrumental))"
    r"[^\])]*[\])]\s*",
    re.IGNORECASE,
)
FEATURE = re.compile(r"\s+\b(?:feat(?:uring)?|ft)\.?\s+.+$", re.IGNORECASE)
NOISE = re.compile(
    r"\b(?:official\s*(?:music\s*)?(?:video|audio)|lyric\s*video|lyrics?|"
    r"remaster(?:ed)?|radio\s*edit|extended\s*mix|original\s*mix|hd|4k)\b",
    re.IGNORECASE,
)


def clean_title(value):
    value = (value or "").strip()
    value = ANNOTATION.sub(" ", value)
    value = FEATURE.sub(" ", value)
    value = NOISE.sub(" ", value)
    value = value.replace("_", " ")
    return re.sub(r"\s+", " ", value).strip()


def language_code(value):
    value = (value or "").strip().lower().replace("_", "-")
    return value.split("-", 1)[0] if value else ""


def stable_key(title, language):
    return hashlib.sha256(f"{language}\0{title.casefold()}".encode("utf-8")).hexdigest()


def read_rows(information_path, languages_path):
    languages = {}
    with languages_path.open(encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            code = language_code(row.get("lang"))
            if code in LANGUAGES:
                languages[row.get("id", "").strip()] = code

    seen = set()
    candidates = defaultdict(list)
    with information_path.open(encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            language = languages.get(row.get("id", "").strip())
            title = clean_title(row.get("song"))
            if not language or not title or len(title.split()) > 10:
                continue
            key = (title.casefold(), language)
            if key in seen:
                continue
            seen.add(key)
            candidates[language].append(title)
    return candidates


def choose_titles(titles, target):
    """Prefer short titles while retaining a deterministic length mix."""
    buckets = defaultdict(list)
    for title in titles:
        buckets[min(len(title.split()), 10)].append(title)
    for bucket in buckets.values():
        bucket.sort(key=lambda title: stable_key(title, ""))

    desired = {1: .20, 2: .25, 3: .25, 4: .15, 5: .15}
    selected = []
    for length, ratio in desired.items():
        selected.extend(buckets.get(length, [])[:round(target * ratio)])
    if len(selected) < target:
        remaining = [title for length in sorted(buckets) if length > 5 for title in buckets[length]]
        selected.extend(remaining[:target - len(selected)])
    if len(selected) < target:
        all_remaining = [title for title in titles if title not in set(selected)]
        selected.extend(all_remaining[:target - len(selected)])
    return sorted(selected[:target], key=lambda title: stable_key(title, ""))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--information", default="data/music4all-raw/music4all/id_information.csv")
    parser.add_argument("--languages", default="data/music4all-raw/music4all/id_lang.csv")
    parser.add_argument("--output", default="data/music-real.csv")
    parser.add_argument("--per-language", type=int, default=0,
                        help="Use the same target count for every language")
    args = parser.parse_args()
    targets = {language: args.per_language for language in LANGUAGES} if args.per_language else DEFAULT_TARGETS
    candidates = read_rows(Path(args.information), Path(args.languages))
    selected = {language: choose_titles(candidates[language], targets[language]) for language in LANGUAGES}

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(("title", "language", "source"))
        for language in LANGUAGES:
            for title in selected[language]:
                writer.writerow((title, language, "Music4All"))

    print(f"Wrote {sum(map(len, selected.values()))} rows to {output}")
    for language in LANGUAGES:
        print(language, len(selected[language]), dict(sorted(Counter(len(t.split()) for t in selected[language]).items())))
        print(f"available_{language}={len(candidates[language])}")


if __name__ == "__main__":
    main()
