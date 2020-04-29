package fftlib.auxiliary;

import fftlib.*;
import fftlib.game.LiteralSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static fftlib.FFTManager.*;

public class Transform {

    // LEVEL OF SYMMETRIES
    public static final int TRANS_HREF = 1;
    public static final int TRANS_VREF = 2;
    public static final int TRANS_ROT = 3;


    // reflect horizontally, with mirror along y-axis
    private static int[][] reflectH(int[][] board) {
        int[][] refH = new int[board.length][board[0].length];
        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                refH[i][j] = board[i][board[i].length - 1 - j];
        return refH;
    }
    // reflect vertically, with mirror along x-axis
    private static int[][] reflectV(int[][] board) {
        int[][] refV = new int[board.length][board[0].length];
        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                refV[i][j] = board[board.length - 1 - i][j];
        return refV;
    }

    // Rotate 90 degrees clockwise
    private static int[][] rotate(int[][] board) {
        int[][] rot = new int[board[0].length][board.length];
        for (int i = 0; i < board[0].length; ++i)
            for (int j = 0; j < board.length; ++j)
                rot[i][j] = board[board.length - j - 1][i];
        return rot;
    }

    public static HashSet<SymmetryRule> getSymmetryRules(int[] transformations, Rule rule) {
        boolean rotate = false;
        boolean refH = false;
        boolean refV = false;
        for (int transformation : transformations) {
            if (transformation == TRANS_ROT)
                rotate = true;
            else if (transformation == TRANS_HREF)
                refH = true;
            else if (transformation == TRANS_VREF)
                refV = true;
        }
        HashSet<SymmetryRule> symmetryRules = new HashSet<>();
        int [][] lBoard;
        Literal transformed;
        for (ArrayList<Integer> trans : getAllTransformations(refH, refV, rotate)) {
            LiteralSet transformedSet = new LiteralSet();
            for (Literal l : rule.getPreconditions()) {
                lBoard = literalToBoard(l);
                lBoard = apply(trans, lBoard);
                transformed = boardToLiteral(lBoard);
                transformedSet.add(transformed);
            }
            Action action = new Action();
            for (Literal l : rule.getAction().adds) {
                lBoard = literalToBoard(l);
                lBoard = apply(trans, lBoard);
                transformed = boardToLiteral(lBoard);
                action.adds.add(transformed);
            }
            for (Literal l : rule.getAction().rems) {
                lBoard = literalToBoard(l);
                lBoard = apply(trans, lBoard);
                transformed = boardToLiteral(lBoard);
                action.rems.add(transformed);
            }
            symmetryRules.add(new SymmetryRule(transformedSet, action));
        }

        return symmetryRules;
    }

    private static ArrayList<ArrayList<Integer>> getAllTransformations(
            boolean refH, boolean refV, boolean rotate) {
        ArrayList<Integer> transforms = new ArrayList<>();
        ArrayList<ArrayList<Integer>> allTransformations = new ArrayList<>(
                reflectTransforms(refH, refV, transforms));


        if (rotate) {
            // Rotate 3 times
            for (int i = 0; i < 3; i++) {
                transforms.add(TRANS_ROT);
                allTransformations.addAll(reflectTransforms(refH, refV, transforms));
            }
        }
        return allTransformations;
    }

    private static ArrayList<ArrayList<Integer>> reflectTransforms(
            boolean refH, boolean refV,  ArrayList<Integer> currentTransforms) {
        ArrayList<ArrayList<Integer>> allReflectTransforms = new ArrayList<>();
        ArrayList<Integer> transforms;
        allReflectTransforms.add(currentTransforms);
        if (refH) {
            transforms = new ArrayList<>(currentTransforms);
            transforms.add(TRANS_HREF);
            allReflectTransforms.add(transforms);
        }
        if (refV) {
            transforms = new ArrayList<>(currentTransforms);
            transforms.add(TRANS_VREF);
            allReflectTransforms.add(transforms);
        }
        if (refH && refV) {
            transforms = new ArrayList<>(currentTransforms);
            transforms.add(TRANS_HREF);
            transforms.add(TRANS_VREF);
            allReflectTransforms.add(transforms);
        }
        return allReflectTransforms;
    }

