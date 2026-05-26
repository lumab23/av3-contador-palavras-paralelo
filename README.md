# Análise comparativa de contagem de palavras

Projeto Java que compara três formas de contar ocorrências de uma palavra em arquivos de texto:

- **CPU serial** — loop simples em um único thread
- **CPU paralela** — `ExecutorService` com pool de threads
- **GPU** — kernel OpenCL via [JOCL](https://github.com/gpu/JOCL) 2.0.4

O programa lê arquivos `.txt` da pasta `samples/`, conta a palavra informada, mede o tempo de cada abordagem e grava os resultados em `results/results.csv`. Também gera dois gráficos PNG com Java puro.

## Estrutura

```
.
├── src/br/com/av3/
│   ├── App.java           # Classe principal
│   ├── Benchmark.java     # Executa os testes e medições
│   ├── WordFile.java      # Leitura e normalização dos textos
│   ├── SerialCPU.java     # Contagem serial
│   ├── ParallelCPU.java   # Contagem paralela na CPU
│   ├── ParallelGPU.java   # Contagem paralela na GPU (OpenCL)
│   ├── CountResult.java   # Representa uma linha de resultado
│   ├── CsvWriter.java     # Gera o CSV
│   └── SimpleCharts.java  # Gera gráficos PNG
├── samples/               # Arquivos .txt de entrada
├── lib/                   # jocl-2.0.4.jar (manual, ver abaixo)
├── results/               # CSV e PNG gerados na execução
├── out/                   # Classes compiladas (não versionado)
└── sources.txt            # Lista temporária de fontes (não versionado)
```

### Amostras em `samples/`

| Arquivo | Descrição |
|---------|-----------|
| `DonQuixote-388208.txt` | *Don Quixote* (espanhol) |
| `Dracula-165307.txt` | *Dracula* (inglês) |
| `MobyDick-217452.txt` | *Moby Dick* (inglês) |

## Requisitos

- **JDK** instalado (17 ou superior recomendado)
- **JOCL 2.0.4** — necessário para compilar e executar a versão GPU
- **OpenCL** no sistema — necessário apenas para rodar com `--use-gpu`
- No **macOS**, versões recentes do Java podem exigir `--enable-native-access=ALL-UNNAMED` ao usar a GPU

## Dependência JOCL

O arquivo `jocl-2.0.4.jar` **não está no repositório**. Baixe e coloque em:

```
lib/jocl-2.0.4.jar
```

Download direto: https://repo1.maven.org/maven2/org/jocl/jocl/2.0.4/jocl-2.0.4.jar

## Como usar

Clone o repositório, entre na pasta raiz, adicione o JAR em `lib/` e use os comandos abaixo.

### macOS / Linux

**Compilar:**

```bash
rm -rf out && mkdir -p out && find src -name "*.java" > sources.txt && javac -encoding UTF-8 -cp "lib/jocl-2.0.4.jar" -d out @sources.txt
```

**Executar sem GPU:**

```bash
java -cp "out:lib/jocl-2.0.4.jar" br.com.av3.App --word the --runs 3 --threads 1,2,4,8 --skip-gpu
```

**Executar com GPU:**

```bash
java --enable-native-access=ALL-UNNAMED -cp "out:lib/jocl-2.0.4.jar" br.com.av3.App --word the --runs 3 --threads 1,2,4,8 --use-gpu
```

### Windows

**Compilar:**

```cmd
rmdir /s /q out 2>nul & mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\jocl-2.0.4.jar" -d out @sources.txt
```

**Executar sem GPU:**

```cmd
java -cp "out;lib\jocl-2.0.4.jar" br.com.av3.App --word the --runs 3 --threads 1,2,4,8 --skip-gpu
```

**Executar com GPU:**

```cmd
java --enable-native-access=ALL-UNNAMED -cp "out;lib\jocl-2.0.4.jar" br.com.av3.App --word the --runs 3 --threads 1,2,4,8 --use-gpu
```

## Argumentos

| Argumento | Descrição |
|-----------|-----------|
| `--word <texto>` | Palavra a buscar (padrão: `the`) |
| `--runs <n>` | Número de execuções por teste (padrão: `3`) |
| `--threads <lista>` | Threads do `ParallelCPU`, separadas por vírgula (padrão: `1,2,4,8`) |
| `--skip-gpu` | Não executa a versão GPU (padrão se `--use-gpu` não for passado) |
| `--use-gpu` | Inclui a versão GPU nos testes |
| `--samples <pasta>` | Pasta dos arquivos `.txt` (padrão: `samples`) |
| `--results <pasta>` | Pasta de saída (padrão: `results`) |

## Saídas

Após a execução, a pasta `results/` contém:

| Arquivo | Descrição |
|---------|-----------|
| `results.csv` | Uma linha por execução: arquivo, palavra, algoritmo, threads/GPU, repetição, ocorrências, tempo (ms) e total de palavras |
| `tempo-medio.png` | Gráfico de barras com tempo médio por arquivo e algoritmo |
| `threads-cpu.png` | Gráfico de barras com tempo médio do `ParallelCPU` por número de threads |

Colunas do CSV: `arquivo`, `palavra`, `algoritmo`, `processadores`, `execucao`, `ocorrencias`, `tempo_ms`, `total_palavras`.

## Exemplo de execução

```
Palavra buscada: the
Execucoes por teste: 3
Threads testadas na CPU: [1, 2, 4, 8]
GPU ignorada: true

Arquivo: Dracula-165307.txt
Total de palavras lidas: 166773
SerialCPU [1]: 8104 ocorrencias em 1.242 ms
ParallelCPU [1]: 8104 ocorrencias em 1.346 ms
ParallelCPU [2]: 8104 ocorrencias em 0.854 ms
ParallelCPU [4]: 8104 ocorrencias em 0.804 ms
ParallelCPU [8]: 8104 ocorrencias em 1.154 ms
...

CSV gerado em: results/results.csv
Graficos gerados na pasta: results
```

## Observações

- A **primeira execução da GPU** costuma ser mais lenta por causa da inicialização do OpenCL (contexto, compilação do kernel, buffers).
- **Don Quixote** está em espanhol; a palavra `the` aparece poucas vezes nesse arquivo em comparação com os textos em inglês.
- Os **tempos variam** conforme hardware, sistema operacional e carga da máquina.
- Os arquivos em `results/` são **sobrescritos** a cada nova execução.

## Licença

Projeto acadêmico. Textos em `samples/` são obras de domínio público (Project Gutenberg).
