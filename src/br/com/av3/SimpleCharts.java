package br.com.av3;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

public class SimpleCharts {

    public void generateAll(Path resultsDir, List<CountResult> results) throws Exception {
        Files.createDirectories(resultsDir);

        generateAverageTimeChart(
                resultsDir.resolve("tempo-medio.png"),
                results
        );

        generateCpuThreadsChart(
                resultsDir.resolve("threads-cpu.png"),
                results
        );
    }

    private void generateAverageTimeChart(Path outputFile, List<CountResult> results) throws Exception {
        Map<String, List<Double>> groups = new LinkedHashMap<>();

        for (CountResult result : results) {
            String label = shortName(result.getFileName()) + " " + result.getAlgorithm();

            if (result.getAlgorithm().equals("ParallelCPU")) {
                label += " " + result.getWorkers() + "t";
            }

            groups.putIfAbsent(label, new ArrayList<>());
            groups.get(label).add(result.getTimeMs());
        }

        List<Bar> bars = new ArrayList<>();

        for (String label : groups.keySet()) {
            bars.add(new Bar(label, average(groups.get(label))));
        }

        drawBarChart(
                outputFile,
                "Tempo medio por arquivo e algoritmo",
                "Tempo medio em ms",
                bars
        );
    }

    private void generateCpuThreadsChart(Path outputFile, List<CountResult> results) throws Exception {
        Map<String, List<Double>> groups = new LinkedHashMap<>();

        for (CountResult result : results) {
            if (!result.getAlgorithm().equals("ParallelCPU")) {
                continue;
            }

            String label = result.getWorkers() + " thread(s)";

            groups.putIfAbsent(label, new ArrayList<>());
            groups.get(label).add(result.getTimeMs());
        }

        List<Bar> bars = new ArrayList<>();

        for (String label : groups.keySet()) {
            bars.add(new Bar(label, average(groups.get(label))));
        }

        drawBarChart(
                outputFile,
                "Impacto do numero de threads na CPU",
                "Tempo medio em ms",
                bars
        );
    }

    private void drawBarChart(Path outputFile, String title, String yLabel, List<Bar> bars) throws Exception {
        int width = 1200;
        int height = 700;

        int left = 80;
        int right = 40;
        int top = 70;
        int bottom = 180;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString(title, left, 40);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString(yLabel, left, 62);

        double max = 1;

        for (Bar bar : bars) {
            if (bar.value > max) {
                max = bar.value;
            }
        }

        int chartWidth = width - left - right;
        int chartHeight = height - top - bottom;

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawLine(left, top, left, top + chartHeight);
        g.drawLine(left, top + chartHeight, left + chartWidth, top + chartHeight);

        int barCount = bars.size();

        if (barCount == 0) {
            g.drawString("Sem dados para gerar grafico.", left + 30, top + 50);
            g.dispose();
            ImageIO.write(image, "png", outputFile.toFile());
            return;
        }

        int space = chartWidth / barCount;
        int barWidth = Math.max(20, space / 2);

        g.setFont(new Font("Arial", Font.PLAIN, 12));

        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);

            int x = left + i * space + (space - barWidth) / 2;
            int barHeight = (int) ((bar.value / max) * (chartHeight - 20));
            int y = top + chartHeight - barHeight;

            g.setColor(new Color(80, 120, 200));
            g.fillRect(x, y, barWidth, barHeight);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, barWidth, barHeight);

            String value = String.format("%.2f", bar.value);
            g.drawString(value, x, y - 5);

            drawRotatedText(g, bar.label, x + 5, top + chartHeight + 15);
        }

        g.dispose();

        ImageIO.write(image, "png", outputFile.toFile());
    }

    private void drawRotatedText(Graphics2D g, String text, int x, int y) {
        Graphics2D copy = (Graphics2D) g.create();

        copy.rotate(Math.toRadians(60), x, y);
        copy.drawString(text, x, y);

        copy.dispose();
    }

    private double average(List<Double> values) {
        double total = 0;

        for (double value : values) {
            total += value;
        }

        return total / values.size();
    }

    private String shortName(String fileName) {
        if (fileName.toLowerCase().contains("dracula")) {
            return "Dracula";
        }

        if (fileName.toLowerCase().contains("moby")) {
            return "MobyDick";
        }

        if (fileName.toLowerCase().contains("donquixote")) {
            return "DonQuixote";
        }

        return fileName;
    }

    private static class Bar {
        private final String label;
        private final double value;

        private Bar(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }
}