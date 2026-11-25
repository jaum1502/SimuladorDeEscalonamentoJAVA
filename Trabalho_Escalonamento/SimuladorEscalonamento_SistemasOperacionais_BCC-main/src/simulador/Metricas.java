package simulador;

/**
 * Classe responsavem pelas Métricas
 */
public class Metricas {
    /**
     * Espera média para a utilização da CPU, tornaround (intervalo em que um processo chega ao programa e o instante em que ele conclui), respostaMedia (intervalo de tempo entre o estado pronto até o uso da CPU).
     */
    public final double esperaMedia, turnaroundMedio, respostaMedia;
    /**
     * Uso da CPU.
     */
    public final double utilizacaoCpu;
    /**
     * Números de processos concluídos em razão do tempo total.
     */
    public final double throughput;
    /**
     * Serve para observar o envelhecimento do processo, evitando starvation (processo esperando infinitamente pela CPU).
     */
    public final int maiorEspera;
    /**
     * Tempo total de execução e número de tracas de contexto (troca de processos).
     */
    public final int tempoTotal, trocasDeContexto;

    /**
     * Construtor que recebe o simulador de escalonamento de processos ao fim da execução para calcular a partir dos dados dele, o conjunto de métricas.
     *
     * @param s objeto simulador de escalonamento de processos.
     */
    public Metricas(Simulador s) {
        /**
         * Lista de processos.
         */
        var ps = s.getProcessos();
        /**
         * Quantidade de processos.
         */
        int n = ps.size();
        /**
         * e (soma do tempo de espera de todos os processos), t (soma de turnaround de todos os processos), r (soma do tempo de resposta de todos os processos).
         */
        double e = 0, t = 0, r = 0;
        /**
         * Guarda a maior espera contínua entre todos os processos, ou seja, o pior caso de espera na fila sem ser atendido.
         */
        int maior = 0;
        /**
         * Realiza as métricas individuais e verifica qual o maior.
         */
        for (Processo p : ps) {
            e += p.getTempoEspera();
            t += p.turnaround();
            r += p.resposta();
            maior = Math.max(maior, p.getMaiorEsperaContinua());
        }
        /**
         * Quantidade de instantes em que a CPU realmente trabalhou.
         */
        long ocupado = s.getGantt().stream().filter(x -> x != null).count();
        /**
         * Duração total da simulação.
         */
        tempoTotal = s.getTempo();
        /**
         * Espera média.
         */
        esperaMedia = e / n;
        /**
         * Turnaruound médio.
         */
        turnaroundMedio = t / n;
        /**
         * Tempo de resposta médio.
         */
        respostaMedia = r / n;
        /**
         * Tempo de utilização da CPU (Se o tempo for 0, a utilização é zero).
         */
        utilizacaoCpu = tempoTotal == 0 ? 0 : (double) ocupado / tempoTotal;
        /**
         * Quantidade de processos dividida pelo tempo total.
         */
        throughput = tempoTotal == 0 ? 0 : (double) n / tempoTotal;
        /**
         * Pior caso de espera.
         */
        maiorEspera = maior;
        /**
         * Número de trocas de contexto.
         */
        trocasDeContexto = s.getTrocasDeContexto();
    }
}
