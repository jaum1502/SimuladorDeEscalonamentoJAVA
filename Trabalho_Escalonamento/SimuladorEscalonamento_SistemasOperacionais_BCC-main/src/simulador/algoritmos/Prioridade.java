package simulador.algoritmos;

import simulador.*;
import java.util.List;

/**
 * Escalonador por prioridade: usa a CPU o processo com o menor número de prioridade
 * (menor número significa mais importante). Pode funcionar de duas formas: na forma
 * preemptiva ou não preemptiva.
 *
 * Opcionalmente, o escalonador usa o aging (envelhecimento), evitando starvation.
 */
public class Prioridade implements Escalonador {
    /**
     * Indica se o escalonador será preemptivo (se o processo na CPU poderá ser substituído por outro de maior prioridade).
     */
    private final boolean preemptivo;
    /**
     * Envelhecimento de um processo. O 0 desativa o aging.
     */
    private final int aging;

    /**
     * Cria o escalonador por prioridade.
     * @param preemptivo identifica se é preemptivo ou não.
     * @param aging quantos instantes na fila fazem o processo ganhar um nível de prioridade.
     */
    public Prioridade(boolean preemptivo, int aging) {
        this.preemptivo = preemptivo;
        this.aging = aging;
    }

    /**
     * Calcula a prioridade que vale no instante informado. Para um processo na fila com
     * aging ativo, é a prioridade original menos um nível a cada {@code aging} instantes
     * de espera. Nos demais casos, é a prioridade original.
     *
     * @param p processo avaliado
     * @param tempo instante atual da simulação
     * @return prioridade considerada na comparação; quanto menor, mais importante
     */
    private int efetiva(Processo p, int tempo) {
        if (aging <= 0 || p.getEstado() != Estado.PRONTO) return p.prioridade;
        return p.prioridade - (tempo - p.getEntradaNaFila()) / aging;
    }

    /**
     * Escolhe o processo de maior importância. Na forma não preemptiva, mantém o processo
     * que já está na CPU. Na forma preemptiva, compara o processo atual com os da fila
     * e escolhe o de menor prioridade efetiva, mantendo o atual em caso de empate.
     *
     * @param prontos processos aguardando a CPU
     * @param atual processo que está usando a CPU, ou {@code null} se ela está livre
     * @param fatiaUsada não é usado neste algoritmo
     * @param tempo instante atual, usado no cálculo do aging
     * @return o processo que deve usar a CPU, ou {@code null} se não há ninguém para rodar
     */
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null && !preemptivo) return atual;
        Processo melhor = atual;
        for (Processo p : prontos)
            if (melhor == null || efetiva(p, tempo) < efetiva(melhor, tempo)) melhor = p;
        return melhor;
    }

    /** @return nome do algoritmo com suas opções, como "Prioridade preemptiva + aging(5)" */
    public String nome() {
        return "Prioridade" + (preemptivo ? " preemptiva" : "") + (aging > 0 ? " + aging(" + aging + ")" : "");
    }
}
