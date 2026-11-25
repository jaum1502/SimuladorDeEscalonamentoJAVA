package simulador.algoritmos;

import simulador.*;
import java.util.List;

/**
 * Escalonador SRTF (Shortest Remaining Time First):
 * é a versão com interrupção do SJF. A cada instante, usa a CPU o processo que tem menos
 * tempo restante na rajada de CPU atual, considerando tanto o que já está na CPU quanto
 * os que esperam na fila. Se chegar (ou voltar de uma entrada e saída) um processo com
 * rajada mais curta do que o tempo que falta ao processo atual, o atual é retirado da CPU
 * e volta para a fila de prontos.
 *
 * Em caso de empate, o processo que já está na CPU continua e, entre os da fila, vence
 * o que entrou primeiro. Isso evita trocas desnecessárias.
 *
 * Esse algoritmo tende a gerar tempos médios de espera muito baixos, mas processos longos
 * podem esperar muito se processos curtos continuarem aparecendo. Também costuma causar
 * mais trocas de contexto do que o SJF.
 */
public class SRTF implements Escalonador {

    /**
     * Escolhe o processo com o menor tempo restante na rajada de CPU, comparando o processo
     * atual com todos os da fila de prontos. O processo atual só é substituído por outro
     * que tenha tempo restante estritamente menor.
     *
     * @param prontos processos aguardando a CPU, na ordem de entrada na fila
     * @param atual processo que está usando a CPU, ou {@code null} se ela está livre
     * @param fatiaUsada não é usado neste algoritmo
     * @param tempo não é usado neste algoritmo
     * @return o processo que deve usar a CPU, ou {@code null} se não há ninguém para rodar
     */
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        Processo melhor = atual;
        for (Processo p : prontos)
            if (melhor == null || p.getRestante() < melhor.getRestante()) melhor = p;
        return melhor;
    }

    /** @return o nome do algoritmo, usado na interface */
    public String nome() { return "SRTF"; }
}
