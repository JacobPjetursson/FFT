package kulibrat.ai.Minimax;

import misc.Config;

import java.util.Random;

public class Zobrist {
    public static long[][][] board;
    public static long[] turn;
    public static long[] redPoints;
    public static long[] blackPoints;

    static {
        Random r = new Random();
        long range = Long.MAX_VALUE;
        int seed = (Config.USE_AUTOGEN && Config.RANDOM_SEED) ? r.nextInt() : Config.ZOBRIST_SEED;
        r.setSeed(seed);
        System.out.println("Zobrist seed: " + seed);

        // board keys
        int rows = Config.kuliBHeight;
        int cols = Config.kuliBWidth;
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
        // points keys
        redPoints = new long[11];
        blackPoints = new long[11];
        for (int i = 0; i < 10; i++) {
            redPoints[i] = (long) (r.nextDouble() * range);
            blackPoints[i] = (long) (r.nextDouble() * range);
        }
    }
}
