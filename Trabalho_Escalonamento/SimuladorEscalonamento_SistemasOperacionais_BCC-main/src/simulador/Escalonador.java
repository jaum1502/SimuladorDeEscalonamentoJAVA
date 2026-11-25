package simulador;

import java.util.List;

/**
 * Contrato que todo algoritmo de escalonamento deve seguir. Um escalonador decide qual
 * processo usa a CPU a cada instante, e o {@link Simulador} consulta essa decisão sempre
 * que precisa escolher. Como o simulador só conhece esta interface, trocar de algoritmo
 * significa apenas trocar a classe informada na criação, sem alterar o restante do código.
 */
public interface Escalonador {

    /**
     * Decide qual processo usa a CPU no próximo instante.
     *
     * Para manter o processo que já está na CPU, basta devolvê-lo. Se for devolvido um
     * processo diferente enquanto a CPU está ocupada, o processo atual é retirado da CPU
     * e volta para o final da fila de prontos. Se for devolvido {@code null}, a CPU fica
     * ociosa.
     *
     * @param prontos processos aguardando a CPU, na ordem em que entraram na fila; não
     *                inclui o processo que está na CPU, e a lista não pode ser alterada
     * @param atual processo que está usando a CPU, ou {@code null} se ela está livre
     * @param fatiaUsada quantos instantes seguidos o processo atual já usou a CPU; é útil
     *                   para algoritmos que limitam esse tempo, como o Round Robin
     * @param tempo instante atual da simulação
     * @return o processo que deve usar a CPU (que pode ser o próprio {@code atual}), ou
     *         {@code null} se não há ninguém para rodar
     */
    Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo);

    /**
     * Informa o nome do algoritmo, para ser exibido na interface e nos resultados.
     *
     * @return nome do algoritmo, incluindo suas opções quando houver, como "Round Robin (q=4)"
     */
    String nome();
}
