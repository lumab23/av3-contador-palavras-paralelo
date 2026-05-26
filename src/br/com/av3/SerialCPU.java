package br.com.av3;

import java.nio.file.Path;

public class SerialCPU {

    public long count(Path file, String word) throws Exception {
        String[] words = WordFile.loadWords(file);
        return count(words, word);
    }

    public long count(String[] words, String word) {
        String target = WordFile.normalizeWord(word);
        long total = 0;

        for (String currentWord : words) {
            if (currentWord.equals(target)) {
                total++;
            }
        }

        return total;
    }
}