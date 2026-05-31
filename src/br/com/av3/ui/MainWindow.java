package br.com.av3.ui;

import br.com.av3.Benchmark;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {

    private final JTextField wordField = new JTextField("the", 20);
    private final JTextField runsField = new JTextField("3", 10);
    private final JTextField threadsField = new JTextField("1,2,4,8", 15);
    private final JTextField samplesDirField = new JTextField("samples", 18);
    private final JTextField resultsDirField = new JTextField("results", 18);

    private final JCheckBox useGpuCheckBox = new JCheckBox("Usar GPU", true);
    private final JButton executeButton = new JButton("Executar testes");
    private final JButton clearButton = new JButton("Limpar saída");

    private final JTextArea logArea = new JTextArea(25, 90);
    private final JLabel averageTimeChartLabel = new JLabel();
    private final JLabel cpuThreadsChartLabel = new JLabel();

    public MainWindow() {
        super("Contador de palavras (AV3 - Paralelo/GPU)");
        initUi();
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel inputsPanel = new JPanel(new GridBagLayout());
        inputsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        inputsPanel.add(new JLabel("Palavra:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        inputsPanel.add(wordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        inputsPanel.add(new JLabel("Execuções:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        inputsPanel.add(runsField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        inputsPanel.add(new JLabel("Threads:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        inputsPanel.add(threadsField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        inputsPanel.add(new JLabel("Pasta Amostras:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        inputsPanel.add(samplesDirField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        inputsPanel.add(new JLabel("Pasta Resultados:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        inputsPanel.add(resultsDirField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        inputsPanel.add(useGpuCheckBox, gbc);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(executeButton);
        buttonsPanel.add(clearButton);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        inputsPanel.add(buttonsPanel, gbc);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        averageTimeChartLabel.setHorizontalAlignment(SwingConstants.CENTER);
        averageTimeChartLabel.setVerticalAlignment(SwingConstants.TOP);
        cpuThreadsChartLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cpuThreadsChartLabel.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane logScrollPane = new JScrollPane(logArea);
        JScrollPane averageTimeScrollPane = new JScrollPane(averageTimeChartLabel);
        JScrollPane cpuThreadsScrollPane = new JScrollPane(cpuThreadsChartLabel);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Saída", logScrollPane);
        tabbedPane.addTab("Tempo médio", averageTimeScrollPane);
        tabbedPane.addTab("Threads CPU", cpuThreadsScrollPane);

        add(inputsPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        clearButton.addActionListener(e -> logArea.setText(""));

        executeButton.addActionListener(e -> {
            executeButton.setEnabled(false);
            appendLog("\n=== Iniciando execução ===\n");

            String word = wordField.getText().trim();
            String runsText = runsField.getText().trim();
            String threadsText = threadsField.getText().trim();
            String samplesDir = samplesDirField.getText().trim();
            String resultsDir = resultsDirField.getText().trim();
            boolean useGpu = useGpuCheckBox.isSelected();

            int runs;
            try {
                runs = Integer.parseInt(runsText);
            } catch (NumberFormatException ex) {
                appendLog("Erro: 'Execuções' deve ser um número inteiro.\n" + ex.getMessage() + "\n");
                executeButton.setEnabled(true);
                return;
            }

            if (word.isEmpty()) {
                appendLog("Erro: Palavra não pode ser vazia.\n");
                executeButton.setEnabled(true);
                return;
            }
            if (threadsText.isEmpty()) {
                appendLog("Erro: Threads não pode ser vazia.\n");
                executeButton.setEnabled(true);
                return;
            }

            List<String> argList = new ArrayList<>();
            argList.add("--word");
            argList.add(word);
            argList.add("--runs");
            argList.add(String.valueOf(runs));
            argList.add("--threads");
            argList.add(threadsText);
            argList.add("--samples");
            argList.add(samplesDir);
            argList.add("--results");
            argList.add(resultsDir);
            argList.add(useGpu ? "--use-gpu" : "--skip-gpu");

            String[] benchArgs = argList.toArray(new String[0]);
            Path csvPath = Paths.get(resultsDir).resolve("results.csv");

            new SwingWorker<RunOutcome, Void>() {
                @Override
                protected RunOutcome doInBackground() {
                    PrintStream originalOut = System.out;
                    PrintStream originalErr = System.err;
                    TextAreaOutputStream textAreaStream = new TextAreaOutputStream();
                    try (PrintStream redirected = new PrintStream(textAreaStream, true, "UTF-8")) {
                        System.setOut(redirected);
                        System.setErr(redirected);

                        Benchmark benchmark = new Benchmark();
                        benchmark.run(benchArgs);

                        boolean csvExists = Files.exists(csvPath);
                        return new RunOutcome(true, null, csvPath, csvExists);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return new RunOutcome(false, ex, csvPath, Files.exists(csvPath));
                    } finally {
                        System.setOut(originalOut);
                        System.setErr(originalErr);
                    }
                }

                @Override
                protected void done() {
                    executeButton.setEnabled(true);

                    RunOutcome outcome;
                    try {
                        outcome = get();
                    } catch (Exception e) {
                        appendLog("\nErro inesperado ao finalizar execução: " + e.getMessage() + "\n");
                        return;
                    }

                    appendLog("\n=== Resumo dos parâmetros ===\n");
                    appendLog("Palavra: " + word + "\n");
                    appendLog("Execuções: " + runs + "\n");
                    appendLog("Threads: " + threadsText + "\n");
                    appendLog("Pasta Amostras: " + samplesDir + "\n");
                    appendLog("Pasta Resultados: " + resultsDir + "\n");
                    appendLog(useGpu ? "Usar GPU: SIM\n" : "Usar GPU: NÃO\n");
                    appendLog("CSV gerado em: " + outcome.csvPath + "\n");

                    if (outcome.success) {
                        appendLog("Status: sucesso.\n");
                        if (outcome.csvExists) {
                            appendLog("Arquivo CSV encontrado.\n");
                        } else {
                            appendLog("Aviso: CSV não encontrado após execução (pode ter falhado antes de gerar o CSV).\n");
                        }
                        loadCharts(resultsDir);
                    } else {
                        appendLog("Status: erro.\n");
                        if (outcome.error != null && outcome.error.getMessage() != null) {
                            appendLog("Mensagem: " + outcome.error.getMessage() + "\n");
                        }
                    }
                    appendLog("=== Fim ===\n");
                }
            }.execute();
        });

        setSize(1100, 700);
        setLocationRelativeTo(null);
    }

    private void loadCharts(String resultsDir) {
        Path averageChart = Paths.get(resultsDir).resolve("tempo-medio.png");
        Path threadsChart = Paths.get(resultsDir).resolve("threads-cpu.png");
        setChartImage(averageTimeChartLabel, averageChart);
        setChartImage(cpuThreadsChartLabel, threadsChart);
    }

    private void setChartImage(JLabel label, Path imagePath) {
        if (Files.exists(imagePath)) {
            ImageIcon icon = new ImageIcon(imagePath.toString());
            label.setIcon(icon);
            label.setText("");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.TOP);
        } else {
            label.setIcon(null);
            label.setText("Gráfico não encontrado: " + imagePath);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.TOP);
        }
    }

    private void appendLog(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(text);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append(text);
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    private class TextAreaOutputStream extends OutputStream {
        @Override
        public void write(int b) {
            appendLog(String.valueOf((char) b));
        }

        @Override
        public void write(byte[] b, int off, int len) {
            if (b == null || len <= 0) return;
            String s = new String(b, off, len, StandardCharsets.UTF_8);
            appendLog(s);
        }

        @Override
        public void flush() throws IOException {
            // sem necessidade
        }
    }

    private static class RunOutcome {
        private final boolean success;
        private final Exception error;
        private final Path csvPath;
        private final boolean csvExists;

        private RunOutcome(boolean success, Exception error, Path csvPath, boolean csvExists) {
            this.success = success;
            this.error = error;
            this.csvPath = csvPath;
            this.csvExists = csvExists;
        }
    }
}
