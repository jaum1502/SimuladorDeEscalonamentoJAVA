package simulador.gui;

import simulador.*;
import simulador.algoritmos.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/** Janela principal do simulador. */
public class TelaPrincipal extends JFrame {
    private static final String TITULO = "Simulador de Escalonamento de Processos";
    private static final String[] ALGORITMOS = {"FCFS", "SJF", "SRTF", "Prioridade", "Round Robin"};

    /** Coluna do quantum próprio no modelo da tabela; só aparece na tela com o Round Robin. */
    private static final int COL_QUANTUM = 4;
    private final DefaultTableModel modeloProcessos = new DefaultTableModel(
            new String[]{"PID", "Chegada", "Prioridade", "Rajadas (CPU,E/S,CPU...)", "Quantum"}, 0) {
        @Override public Class<?> getColumnClass(int c) { return c >= 3 ? String.class : Integer.class; }
        @Override public boolean isCellEditable(int r, int c) { return c != 0; }
    };
    private final JTable tabelaProcessos = new JTable(modeloProcessos);
    private TableColumn colunaQuantum;
    private final JLabel dicaProcessos = new JLabel();

    private final JComboBox<String> comboAlgoritmo = new JComboBox<>(ALGORITMOS);
    private final JSpinner spQuantum = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
    private final JCheckBox ckPreemptivo = new JCheckBox("Preemptiva", true);
    private final JSpinner spAging = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JSlider slVelocidade = new JSlider(1, 20, 6);

    private final JButton btPasso = new JButton("Passo ▸");
    private final JButton btExecutar = new JButton("Executar ▶");
    private final JButton btFim = new JButton("Até o fim ⏭");
    private final JButton btReiniciar = new JButton("Reiniciar ↺");

