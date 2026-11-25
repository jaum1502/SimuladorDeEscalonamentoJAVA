package simulador.algoritmos;

import simulador.*;
import java.util.List;

/**
 * Escalonador Round Robin: cada processo pode usar a CPU por, no máximo, um número fixo
 * de instantes seguidos, chamado de quantum. Quando esse limite é atingido e há outros
 * processos esperando, o processo atual é retirado da CPU e volta para o final da fila
 * de prontos, dando a vez ao primeiro da fila.
 *
 * Cada processo pode ter um quantum próprio ({@link Processo#quantum}). Quem não tem usa o
 * quantum padrão informado na criação do escalonador.
 *
 * Se não houver ninguém esperando, o processo atual continua na CPU mesmo depois de
 * atingir o quantum, pois não há para quem ceder a vez. Um processo que termina sua
 * rajada de CPU antes do quantum libera a CPU imediatamente.
 */
public class RoundRobin implements Escalonador {
    /** Quantum padrão: vale para os processos que não têm um quantum próprio. */
    private final int quantum;
    /** Indica se algum processo tem quantum próprio (só muda o nome mostrado na tela). */
    private final boolean individuais;

    /**
     * Cria o escalonador Round Robin.
     *
     * @param quantum número máximo de instantes seguidos que cada processo pode usar a CPU;
     *                deve ser maior que zero
     */
    public RoundRobin(int quantum) { this(quantum, false); }

    /**
     * Cria o escalonador Round Robin indicando se há processos com quantum próprio.
     *
     * @param quantum     quantum padrão, usado pelos processos sem quantum próprio; deve ser maior que zero
     * @param individuais verdadeiro se algum processo tem quantum próprio; só altera o {@link #nome()}
     */
    public RoundRobin(int quantum, boolean individuais) {
        this.quantum = quantum;
        this.individuais = individuais;
    }

    /**
     * Quantum que vale para o processo: o próprio, se ele tiver, ou o padrão.
     *
     * @param p processo avaliado
     * @return número máximo de instantes seguidos que esse processo pode usar a CPU
     */
    public int quantumDe(Processo p) { return p.quantum > 0 ? p.quantum : quantum; }

    /**
     * Mantém o processo atual enquanto ele não atingiu o quantum ou enquanto não há
     * processos esperando. Caso contrário, ou se a CPU estiver livre, escolhe o primeiro
     * da fila de prontos.
     *
     * @param prontos processos aguardando a CPU, na ordem de entrada na fila
     * @param atual processo que está usando a CPU, ou {@code null} se ela está livre
     * @param fatiaUsada quantos instantes seguidos o processo atual já usou a CPU
     * @param tempo não é usado neste algoritmo
     * @return o processo que deve usar a CPU, ou {@code null} se não há ninguém para rodar
     */
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null && (fatiaUsada < quantumDe(atual) || prontos.isEmpty())) return atual;
        return prontos.isEmpty() ? null : prontos.get(0);
    }

    /** @return nome do algoritmo com o valor do quantum, como "Round Robin (q=4)" ou "Round Robin (q=4 + individuais)" */
    public String nome() { return "Round Robin (q=" + quantum + (individuais ? " + individuais" : "") + ")"; }
}
