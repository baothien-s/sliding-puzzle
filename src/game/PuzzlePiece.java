package game;

/**
 * One tile of the sliding puzzle. Number 0 is the empty space.
 * A piece always knows where it currently sits on the board.
 */
public class PuzzlePiece {

    public static final int EMPTY = 0;

    private final int number;
    private int row;
    private int col;

    public PuzzlePiece(int number, int row, int col) {
        this.number = number;
        this.row = row;
        this.col = col;
    }

    public int getNumber() { return number; }
    public int getRow()    { return row; }
    public int getCol()    { return col; }
    public boolean isEmpty() { return number == EMPTY; }

    /** Only the board moves pieces, so this is package-private. */
    void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** Row this piece belongs in once the puzzle is solved (empty space goes last). */
    public int getTargetRow(int size) {
        return isEmpty() ? size - 1 : (number - 1) / size;
    }

    /** Column this piece belongs in once the puzzle is solved. */
    public int getTargetCol(int size) {
        return isEmpty() ? size - 1 : (number - 1) % size;
    }

    public boolean isInPlace(int size) {
        return row == getTargetRow(size) && col == getTargetCol(size);
    }

    @Override
    public String toString() {
        return (isEmpty() ? "_" : String.valueOf(number)) + "@(" + row + "," + col + ")";
    }
}
