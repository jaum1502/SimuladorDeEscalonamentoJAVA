package simulador.algoritmos;

import simulador.*;
import java.util.List;

/**
 * First-Come, First-Served: o primeiro processo a chegar é o primeiro a ser atendido pela CPU. É do tipo não preemptivo (não pode ser retirado da CPU até finalizar).
 */
public class FCFS implements Escalonador {
    /**
     * Mantém o processo que já está na CPU. Se a CPU estiver livre, escolhe o primeiro
     * da fila de prontos.
     *
     * @param prontos processos aguardando a CPU, na ordem de entrada na fila
     * @param atual processo que está usando a CPU, ou {@code null} se ela está livre
     * @param fatiaUsada não é usado neste algoritmo
     * @param tempo não é usado neste algoritmo
     * @return o processo que deve usar a CPU, ou {@code null} se não há ninguém para rodar
     */
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null) return atual;
        return prontos.isEmpty() ? null : prontos.get(0);
    }

    /** @return o nome do algoritmo, usado na interface */
    public String nome() { return "FCFS"; }
}
