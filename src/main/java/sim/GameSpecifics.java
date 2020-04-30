package sim;

import com.google.common.collect.Sets;
import fftlib.logic.Action;
import fftlib.logic.Literal;
import fftlib.logic.Rule;
import fftlib.logic.SymmetryRule;
import fftlib.auxiliary.Position;
import fftlib.auxiliary.Transform;
import fftlib.game.*;
import fftlib.gui.FFTFailNode;
import fftlib.gui.interactiveFFTNode;
import misc.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static misc.Globals.PLAYER1;
import static misc.Globals.PLAYER2;
import static sim.Line.NO_COLOR;

public class GameSpecifics implements FFTGameSpecifics {


    @Override
    public FFTMove actionToMove(Action a) {
        if (a == null || a.adds.size() != 1)
            return null;
        FFTMove m = null;
        for (Literal l : a.adds) {
            Position pos = Atoms.idToPos.get(l.id);
            m = new Move(pos. occ, new Line(pos.row, pos.col));
        }
        return m;
    }

    @Override
    public Action moveToAction(FFTMove move) {
        if (move == null)
            return null;
        Move m = (Move) move;
        Position pos = new Position(m.line.n1, m.line.n2, m.team);
        return new Action(Atoms.posToId.get(pos));
    }

    @Override
    public LiteralSet nodeToLiterals(FFTNode n) {
        Node node = (Node) n;
        LiteralSet literals = new LiteralSet();
        for (Line l : node.lines) {
            int occ = l.color;
            if (occ == NO_COLOR)
                continue;
            Position pos = new Position(l.n1, l.n2, occ);
            literals.add(new Literal(Atoms.posToId.get(pos)));
        }
        return new LiteralSet(literals);
    }

    @Override
    public Rule gdlToRule(String precons, String action) {
        return null;
    }

    @Override
    public String getFFTFilePath() {
        if (Config.ENABLE_GGP_PARSER)
            return "FFTs/sim_GGP_FFT.txt";
        return "FFTs/simFFT.txt";
    }

    @Override
    public int[] getBoardDim() {
        return new int[] {6, 6};
    }

    @Override
    public String[] getPlayerNames() {
        return new String[0];
    }

    @Override
    public FFTNode getInitialNode() {
        return new Node();
    }

    @Override
    public ArrayList<Integer> getGameAtoms() {
        return Atoms.gameAtoms;
    }

    @Override
    public String getAtomName(int atom) {
        return Atoms.idToString.get(atom);
    }

    @Override
    public int getAtomId(String name) {
        return Atoms.stringToId.get(name);
    }

    @Override
    public FFTFailNode getFailNode() {
        return null;
    }

    @Override
    public interactiveFFTNode getInteractiveNode() {
        return null;
    }

    @Override
    public HashSet<SymmetryRule> getSymmetryRules(Rule rule) {
        return Transform.findAutomorphisms(rule);
    }

    @Override
    public int posToId(Position pos) {
        return Atoms.posToId.get(pos);
    }

    @Override
    public Position idToPos(int id) {
        return Atoms.idToPos.get(id);
    }

    @Override
    public LiteralSet getActionPreconditions(Action action) {
        return Atoms.actionToPrecons.get(action);
    }

    @Override
    public int getMaxStateLiterals() {
        return 15;
    }

    @Override
    public HashSet<Long> getCoveredStateBitCodes(Rule rule) {
        HashSet<Long> bitCodes = new HashSet<>();
        List<Set<Literal>> subsets = new ArrayList<>();
        LiteralSet precons = rule.getAllPreconditions();
        int n1, n2, occ;
        Position pos;
        // find sets of preconditions that can not co-exist
        for (n1 = 0; n1 < 6; n1++) {
            for (n2 = n1+1; n2 < 6; n2++) {
                boolean setExists = false;
                LiteralSet litSet = new LiteralSet();
                // make set of all relevant literals from this cell
                for (occ = PLAYER1; occ <= PLAYER2; occ++) {
                    pos = new Position(n1, n2, occ);
                    Literal l = new Literal(posToId(pos), false);
                    if (precons.contains(l)) {
                        setExists = true;
                        break;
                    }
                    Literal negLit = new Literal(posToId(pos), true);
                    if (!precons.contains(negLit)) {
                        litSet.add(l);
                    }
                }
                if (!setExists) {
                    litSet.add(Literal.NULL);
                    subsets.add(litSet);
                }
            }
        }
        // Find the cartesian product of those sets (combination of sets where
        // we only pick one from each set)
        Set<List<Literal>> cartesianSet = Sets.cartesianProduct(subsets);
        // Add each if not null literal
        for (List<Literal> literals : cartesianSet) {
            LiteralSet newSet = new LiteralSet(precons);
            for (Literal l : literals) {
                if (!l.equals(Literal.NULL))
                    newSet.add(l);
            }
            bitCodes.add(newSet.getBitString());
        }
        return bitCodes;
    }

    @Override
    public long getNumberOfCoveredStates(Rule rule) {
        LiteralSet precons = rule.getAllPreconditions();
        long coveredStates = 1;
        int n1, n2, occ;
        Position pos;

        for (n1 = 0; n1 < 6; n1++) {
            for (n2 = n1+1; n2 < 6; n2++) {
                int combinations = 3;
                // make set of all relevant literals from this cell
                for (occ = PLAYER1; occ <= PLAYER2; occ++) {
                    pos = new Position(n1, n2, occ);
                    Literal l = new Literal(posToId(pos), false);
                    if (precons.contains(l)) {
                        combinations = 1;
                        break;
                    }
                    Literal negLit = new Literal(posToId(pos), true);
                    if (precons.contains(negLit))
                        combinations--;
                }
                coveredStates *= combinations;
            }
        }
        return coveredStates;
    }
}
