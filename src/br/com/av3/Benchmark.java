package br.com.av3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Benchmark {

    public void run(String[] args) throws Exception {
        Options options = readOptions(args);

        List<Path> sampleFiles = findSampleFiles(options.samplesDir);

        if (sampleFiles.isEmpty()) {
            System.out.println("Nenhum arquivo .txt foi encontrado na pasta samples.");
            return;
        }

        System.out.println("Palavra buscada: " + options.word);
        System.out.println("Execucoes por teste: " + options.runs);
        System.out.println("Threads testadas na CPU: " + Arrays.toString(options.threads));
        System.out.println("GPU ignorada: " + options.skipGpu);
        System.out.println();

        List<CountResult> results = new ArrayList<>();

        SerialCPU serialCPU = new SerialCPU();
        ParallelCPU parallelCPU = new ParallelCPU();
        ParallelGPU parallelGPU = new ParallelGPU();

        for (Path file : sampleFiles) {
            System.out.println("Arquivo: " + file.getFileName());

            String[] words = WordFile.loadWords(file);

            System.out.println("Total de palavras lidas: " + words.length);

            for (int execution = 1; execution <= options.runs; execution++) {
                runSerialTest(
                        results,
                        serialCPU,
                        file,
                        words,
                        options.word,
                        execution
                );

                for (int threadCount : options.threads) {
                    runParallelCpuTest(
                            results,
                            parallelCPU,
                            file,
                            words,
                            options.word,
                            execution,
                            threadCount
                    );
                }

                if (!options.skipGpu) {
                    runParallelGpuTest(
                            results,
                            parallelGPU,
                            file,
                            words,
                            options.word,
                            execution
                    );
                }
            }

            System.out.println();
        }

        CsvWriter csvWriter = new CsvWriter();
        Path csvFile = options.resultsDir.resolve("results.csv");
        csvWriter.write(csvFile, results);

        SimpleCharts charts = new SimpleCharts();
        charts.generateAll(options.resultsDir, results);

        System.out.println("CSV gerado em: " + csvFile);
        System.out.println("Graficos gerados na pasta: " + options.resultsDir);
    }

    private void runSerialTest(
            List<CountResult> results,
            SerialCPU serialCPU,
            Path file,
            String[] words,
            String word,
            int execution) {

        long start = System.nanoTime();

        long occurrences = serialCPU.count(words, word);

        double timeMs = elapsedMs(start);

        CountResult result = new CountResult(
                file.getFileName().toString(),
                word,
                "SerialCPU",
                "1",
                execution,
                occurrences,
                timeMs,
                words.length
        );

        results.add(result);

        printResult(result);
    }

    private void runParallelCpuTest(
            List<CountResult> results,
            ParallelCPU parallelCPU,
            Path file,
            String[] words,
            String word,
            int execution,
            int threadCount) throws Exception {

        long start = System.nanoTime();

        long occurrences = parallelCPU.count(words, word, threadCount);

        double timeMs = elapsedMs(start);

        CountResult result = new CountResult(
                file.getFileName().toString(),
                word,
                "ParallelCPU",
                String.valueOf(threadCount),
                execution,
                occurrences,
                timeMs,
                words.length
        );

        results.add(result);

        printResult(result);
    }

    private void runParallelGpuTest(
            List<CountResult> results,
            ParallelGPU parallelGPU,
            Path file,
            String[] words,
            String word,
            int execution) {

        try {
            long start = System.nanoTime();

            long occurrences = parallelGPU.count(words, word);

            double timeMs = elapsedMs(start);

            CountResult result = new CountResult(
                    file.getFileName().toString(),
                    word,
                    "ParallelGPU",
                    "GPU",
                    execution,
                    occurrences,
                    timeMs,
                    words.length
            );

            results.add(result);

            printResult(result);
        } catch (Exception e) {
            System.out.println("ParallelGPU: erro ao executar GPU.");
            System.out.println("Motivo: " + e.getMessage());
        }
    }

    private void printResult(CountResult result) {
        System.out.printf(
                "%s [%s]: %d ocorrencias em %.3f ms%n",
                result.getAlgorithm(),
                result.getWorkers(),
                result.getOccurrences(),
                result.getTimeMs()
        );
    }

    private double elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    private List<Path> findSampleFiles(Path samplesDir) throws Exception {
        List<Path> files = new ArrayList<>();

        if (!Files.exists(samplesDir)) {
            return files;
        }

        try (var stream = Files.list(samplesDir)) {
            stream
                    .filter(path -> path.toString().toLowerCase().endsWith(".txt"))
                    .sorted()
                    .forEach(files::add);
        }

        return files;
    }

    private Options readOptions(String[] args) {
        Options options = new Options();

        options.word = "the";
        options.runs = 3;
        options.threads = new int[] { 1, 2, 4, 8 };
        options.samplesDir = Paths.get("samples");
        options.resultsDir = Paths.get("results");
        options.skipGpu = true;

        for (int i = 0; i < args.length; i++) {
            String current = args[i];

            if (current.equals("--word") && i + 1 < args.length) {
                options.word = args[++i];
            } else if (current.equals("--runs") && i + 1 < args.length) {
                options.runs = Integer.parseInt(args[++i]);
            } else if (current.equals("--threads") && i + 1 < args.length) {
                options.threads = parseThreads(args[++i]);
            } else if (current.equals("--samples") && i + 1 < args.length) {
                options.samplesDir = Paths.get(args[++i]);
            } else if (current.equals("--results") && i + 1 < args.length) {
                options.resultsDir = Paths.get(args[++i]);
            } else if (current.equals("--skip-gpu")) {
                options.skipGpu = true;
            } else if (current.equals("--use-gpu")) {
                options.skipGpu = false;
            }
        }

        return options;
    }

    private int[] parseThreads(String text) {
        String[] parts = text.split(",");
        int[] threads = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            threads[i] = Integer.parseInt(parts[i].trim());
        }

        return threads;
    }

    private static class Options {
        private String word;
        private int runs;
        private int[] threads;
        private Path samplesDir;
        private Path resultsDir;
        private boolean skipGpu;
    }
}