package tictactoe.game;

import fftlib.Literal;
import fftlib.game.FFTMove;
import fftlib.game.FFTState;

import java.util.*;

import static fftlib.game.Transform.*;
import static misc.Config.PLAYER1;
import static misc.Config.PLAYER_ANY;


public class State implements FFTState {
    private int[][] board;
    private int turn;
    private ArrayList<Move> legalMoves;
    private Move move;
    private static int[] transformations = {TRANS_HREF, TRANS_VREF, TRANS_ROT};

    // Starting state
    public State() {
        board = new int[3][3];
        turn = PLAYER1;
    }

    // Duplicate constructor
    public State(State state) {
        board = new int[state.board.length][];
        for (int i = 0; i < state.board.length; i++) {
            board[i] = Arrays.copyOf(state.board[i], state.board[i].length);
        }
        turn = state.turn;
        move = state.move;
    }

    // Non-root state
    private State(State parent, Move m) {
        this(parent);
        Logic.doTurn(m, this);
        this.move = m;
    }

    @Override
    public State getNextState(FFTMove move) {
        return getNextState((Move) move);
    }

    public State getNextState(Move m) {
        return new State(this, m);
    }

    public ArrayList<State> getChildren() {
        ArrayList<State> children = new ArrayList<>();
        for (Move m : getLegalMoves()) {
            State child = new State(this, m);
            children.add(child);
        }
        return children;
    }

    public int[][] getBoard() {
        return board;
    }

    public void setBoardEntry(int row, int col, int team) {
        board[row][col] = team;
    }

    public Move getMove() {
        return move;
    }

    public void setMove(Move move) {
        this.move = move;
    }

    public HashSet<Literal> getLiterals() {
        HashSet<Literal> literals = new HashSet<>();

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                int pieceOcc = board[i][j];
                if (pieceOcc > 0) {
                    if (turn == PLAYER1)
                        literals.add(new Literal(i, j, pieceOcc, false));
                    else {
                        pieceOcc = (pieceOcc == 1) ? 2 : 1;
                        literals.add(new Literal(i, j, pieceOcc, false));
                    }
                }
            }
        }
        return literals;
    }

    public HashSet<Literal> getAllLiterals() { // Including negatives
        HashSet<Literal> literals = new HashSet<>();

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                int pieceOcc = board[i][j];
                if (pieceOcc > 0) {
                    if (turn == PLAYER1)
                        literals.add(new Literal(i, j, pieceOcc, false));
                    else {
                        pieceOcc = (pieceOcc == 1) ? 2 : 1;
                        literals.add(new Literal(i, j, pieceOcc, false));
                    }
                } else
                    literals.add(new Literal(i, j, PLAYER_ANY, true));
            }
        }
        return literals;
    }

    public String print() {
        return Arrays.deepToString(board) + " , TEAM: " + turn;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    // Creates and/or returns a list of new state objects which correspond to the children of the given state.
    public ArrayList<Move> getLegalMoves() {
        if (legalMoves != null) return legalMoves;
        legalMoves = Logic.legalMoves(turn, this);
        return legalMoves;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof State)) return false;

        State state = (State) obj;
        return this == state ||
                Arrays.deepEquals(this.board, state.board);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.deepHashCode(board);
    }

}
