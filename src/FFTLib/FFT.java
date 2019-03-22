package fftlib;

import fftlib.game.*;
import misc.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import static misc.Config.PLAYER2;


public class FFT {
    public String name;
    public ArrayList<RuleGroup> ruleGroups;
    public FFTStateAndMove failingPoint = null;

    public FFT(String name) {
        this.name = name;
        ruleGroups = new ArrayList<>();
    }

    public void addRuleGroup(RuleGroup ruleGroup) {
        ruleGroups.add(ruleGroup);
        FFTManager.save();
    }

    public boolean verify(int team, boolean wholeFFT) {
        FFTState initialState = FFTManager.initialFFTState;
        LinkedList<FFTState> frontier = new LinkedList<>();
        HashSet<FFTState> closedSet = new HashSet<>();
        frontier.add(initialState);
        int opponent = (team == Config.PLAYER1) ? PLAYER2 : Config.PLAYER1;
        // Check if win or draw is even possible
        int score = FFTManager.db.queryPlay(initialState).getScore();
        if (team == Config.PLAYER1 && score < -1000) {
            System.out.println("A perfect player 2 has won from start of the game");
            return false;
        } else if (team == PLAYER2 && score > 1000) {
            System.out.println("A perfect player 1 has won from the start of the game");
            return false;
        }

        while (!frontier.isEmpty()) {
            FFTState state = frontier.pop();
            if (FFTManager.logic.gameOver(state)) {
                if (FFTManager.logic.getWinner(state) == opponent) {
                    // Should not hit this given initial check
                    System.out.println("No chance of winning vs. perfect player");
                    return false;
                }
            } else if (team != state.getTurn()) {
                for (FFTState child : state.getChildren())
                    if (!closedSet.contains(child)) {
                        closedSet.add(child);
                        frontier.add(child);
                    }
            } else {
                FFTMove move = apply(state);
                ArrayList<? extends FFTMove> nonLosingPlays = FFTManager.db.nonLosingPlays(state);
                // If move is null, check that all possible (random) moves are ok
                if (move == null) {
                    for (FFTMove m : state.getLegalMoves()) {
                        if (nonLosingPlays.contains(m)) {
                            FFTState nextState = state.getNextState(m);
                            if (!closedSet.contains(nextState)) {
                                closedSet.add(nextState);
                                frontier.add(nextState);
                            }
                        } else if (wholeFFT) {
                            System.out.println("FFT did not apply to certain state, random move lost you the game");
                            failingPoint = new FFTStateAndMove(state, m, true);
                            return false;
                        }
                    }
                } else if (!nonLosingPlays.contains(move)) {
                    System.out.println("FFT applied, but its move lost you the game");
                    failingPoint = new FFTStateAndMove(state, move, false);
                    return false;
                } else {
                    FFTState nextNode = state.getNextState(move);
                    if (!closedSet.contains(nextNode)) {
                        closedSet.add(nextNode);
                        frontier.add(nextNode);
                    }
                }
            }
        }
        return true;
    }

    public FFTMove apply(FFTState state) {
        for (RuleGroup ruleGroup : ruleGroups) {
            for (Rule rule : ruleGroup.rules) {
                    FFTMove move = rule.apply(state);
                    if (move != null) {
                        return move;
                    }
            }
        }
        return null;
    }
}
