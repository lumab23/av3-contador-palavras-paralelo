package br.com.av3;

public class App {
    public static void main(String[] args) {
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