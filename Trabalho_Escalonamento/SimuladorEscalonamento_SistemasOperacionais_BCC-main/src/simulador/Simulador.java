package simulador;

import java.util.*;

/**
 * Responsável por mover os processos entre os estados possíveis e registrar as métricas. É o motor da simulção.
 */
public class Simulador {
    /**
     * Escalonador.
     */
    private final Escalonador escalonador;
    /**
     * Todos os processos da simulção, ordenados por chegada e pid, em caso de empate.
     */
    private final List<Processo> todos = new ArrayList<>();
    /**
     * Fila dos processos que estão prontos, isto é, aguardando a CPU.
     */
    private final List<Processo> prontos = new ArrayList<>();
    /**
     * Fila dos processos que estão fazendo operações de E/S.
     */
    private final List<Processo> espera = new ArrayList<>();
    /**
     * Faz o registro de quem usou a CPU a cada instante, sendo null a representação para ociosa. É utilizado por Metricas para calcular a utilização da CPU.
     */
    private final List<Processo> gantt = new ArrayList<>();
    /**
     * Mensagens de texto contando cada mudança da situação.
     */
    private final List<String> log = new ArrayList<>();
    /**
     * "Foto" do estado de todos os processos em cada instante. É usado para desenhar o Gantt.
     */
    private final List<Estado[]> historico = new ArrayList<>();
    /**
     * O processo que esta rodando na CPU naquele momento. É null se não houver um processo na CPU no instante.
     */
    private Processo atual = null;
    /**
     * Há quantos instantes o processo atual está seguido na CPU. É importante para o algoritmo Round Robin.
     */
    private int fatiaUsada = 0;
    /**
     * Instante atual.
     */
    private int tempo = 0;
    /**
     * Quantas vezes um processo foi tirado da CPU para dar lugar a outro.
     */
    private int trocasDeContexto = 0;
    /**
     * Indica se o instante atual já foi montado. Serve de segurança para que o primeiro passo nunca rode sem essa montagem.
     */
    private boolean preparado = false;

    /**
     * Cópia cada um dos processos para uma lista auxiliar para que os originais nunca sejam alterados. Também ordena todos os processos por ordem de chegada e o método preparar é chamado para montar o instante zero.
     * @param escalonador escalonador de processos.
     * @param processos lista que contém todos os processos da simulação.
     */
    public Simulador(Escalonador escalonador, List<Processo> processos) {
        this.escalonador = escalonador;
        for (Processo p : processos) todos.add(p.copia());
        todos.sort(Comparator.comparingInt((Processo p) -> p.chegada).thenComparingInt(p -> p.pid));
        preparar();
    }

    /**
     * Verifica se a simulação terminou, isto é, se todos os processos estão com estado terminado.
     * @return verdadeiro ou falso.
     */
    public boolean terminou() {
        for (Processo p : todos) if (p.estado != Estado.TERMINADO) return false;
        return true;
    }

    /**
     * Avança a simulação em uma unidade de tempo. Nesse instante, o processo que está na CPU
     * executa, as operações de entrada e saída andam e os processos da fila de prontos esperam.
     * Se a rajada de CPU do processo atual terminar, ele vai para a espera de entrada e saída
     * ou, se era a última rajada, é marcado como terminado.
     *
     * Ao final do passo, o instante seguinte já está montado: chegadas, fim de entrada e saída
     * e escolha do escalonador já foram resolvidos. Por isso a tela nunca mostra a CPU livre
     * enquanto há processos esperando na fila.
     *
     * Se a simulação já terminou, este método não faz nada.
     */
    public void passo() {
        if (!preparado) preparar();
        if (terminou()) return;

        gantt.add(atual);
        Estado[] foto = new Estado[todos.size()];
        for (int i = 0; i < foto.length; i++) foto[i] = todos.get(i).estado;
        historico.add(foto);
        if (atual != null) { atual.restante--; fatiaUsada++; }
        for (Processo p : espera) p.restante--;
        for (Processo p : prontos) {
            p.tempoEspera++;
            p.esperaContinua++;
            p.maiorEsperaContinua = Math.max(p.maiorEsperaContinua, p.esperaContinua);
        }
        tempo++;

        if (atual != null && atual.restante == 0) {
            if (atual.rajadaAtual + 1 < atual.rajadas.length) {
                atual.rajadaAtual++;
                atual.restante = atual.rajadas[atual.rajadaAtual];
                atual.estado = Estado.ESPERA;
                espera.add(atual);
                log.add(tempo + ": " + atual + " -> ESPERA (E/S de " + atual.restante + ")");
            } else {
                atual.estado = Estado.TERMINADO;
                atual.fim = tempo;
                log.add(tempo + ": " + atual + " -> TERMINADO");
            }
            atual = null;
            fatiaUsada = 0;
        }
        preparar();
    }

