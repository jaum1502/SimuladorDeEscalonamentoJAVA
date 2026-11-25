package simulador;

/**
 * Representa um processo (programa) dentro da simulação.
 */
public class Processo {
    /**
     * PID (Process IDentification).
     */
    public final int pid;
    /**
     * Instante que surgiu no sistema.
     */
    public final int chegada;
    /**
     * Valor de prioridade no sistema, sendo que quanto menor o número, maior a prioridade.
     */
    public final int prioridade;
    /**
     * Período contínuo em que um processo execta o mesmo tipo de atividade, sendo do tipo CPU (intervalo usando a CPU) e E/S (intervalo aguardadndo operação externa). O tipo CPU é representado por posições pares (0, 2, ...) e o tipo E/S por ímpares (1, 3, ...). Por tanto, para que o processo sempre comece e termine usando a CPU.
     */
    public final int[] rajadas;
    /**
     * Quantum próprio do processo no Round Robin. Se for 0, o processo usa o quantum padrão do escalonador.
     */
    public final int quantum;

    /**
     * Estado atual do processo. Se for null, o processo ainda não chegou no escalonador, apenas existe em memória.
     */
    Estado estado = null;
    /**
     * Qual rajada está, ou seja, a posição do vetor.
     */
    int rajadaAtual = 0;
    /**
     * Quanto falta para terminar a rajada atual.
     */
    int restante;
    /**
     * Instante em que entrou na fila de prontos (aging - técnica que aumenta a prioridade de quem espera muito, para que não seja esquecido).
     */
    int entradaNaFila;

    /**
     * Instante em que rodou pela primeira vez. Se for -1, não executou ainda.
     */
    int primeiroInicio = -1;
    /**
     * Instante em que terminou a execução. Se for -1, não terminou ainda.
     */
    int fim = -1;
    /**
     * Total de instantes que passou esperando na fila.
     */
    int tempoEspera = 0;
    /**
     * Maior período seguido esperando na fila (indicador de starvation).
     */
    int maiorEsperaContinua = 0;
    /**
     * Contador auxiliar usado para cada tempo de espera na fila.
     */
    int esperaContinua = 0;

    /**
     * Define os valores iniciais de um processo.
     *
     * @param pid        número de identificação do processo.
     * @param chegada    instante de chegada na simulação.
     * @param prioridade número de prioridade.
     * @param rajadas    duração das rajadas do processo, informadas uma após a outra e separadas por vírgula, alternando entre CPU e E/S. Deve haver ao menos uma rajada e a quantidade deve ser ímpar. Por exemplo, {@code new Processo (1, 0, 2, 5, 3, 2)} cria um processo com pid 1, 0 chegada, 2 prioridade e 5 unidades de CPU, 3 de E/S e 2 de CPU.
     */
    public Processo(int pid, int chegada, int prioridade, int... rajadas) {
        this(pid, chegada, prioridade, rajadas, 0);
    }

    /**
     * Define os valores iniciais de um processo com quantum próprio para o Round Robin.
     *
     * @param pid        número de identificação do processo.
     * @param chegada    instante de chegada na simulação.
     * @param prioridade número de prioridade.
     * @param rajadas    duração das rajadas, alternando CPU e E/S (quantidade ímpar).
     * @param quantum    quantum próprio do processo no Round Robin; 0 usa o quantum padrão do escalonador.
     */
    public Processo(int pid, int chegada, int prioridade, int[] rajadas, int quantum) {
        if (quantum < 0)
            throw new IllegalArgumentException("o quantum não pode ser negativo");
        if (rajadas.length == 0 || rajadas.length % 2 == 0)
            throw new IllegalArgumentException("rajadas devem começar e terminar com CPU (tamanho ímpar)");
        if (chegada < 0)
            throw new IllegalArgumentException("a chegada não pode ser negativa");
        this.pid = pid;
        this.chegada = chegada;
        this.prioridade = prioridade;
        this.quantum = quantum;
        /**
         * Faz um clone do vetor de rajadas
         */
        this.rajadas = rajadas.clone();
        /**
         * Começa com o tempo da primeira rajada de CPU.
         */
        this.restante = rajadas[0];
    }


    /**
     * Cria um processo cópia com os mesmos dados fixos, mas com o estado zerado, permitindo rodar o mesmo processo para vários algoritmos.
     *
     * @return o novo processo.
     */
    public Processo copia() {
        return new Processo(pid, chegada, prioridade, rajadas, quantum);
    }

    //Getters das Variáveis.
    public Estado getEstado() {
        return estado;
    }

    public int getRestante() {
        return restante;
    }

    public int getRajadaAtual() {
        return rajadaAtual;
    }

    public int getEntradaNaFila() {
        return entradaNaFila;
    }

    public int getPrimeiroInicio() {
        return primeiroInicio;
    }

    public int getFim() {
        return fim;
    }

    public int getTempoEspera() {
        return tempoEspera;
    }

    public int getMaiorEsperaContinua() {
        return maiorEsperaContinua;
    }

    /**
     * Soma as rajadas de CPU (posições pares do vetor).
     *
     * @return o tempo total de CPU que o processo irá precisar.
     */
    public int cpuTotal() {
        int s = 0;
        for (int i = 0; i < rajadas.length; i += 2) s += rajadas[i];
        return s;
    }

    /**
     * O tempo total desde que chegou até terminar.
     *
     * @return tempo total do ínicio ao fim.
     */
    public int turnaround() {
        return fim - chegada;
    }

    /**
     * Tempo total de quanto esperou até receber a CPU pela primeira vez.
     *
     * @return tempo total de espera pela CPU.
     */
    public int resposta() {
        return primeiroInicio - chegada;
    }

    /**
     * Identifica o processo no formato P + número de PID.
     *
     * @return P1, P2, etc.
     */
    @Override
    public String toString() {
        return "P" + pid;
    }
}
