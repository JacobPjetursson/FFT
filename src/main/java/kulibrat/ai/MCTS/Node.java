package kulibrat.ai.MCTS;

import kulibrat.game.Logic;
import kulibrat.game.Move;

import java.util.ArrayList;

class Node {
    private kulibrat.game.Node node;
    private ArrayList<Node> children;
    // UCB stuff
    private double plays = 0;
    private double wins = 0;
    private Node parent;

    // Starting Root state
    public Node(kulibrat.game.Node startState) {
        this.node = new kulibrat.game.Node(startState);
    }

    // Non-root state
    private Node(Node parent, Move m) {
        this.node = new kulibrat.game.Node(parent.getState());
        this.parent = parent;
        Logic.doTurn(m, this.node);
        this.node.setMove(m);
    }

    // Duplicate constructor
    private Node(Node node) {
        this.node = new kulibrat.game.Node(node.getState());
    }

    Node getNextNode(Move m) {
        if (children == null) {
            Node node = new Node(this);
            Logic.doTurn(m, node.getState());
            node.getState().setMove(m);
            return node;
        }
        for (Node child : children) {
            if (child.getState().getMove().equals(m)) return child;
        }
        return null;
    }

    public kulibrat.game.Node getState() {
        return node;
    }

    double getPlays() {
        return plays;
    }

    double getWins() {
        return wins;
    }

    void incrementPlays() {
        plays++;
    }

    void setParent(Node node) {
        parent = node;
    }

    // Creates and/or returns a list of new state objects which correspond to the children of the given state.
    public ArrayList<Node> getChildren() {
        if (children != null) return children;
        children = new ArrayList<>();
        for (Move m : node.getLegalMoves()) {
            Node child = new Node(this, m);
            children.add(child);
        }
        return children;
    }

    double UCB(double explorationConstant) {
        double payOff = (wins / plays);
        return payOff + explorationConstant * Math.sqrt(Math.log(parent.plays) / plays);
    }

    void backPropagate(int winner) {
        Node node = this;
        while (node.parent != null) {
            node.plays++;
            if (node.getState().getMove().team == winner) {
                node.wins++;
            }
            node = node.parent;
        }
    }
}
