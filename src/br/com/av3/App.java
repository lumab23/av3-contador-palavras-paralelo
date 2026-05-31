package br.com.av3;

import br.com.av3.ui.MainWindow;

public class App {
    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equalsIgnoreCase("--gui")) {
                MainWindow.open();
                return;
            }

            Benchmark benchmark = new Benchmark();
            benchmark.run(args);

        } catch (Exception e) {
            System.out.println("Erro durante a execucao:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}