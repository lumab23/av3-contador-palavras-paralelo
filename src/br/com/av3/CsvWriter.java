package br.com.av3;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CsvWriter {

    public void write(Path outputFile, List<CountResult> results) throws Exception {
        Files.createDirectories(outputFile.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("arquivo,palavra,algoritmo,processadores,execucao,ocorrencias,tempo_ms,total_palavras");
            writer.newLine();

            for (CountResult result : results) {
                writer.write(format(result));
                writer.newLine();
            }
        }
    }

    private String format(CountResult result) {
        return csv(result.getFileName()) + "," +
                csv(result.getWord()) + "," +
                csv(result.getAlgorithm()) + "," +
                csv(result.getWorkers()) + "," +
                result.getExecutionNumber() + "," +
                result.getOccurrences() + "," +
                String.format(Locale.US, "%.3f", result.getTimeMs()) + "," +
                result.getTotalWords();
    }

    private String csv(String value) {
        String fixed = value.replace("\"", "\"\"");
        return "\"" + fixed + "\"";
    }
}