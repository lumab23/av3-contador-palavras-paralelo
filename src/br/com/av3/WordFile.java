package br.com.av3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class WordFile {

    public static String[] loadWords(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        text = text.toLowerCase(Locale.ROOT);

        /*
         * Separa por qualquer coisa que nao seja letra ou numero.
         * Assim "the," vira "the" e "The" vira "the".
         */
        String[] words = text.split("[^\\p{L}\\p{N}]+");

        int validCount = 0;
        for (String word : words) {
            if (!word.isBlank()) {
                validCount++;
            }
        }

        String[] cleanWords = new String[validCount];

        int index = 0;
        for (String word : words) {
            if (!word.isBlank()) {
                cleanWords[index] = word;
                index++;
            }
        }

        return cleanWords;
    }

    public static String normalizeWord(String word) {
        String normalized = word.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}]+", "");
        return normalized;
    }
}