    private final PainelEstados painelEstados = new PainelEstados();
    private final PainelGantt painelGantt = new PainelGantt();
    private final DefaultTableModel modeloMetricas = new DefaultTableModel(
            new String[]{"PID", "Chegada", "CPU total", "1ª execução", "Fim", "Espera", "Turnaround", "Resposta", "Maior espera seguida"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel lblResumo = new JLabel(" ");
    private final JTextArea txtLog = new JTextArea();

    private final javax.swing.Timer timer = new javax.swing.Timer(300, e -> passo());
    private Simulador sim;
    private int proximoPid = 1;
    private boolean preenchendo = false;

    public TelaPrincipal() {
        super(TITULO);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(new EmptyBorder(8, 8, 8, 8));

        add(montarLateral(), BorderLayout.WEST);
        add(montarCentro(), BorderLayout.CENTER);

        carregarExemplo();
        atualizarOpcoes();
        ligarEventos();
        reiniciar();

        setSize(1360, 860);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------ montagem
    private JComponent montarLateral() {
        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setPreferredSize(new Dimension(390, 0));

        // processos
        JPanel pProc = new JPanel(new BorderLayout(4, 4));
        pProc.setBorder(new TitledBorder("Processos"));
        tabelaProcessos.setRowHeight(22);
        tabelaProcessos.getColumnModel().getColumn(0).setPreferredWidth(35);
        tabelaProcessos.getColumnModel().getColumn(1).setPreferredWidth(60);
        tabelaProcessos.getColumnModel().getColumn(2).setPreferredWidth(70);
        tabelaProcessos.getColumnModel().getColumn(3).setPreferredWidth(170);
        // coluna do quantum: em branco = usa o quantum padrão, que aparece em cinza
        colunaQuantum = tabelaProcessos.getColumnModel().getColumn(COL_QUANTUM);
        colunaQuantum.setPreferredWidth(62);
        colunaQuantum.setHeaderValue("Quantum");
        colunaQuantum.setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                boolean vazio = v == null || v.toString().isBlank();
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, vazio ? spQuantum.getValue() : v, s, f, r, c);
                l.setHorizontalAlignment(RIGHT);
                l.setFont(l.getFont().deriveFont(vazio ? Font.ITALIC : Font.PLAIN));
                if (vazio && !s) l.setForeground(Cores.TEXTO_SUAVE);
                else if (!s) l.setForeground(t.getForeground());
                l.setToolTipText(vazio ? "Usa o quantum padrão. Digite um número para dar a este processo um quantum próprio."
                                       : "Quantum próprio deste processo. Apague para voltar ao padrão.");
                return l;
            }
        });
        tabelaProcessos.setAutoCreateColumnsFromModel(false);   // para poder esconder e mostrar a coluna do quantum
        tabelaProcessos.removeColumn(colunaQuantum);
        tabelaProcessos.putClientProperty("terminateEditOnFocusLost", true);
        tabelaProcessos.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, "P" + v, s, f, r, c);
                l.setForeground(Cores.doProcesso((Integer) v));
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                return l;
            }
        });
        JScrollPane sp = new JScrollPane(tabelaProcessos);
        sp.setPreferredSize(new Dimension(360, 230));
        pProc.add(sp, BorderLayout.CENTER);

        JPanel botoesProc = new JPanel(new GridLayout(3, 2, 4, 4));
        JButton btAdd = new JButton("Adicionar"), btRem = new JButton("Remover");
        JButton btAleat = new JButton("Processos aleatórios"), btEx = new JButton("Exemplo da aula");
        JButton btChegada = new JButton("Chegada aleatória");
        btChegada.setToolTipText("Mantém os processos da tabela e sorteia só a ordem / os instantes de chegada");
        btChegada.addActionListener(e -> { if (chegadaAleatoria()) { setTitle(TITULO); reiniciar(); } });
        JButton btSalvar = new JButton("Salvar..."), btCarregar = new JButton("Carregar...");
        btSalvar.setToolTipText("Salva os processos da tabela num arquivo .txt");
        btCarregar.setToolTipText("Carrega processos de um arquivo .txt salvo antes");
        btSalvar.addActionListener(e -> salvarArquivo());
        btCarregar.addActionListener(e -> carregarArquivo());
        btAdd.addActionListener(e -> { modeloProcessos.addRow(new Object[]{proximoPid++, 0, 1, "4", ""}); reiniciar(); });
        btRem.addActionListener(e -> {
            int[] sel = tabelaProcessos.getSelectedRows();
            for (int i = sel.length - 1; i >= 0; i--) modeloProcessos.removeRow(sel[i]);
            reiniciar();
        });
        btAleat.setToolTipText("Gera processos com valores aleatórios (você escolhe quantos)");
        btAleat.addActionListener(e -> { if (gerarAleatorio()) { setTitle(TITULO); reiniciar(); } });
        btEx.addActionListener(e -> { carregarExemplo(); setTitle(TITULO); reiniciar(); });
        botoesProc.add(btAdd); botoesProc.add(btRem); botoesProc.add(btAleat); botoesProc.add(btChegada);
        botoesProc.add(btSalvar); botoesProc.add(btCarregar);
        JPanel rodapeProc = new JPanel(new BorderLayout(0, 4));
        rodapeProc.add(botoesProc, BorderLayout.CENTER);
        rodapeProc.add(btEx, BorderLayout.SOUTH);
        pProc.add(rodapeProc, BorderLayout.SOUTH);
        dicaProcessos.setForeground(Cores.TEXTO_SUAVE);
        pProc.add(dicaProcessos, BorderLayout.NORTH);

        // algoritmo
        JPanel pAlg = new JPanel(new GridBagLayout());
        pAlg.setBorder(new TitledBorder("Algoritmo"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4); c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; pAlg.add(new JLabel("Escalonador:"), c);
        c.gridx = 1; c.weightx = 1; pAlg.add(comboAlgoritmo, c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0; pAlg.add(new JLabel("Quantum padrão:"), c);
        c.gridx = 1; pAlg.add(spQuantum, c);
        spQuantum.setToolTipText("Vale para os processos com a coluna Quantum em branco na tabela");
        c.gridx = 0; c.gridy = 2; pAlg.add(new JLabel("Aging (0 = sem):"), c);
        c.gridx = 1; pAlg.add(spAging, c);
        c.gridx = 1; c.gridy = 3; pAlg.add(ckPreemptivo, c);
        spAging.setToolTipText("A cada N ticks na fila, o processo ganha 1 nível de prioridade (evita starvation)");

        // controles
        JPanel pCtl = new JPanel(new GridBagLayout());
        pCtl.setBorder(new TitledBorder("Simulação"));
        c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        c.gridx = 0; c.gridy = 0; pCtl.add(btPasso, c);
        c.gridx = 1; pCtl.add(btExecutar, c);
        c.gridx = 0; c.gridy = 1; pCtl.add(btFim, c);
        c.gridx = 1; pCtl.add(btReiniciar, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        JPanel vel = new JPanel(new BorderLayout(4, 0));
        vel.add(new JLabel("Velocidade:"), BorderLayout.WEST);
        vel.add(slVelocidade, BorderLayout.CENTER);
        pCtl.add(vel, c);
        JButton btComparar = new JButton("Comparar todos os algoritmos");
        btComparar.setFont(btComparar.getFont().deriveFont(Font.BOLD));
        btComparar.addActionListener(e -> comparar());
        c.gridy = 3; pCtl.add(btComparar, c);

        for (JComponent j : new JComponent[]{pProc, pAlg, pCtl}) {
            j.setAlignmentX(Component.LEFT_ALIGNMENT);
            lateral.add(j);
            lateral.add(Box.createVerticalStrut(6));
        }
        pAlg.setMaximumSize(new Dimension(Integer.MAX_VALUE, pAlg.getPreferredSize().height));
        pCtl.setMaximumSize(new Dimension(Integer.MAX_VALUE, pCtl.getPreferredSize().height));
        return lateral;
    }

    private JComponent montarCentro() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(new TitledBorder("Estados dos processos"));
        topo.add(painelEstados);

        JScrollPane spGantt = new JScrollPane(painelGantt);
        spGantt.setRowHeaderView(painelGantt.getRotulos());   // nomes das linhas fixos ao rolar para a direita
        spGantt.getRowHeader().setBackground(Color.WHITE);
        spGantt.getVerticalScrollBar().setUnitIncrement(24);
        spGantt.setBorder(new TitledBorder("Gráfico de Gantt"));
        spGantt.getHorizontalScrollBar().setUnitIncrement(24);

        JTable tabelaMetricas = new JTable(modeloMetricas);
        tabelaMetricas.setRowHeight(22);
        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < modeloMetricas.getColumnCount(); i++) tabelaMetricas.getColumnModel().getColumn(i).setCellRenderer(centro);
        JPanel pMet = new JPanel(new BorderLayout(4, 4));
        pMet.add(new JScrollPane(tabelaMetricas), BorderLayout.CENTER);
        lblResumo.setBorder(new EmptyBorder(4, 6, 4, 6));
        pMet.add(lblResumo, BorderLayout.SOUTH);

        txtLog.setEditable(false);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Métricas", pMet);
        abas.addTab("Log de transições", new JScrollPane(txtLog));

        JSplitPane baixo = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spGantt, abas);
        baixo.setResizeWeight(0.62);
        JSplitPane tudo = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topo, baixo);
        tudo.setResizeWeight(0.38);
        return tudo;
    }

    private void ligarEventos() {
        btPasso.addActionListener(e -> passo());
        btExecutar.addActionListener(e -> alternarExecucao());
        btFim.addActionListener(e -> {
            timer.stop();
            if (garantirSimulador()) { sim.executarTudo(); atualizarTela(); }
        });
        btReiniciar.addActionListener(e -> reiniciar());
        comboAlgoritmo.addActionListener(e -> { atualizarOpcoes(); reiniciar(); });
        spQuantum.addChangeListener(e -> { tabelaProcessos.repaint(); reiniciar(); });   // repinta o padrão em cinza
        spAging.addChangeListener(e -> reiniciar());
        ckPreemptivo.addActionListener(e -> reiniciar());
        slVelocidade.addChangeListener(e -> timer.setDelay(velocidadeMs()));
        modeloProcessos.addTableModelListener(e -> { if (!preenchendo) reiniciar(); });
    }

    // ------------------------------------------------------------ lógica
    private int velocidadeMs() { return 1050 - slVelocidade.getValue() * 50; }

    private void atualizarOpcoes() {
        String alg = (String) comboAlgoritmo.getSelectedItem();
        boolean rr = "Round Robin".equals(alg);
        spQuantum.setEnabled(rr);
        boolean prio = "Prioridade".equals(alg);
        spAging.setEnabled(prio);
        ckPreemptivo.setEnabled(prio);
        mostrarColunaQuantum(rr);
        dicaProcessos.setText("<html><small>Rajadas: \"5\" = só CPU; \"3,4,2\" = 3 de CPU, 4 de E/S, 2 de CPU. "
                + "Prioridade: número menor = mais prioritário."
                + (rr ? " Quantum em branco (cinza) = usa o quantum padrão." : "") + "</small></html>");
    }

    /** A coluna do quantum só aparece com o Round Robin; os valores continuam guardados quando ela some. */
    private void mostrarColunaQuantum(boolean mostrar) {
        boolean visivel = false;
        for (int i = 0; i < tabelaProcessos.getColumnCount(); i++)
            if (tabelaProcessos.getColumnModel().getColumn(i) == colunaQuantum) visivel = true;
        if (mostrar == visivel) return;
        if (tabelaProcessos.isEditing()) tabelaProcessos.getCellEditor().stopCellEditing();
        if (mostrar) tabelaProcessos.addColumn(colunaQuantum);
        else tabelaProcessos.removeColumn(colunaQuantum);
        // com a coluna a mais, o cabeçalho das rajadas fica curto (o formato está na dica acima da tabela)
        TableColumnModel cm = tabelaProcessos.getColumnModel();
        cm.getColumn(3).setHeaderValue(mostrar ? "Rajadas" : "Rajadas (CPU,E/S,CPU...)");
        int[] larguras = mostrar ? new int[]{35, 60, 70, 110, 62} : new int[]{35, 60, 70, 170};
        for (int i = 0; i < larguras.length; i++) cm.getColumn(i).setPreferredWidth(larguras[i]);
        tabelaProcessos.getTableHeader().repaint();
    }

    private Escalonador criarEscalonador(List<Processo> ps) {
        return switch ((String) comboAlgoritmo.getSelectedItem()) {
            case "SJF" -> new SJF();
            case "SRTF" -> new SRTF();
            case "Prioridade" -> new Prioridade(ckPreemptivo.isSelected(), (Integer) spAging.getValue());
            case "Round Robin" -> new RoundRobin((Integer) spQuantum.getValue(), temQuantumProprio(ps));
            default -> new FCFS();
        };
    }

    private static boolean temQuantumProprio(List<Processo> ps) {
        return ps.stream().anyMatch(p -> p.quantum > 0);
    }

    /** Lê a célula do quantum: vazia = 0 (usa o padrão); senão, um número maior que zero. */
    private static int lerQuantum(Object v) {
        if (v == null || v.toString().isBlank()) return 0;
        try {
            int q = Integer.parseInt(v.toString().trim());
            if (q > 0) return q;
        } catch (NumberFormatException ignorado) { }
        throw new IllegalArgumentException("quantum inválido (\"" + v + "\"). Use um número maior que zero, "
                + "ou deixe em branco para usar o quantum padrão");
    }

    /** Lê a tabela de processos. Mostra erro e devolve null se algo estiver inválido. */
    private List<Processo> lerProcessos() {
        if (tabelaProcessos.isEditing()) tabelaProcessos.getCellEditor().stopCellEditing();
        List<Processo> ps = new ArrayList<>();
        for (int r = 0; r < modeloProcessos.getRowCount(); r++) {
            Object pid = modeloProcessos.getValueAt(r, 0);
            try {
                int chegada = (Integer) modeloProcessos.getValueAt(r, 1);
                int prio = (Integer) modeloProcessos.getValueAt(r, 2);
                String[] partes = String.valueOf(modeloProcessos.getValueAt(r, 3)).trim().split("\\s*[,; ]\\s*");
                int[] raj = new int[partes.length];
                for (int i = 0; i < partes.length; i++) {
                    raj[i] = Integer.parseInt(partes[i]);
                    if (raj[i] <= 0) throw new IllegalArgumentException("as rajadas devem ser maiores que zero");
                }
                if (chegada < 0) throw new IllegalArgumentException("a chegada não pode ser negativa");
                int quantum = lerQuantum(modeloProcessos.getValueAt(r, COL_QUANTUM));
                ps.add(new Processo((Integer) pid, chegada, prio, raj, quantum));
            } catch (NumberFormatException | NullPointerException ex) {
                return erro("P" + pid + ": rajadas inválidas. Use números separados por vírgula, ex.: 3,4,2");
            } catch (IllegalArgumentException ex) {
                return erro("P" + pid + ": " + ex.getMessage());
            }
        }
        if (ps.isEmpty()) return erro("Adicione pelo menos um processo.");
        return ps;
    }

    private List<Processo> erro(String msg) {
        timer.stop();
        btExecutar.setText("Executar ▶");
        JOptionPane.showMessageDialog(this, msg, "Processo inválido", JOptionPane.WARNING_MESSAGE);
        return null;
    }

    private boolean garantirSimulador() {
        if (sim != null) return true;
        List<Processo> ps = lerProcessos();
        if (ps == null) return false;
        sim = new Simulador(criarEscalonador(ps), ps);
        return true;
    }

    private void passo() {
        if (!garantirSimulador()) return;
        if (sim.terminou()) { timer.stop(); btExecutar.setText("Executar ▶"); return; }
        sim.passo();
        atualizarTela();
    }

    private void alternarExecucao() {
        if (timer.isRunning()) { timer.stop(); btExecutar.setText("Executar ▶"); return; }
        if (!garantirSimulador()) return;
        if (sim.terminou()) reiniciar();
        timer.setDelay(velocidadeMs());
        timer.start();
        btExecutar.setText("Pausar ⏸");
    }

    private void reiniciar() {
        timer.stop();
        btExecutar.setText("Executar ▶");
        sim = null;
        // simulador "vazio" só para desenhar o estado inicial, se a tabela for válida
        List<Processo> ps = lerProcessosSilencioso();
        if (ps != null) sim = new Simulador(criarEscalonador(ps), ps);
        atualizarTela();
    }

    private List<Processo> lerProcessosSilencioso() {
        try {
            List<Processo> ps = new ArrayList<>();
            for (int r = 0; r < modeloProcessos.getRowCount(); r++) {
                String[] partes = String.valueOf(modeloProcessos.getValueAt(r, 3)).trim().split("\\s*[,; ]\\s*");
                int[] raj = Arrays.stream(partes).mapToInt(Integer::parseInt).toArray();
                if (Arrays.stream(raj).anyMatch(x -> x <= 0)) return null;
                ps.add(new Processo((Integer) modeloProcessos.getValueAt(r, 0), (Integer) modeloProcessos.getValueAt(r, 1),
                        (Integer) modeloProcessos.getValueAt(r, 2), raj, lerQuantum(modeloProcessos.getValueAt(r, COL_QUANTUM))));
            }
            return ps.isEmpty() ? null : ps;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void atualizarTela() {
        painelEstados.setSimulador(sim);
        painelGantt.setSimulador(sim);
        modeloMetricas.setRowCount(0);
        if (sim == null) { lblResumo.setText(" "); txtLog.setText(""); return; }

        for (Processo p : sim.getProcessos()) {
            boolean fim = p.getEstado() == Estado.TERMINADO;
            boolean iniciou = p.getPrimeiroInicio() >= 0;
            modeloMetricas.addRow(new Object[]{
                "P" + p.pid, p.chegada, p.cpuTotal(),
                iniciou ? p.getPrimeiroInicio() : "-",
                fim ? p.getFim() : "-",
                p.getTempoEspera(),
                fim ? p.turnaround() : "-",
                iniciou ? p.resposta() : "-",
                p.getMaiorEsperaContinua()
            });
        }
        if (sim.terminou()) {
            Metricas m = new Metricas(sim);
            lblResumo.setText(String.format("<html><b>Concluído em t=%d</b> &nbsp;|&nbsp; Espera média: <b>%.2f</b> &nbsp;|&nbsp; "
                    + "Turnaround médio: <b>%.2f</b> &nbsp;|&nbsp; Resposta média: <b>%.2f</b><br>"
                    + "Utilização da CPU: <b>%.1f%%</b> &nbsp;|&nbsp; Throughput: <b>%.3f</b> proc/tick &nbsp;|&nbsp; "
                    + "Maior espera seguida (starvation): <b>%d</b> &nbsp;|&nbsp; Preempções: <b>%d</b></html>",
                    m.tempoTotal, m.esperaMedia, m.turnaroundMedio, m.respostaMedia, m.utilizacaoCpu * 100,
                    m.throughput, m.maiorEspera, m.trocasDeContexto));
            timer.stop();
            btExecutar.setText("Executar ▶");
        } else {
            lblResumo.setText("Simulando... as médias aparecem quando todos os processos terminarem.");
        }
        txtLog.setText(String.join("\n", sim.getLog()));
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void comparar() {
        List<Processo> ps = lerProcessos();
        if (ps == null) return;
        int q = (Integer) spQuantum.getValue();
        int aging = Math.max(1, (Integer) spAging.getValue() == 0 ? 3 : (Integer) spAging.getValue());
        List<Escalonador> todos = List.of(new FCFS(), new SJF(), new SRTF(),
                new Prioridade(false, 0), new Prioridade(true, 0), new Prioridade(true, aging), new RoundRobin(q, temQuantumProprio(ps)));
        new DialogoComparacao(this, todos, ps).setVisible(true);
    }

    // ------------------------------------------------------------ salvar / carregar
    private static final String PREF_PASTA = "ultimaPasta";
    private static final Preferences prefs = Preferences.userNodeForPackage(TelaPrincipal.class);

    /** Seletor de arquivos que abre na última pasta usada (ou em "exemplos", se existir ao lado do programa). */
    private JFileChooser criarSeletor() {
        File pasta = new File(prefs.get(PREF_PASTA, ""));
        if (!pasta.isDirectory()) pasta = new File("exemplos");
        if (!pasta.isDirectory()) pasta = new File(System.getProperty("user.dir"));
        JFileChooser fc = new JFileChooser(pasta);
        fc.setFileFilter(new FileNameExtensionFilter("Conjunto de processos (*.txt)", "txt"));
        return fc;
    }

    private void lembrarPasta(File arquivo) {
        File pasta = arquivo.getAbsoluteFile().getParentFile();
        if (pasta != null) prefs.put(PREF_PASTA, pasta.getPath());
    }

    private void salvarArquivo() {
        List<Processo> ps = lerProcessos();
        if (ps == null) return;
        JFileChooser fc = criarSeletor();
        fc.setDialogTitle("Salvar processos");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File arq = fc.getSelectedFile();
        if (!arq.getName().toLowerCase().endsWith(".txt")) arq = new File(arq.getPath() + ".txt");
        if (arq.exists() && JOptionPane.showConfirmDialog(this, "\"" + arq.getName() + "\" já existe. Substituir?",
                "Salvar processos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            ArquivoProcessos.salvar(arq.toPath(), ps);
            lembrarPasta(arq);
            setTitle(TITULO + " - " + arq.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível salvar:\n" + ex.getMessage(),
                    "Salvar processos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarArquivo() {
        JFileChooser fc = criarSeletor();
        fc.setDialogTitle("Carregar processos");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File arq = fc.getSelectedFile();
        List<Processo> ps;
        try {
            ps = ArquivoProcessos.carregar(arq.toPath());
        } catch (ArquivoProcessos.FormatoInvalido ex) {
            JOptionPane.showMessageDialog(this, arq.getName() + "\n" + ex.getMessage(),
                    "Arquivo inválido", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível abrir:\n" + ex.getMessage(),
                    "Carregar processos", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Object[]> linhas = new ArrayList<>();
        int maiorPid = 0;
        for (Processo p : ps) {
            String raj = Arrays.stream(p.rajadas).mapToObj(String::valueOf).collect(Collectors.joining(","));
            linhas.add(new Object[]{p.pid, p.chegada, p.prioridade, raj, p.quantum > 0 ? String.valueOf(p.quantum) : ""});
            maiorPid = Math.max(maiorPid, p.pid);
        }
        preencherTabela(linhas);
        proximoPid = maiorPid + 1;
        lembrarPasta(arq);
        setTitle(TITULO + " - " + arq.getName());
        reiniciar();
    }

    /**
     * Troca todas as linhas da tabela de processos de uma vez. Sem isso, cada addRow dispararia um
     * reinício da simulação, o que fica lento com centenas de processos.
     */
    private void preencherTabela(List<Object[]> linhas) {
        if (tabelaProcessos.isEditing()) tabelaProcessos.getCellEditor().cancelCellEditing();
        preenchendo = true;
        try {
            modeloProcessos.setRowCount(0);
            for (Object[] l : linhas) modeloProcessos.addRow(l);
        } finally {
            preenchendo = false;
        }
    }

    // ------------------------------------------------------------ dados de exemplo
    private void carregarExemplo() {
        Object[][] ex = {
            {1, 0, 3, "6,3,2", ""},
            {2, 1, 1, "3", ""},
            {3, 2, 4, "8", ""},
            {4, 3, 2, "2,4,2", ""},
            {5, 5, 5, "4", ""},
        };
        preencherTabela(Arrays.asList(ex));
        proximoPid = 6;
    }

    // ------------------------------------------------------------ chegada aleatória
    private boolean ultimoModoEmbaralhar = true;

    /**
     * Mantém os processos da tabela (rajadas, prioridade, quantum) e muda só a chegada, de um destes jeitos:
     * embaralhando os instantes que já existem entre os processos (a ordem muda, os instantes são os mesmos)
     * ou sorteando instantes novos entre 0 e um limite. Devolve false se o usuário cancelou.
     */
    private boolean chegadaAleatoria() {
        if (tabelaProcessos.isEditing()) tabelaProcessos.getCellEditor().stopCellEditing();
        int n = modeloProcessos.getRowCount();
        if (n == 0) {
            JOptionPane.showMessageDialog(this, "Adicione processos antes de sortear a chegada.",
                    "Chegada aleatória", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        List<Integer> atuais = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            Object v = modeloProcessos.getValueAt(r, 1);
            atuais.add(v instanceof Integer i ? i : 0);
        }

        JRadioButton rbEmbaralhar = new JRadioButton("Embaralhar a ordem de chegada (os mesmos instantes, trocados entre os processos)");
        JRadioButton rbSortear = new JRadioButton("Sortear novos instantes de chegada, entre 0 e");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbEmbaralhar); grupo.add(rbSortear);
        (ultimoModoEmbaralhar ? rbEmbaralhar : rbSortear).setSelected(true);
        JSpinner spLimite = new JSpinner(new SpinnerNumberModel(Math.max(10, n * 2), 0, 100_000, 1));
        ((JSpinner.DefaultEditor) spLimite.getEditor()).getTextField().setColumns(5);
        spLimite.setEnabled(rbSortear.isSelected());
        rbSortear.addItemListener(e -> spLimite.setEnabled(rbSortear.isSelected()));

        JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
        p.add(new JLabel("Os processos continuam os mesmos; só a coluna Chegada muda."));
        p.add(rbEmbaralhar);
        JPanel linhaSortear = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaSortear.add(rbSortear);
        linhaSortear.add(Box.createHorizontalStrut(4));
        linhaSortear.add(spLimite);
        p.add(linhaSortear);
        int op = JOptionPane.showConfirmDialog(this, p, "Chegada aleatória", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return false;
        try { ((JSpinner.DefaultEditor) spLimite.getEditor()).getTextField().commitEdit(); }
        catch (java.text.ParseException ignorado) { }
        ultimoModoEmbaralhar = rbEmbaralhar.isSelected();

        Random r = new Random();
        List<Integer> novas = new ArrayList<>(atuais);
        if (ultimoModoEmbaralhar) {
            // embaralha; se por acaso sair igual, tenta de novo (só não dá quando todos chegam juntos)
            boolean daPraMudar = new HashSet<>(atuais).size() > 1;
            for (int tentativa = 0; tentativa < 50; tentativa++) {
                Collections.shuffle(novas, r);
                if (!daPraMudar || !novas.equals(atuais)) break;
            }
        } else {
            int limite = (Integer) spLimite.getValue();
            for (int i = 0; i < n; i++) novas.set(i, r.nextInt(limite + 1));
        }
        preenchendo = true;
        try {
            for (int i = 0; i < n; i++) modeloProcessos.setValueAt(novas.get(i), i, 1);
        } finally {
            preenchendo = false;
        }
        return true;
    }

    private static final int MAX_ALEATORIOS = 1000;
    private int ultimaQtdAleatoria = 6;

    /** Pergunta quantos processos gerar. Devolve 0 se o usuário cancelar. */
    private int perguntarQuantidade() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(ultimaQtdAleatoria, 1, MAX_ALEATORIOS, 1));
        JFormattedTextField campo = ((JSpinner.DefaultEditor) sp.getEditor()).getTextField();
        campo.setColumns(6);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel("Quantos processos você quer gerar? (1 a " + MAX_ALEATORIOS + ")"), BorderLayout.NORTH);
        JPanel linhaCampo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaCampo.add(sp);
        linhaCampo.add(new JLabel("  processos"));
        p.add(linhaCampo, BorderLayout.CENTER);
        JLabel obs = new JLabel("<html><small>Chegada, prioridade e rajadas são sorteadas. "
                + "As chegadas se espalham conforme a quantidade.</small></html>");
        obs.setForeground(Cores.TEXTO_SUAVE);
        p.add(obs, BorderLayout.SOUTH);

        JOptionPane painel = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
            // o JOptionPane põe o foco no botão OK ao abrir; aqui ele vai para o campo, já selecionado,
            // para dar para digitar o número direto e apertar Enter
            @Override public void selectInitialValue() {
                campo.requestFocusInWindow();
                SwingUtilities.invokeLater(campo::selectAll);
            }
        };
        JDialog d = painel.createDialog(this, "Processos aleatórios");
        campo.addActionListener(e -> painel.setValue(JOptionPane.OK_OPTION)); // Enter confirma
        d.setVisible(true);
        d.dispose();
        if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(painel.getValue())) return 0;
        try {
            campo.commitEdit();
        } catch (java.text.ParseException ex) {
            // texto inválido: fica o último valor válido do spinner
        }
        ultimaQtdAleatoria = (Integer) sp.getValue();
        return ultimaQtdAleatoria;
    }

    /** Pergunta a quantidade e preenche a tabela. Devolve false se o usuário cancelou. */
    private boolean gerarAleatorio() {
        int n = perguntarQuantidade();
        if (n <= 0) return false;
        Random r = new Random();
        // espalha as chegadas para que a fila não fique com todos os processos de uma vez
        int limiteChegada = Math.max(10, n * 2);
        List<Object[]> linhas = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            String raj = r.nextBoolean()
                    ? String.valueOf(2 + r.nextInt(7))
                    : (1 + r.nextInt(5)) + "," + (2 + r.nextInt(4)) + "," + (1 + r.nextInt(4));
            linhas.add(new Object[]{i, r.nextInt(limiteChegada), 1 + r.nextInt(5), raj, ""});
        }
        preencherTabela(linhas);
        proximoPid = n + 1;
        return true;
    }
}
