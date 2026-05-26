package br.com.av3;

public class CountResult {
    private final String fileName;
    private final String word;
    private final String algorithm;
    private final String workers;
    private final int executionNumber;
    private final long occurrences;
    private final double timeMs;
    private final int totalWords;

    public CountResult(
            String fileName,
            String word,
            String algorithm,
            String workers,
            int executionNumber,
            long occurrences,
            double timeMs,
            int totalWords) {

        this.fileName = fileName;
        this.word = word;
        this.algorithm = algorithm;
        this.workers = workers;
        this.executionNumber = executionNumber;
        this.occurrences = occurrences;
        this.timeMs = timeMs;
        this.totalWords = totalWords;
    }

    public String getFileName() {
        return fileName;
    }

    public String getWord() {
        return word;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getWorkers() {
        return workers;
    }

    public int getExecutionNumber() {
        return executionNumber;
    }

    public long getOccurrences() {
        return occurrences;
    }

    public double getTimeMs() {
        return timeMs;
    }

    public int getTotalWords() {
        return totalWords;
    }
}