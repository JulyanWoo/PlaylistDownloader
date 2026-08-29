package com.example.interfaz.service.analyzer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static java.util.Objects.hash;

/** Small resident character n-gram linear model. The model is data-only and ships in the JAR. */
final class MiniLanguageModel {
    static final String[] LANGUAGES = {"es", "en", "pt", "fr"};
    private static final int MIN_N = 2;
    private static final int MAX_N = 5;
    private static final int FEATURE_COUNT = 65536;
    private static final int SEED = 42;
    private final Map<Integer, double[]> weights = new HashMap<>();
    private final double[] biases = new double[LANGUAGES.length];

    static MiniLanguageModel load() {
        MiniLanguageModel model = new MiniLanguageModel();
        try (InputStream input = MiniLanguageModel.class.getResourceAsStream("/models/language-chargram-v1.bin")) {
            if (input != null) model.readBinary(input);
        } catch (IOException ignored) {
            // Keep the development TSV as a backwards-compatible migration fallback.
            try (InputStream input = MiniLanguageModel.class.getResourceAsStream("/models/language-chargram-v1.tsv")) {
                if (input != null) model.read(input);
            } catch (IOException ignoredTsv) {
                // Lingua remains the final fallback.
            }
        }
        return model;
    }

    boolean isLoaded() { return !weights.isEmpty(); }

    double[] predict(String text) {
        double[] scores = biases.clone();
        if (text == null || text.isBlank()) return scores;
        String value = " " + text.toLowerCase(Locale.ROOT) + " ";
        int featureTotal = 0;
        for (int i = 0; i < value.length(); i++) {
            for (int n = MIN_N; n <= MAX_N && i + n <= value.length(); n++) {
                featureTotal++;
                int index = featureIndex(value.substring(i, i + n));
                double[] row = weights.get(index);
                if (row != null) for (int language = 0; language < scores.length; language++) scores[language] += row[language];
            }
        }
        if (featureTotal > 0) for (int language = 0; language < scores.length; language++) scores[language] /= featureTotal;
        double max = scores[0];
        for (int i = 1; i < scores.length; i++) max = Math.max(max, scores[i]);
        double sum = 0;
        for (int i = 0; i < scores.length; i++) sum += Math.exp(scores[i] - max);
        for (int i = 0; i < scores.length; i++) scores[i] = Math.exp(scores[i] - max) / sum;
        return scores;
    }

    private void read(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split("\\t");
                if (fields.length != 5) continue;
                int index = hash(fields[0], 0, fields[0].length());
                double[] row = weights.computeIfAbsent(index, ignored -> new double[LANGUAGES.length]);
                for (int i = 0; i < LANGUAGES.length; i++) row[i] += Double.parseDouble(fields[i + 1]);
            }
        }
    }

    private void readBinary(InputStream input) throws IOException {
        try (DataInputStream data = new DataInputStream(new BufferedInputStream(input))) {
            byte[] magic = data.readNBytes(8);
            if (!"PLDMODEL".equals(new String(magic, StandardCharsets.US_ASCII))) throw new IOException("Invalid model header");
            data.readInt(); // version
            data.readInt(); // feature count
            data.readInt(); // minimum n-gram
            data.readInt(); // maximum n-gram
            data.readInt(); // hash seed
            int languageCount = data.readInt();
            if (languageCount != LANGUAGES.length) throw new IOException("Unsupported language count");
            for (int i = 0; i < languageCount; i++) {
                while (data.readUnsignedByte() != 0) { /* read language id */ }
            }
            for (int i = 0; i < languageCount; i++) biases[i] = data.readFloat();
            int rows = data.readInt();
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                int index = data.readInt();
                double[] row = new double[LANGUAGES.length];
                for (int i = 0; i < languageCount; i++) row[i] = data.readFloat();
                weights.put(index, row);
            }
        }
    }

    static int featureIndex(String ngram) {
        int hash = SEED;
        for (int i = 0; i < ngram.length(); i++) hash = 31 * hash + ngram.charAt(i);
        return (hash & 0x7fffffff) % FEATURE_COUNT;
    }
}
