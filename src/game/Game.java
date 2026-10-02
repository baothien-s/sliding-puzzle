package game;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Game logic with no GUI code: state, move counter, timer, win detection and best scores.
 * The clock is injectable so the timer can be tested without waiting.
 */
public class Game {

    public enum State { MENU, PLAYING, WON }

    /** Returned by the best-score getters when no game has been won yet. */
    public static final int NONE = -1;

    private final LongSupplier clock;
    private final Path scoreFile;
    private final Random random = new Random();
    private final Properties scores = new Properties();

    private PuzzleBoard board;
    private State state = State.MENU;
    private int size = 3;
    private int moves;
    private long startMillis;
    private long endMillis;
    private boolean newBestMoves;
    private boolean newBestTime;

    public Game() {
        this(System::currentTimeMillis,
                Path.of(System.getProperty("user.home"), ".sliding-puzzle-scores.properties"));
    }

    /** @param scoreFile where best scores are saved; null keeps them in memory only */
    public Game(LongSupplier clock, Path scoreFile) {
        this.clock = clock;
        this.scoreFile = scoreFile;
        loadScores();
    }

    // ---- starting and restarting ------------------------------------------------

    /** Starts a new shuffled game of the given size (3 = 3x3, 4 = 4x4). */
    public void start(int size) {
        PuzzleBoard fresh = new PuzzleBoard(size, random);
        fresh.shuffle();
        startWith(fresh);
    }

    /** Starts a game on a specific board (used by start() and by tests). */
    public void startWith(PuzzleBoard board) {
        this.board = board;
        this.size = board.getSize();
        this.moves = 0;
        this.newBestMoves = false;
        this.newBestTime = false;
        this.startMillis = clock.getAsLong();
        this.endMillis = startMillis;
        this.state = State.PLAYING;
    }

    /** New shuffle, same difficulty, counters reset. */
    public void restart() {
        start(size);
    }

    public void backToMenu() {
        state = State.MENU;
    }

    // ---- playing ---------------------------------------------------------------

    /** Tries to move the piece at (row, col). Returns true only if a legal move was made. */
    public boolean tryMove(int row, int col) {
        if (state != State.PLAYING || !board.move(row, col)) {
            return false;
        }
        moves++;
        if (board.isSolved()) {
            finish();
        }
        return true;
    }

    /**
     * Moves a piece in a direction, e.g. (-1, 0) slides the piece below the gap UP.
     * Used by the arrow keys.
     */
    public boolean tryMoveDirection(int rowDelta, int colDelta) {
        if (state != State.PLAYING) {
            return false;
        }
        return tryMove(board.getEmptyRow() - rowDelta, board.getEmptyCol() - colDelta);
    }

    private void finish() {
        endMillis = clock.getAsLong();
        state = State.WON;

        long elapsed = endMillis - startMillis;
        int bestMoves = getBestMoves(size);
        long bestTime = getBestTimeMillis(size);
        newBestMoves = bestMoves == NONE || moves < bestMoves;
        newBestTime = bestTime == NONE || elapsed < bestTime;
        if (newBestMoves) scores.setProperty("moves." + size, String.valueOf(moves));
        if (newBestTime)  scores.setProperty("time." + size, String.valueOf(elapsed));
        if (newBestMoves || newBestTime) saveScores();
    }

    // ---- information for the GUI -------------------------------------------------

    public PuzzleBoard getBoard() { return board; }
    public State getState()       { return state; }
    public int getSize()          { return size; }
    public int getMoves()         { return moves; }
    public boolean isNewBestMoves() { return newBestMoves; }
    public boolean isNewBestTime()  { return newBestTime; }

    /** Time since the game started; frozen at the moment the puzzle was solved. */
    public long getElapsedMillis() {
        switch (state) {
            case PLAYING: return clock.getAsLong() - startMillis;
            case WON:     return endMillis - startMillis;
            default:      return 0;
        }
    }

    public int getBestMoves(int size) {
        return (int) readScore("moves." + size);
    }

    public long getBestTimeMillis(int size) {
        return readScore("time." + size);
    }

    public static String formatTime(long millis) {
        long seconds = millis / 1000;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    // ---- best score storage ------------------------------------------------------

    private long readScore(String key) {
        String value = scores.getProperty(key);
        if (value == null) return NONE;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return NONE;
        }
    }

    private void loadScores() {
        if (scoreFile == null || !Files.exists(scoreFile)) return;
        try (InputStream in = Files.newInputStream(scoreFile)) {
            scores.load(in);
        } catch (IOException e) {
            System.err.println("Could not read best scores: " + e.getMessage());
        }
    }

    private void saveScores() {
        if (scoreFile == null) return;
        try (OutputStream out = Files.newOutputStream(scoreFile)) {
            scores.store(out, "Sliding Puzzle best scores");
        } catch (IOException e) {
            System.err.println("Could not save best scores: " + e.getMessage());
        }
    }
}
