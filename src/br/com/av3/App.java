package br.com.av3;

public class App {
    public static void main(String[] args) {
        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("--gui")) {
            javax.swing.SwingUtilities.invokeLater(() -> new br.com.av3.ui.MainWindow().setVisible(true));
            return;
        }

        try {
            Benchmark benchmark = new Benchmark();
            benchmark.run(args);
        } catch (Exception e) {
            System.out.println("Erro durante a execucao:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}