package game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Dependency-free tests (no JUnit needed). Run with:
 *   java -cp out game.GameTests
 * Exits with a non-zero status if any check fails.
 */
public class GameTests {

    private static int passed = 0;
    private static int failed = 0;

    // A fake clock lets the timer tests run instantly instead of sleeping.
    private static final long[] now = {0};
    private static final LongSupplier CLOCK = () -> now[0];

    public static void main(String[] args) throws Exception {
        run("Puzzle generation", GameTests::testGeneration);
        run("Shuffle", GameTests::testShuffle);
        run("Movement", GameTests::testMovement);
        run("Illegal movement", GameTests::testIllegalMovement);
        run("Win detection", GameTests::testWinDetection);
        run("Restart", GameTests::testRestart);
        run("Timer", GameTests::testTimer);
        run("Multiple games", GameTests::testMultipleGames);
        run("Best scores", GameTests::testBestScores);

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---- tests -------------------------------------------------------------------

    static void testGeneration() {
        PuzzleBoard board = new PuzzleBoard(3);
        check("3x3 board has 9 cells", board.toLayout().length == 9);
        check("3x3 board holds pieces 1-8 and one empty space", isPermutation(board.toLayout()));
        check("new board starts solved", board.isSolved());
        check("empty space starts bottom-right", board.getEmptyRow() == 2 && board.getEmptyCol() == 2);
        check("exactly one empty piece", countEmpty(board) == 1);
        check("4x4 board holds pieces 1-15 and one empty space", isPermutation(new PuzzleBoard(4).toLayout()));
        check("size below 2 is rejected", throwsIllegalArgument(() -> new PuzzleBoard(1)));
        check("invalid layout is rejected",
                throwsIllegalArgument(() -> PuzzleBoard.fromLayout(3, new int[]{1, 1, 2, 3, 4, 5, 6, 7, 0})));
    }

    static void testShuffle() {
        Random random = new Random(42);
        Set<String> distinct = new HashSet<>();
        boolean allPermutations = true, noneSolved = true, allSolvable = true, positionsConsistent = true;

        for (int i = 0; i < 200; i++) {
            PuzzleBoard board = new PuzzleBoard(i % 2 == 0 ? 3 : 4, random);
            board.shuffle();
            allPermutations &= isPermutation(board.toLayout());
            noneSolved &= !board.isSolved();
            allSolvable &= board.isSolvable();
            positionsConsistent &= piecePositionsMatchGrid(board);
            distinct.add(Arrays.toString(board.toLayout()));
        }
        check("shuffled boards still contain every piece once", allPermutations);
        check("shuffle never leaves the puzzle solved", noneSolved);
        check("every shuffled board is solvable", allSolvable);
        check("pieces know their own position after shuffling", positionsConsistent);
        check("shuffles are different from each other", distinct.size() > 150);

        PuzzleBoard unsolvable = PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 8, 7, 0});
        check("isSolvable detects an unsolvable board", !unsolvable.isSolvable());
    }

    static void testMovement() {
        PuzzleBoard board = PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 0});
        check("piece above the gap can move", board.canMove(1, 2));
        check("piece left of the gap can move", board.canMove(2, 1));

        check("legal move succeeds", board.move(1, 2));
        check("piece moved into the gap", Arrays.equals(board.toLayout(), new int[]{1, 2, 3, 4, 5, 0, 7, 8, 6}));
        check("moved piece knows its new position",
                board.getPiece(2, 2).getNumber() == 6 && board.getPiece(2, 2).getRow() == 2);
        check("gap moved to where the piece was", board.getEmptyRow() == 1 && board.getEmptyCol() == 2);
        check("board is no longer solved", !board.isSolved());

        board.move(2, 2);
        check("moving the piece back restores the solved board", board.isSolved());
    }

    static void testIllegalMovement() {
        PuzzleBoard board = PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 0});
        int[] before = board.toLayout();
        check("diagonal move rejected", !board.move(1, 1));
        check("distant piece rejected", !board.move(0, 0));
        check("clicking the empty space rejected", !board.move(2, 2));
        check("out-of-bounds rejected", !board.move(-1, 0) && !board.move(3, 0) && !board.move(0, 3));
        check("illegal moves leave the board unchanged", Arrays.equals(before, board.toLayout()));

        Game game = new Game(CLOCK, null);
        game.startWith(PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 0, 6, 7, 5, 8}));
        check("illegal move does not count", !game.tryMove(0, 0) && game.getMoves() == 0);
        check("legal move counts", game.tryMove(0, 1) && game.getMoves() == 1);
        check("another illegal move still does not count", !game.tryMove(2, 2) && game.getMoves() == 1);
    }

    static void testWinDetection() {
        now[0] = 0;
        Game game = new Game(CLOCK, null);
        game.startWith(PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 0, 7, 8}));
        check("game starts in PLAYING state", game.getState() == Game.State.PLAYING);

        game.tryMove(2, 1);
        check("not won halfway through", game.getState() == Game.State.PLAYING);
        game.tryMove(2, 2);
        check("won when the last piece slides into place", game.getState() == Game.State.WON);
        check("both moves were counted", game.getMoves() == 2);

        check("moves are rejected after winning", !game.tryMove(2, 1) && game.getMoves() == 2);
        check("arrow-key moves are rejected after winning", !game.tryMoveDirection(0, 1));

        Game game4 = new Game(CLOCK, null);
        game4.startWith(PuzzleBoard.fromLayout(4,
                new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0, 15}));
        game4.tryMove(3, 3);
        check("win is detected on a 4x4 board too", game4.getState() == Game.State.WON);
    }

    static void testRestart() {
        now[0] = 0;
        Game game = new Game(CLOCK, null);
        game.start(4);
        anyLegalMove(game);
        now[0] = 7000;
        check("moves and time accumulated before restart", game.getMoves() == 1 && game.getElapsedMillis() == 7000);

        game.restart();
        check("restart resets move counter", game.getMoves() == 0);
        check("restart resets timer", game.getElapsedMillis() == 0);
        check("restart keeps the difficulty", game.getSize() == 4 && game.getBoard().getSize() == 4);
        check("restart gives a fresh unsolved board", !game.getBoard().isSolved());
        check("restart puts the game in PLAYING state", game.getState() == Game.State.PLAYING);

        game.startWith(PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 7, 0, 8}));
        game.tryMove(2, 2);
        check("game is won", game.getState() == Game.State.WON);
        game.restart();
        check("restart works after winning (Play Again)",
                game.getState() == Game.State.PLAYING && game.getMoves() == 0);
    }

    static void testTimer() {
        now[0] = 1000;
        Game game = new Game(CLOCK, null);
        game.startWith(PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 7, 0, 8}));
        check("timer starts at zero", game.getElapsedMillis() == 0);

        now[0] = 6500;
        check("timer counts elapsed time", game.getElapsedMillis() == 5500);

        game.tryMove(2, 2);
        now[0] = 20000;
        check("timer stops when the puzzle is solved", game.getElapsedMillis() == 5500);

        check("formatTime 5.5s", Game.formatTime(5500).equals("0:05"));
        check("formatTime 65s", Game.formatTime(65000).equals("1:05"));
        check("formatTime 10min", Game.formatTime(600000).equals("10:00"));
    }

    static void testMultipleGames() {
        now[0] = 0;
        Game game = new Game(CLOCK, null);
        boolean allValid = true;
        for (int i = 0; i < 50; i++) {
            int size = (i % 2 == 0) ? 3 : 4;
            game.start(size);
            PuzzleBoard board = game.getBoard();
            allValid &= board.getSize() == size
                    && isPermutation(board.toLayout())
                    && !board.isSolved()
                    && board.isSolvable()
                    && game.getMoves() == 0
                    && game.getState() == Game.State.PLAYING;
        }
        check("50 consecutive games of mixed sizes are all valid", allValid);

        game.startWith(PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 7, 0, 8}));
        game.tryMove(2, 2);
        game.start(3);
        check("a new game after a win starts clean",
                game.getState() == Game.State.PLAYING && game.getMoves() == 0
                        && !game.isNewBestMoves() && !game.isNewBestTime());
    }

    static void testBestScores() throws Exception {
        now[0] = 0;
        Game game = new Game(CLOCK, null);
        check("no best score before the first win", game.getBestMoves(3) == Game.NONE);

        playOneMoveWin(game, 4000);
        check("first win sets best moves and time", game.getBestMoves(3) == 1 && game.getBestTimeMillis(3) == 4000);
        check("first win is flagged as a record", game.isNewBestMoves() && game.isNewBestTime());

        playTwoMoveWin(game, 9000);
        check("a worse game does not replace the best", game.getBestMoves(3) == 1 && game.getBestTimeMillis(3) == 4000);
        check("a worse game is not flagged as a record", !game.isNewBestMoves() && !game.isNewBestTime());

        playOneMoveWin(game, 2000);
        check("a faster game improves the best time", game.getBestTimeMillis(3) == 2000 && game.isNewBestTime());
        check("same move count is not a new best", !game.isNewBestMoves());
        check("best scores are tracked per difficulty", game.getBestMoves(4) == Game.NONE);

        Path file = Files.createTempFile("puzzle-scores", ".properties");
        try {
            now[0] = 0;
            playOneMoveWin(new Game(CLOCK, file), 3000);
            Game reloaded = new Game(CLOCK, file);
            check("best scores survive restarting the app",
                    reloaded.getBestMoves(3) == 1 && reloaded.getBestTimeMillis(3) == 3000);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    // ---- helpers -----------------------------------------------------------------

    private static void playOneMoveWin(Game game, long durationMillis) {
        now[0] = 0;
        game.startWith(PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 7, 0, 8}));
        now[0] = durationMillis;
        game.tryMove(2, 2);
    }

    private static void playTwoMoveWin(Game game, long durationMillis) {
        now[0] = 0;
        game.startWith(PuzzleBoard.fromLayout(3, new int[]{1, 2, 3, 4, 5, 6, 0, 7, 8}));
        game.tryMove(2, 1);
        now[0] = durationMillis;
        game.tryMove(2, 2);
    }

    private static boolean anyLegalMove(Game game) {
        return game.tryMoveDirection(-1, 0) || game.tryMoveDirection(1, 0)
                || game.tryMoveDirection(0, -1) || game.tryMoveDirection(0, 1);
    }

    /** True if the layout contains each of 0..n*n-1 exactly once. */
    private static boolean isPermutation(int[] layout) {
        boolean[] seen = new boolean[layout.length];
        for (int n : layout) {
            if (n < 0 || n >= layout.length || seen[n]) return false;
            seen[n] = true;
        }
        return true;
    }

    private static int countEmpty(PuzzleBoard board) {
        int count = 0;
        for (int n : board.toLayout()) if (n == 0) count++;
        return count;
    }

    private static boolean piecePositionsMatchGrid(PuzzleBoard board) {
        for (int r = 0; r < board.getSize(); r++) {
            for (int c = 0; c < board.getSize(); c++) {
                PuzzlePiece p = board.getPiece(r, c);
                if (p.getRow() != r || p.getCol() != c) return false;
                if (p.isEmpty() && (board.getEmptyRow() != r || board.getEmptyCol() != c)) return false;
            }
        }
        return true;
    }

    private static boolean throwsIllegalArgument(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void run(String name, ThrowingRunnable test) {
        System.out.println(name);
        try {
            test.run();
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL  threw " + e);
        }
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + description);
        } else {
            failed++;
            System.out.println("  FAIL  " + description);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
