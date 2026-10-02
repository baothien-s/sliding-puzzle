package game;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Draws the puzzle board and the move/time display, and turns mouse clicks
 * and arrow keys into moves.
 */
public class GamePanel extends JPanel {

    public static final Color BACKGROUND = new Color(0x1E2433);
    private static final Color BOARD_COLOR = new Color(0x2B3348);
    private static final Color TILE_COLOR = new Color(0x4F7CFF);
    private static final Color TILE_IN_PLACE_COLOR = new Color(0x2FAE84);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color MUTED_TEXT_COLOR = new Color(0x9AA4BF);

    private static final int PADDING = 30;
    private static final int HUD_HEIGHT = 80;
    private static final int FOOTER_HEIGHT = 40;
    private static final int TILE_GAP = 6;
    private static final int WIN_SCREEN_DELAY_MS = 700;

    private final Game game;
    private final Runnable onWin;
    private final Timer ticker = new Timer(250, e -> repaint());

    /** @param onWin called shortly after the puzzle is solved so the window can show the win screen */
    public GamePanel(Game game, Runnable onWin) {
        this.game = game;
        this.onWin = onWin;

        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(480 + 2 * PADDING, HUD_HEIGHT + 480 + FOOTER_HEIGHT));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int[] cell = cellAt(e.getX(), e.getY());
                if (cell != null) {
                    afterMove(game.tryMove(cell[0], cell[1]));
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int[] cell = cellAt(e.getX(), e.getY());
                boolean movable = cell != null
                        && game.getState() == Game.State.PLAYING
                        && game.getBoard().canMove(cell[0], cell[1]);
                setCursor(Cursor.getPredefinedCursor(movable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        bindArrowKey(KeyEvent.VK_UP, -1, 0);
        bindArrowKey(KeyEvent.VK_DOWN, 1, 0);
        bindArrowKey(KeyEvent.VK_LEFT, 0, -1);
        bindArrowKey(KeyEvent.VK_RIGHT, 0, 1);
    }

    /** Starts the repaint timer that keeps the clock on screen ticking. */
    public void startTicking() {
        ticker.start();
        repaint();
    }

    public void stopTicking() {
        ticker.stop();
    }

    // ---- input -----------------------------------------------------------------

    private void bindArrowKey(int keyCode, int rowDelta, int colDelta) {
        String name = "slide-" + keyCode;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                afterMove(game.tryMoveDirection(rowDelta, colDelta));
            }
        });
    }

    private void afterMove(boolean moved) {
        if (!moved) {
            return; // illegal moves are simply ignored
        }
        repaint();
        if (game.getState() == Game.State.WON) {
            ticker.stop();
            // short pause so the player sees the finished puzzle before the win screen
            Timer delay = new Timer(WIN_SCREEN_DELAY_MS, e -> {
                if (game.getState() == Game.State.WON) {
                    onWin.run();
                }
            });
            delay.setRepeats(false);
            delay.start();
        }
    }

    /** Converts a pixel position to {row, col}, or null if it is outside the board. */
    private int[] cellAt(int x, int y) {
        PuzzleBoard board = game.getBoard();
        Rectangle area = boardArea();
        if (board == null || !area.contains(x, y)) {
            return null;
        }
        int cell = area.width / board.getSize();
        return new int[]{(y - area.y) / cell, (x - area.x) / cell};
    }

    /** Square area for the board, centered, with a width that divides evenly into cells. */
    private Rectangle boardArea() {
        int size = game.getBoard() == null ? 3 : game.getBoard().getSize();
        int available = Math.min(getWidth() - 2 * PADDING, getHeight() - HUD_HEIGHT - FOOTER_HEIGHT);
        int side = Math.max(available, size) / size * size;
        return new Rectangle((getWidth() - side) / 2, HUD_HEIGHT, side, side);
    }

    // ---- drawing -----------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawHud(g2);
        drawBoard(g2);
        drawFooter(g2);
        g2.dispose();
    }

    private void drawHud(Graphics2D g2) {
        int best = game.getBestMoves(game.getSize());
        String bestText = best == Game.NONE ? "-" : String.valueOf(best);
        int third = getWidth() / 3;
        drawStat(g2, "MOVES", String.valueOf(game.getMoves()), third / 2);
        drawStat(g2, "TIME", Game.formatTime(game.getElapsedMillis()), third + third / 2);
        drawStat(g2, "BEST MOVES", bestText, 2 * third + third / 2);
    }

    private void drawStat(Graphics2D g2, String label, String value, int centerX) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.setColor(MUTED_TEXT_COLOR);
        drawCentered(g2, label, centerX, 28);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        g2.setColor(TEXT_COLOR);
        drawCentered(g2, value, centerX, 62);
    }

    private void drawBoard(Graphics2D g2) {
        PuzzleBoard board = game.getBoard();
        if (board == null) {
            return;
        }
        Rectangle area = boardArea();
        int n = board.getSize();
        int cell = area.width / n;

        g2.setColor(BOARD_COLOR);
        g2.fillRoundRect(area.x - 8, area.y - 8, area.width + 16, area.height + 16, 24, 24);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, cell / 2));
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                PuzzlePiece piece = board.getPiece(row, col);
                if (piece.isEmpty()) {
                    continue;
                }
                int x = area.x + col * cell + TILE_GAP / 2;
                int y = area.y + row * cell + TILE_GAP / 2;
                int w = cell - TILE_GAP;
                g2.setColor(piece.isInPlace(n) ? TILE_IN_PLACE_COLOR : TILE_COLOR);
                g2.fillRoundRect(x, y, w, w, 18, 18);
                g2.setColor(TEXT_COLOR);
                drawCentered(g2, String.valueOf(piece.getNumber()), x + w / 2, y + w / 2 + cell / 6);
            }
        }
    }

    private void drawFooter(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g2.setColor(MUTED_TEXT_COLOR);
        drawCentered(g2, "Click a tile next to the gap, or use the arrow keys",
                getWidth() / 2, getHeight() - FOOTER_HEIGHT / 2 + 4);
    }

    /** Draws text horizontally centered on centerX, with its baseline at baselineY. */
    private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }
}
