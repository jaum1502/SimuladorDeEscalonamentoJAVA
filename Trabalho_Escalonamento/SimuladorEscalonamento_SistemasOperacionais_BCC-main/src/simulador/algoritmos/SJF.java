package simulador.algoritmos;

import simulador.*;
import java.util.Comparator;
import java.util.List;

/**
 * Escalonador SJF (Shortest Job First): quando a CPU fica livre, escolhe o processo da fila de prontos que precisa de menos tempo de CPU
 * na rajada atual. Depois que um processo ganha a CPU, ele a mantém até terminar sua
 * rajada, sem ser interrompido por outro, mesmo que chegue um processo mais curto.
 *
 * O tempo considerado é o que falta da rajada de CPU atual, e não o tempo total do
 * processo. Em caso de empate, vence o processo que entrou primeiro na fila.
 *
 * Esse algoritmo tende a reduzir o tempo médio de espera, mas processos longos podem
 * esperar muito se processos curtos continuarem chegando.
 */
public class SJF implements Escalonador {

    /**
     * Mantém o processo que já está na CPU. Se a CPU estiver livre, escolhe o processo
     * da fila de prontos com o menor tempo restante na rajada de CPU.
     *
     * @param prontos processos aguardando a CPU, na ordem de entrada na fila
     * @param atual processo que está usando a CPU, ou {@code null} se ela está livre
     * @param fatiaUsada não é usado neste algoritmo
     * @param tempo não é usado neste algoritmo
     * @return o processo que deve usar a CPU, ou {@code null} se não há ninguém para rodar
     */
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null) return atual;
        return prontos.stream().min(Comparator.comparingInt(Processo::getRestante)).orElse(null);
    }

    /** @return o nome do algoritmo, usado na interface */
    public String nome() { return "SJF"; }
}
