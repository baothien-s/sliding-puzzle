package game;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Entry point. Creates the window and switches between three screens:
 * main menu, the game itself, and the win screen.
 */
public class Main {

    private static final String MENU = "menu";
    private static final String PLAY = "play";
    private static final String WIN = "win";

    private final Game game = new Game();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JLabel winStats = new JLabel("", SwingConstants.CENTER);
    private final GamePanel gamePanel = new GamePanel(game, this::showWinScreen);
    private int selectedSize = 3;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // the default look and feel is fine
        }
        SwingUtilities.invokeLater(() -> new Main().createWindow());
    }

    private void createWindow() {
        root.add(buildMenuScreen(), MENU);
        root.add(buildPlayScreen(), PLAY);
        root.add(buildWinScreen(), WIN);

        JFrame frame = new JFrame("Sliding Puzzle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(root);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---- screens -----------------------------------------------------------------

    private JPanel buildMenuScreen() {
        JRadioButton easy = difficultyButton("Easy  3 \u00D7 3", 3, true);
        JRadioButton hard = difficultyButton("Hard  4 \u00D7 4", 4, false);
        ButtonGroup group = new ButtonGroup();
        group.add(easy);
        group.add(hard);
        JPanel difficulty = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        difficulty.setOpaque(false);
        difficulty.add(easy);
        difficulty.add(hard);

        return stack(
                label("Sliding Puzzle", 40, true),
                label("Slide the tiles into order", 16, false),
                difficulty,
                button("Start Game", this::startGame),
                button("Exit", () -> System.exit(0)));
    }

    private JPanel buildPlayScreen() {
        JButton restart = button("Restart", () -> {
            game.restart();
            gamePanel.startTicking();
            gamePanel.requestFocusInWindow();
        });
        JButton menu = button("Main Menu", this::showMenu);
        restart.setFocusable(false);
        menu.setFocusable(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttons.setBackground(GamePanel.BACKGROUND);
        buttons.add(restart);
        buttons.add(menu);

        JPanel screen = new JPanel(new BorderLayout());
        screen.add(gamePanel, BorderLayout.CENTER);
        screen.add(buttons, BorderLayout.SOUTH);
        return screen;
    }

    private JPanel buildWinScreen() {
        winStats.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        winStats.setForeground(Color.WHITE);
        return stack(
                label("Puzzle Solved!", 40, true),
                winStats,
                button("Play Again", this::startGame),
                button("Main Menu", this::showMenu),
                button("Exit", () -> System.exit(0)));
    }

    // ---- navigation ----------------------------------------------------------------

    private void startGame() {
        game.start(selectedSize);
        cards.show(root, PLAY);
        gamePanel.startTicking();
        gamePanel.requestFocusInWindow();
    }

    private void showMenu() {
        gamePanel.stopTicking();
        game.backToMenu();
        cards.show(root, MENU);
    }

    private void showWinScreen() {
        StringBuilder text = new StringBuilder("<html><div style='text-align:center'>");
        text.append("Moves: <b>").append(game.getMoves()).append("</b>")
            .append("&nbsp;&nbsp;&middot;&nbsp;&nbsp;")
            .append("Time: <b>").append(Game.formatTime(game.getElapsedMillis())).append("</b>");
        if (game.isNewBestMoves()) {
            text.append("<br><br>New best move count!");
        }
        if (game.isNewBestTime()) {
            text.append("<br>New best time!");
        }
        text.append("</div></html>");
        winStats.setText(text.toString());
        cards.show(root, WIN);
    }

    // ---- small UI helpers ------------------------------------------------------------

    /** Vertical, centered stack of components on the dark background. */
    private JPanel stack(JComponent... items) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(GamePanel.BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        for (JComponent item : items) {
            panel.add(item, gbc);
        }
        return panel;
    }

    private JLabel label(String text, int fontSize, boolean bold) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, fontSize));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        button.setPreferredSize(new Dimension(240, 46));
        button.addActionListener(e -> action.run());
        return button;
    }

    private JRadioButton difficultyButton(String text, int size, boolean selected) {
        JRadioButton radio = new JRadioButton(text, selected);
        radio.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        radio.setForeground(Color.WHITE);
        radio.setOpaque(false);
        radio.addActionListener(e -> selectedSize = size);
        return radio;
    }
}
