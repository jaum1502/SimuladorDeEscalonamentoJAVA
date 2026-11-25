package simulador.gui;

import simulador.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Gráfico de Gantt: primeira linha = CPU; depois uma linha por processo mostrando
 * Execução (cor cheia), Pronto (cor clara) e Espera/E/S (listrado cinza).
 * <p>
 * Os nomes das linhas (CPU, P1, P2...) ficam num componente separado, {@link #getRotulos()}, que vai
 * no cabeçalho de linhas do JScrollPane: assim eles continuam visíveis quando o gráfico rola para a direita.
 */
class PainelGantt extends JPanel {
    static final int ROTULO = 56;
    private static final int ESQ = 8, CEL = 24, LIN = 24, TOPO = 8;
    private Simulador sim;
    private final Rotulos rotulos = new Rotulos();

    PainelGantt() { setBackground(Color.WHITE); }

    /** Coluna fixa com os nomes das linhas, para usar em {@code JScrollPane.setRowHeaderView}. */
    JComponent getRotulos() { return rotulos; }

    void setSimulador(Simulador s) {
        this.sim = s;
        revalidate();
        repaint();
        rotulos.revalidate();
        rotulos.repaint();
        SwingUtilities.invokeLater(() -> {
            Rectangle vis = getVisibleRect();
            scrollRectToVisible(new Rectangle(getPreferredSize().width - 1, vis.y, 1, 1));
        });
    }

    private int altura() {
        int n = sim == null ? 0 : sim.getProcessos().size();
        return TOPO + (n + 1) * LIN + 62;
    }

    @Override public Dimension getPreferredSize() {
        int ticks = sim == null ? 0 : sim.getGantt().size();
        return new Dimension(ESQ + Math.max(ticks, 20) * CEL + 20, altura());
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (sim == null) return;
        List<Processo> cpu = sim.getGantt();
        List<Estado[]> hist = sim.getHistorico();
        List<Processo> ps = sim.getProcessos();
        int ticks = cpu.size();

        // só desenha o trecho visível (com folga), para continuar leve com centenas de processos e instantes
        Rectangle clip = g.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());
        int t0 = Math.max(0, (clip.x - ESQ) / CEL - 2);
        int t1 = Math.min(ticks, (clip.x + clip.width - ESQ) / CEL + 2);
        int p0 = Math.max(0, (clip.y - TOPO) / LIN - 2);
        int p1 = Math.min(ps.size(), (clip.y + clip.height - TOPO) / LIN + 1);

        // linha da CPU: primeiro os blocos, depois os nomes (senão o bloco seguinte cobre o texto)
        int y = TOPO;
        for (int t = t0; t < t1; t++) {
            Processo p = cpu.get(t);
            int x = ESQ + t * CEL;
            g.setColor(p == null ? new Color(0xEEEEEE) : Cores.doProcesso(p.pid));
            g.fillRect(x, y, CEL, LIN - 4);
            if (t > 0 && cpu.get(t - 1) != p) { g.setColor(Cores.BORDA); g.drawLine(x, y, x, y + LIN - 4); }
        }
        g.setFont(getFont().deriveFont(Font.BOLD, 10f));
        g.setColor(Color.WHITE);
        Shape clipAntigo = g.getClip();
        for (int t = t0; t < t1; t++) {
            Processo p = cpu.get(t);
            if (p == null || (t > 0 && cpu.get(t - 1) == p)) continue;   // nome só no início de cada bloco
            int fim = t + 1;
            while (fim < ticks && cpu.get(fim) == p) fim++;
            int x = ESQ + t * CEL;
            g.clipRect(x + 1, y, (fim - t) * CEL - 2, LIN - 4);               // não passa do próprio bloco
            g.drawString("P" + p.pid, x + 3, y + 14);
            g.setClip(clipAntigo);
        }

        // uma linha por processo
        Stroke fino = new BasicStroke(1f);
        for (int i = p0; i < p1; i++) {
            Processo p = ps.get(i);
            y = TOPO + (i + 1) * LIN + 6;
            Color c = Cores.doProcesso(p.pid);
            for (int t = t0; t < t1; t++) {
                Estado e = hist.get(t)[i];
                int x = ESQ + t * CEL;
                if (e == Estado.EXECUCAO) { g.setColor(c); g.fillRect(x, y + 2, CEL, LIN - 8); }
                else if (e == Estado.PRONTO) { g.setColor(Cores.clara(c)); g.fillRect(x, y + 2, CEL, LIN - 8); }
                else if (e == Estado.ESPERA) {
                    g.setColor(new Color(0xD5D8DC));
                    g.fillRect(x, y + 2, CEL, LIN - 8);
                    g.setColor(new Color(0x99A3A4));
                    g.setStroke(fino);
                    g.clipRect(x, y + 2, CEL, LIN - 8);
                    for (int k = -LIN; k < CEL; k += 6) g.drawLine(x + k, y + LIN - 6, x + k + LIN, y + 2 - 4);
                    g.setClip(clipAntigo);
                }
            }
            g.setColor(new Color(0xEAECEE));
            g.drawLine(ESQ, y + LIN - 3, ESQ + Math.max(ticks, 20) * CEL, y + LIN - 3);
        }

        // eixo do tempo
        int yEixo = TOPO + (ps.size() + 1) * LIN + 12;
        g.setColor(Cores.BORDA);
        g.drawLine(ESQ, yEixo, ESQ + Math.max(ticks, 20) * CEL, yEixo);
        g.setFont(getFont().deriveFont(10f));
        int passo = ticks > 60 ? 5 : 1;
        for (int t = 0; t <= Math.max(ticks, 20); t += passo) {
            int x = ESQ + t * CEL;
            g.drawLine(x, yEixo, x, yEixo + 4);
            String s = String.valueOf(t);
            g.drawString(s, x - g.getFontMetrics().stringWidth(s) / 2, yEixo + 16);
        }

        // legenda
        int lx = ESQ, ly = yEixo + 38;
        g.setFont(getFont().deriveFont(11f));
        legenda(g, lx, ly, new Color(0x2E86DE), "Execução");
        legenda(g, lx + 100, ly, Cores.clara(new Color(0x2E86DE)), "Pronto (esperando CPU)");
        legenda(g, lx + 270, ly, new Color(0xD5D8DC), "Espera (E/S)");
    }

    private void legenda(Graphics2D g, int x, int y, Color c, String s) {
        g.setColor(c);
        g.fillRect(x, y - 9, 14, 10);
        g.setColor(Cores.TEXTO_SUAVE);
        g.drawString(s, x + 18, y);
    }

    /** Nomes das linhas do Gantt, alinhados com as linhas do painel principal. */
    private class Rotulos extends JComponent {
        Rotulos() { setOpaque(true); setBackground(Color.WHITE); }

        @Override public Dimension getPreferredSize() { return new Dimension(ROTULO, altura()); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (sim == null) return;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g.setColor(Cores.BORDA);
            g.drawString("CPU", 10, TOPO + 16);
            List<Processo> ps = sim.getProcessos();
            Rectangle clip = g.getClipBounds();
            if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());
            int p0 = Math.max(0, (clip.y - TOPO) / LIN - 2);
            int p1 = Math.min(ps.size(), (clip.y + clip.height - TOPO) / LIN + 1);
            for (int i = p0; i < p1; i++) {
                Processo p = ps.get(i);
                g.setColor(Cores.doProcesso(p.pid));
                g.drawString("P" + p.pid, 14, TOPO + (i + 1) * LIN + 6 + 15);
            }
        }
    }
}