    public static int[][] apply(ArrayList<Integer> transformations, int[][] board) {
        int[][] copy = copyArray(board);
        for (int transformation : transformations) {
            switch (transformation) {
                case TRANS_HREF:
                    copy = reflectH(copy);
                    break;
                case TRANS_VREF:
                    copy = reflectV(copy);
                    break;
                case TRANS_ROT:
                    copy = rotate(copy);
                    break;
            }
        }
        return copy;
    }

    // Returns a board with the literals on it, the value equals to the piece occ.
    public static int[][] literalToBoard(Literal l) {
        int height = FFTManager.gameBoardHeight;
        int width = FFTManager.gameBoardWidth;
        int[][] preconBoard = new int[height][width];

        Position pos = getPosFromId.apply(l.id);
        if (pos != null) {
            preconBoard[pos.row][pos.col] = l.negated ? -pos.occ : pos.occ;
        }

        return preconBoard;
    }

    // returns preconditions derived from transformed integer matrix and non-boardplacement literals
    private static Literal boardToLiteral(int[][] board) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                Position pos = new Position(i, j, board[i][j]);
                if (pos.occ < 0) {
                    pos.occ = Math.abs(pos.occ);
                    return new Literal(getIdFromPos.apply(pos), true);
                }
                else if (pos.occ > 0)
                    return new Literal(getIdFromPos.apply(pos), false);
            }
        }
        return null;
    }

    public static int[][] copyArray(int[][] arr) {
        int[][] copy = new int[arr.length][arr[0].length];
        for (int i = 0; i < arr.length; i++)
            copy[i] = Arrays.copyOf(arr[i], arr[i].length);
        return copy;
    }

    public static HashSet<SymmetryRule> findAutomorphisms(Rule rule) {
        int [] vertices = new int[gameBoardHeight];
        for (int i = 0; i < gameBoardHeight; i++) {
            vertices[i] = i;
        }
        ArrayList<int[]> permutations = findPermutations(vertices);
        HashSet<SymmetryRule> transformations = new HashSet<>();
        for(int[] arr : permutations) {

            LiteralSet precons = new LiteralSet();
            Action action = null;
            for (Literal lit : rule.getAction().adds) {
                Position pos = getPosFromId.apply(lit.id);
                int n1 = arr[pos.row];
                int n2 = arr[pos.col];
                if (Math.abs(n1) > Math.abs(n2)) { // enforce lowest number first
                    int temp = n1;
                    n1 = n2;
                    n2 = temp;
                }
                Position newPos = new Position(n1, n2, pos.occ);
                action = new Action(getIdFromPos.apply(newPos));
            }
            for (Literal lit : rule.getPreconditions()) {
                Position pos = getPosFromId.apply(lit.id);
                int n1 = arr[pos.row];
                int n2 = arr[pos.col];
                if (Math.abs(n1) > Math.abs(n2)) { // enforce lowest number first
                    int temp = n1;
                    n1 = n2;
                    n2 = temp;
                }
                Position newPos = new Position(n1, n2, pos.occ);
                precons.add(new Literal(getIdFromPos.apply(newPos), lit.negated));
            }
            transformations.add(new SymmetryRule(precons, action));
        }
        return transformations;
    }

    public static ArrayList<int[]> findPermutations(int[] elements) {
        int[] indexes = new int[elements.length];
        ArrayList<int[]> permutations = new ArrayList<>();
        permutations.add(elements.clone());

        int i = 0;
        while (i < elements.length) {
            if (indexes[i] < i) {
                swap(elements, i % 2 == 0 ?  0: indexes[i], i);
                permutations.add(elements.clone());
                indexes[i]++;
                i = 0;
            }
            else {
                indexes[i] = 0;
                i++;
            }
        }
        return permutations;
    }

    private static void swap(int[] input, int a, int b) {
        int tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }

}
