package simulador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Salva e carrega conjuntos de processos em arquivos de texto, para reaproveitar exemplos.
 * <p>
 * Formato: um processo por linha, com os campos separados por ponto e vírgula:
 * <pre>
 * # PID;Chegada;Prioridade;Rajadas
 * 1;0;3;6,3,2
 * 2;1;1;3
 * </pre>
 * Um quinto campo opcional guarda o quantum próprio do processo no Round Robin ({@code 1;0;3;6,3,2;4}).
 * Vazio ou ausente, o processo usa o quantum padrão.
 * <p>
 * Linhas vazias e linhas que começam com {@code #} são ignoradas, então o arquivo pode ter comentários
 * e ser editado à mão em qualquer editor de texto.
 */
public final class ArquivoProcessos {
    private ArquivoProcessos() { }

    /** Erro de leitura com o número da linha em que aconteceu. */
    public static class FormatoInvalido extends Exception {
        public FormatoInvalido(String msg) { super(msg); }
    }

    /**
     * Grava os processos no arquivo, substituindo o conteúdo anterior.
     *
     * @param arquivo   caminho do arquivo.
     * @param processos processos a salvar (só os dados fixos: PID, chegada, prioridade e rajadas).
     */
    public static void salvar(Path arquivo, List<Processo> processos) throws IOException {
        List<String> linhas = new ArrayList<>();
        linhas.add("# Simulador de Escalonamento - conjunto de processos");
        linhas.add("# Rajadas alternam CPU e E/S: \"3,4,2\" = 3 de CPU, 4 de E/S, 2 de CPU.");
        boolean comQuantum = processos.stream().anyMatch(p -> p.quantum > 0);
        linhas.add(comQuantum ? "# PID;Chegada;Prioridade;Rajadas;Quantum (vazio = quantum padrão)"
                              : "# PID;Chegada;Prioridade;Rajadas");
        for (Processo p : processos) {
            StringBuilder raj = new StringBuilder();
            for (int i = 0; i < p.rajadas.length; i++) {
                if (i > 0) raj.append(',');
                raj.append(p.rajadas[i]);
            }
            String linha = p.pid + ";" + p.chegada + ";" + p.prioridade + ";" + raj;
            if (comQuantum) linha += ";" + (p.quantum > 0 ? String.valueOf(p.quantum) : "");
            linhas.add(linha);
        }
        Files.write(arquivo, linhas, StandardCharsets.UTF_8);
    }

    /**
     * Lê os processos de um arquivo salvo por {@link #salvar}.
     *
     * @param arquivo caminho do arquivo.
     * @return os processos, na ordem do arquivo.
     * @throws FormatoInvalido se alguma linha estiver mal formada (a mensagem indica a linha).
     */
    public static List<Processo> carregar(Path arquivo) throws IOException, FormatoInvalido {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        List<Processo> ps = new ArrayList<>();
        Set<Integer> pids = new HashSet<>();
        for (int n = 0; n < linhas.size(); n++) {
            String linha = linhas.get(n).strip();
            if (n == 0 && linha.startsWith("﻿")) linha = linha.substring(1).strip(); // BOM do Bloco de Notas
            if (linha.isEmpty() || linha.startsWith("#")) continue;

            String onde = "Linha " + (n + 1) + ": ";
            String[] campos = linha.split("\\s*[;\\t]\\s*");   // um quantum vazio no fim some aqui (= padrão)
            if (campos.length != 4 && campos.length != 5)
                throw new FormatoInvalido(onde + "esperado PID;Chegada;Prioridade;Rajadas[;Quantum], encontrado \"" + linha + "\"");
            try {
                int pid = Integer.parseInt(campos[0].replaceFirst("^[Pp]", ""));
                int chegada = Integer.parseInt(campos[1]);
                int prioridade = Integer.parseInt(campos[2]);
                String[] partes = campos[3].split("\\s*[, ]\\s*");
                int[] raj = new int[partes.length];
                for (int i = 0; i < partes.length; i++) {
                    raj[i] = Integer.parseInt(partes[i]);
                    if (raj[i] <= 0) throw new FormatoInvalido(onde + "as rajadas devem ser maiores que zero");
                }
                if (pid <= 0) throw new FormatoInvalido(onde + "o PID deve ser maior que zero");
                int quantum = campos.length == 5 && !campos[4].isEmpty() ? Integer.parseInt(campos[4]) : 0;
                if (campos.length == 5 && quantum <= 0)
                    throw new FormatoInvalido(onde + "o quantum deve ser maior que zero (ou vazio, para usar o padrão)");
                if (!pids.add(pid)) throw new FormatoInvalido(onde + "PID " + pid + " repetido");
                ps.add(new Processo(pid, chegada, prioridade, raj, quantum));
            } catch (NumberFormatException e) {
                throw new FormatoInvalido(onde + "número inválido em \"" + linha + "\"");
            } catch (IllegalArgumentException e) {
                throw new FormatoInvalido(onde + e.getMessage());
            }
        }
        if (ps.isEmpty()) throw new FormatoInvalido("O arquivo não tem nenhum processo.");
        return ps;
    }
}
