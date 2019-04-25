package tictactoe.ai;

import misc.Config;

import java.util.Random;

public class Zobrist {
    public static long[][][] board;
    public static long[] turn;

    static {
        long range = Long.MAX_VALUE;
        Random r = new Random();
        int seed = (Config.RANDOM_ZOBRIST_SEED) ? r.nextInt() : Config.ZOBRIST_SEED;
        System.out.println("Zobrist seed: " + seed);
        r.setSeed(seed);
        // board keys
        int rows = 3;
        int cols = 3;
        int unique_pieces = 3;
        board = new long[rows][cols][unique_pieces];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                for (int k = 0; k < board[i][j].length; k++) {
                    long rLong = (long) (r.nextDouble() * range);
                    board[i][j][k] = rLong;
                }
            }
        }
        // turn keys
        turn = new long[3];
        for (int i = 0; i < turn.length; i++) {
            turn[i] = (long) (r.nextDouble() * range);
        }
    }
}
