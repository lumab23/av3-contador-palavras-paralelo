package br.com.av3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelCPU {

    public long count(Path file, String word, int threadCount) throws Exception {
        String[] words = WordFile.loadWords(file);
        return count(words, word, threadCount);
    }

    public long count(String[] words, String word, int threadCount) throws Exception {
        String target = WordFile.normalizeWord(word);

        int threads = threadCount;

        if (threads < 1) {
            threads = 1;
        }

        if (threads > words.length) {
            threads = words.length;
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Long>> results = new ArrayList<>();

        int partSize = (int) Math.ceil(words.length / (double) threads);

        for (int i = 0; i < threads; i++) {
            int start = i * partSize;
            int end = Math.min(start + partSize, words.length);

            Callable<Long> task = () -> countPart(words, target, start, end);
            results.add(pool.submit(task));
        }

        long total = 0;

        for (Future<Long> result : results) {
            total += result.get();
        }

        pool.shutdown();

        return total;
    }

    private long countPart(String[] words, String target, int start, int end) {
        long total = 0;

        for (int i = start; i < end; i++) {
            if (words[i].equals(target)) {
                total++;
            }
        }

        return total;
    }
}