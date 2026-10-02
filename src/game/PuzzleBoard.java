package game;

import java.util.Random;

/**
 * The puzzle grid. Holds the pieces, shuffles them, validates and performs moves,
 * and knows whether the puzzle is solved.
 */
public class PuzzleBoard {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final int size;
    private final Random random;
    private final PuzzlePiece[][] grid;
    private int emptyRow;
    private int emptyCol;

    public PuzzleBoard(int size) {
        this(size, new Random());
    }

    public PuzzleBoard(int size, Random random) {
        if (size < 2) {
            throw new IllegalArgumentException("Board size must be at least 2");
        }
        this.size = size;
        this.random = random;
        this.grid = new PuzzlePiece[size][size];
        reset();
    }

    /** Builds a board from a row-major layout, 0 = empty. Mainly useful for tests. */
    public static PuzzleBoard fromLayout(int size, int[] layout) {
        if (layout.length != size * size) {
            throw new IllegalArgumentException("Layout must have " + (size * size) + " entries");
        }
        boolean[] seen = new boolean[size * size];
        for (int n : layout) {
            if (n < 0 || n >= seen.length || seen[n]) {
                throw new IllegalArgumentException("Layout must contain each of 0.." + (seen.length - 1) + " once");
            }
            seen[n] = true;
        }
        PuzzleBoard board = new PuzzleBoard(size);
        for (int i = 0; i < layout.length; i++) {
            int r = i / size, c = i % size;
            board.grid[r][c] = new PuzzlePiece(layout[i], r, c);
            if (layout[i] == PuzzlePiece.EMPTY) {
                board.emptyRow = r;
                board.emptyCol = c;
            }
        }
        return board;
    }

    /** Puts the pieces in solved order: 1, 2, 3 ... with the empty space bottom-right. */
    public void reset() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int number = r * size + c + 1;
                if (number == size * size) {
                    number = PuzzlePiece.EMPTY;
                }
                grid[r][c] = new PuzzlePiece(number, r, c);
            }
        }
        emptyRow = size - 1;
        emptyCol = size - 1;
    }

    /**
     * Shuffles by making many random LEGAL moves starting from the solved state.
     * This guarantees the result is always solvable (random placement would be
     * unsolvable half the time). Never returns an already-solved board.
     */
    public void shuffle() {
        do {
            reset();
            int lastRow = -1, lastCol = -1;
            for (int i = 0; i < size * size * 30; i++) {
                int[] d = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                int r = emptyRow + d[0], c = emptyCol + d[1];
                boolean undoesLastMove = (r == lastRow && c == lastCol);
                if (!inBounds(r, c) || undoesLastMove) {
                    continue;
                }
                lastRow = emptyRow;
                lastCol = emptyCol;
                swapWithEmpty(r, c);
            }
        } while (isSolved());
    }

    /** A piece may move only if it is directly above, below, left or right of the empty space. */
    public boolean canMove(int row, int col) {
        return inBounds(row, col)
                && Math.abs(row - emptyRow) + Math.abs(col - emptyCol) == 1;
    }

    /** Moves the piece at (row, col) into the empty space. Returns false for illegal moves. */
    public boolean move(int row, int col) {
        if (!canMove(row, col)) {
            return false;
        }
        swapWithEmpty(row, col);
        return true;
    }

    public boolean isSolved() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (!grid[r][c].isInPlace(size)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Standard inversion-count test: can this arrangement be solved with legal moves? */
    public boolean isSolvable() {
        int[] flat = toLayout();
        int inversions = 0;
        for (int i = 0; i < flat.length; i++) {
            for (int j = i + 1; j < flat.length; j++) {
                if (flat[i] != 0 && flat[j] != 0 && flat[i] > flat[j]) {
                    inversions++;
                }
            }
        }
        if (size % 2 == 1) {
            return inversions % 2 == 0;
        }
        int emptyRowFromBottom = size - emptyRow; // 1 = bottom row
        return (inversions + emptyRowFromBottom) % 2 == 1;
    }

    public int[] toLayout() {
        int[] layout = new int[size * size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                layout[r * size + c] = grid[r][c].getNumber();
            }
        }
        return layout;
    }

    public PuzzlePiece getPiece(int row, int col) { return grid[row][col]; }
    public int getSize()     { return size; }
    public int getEmptyRow() { return emptyRow; }
    public int getEmptyCol() { return emptyCol; }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    private void swapWithEmpty(int row, int col) {
        PuzzlePiece piece = grid[row][col];
        PuzzlePiece empty = grid[emptyRow][emptyCol];
        grid[emptyRow][emptyCol] = piece;
        grid[row][col] = empty;
        piece.setPosition(emptyRow, emptyCol);
        empty.setPosition(row, col);
        emptyRow = row;
        emptyCol = col;
    }
}