    /**
     * Monta a situação do instante atual, em três etapas: coloca na fila de prontos os
     * processos que chegam agora, devolve à fila os que terminaram a entrada e saída e pede
     * ao escalonador que escolha quem usa a CPU.
     *
     * Se o escalonador escolher um processo diferente do que está na CPU, o processo atual
     * volta para a fila de prontos e a troca é contada. Quando a CPU estava livre, a entrada
     * de um processo não conta como troca. O processo escolhido sai da fila e passa a executar;
     * se for a primeira vez que ele usa a CPU, o instante é guardado para o cálculo do tempo de resposta.
     */
    private void preparar() {
        preparado = true;
        if (terminou()) return;

        // 1. Processos que chegam agora entram em PRONTO
        for (Processo p : todos)
            if (p.estado == null && p.chegada == tempo) paraPronto(p, "chegou");

        // 2. E/S: quem terminou volta para PRONTO
        Iterator<Processo> it = espera.iterator();
        while (it.hasNext()) {
            Processo p = it.next();
            if (p.restante == 0) {
                it.remove();
                p.rajadaAtual++;
                p.restante = p.rajadas[p.rajadaAtual];
                paraPronto(p, "terminou E/S");
            }
        }

        // 3. Escalonador decide (Pronto -> Execução, ou preempção Execução -> Pronto)
        Processo escolhido = escalonador.escolher(Collections.unmodifiableList(prontos), atual, fatiaUsada, tempo);
        if (escolhido != atual) {
            if (atual != null) {
                paraPronto(atual, "preempção");
                trocasDeContexto++;
            }
            if (escolhido != null) {
                prontos.remove(escolhido);
                escolhido.estado = Estado.EXECUCAO;
                escolhido.esperaContinua = 0;
                if (escolhido.primeiroInicio < 0) escolhido.primeiroInicio = tempo;
                log.add(tempo + ": " + escolhido + " -> EXECUÇÃO");
            }
            atual = escolhido;
            fatiaUsada = 0;
        }
    }

    /**
     * Executa a simulação do início ao fim, chamando {@link #passo()} até que todos os
     * processos terminem. Existe um limite de um milhão de passos para evitar que um
     * escalonador com defeito faça a simulação rodar para sempre.
     */
    public void executarTudo() {
        int limite = 1_000_000;
        while (!terminou() && limite-- > 0) passo();
    }

    /**
     * Coloca o processo na fila de prontos, guarda o instante em que ele entrou e registra
     * a mudança no log.
     *
     * @param p processo que passa a aguardar a CPU
     * @param motivo explicação que aparece no log, como "chegou" ou "preempção"
     */
    private void paraPronto(Processo p, String motivo) {
        p.estado = Estado.PRONTO;
        p.entradaNaFila = tempo;
        prontos.add(p);
        log.add(tempo + ": " + p + " -> PRONTO (" + motivo + ")");
    }

    //Getters.
    public int getTempo() { return tempo; }
    public Processo getAtual() { return atual; }
    public List<Processo> getProcessos() { return Collections.unmodifiableList(todos); }
    public List<Processo> getProntos() { return Collections.unmodifiableList(prontos); }
    public List<Processo> getEspera() { return Collections.unmodifiableList(espera); }
    public List<Processo> getGantt() { return Collections.unmodifiableList(gantt); }
    public List<Estado[]> getHistorico() { return Collections.unmodifiableList(historico); }
    public List<String> getLog() { return Collections.unmodifiableList(log); }
    public int getTrocasDeContexto() { return trocasDeContexto; }
    public Escalonador getEscalonador() { return escalonador; }
}
