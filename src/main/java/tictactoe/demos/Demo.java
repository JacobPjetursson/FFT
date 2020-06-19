package tictactoe.demos;

import fftlib.FFTManager;
import fftlib.game.FFTSolver;
import fftlib.logic.Action;
import fftlib.logic.Rule;
import fftlib.logic.RuleList;
import tictactoe.FFT.GameSpecifics;
import tictactoe.game.Node;

import java.util.ArrayList;

public class Demo {

    public static void main(String[] args) {
        GameSpecifics gs = new GameSpecifics();
        FFTManager.initialize(gs);
        FFTSolver.solveGame();

        Node test1 = new Node();
        Node test2 = new Node(new int[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 1}}, 2);
        Node test3 = new Node(new int[][] {{0, 0, 0}, {0, 0, 0}, {0, 2, 1}}, 1);
        Node test4 = new Node(new int[][] {{1, 0, 0}, {0, 0, 0}, {0, 2, 1}}, 2);
        Node test5 = new Node(new int[][] {{1, 0, 2}, {0, 0, 0}, {0, 2, 1}}, 1);

        Rule r1 = new Rule(test1.convert().getAll(), new Action("P1(0, 0)"));
        Rule r2 = new Rule(test2.convert().getAll(), new Action("P1(0, 1)"));
        Rule r3 = new Rule(test3.convert().getAll(), new Action("P1(0, 2)"));
        Rule r4 = new Rule(test4.convert().getAll(), new Action("P1(1, 0)"));
        Rule r5 = new Rule(test5.convert().getAll(), new Action("P1(1, 1)"));
        Rule r6 = new Rule("P1(1, 2) AND !P2(2, 0) AND !P2(0, 1)", "+P1(1, 1)");
        Rule r7 = new Rule("!P1(1, 2) AND P2(2, 0) AND P2(0, 1)", "+P1(1, 1)");
        Rule r8 = new Rule("!P2(0, 1)", "+P1(1, 1)");
        Rule r9 = new Rule("P2(0, 1)", "+P1(1, 1)");
        Rule r10 = new Rule("!P2(2, 0) AND !P2(0, 1)", "+P1(1, 1)");
        RuleList ruleList = new RuleList();
        ruleList.sortedAdd(r1);
        ruleList.sortedAdd(r2);
        ruleList.sortedAdd(r3);
        ruleList.sortedAdd(r4);
        ruleList.sortedAdd(r5);
        ruleList.sortedAdd(r6);
        ruleList.sortedAdd(r7);
        ruleList.sortedAdd(r8);
        ruleList.sortedAdd(r9);
        ruleList.sortedAdd(r10);

        ArrayList<Rule> arrayList = new ArrayList<>();
        arrayList.add(r1);
        arrayList.add(r2);
        arrayList.add(r3);
        arrayList.add(r4);
        arrayList.add(r5);
        arrayList.add(r6);
        arrayList.add(r7);
        arrayList.add(r8);
        arrayList.add(r9);
        arrayList.add(r10);
        //arrayList.sort(new RuleList.RuleComparator());

        for (int i = 0; i < arrayList.size(); i++) {
            System.out.println(arrayList.get(i));
            System.out.println(ruleList.get(i));
            System.out.println();
        }

        ruleList.sortedRemove(r2);
        ruleList.sortedRemove(r5);
        System.out.println(ruleList.size());
        for (int i = 0; i < ruleList.size(); i++) {
            System.out.println(arrayList.get(i));
            System.out.println(ruleList.get(i));
            System.out.println();
        }
/*
        TreeMap<Long, Node> codes = new TreeMap<>();
        Node n = new Node();
        codes.put(n.convert().getBitString(), n);
        n = n.getNextNode(new Move(1, 1, 1));
        codes.put(n.convert().getBitString(), n);
        n = n.getNextNode(new Move(0, 0, 2));
        codes.put(n.convert().getBitString(), n);
        n = n.getNextNode(new Move(2, 2, 1));
        codes.put(n.convert().getBitString(), n);

        n = new Node();
        n = n.getNextNode(new Move(2, 2, 1));
        codes.put(n.convert().getBitString(), n);

        for (Map.Entry<Long, Node> entry : codes.entrySet()) {
            System.out.println("Node: " + entry.getValue() + " , with code: " + entry.getKey());
        }

        FFTNode highest_code_node = codes.pollLastEntry().getValue();
        System.out.println("highest code node: " + highest_code_node.convert().getBitString());
        Rule highest_code_rule = new Rule(highest_code_node.convert(), new Action("P2(2, 0)"));
        System.out.println("highest code rule: " + highest_code_rule.getAllPreconditions().getBitString());

        TreeMap<Integer, Literal> literalSet = new TreeMap<>();
        for (Literal l : highest_code_rule.getPreconditions()) {
            literalSet.put(l.id, l);
        }
        System.out.println("Literals in highest code rule:");
        for (Map.Entry<Integer, Literal> entry : literalSet.entrySet()) {
            System.out.println("Literal: " + entry.getValue() + " , with key: " + (1 << entry.getKey()));
        }

 */



        // Make strategy with meta rules
        //ArrayList<FFT> ffts = FFTManager.load("FFTs/tictactoe_meta_fft.txt");
        //FFT fft = FFTManager.autogenFFT(ffts.get(0));
    }
}

