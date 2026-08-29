# Playlist Language Model

This is a development-only pipeline. It uses deterministic character n-grams (2–5), the same Java-compatible hash (`seed=42`, `31 * hash + UTF-16 code unit`) and a small softmax logistic regression implemented with NumPy.

```text
python download_openlid.py --per-language 50000
python train.py --data datasets/openlid-four-language.csv --output build-openlid
python export_model.py --input build-openlid/language-model.json
```

To mine and train a domain candidate without reading the golden set:

```text
python mine_hard_examples.py --model build-domain-diagnostic/language-model.json --input datasets/openlid-four-language.csv --input datasets/music_titles.csv --output data/hard-es-pt.csv
python train.py --data datasets/openlid-four-language.csv --domain-data datasets/music_titles.csv --hard-data data/hard-es-pt.csv --output build-hard
```

The downloader uses the official OpenLID-v3 language-specific Parquet shards in streaming mode and stores only 50,000 short fragments per target language. It does not download the complete 118M-row corpus. The OpenLID subset is training material; `src/test/resources/language/golden-dataset.csv` remains a separate domain/adversarial test.

`train.py` writes `build/metrics.json` with accuracy by title length, split sizes, confusion matrices, explicit ES→PT/PT→ES false-positive rates, and a separate golden evaluation. The golden dataset is never used for training. The exporter writes the compact binary consumed by Java. The runtime model is replaced only when validation/test accuracy is at least 95%, short-title accuracy is at least 90%, ES→PT is at most 2%, and PT→ES is at most 3%.
