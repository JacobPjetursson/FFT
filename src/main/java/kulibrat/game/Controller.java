package kulibrat.game;

import fftlib.FFT;
import fftlib.FFTManager;
import fftlib.game.FFTMove;
import fftlib.game.FFTSolver;
import fftlib.game.StateMapping;
import fftlib.gui.FFTInteractivePane;
import fftlib.gui.FFTOverviewPane;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import kulibrat.FFT.FFTFollower;
import kulibrat.FFT.GameSpecifics;
import kulibrat.FFT.InteractiveState;
import kulibrat.ai.AI;
import kulibrat.ai.MCTS.MCTS;
import kulibrat.ai.Minimax.LookupTableMinimax;
import kulibrat.ai.Minimax.Minimax;
import kulibrat.ai.Minimax.PerfectPlayer;
import kulibrat.gui.*;
import kulibrat.gui.board.BoardPiece;
import kulibrat.gui.board.BoardTile;
import kulibrat.gui.board.Goal;
import kulibrat.gui.board.Player;
import kulibrat.misc.Database;
import misc.Globals;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static kulibrat.game.Logic.POS_NONBOARD;
import static misc.Config.USE_DB;
import static misc.Globals.*;

public class Controller {
    public Stage primaryStage;
    private int mode;
    private int playerRedInstance;
    private int playerBlackInstance;
    private int redTime;
    private int blackTime;
    private boolean overwriteDB;
    private int turnNo;
    private AI aiRed;
    private AI aiBlack;
    private Button startAIButton;
    private Button stopAIButton;
    private Button reviewButton;
    private Button editFFTButton;
    private Button addRuleFFTButton;
    private CheckBox automaticFFTBox;
    private Button[] swapButtons;
    private CheckBox helpHumanBox;
    private Thread aiThread;
    private NavPane navPane;
    private BoardPiece selected;
    private ArrayList<Move> curHighLights;
    private State state;
    private PlayArea playArea;
    private Goal goalRed;
    private Goal goalBlack;
    private boolean endGamePopup;
    private ArrayList<StateAndMove> previousStates;
    private Window window;
    private boolean fftAutomaticMode;
    private boolean fftUserInteraction;
    private GameSpecifics gameSpecifics;

    FFTOverviewPane fftOverviewPane;
    FFTInteractivePane fftInteractivePane;

    public Controller(Stage primaryStage, int playerRedInstance, int playerBlackInstance,
                      kulibrat.game.State state, int redTime, int blackTime, boolean overwriteDB) {
        this.mode = setMode(playerRedInstance, playerBlackInstance);
        this.playerRedInstance = playerRedInstance;
        this.playerBlackInstance = playerBlackInstance;
        this.redTime = redTime;
        this.blackTime = blackTime;
        this.overwriteDB = overwriteDB;
        this.turnNo = 0;
        this.state = state;
        this.primaryStage = primaryStage;
        this.endGamePopup = false;
        this.curHighLights = new ArrayList<>();
        this.previousStates = new ArrayList<>();

        instantiateAI(PLAYER1);
        instantiateAI(Globals.PLAYER2);

        // Prepare the FFT Stuff
        gameSpecifics = new GameSpecifics(this);
        FFTManager.initialize(gameSpecifics);

        // Do database stuff
        if (USE_DB) {
            if (overwriteDB) {
                System.out.println("Rebuilding lookup table. This will take some time.");
                try {
                    Database.createLookupTable();
                    HashMap<Long, StateMapping> solution = LookupTableMinimax.solveGame(state);
                    Database.fillLookupTable(solution);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                Database.connectAndVerify();
            }
        } else if (aiRed instanceof PerfectPlayer || aiBlack instanceof PerfectPlayer) {
            FFTSolver.solveGame(state);
        }

        // Autogenerate
        FFTManager.autogenFFT();


        PlayPane playPane = new PlayPane(this);
        primaryStage.setScene(new Scene(playPane,
                Globals.WIDTH, Globals.HEIGHT));
        navPane = playPane.getNavPane();
        playArea = playPane.getPlayArea();
        window = playArea.getScene().getWindow();

        playArea.update(this);

        // Fetch all gui elements that invoke something game-related
        startAIButton = navPane.getStartAIButton();
        stopAIButton = navPane.getStopAIButton();
        helpHumanBox = navPane.getHelpHumanBox();
        reviewButton = navPane.getReviewButton();
        editFFTButton = navPane.getEditFFTButton();
        addRuleFFTButton = navPane.getAddRuleFFTButton();
        automaticFFTBox = navPane.getAutomaticFFTBox();
        goalRed = playArea.getPlayBox().getGoal(PLAYER1);
        goalBlack = playArea.getPlayBox().getGoal(PLAYER2);

        fftInteractivePane = new FFTInteractivePane();
        fftOverviewPane = new FFTOverviewPane(primaryStage, fftInteractivePane);
        new Scene(fftInteractivePane, Globals.WIDTH, Globals.HEIGHT);
        new Scene(fftOverviewPane, Globals.WIDTH, Globals.HEIGHT);

        showNavButtons();

        // Set event handlers for gui elements
        // Swap player buttons
        swapButtons = new Button[2];
        Player[] players = new Player[2];
        players[0] = playArea.getPlayBox().getPlayer(PLAYER1);
        players[1] = playArea.getPlayBox().getPlayer(PLAYER2);
        swapButtons[0] = players[0].getSwapBtn();
        swapButtons[1] = players[1].getSwapBtn();
        for (int i = 0; i < swapButtons.length; i++) {
            Player p = players[i];
            Button b = swapButtons[i];
            b.setOnAction(event -> {
                deselect();
                Stage newStage = new Stage();
                newStage.setScene(new Scene(new SwapPlayerPane(this, p), 325, 400));
                newStage.initModality(Modality.APPLICATION_MODAL);
                newStage.initOwner(window);
                newStage.setOnCloseRequest(Event::consume);
                newStage.show();
            });
        }


        // Selected piece
        playPane.setFocusTraversable(true);
        playPane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) { //Escape to deselect
                deselect();
            }
        });

        startAIButton.setOnAction(event -> {
            startAI();
            stopAIButton.setDisable(false);
        });
        // Stop AI button
        stopAIButton.setDisable(true);
        stopAIButton.setOnAction(event -> {
            aiThread.interrupt();
            stopAIButton.setDisable(true);
        });
        // Review button
        reviewButton.setOnAction(event -> {
            if (Database.connectAndVerify()) {
                reviewGame();
            }
        });

        // help human checkbox
        helpHumanBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            helpHumanBox.setDisable(true);
            deselect();
            if (newValue) {
                if (Database.connectAndVerify()) {
                    helpHumanBox.setSelected(true);
                    highlightBestPieces(true);
                } else {
                    helpHumanBox.setSelected(false);
                }
            } else {
                Database.close();
                helpHumanBox.setSelected(false);
                highlightBestPieces(false);
            }
            helpHumanBox.setDisable(false);
        });
        // FFTManager LISTENERS
        // edit fftManager button
        editFFTButton.setOnAction(event -> {
            primaryStage.setScene(fftOverviewPane.getScene());
        });

        // add rule to FFT button
        addRuleFFTButton.setOnAction(event -> {
            primaryStage.setScene(fftInteractivePane.getScene());
            fftInteractivePane.update(this.state);
            fftInteractivePane.setPrevScene(primaryStage.getScene());
        });

        // automatic mode
        fftAutomaticMode = true;
        automaticFFTBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            deselect();
            fftAutomaticMode = newValue;
        });


        if (mode == HUMAN_VS_AI && playerRedInstance != HUMAN && state.getTurn() == PLAYER1) {
            aiThread = new Thread(this::doAITurn);
            aiThread.setDaemon(true);
            aiThread.start();
        }
    }

    private void showNavButtons() {
        navPane.removeWidgets();
        if (mode == AI_VS_AI && !navPane.containsAIWidgets())
            navPane.addAIWidgets();
        if (mode == HUMAN_VS_AI && !navPane.containsReviewButton())
            navPane.addReviewButton();
        if ((mode != AI_VS_AI || playerBlackInstance == FFT || playerRedInstance == FFT)) {
            if (!navPane.containsHelpBox())
                navPane.addHelpHumanBox();
        }
        if ((playerBlackInstance == FFT || playerRedInstance == FFT) && !navPane.containsFFTWidgets())
            navPane.addFFTWidgets();
    }

    private void instantiateAI(int team) {
        if (team == PLAYER1) {
            if (playerRedInstance == MINIMAX) {
                aiRed = new Minimax(PLAYER1, redTime);
            } else if (playerRedInstance == LOOKUP_TABLE) {
                aiRed = new PerfectPlayer(PLAYER1);
            } else if (playerRedInstance == MONTE_CARLO) {
                aiRed = new MCTS(state, PLAYER1, redTime);
            } else if (playerRedInstance == FFT) {
                aiRed = new FFTFollower(PLAYER1);
            }
        } else {
            if (playerBlackInstance == MINIMAX) {
                aiBlack = new Minimax(PLAYER2, blackTime);
            } else if (playerBlackInstance == LOOKUP_TABLE) {
                if (playerRedInstance == LOOKUP_TABLE) {
                    overwriteDB = false;
                }
                aiBlack = new PerfectPlayer(PLAYER2);
            } else if (playerBlackInstance == MONTE_CARLO) {
                aiBlack = new MCTS(state, PLAYER2, blackTime);
            } else if (playerBlackInstance == FFT) {
                aiBlack = new FFTFollower(PLAYER2);
            }
        }
    }

    // Is called when a tile is pressed by the user. If vs. the AI, it calls the doAITurn after. This function also highlights
    // the best pieces for the opponent, if it is human vs human.
    public void doHumanTurn(Move move) {
        previousStates.add(new StateAndMove(state, move, turnNo));
        state = state.getNextState(move);
        updatePostHumanTurn();
        if (Logic.gameOver(state)) return;
        if (state.getTurn() == move.team) {
            String skipped = (state.getTurn() == PLAYER1) ? "Black" : "Red";
            System.out.println("TEAM " + skipped + "'s turn has been skipped!");
            playArea.getInfoPane().displaySkippedTurn(skipped);
            if (playerRedInstance == FFT && state.getTurn() == PLAYER1 ||
                    playerBlackInstance == FFT && state.getTurn() == PLAYER2)
                doAITurn();
            else if (helpHumanBox.isSelected()) {
                highlightBestPieces(true);
            }
            return;
        }
        // FFT vs. AI
        if (mode == AI_VS_AI && ((playerRedInstance == FFT && state.getTurn() == PLAYER2) ||
                (playerBlackInstance == FFT && state.getTurn() == PLAYER1))) {
            startAIButton.fire();
        // Human vs. AI
        } else if ((playerBlackInstance != HUMAN && state.getTurn() == PLAYER2) ||
                (playerRedInstance != HUMAN && state.getTurn() == PLAYER1)) {
            doAITurn();
        // Human/FFT vs. human
        } else if (helpHumanBox.isSelected()) {
            highlightBestPieces(true);
        }
    }

    private void updatePostHumanTurn() {
        turnNo++;
        if (aiRed != null) aiRed.update(state);
        if (aiBlack != null) aiBlack.update(state);
        deselect();
        playArea.update(this);
        checkGameOver();
    }

    private void updatePostAITurn() {
        turnNo++;
        if (aiRed != null) aiRed.update(state);
        if (aiBlack != null) aiBlack.update(state);
        // Update gui elements on another thread
        Platform.runLater(() -> {
            playArea.update(this);
            checkGameOver();
        });
    }

    // This function is called when two AI's are matched against each other. It can be interrupted by the user.
    // For the lookup table, a delay can be set
    private void startAI() {
        // For the AI vs. AI mode. New thread is needed to updateRuleFromTile the gui while running the AI
        navPane.getRestartButton().setDisable(true);
        navPane.getMenuButton().setDisable(true);
        startAIButton.setDisable(true);
        for (Button b : swapButtons)
            b.setDisable(true);

        aiThread = new Thread(() -> {
            try {
                while (!Logic.gameOver(state)) {
                    doAITurn();
                    if (fftUserInteraction) {
                        stopAIButton.fire();
                    }
                    if (playerRedInstance == LOOKUP_TABLE && playerBlackInstance == LOOKUP_TABLE) {
                        Thread.sleep(redTime);
                    } else if (state.getTurn() == PLAYER1 && playerRedInstance == FFT && !fftUserInteraction)
                        Thread.sleep(redTime);
                    else if (state.getTurn() == PLAYER2 && playerBlackInstance == FFT && !fftUserInteraction)
                        Thread.sleep(blackTime);
                    else {
                        Thread.sleep(0); // To allow thread interruption
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                startAIButton.setDisable(false);
                navPane.getMenuButton().setDisable(false);
                navPane.getRestartButton().setDisable(false);
                for (Button b : swapButtons)
                    b.setDisable(false);
            }
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }

    // The AI makes its turn, and the GUI is updated while doing so
    private void doAITurn() {
        fftUserInteraction = false;
        boolean makeDefaultMove = false;
        int turn = state.getTurn();
        Move move;
        if (aiRed != null && turn == PLAYER1) {
            move = aiRed.makeMove(state);
            if (playerRedInstance == FFT && move == null)
                makeDefaultMove = true;
        } else {
            move = aiBlack.makeMove(state);
            if (playerBlackInstance == FFT && move == null)
                makeDefaultMove = true;
        }
        if (makeDefaultMove) {
            if (fftAutomaticMode) {
                System.out.println("Defaulting to random move");
                move = getDefaultFFTMove();

            } else {
                System.out.println("Defaulting to user interaction");
                fftUserInteraction = true;
                if (helpHumanBox.isSelected())
                    highlightBestPieces(true);
                return;
            }
        }
        state = state.getNextState(move);
        updatePostAITurn();
        if (Logic.gameOver(state)) return;
        if (mode == HUMAN_VS_AI) {
            if (turn == state.getTurn()) {
                String skipped = (turn == PLAYER1) ? "Black" : "Red";
                System.out.println("TEAM " + skipped + "'s turn has been skipped!");
                playArea.getInfoPane().displaySkippedTurn(skipped);
                doAITurn();
            } else if (helpHumanBox.isSelected()) {
                highlightBestPieces(true);
            }
        } else if (((state.getTurn() == PLAYER1 && playerRedInstance == FFT) ||
                (state.getTurn() == PLAYER2 && playerBlackInstance == FFT)) &&
                helpHumanBox.isSelected()) {
            highlightBestPieces(true);
        }
    }

    private Move getDefaultFFTMove() {
        Random r = new Random();
        int moveSize = state.getLegalMoves().size();
        int index = r.nextInt(moveSize);
        return state.getLegalMoves().get(index);
    }

    // Checks if the game is over and shows a popup. Popup allows a restart, go to menu, or review game
    private void checkGameOver() {
        if (Logic.gameOver(state) && !endGamePopup) {
            endGamePopup = true;
            Stage newStage = new Stage();
            int winner = Logic.getWinner(state);
            if (playerRedInstance == HUMAN) state.setTurn(PLAYER1);
            else if (playerBlackInstance == HUMAN) state.setTurn(PLAYER2);

            newStage.setScene(new Scene(new EndGamePane(primaryStage, winner,
                    this), 500, 300));
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.initOwner(window);
            newStage.setOnCloseRequest(Event::consume);
            newStage.show();
        }
    }

    // Deselects the selected piece
    public void deselect() {
        if (selected != null) {
            highlightMoves(selected.getRow(), selected.getCol(), selected.getTeam(), false);
            selected.deselect();
            selected = null;
        }
    }

    // Shows the red/green/yellow addHighlight on the tiles when a piece has been selected
    private void highlightMoves(int row, int col, int team, boolean highlight) {
        if (highlight) curHighLights = Logic.legalMovesFromPiece(row,
                col, team, state.getBoard());
        ArrayList<? extends FFTMove> bestMoves = null;
        if (highlight && helpHumanBox.isSelected()) {
            bestMoves = Database.bestMoves(new State(state));
        }
        ArrayList<String> turnsToTerminalList = null;
        if (highlight && helpHumanBox.isSelected()) {
            turnsToTerminalList = getScores(curHighLights);
        }
        BoardTile[][] tiles = playArea.getPlayBox().getBoard().getTiles();
        for (int i = 0; i < curHighLights.size(); i++) {
            Move m = curHighLights.get(i);
            String turns = "";
            if (turnsToTerminalList != null) {
                turns = turnsToTerminalList.get(i);
            }
            boolean bestMove = false;
            if (bestMoves != null && bestMoves.contains(m)) {
                bestMove = true;
            }
            if (m.newCol == POS_NONBOARD && m.newRow == POS_NONBOARD) {
                if (team == PLAYER1) {
                    goalRed.setHighlight(highlight, helpHumanBox.isSelected(), bestMove, turns);
                } else {
                    goalBlack.setHighlight(highlight, helpHumanBox.isSelected(), bestMove, turns);
                }
            } else tiles[m.newRow][m.newCol].setHighlight(highlight, helpHumanBox.isSelected(), bestMove, turns);
        }
    }

    // Highlights the best pieces found above
    private void highlightBestPieces(boolean highlight) {
        State n = new State(state);
        ArrayList<Move> bestMoves = null;
        if (highlight) bestMoves = (ArrayList<Move>) Database.bestMoves(n);
        BoardTile[][] tiles = playArea.getPlayBox().getBoard().getTiles();

        for (BoardTile[] tile : tiles) {
            for (BoardTile aTile : tile) {
                BoardPiece p = aTile.getPiece();
                if (p == null)
                    continue;
                p.setBest(false);
                if (!highlight)
                    continue;

                for (Move m : bestMoves) {
                    if (m.oldCol == p.getCol() && m.oldRow == p.getRow())
                        p.setBest(true);
                }
            }
        }
        int player = state.getTurn();
        for (BoardPiece p : playArea.getPlayBox().getPlayer(player).getPieces()) {
            p.setBest(false);
            if (!highlight) continue;
            for (Move m : bestMoves) {
                if (m.oldCol == p.getCol() && m.oldRow == p.getRow()) {
                    p.setBest(true);
                }
            }
        }
        int opponent = (player == PLAYER1) ? PLAYER2 : PLAYER1;
        for (BoardPiece p : playArea.getPlayBox().getPlayer(opponent).getPieces()) {
            p.setBest(false);
        }
    }

    // Adds string scores to all moves from a piece
    private ArrayList<String> getScores(ArrayList<Move> curHighLights) {
        ArrayList<String> turnsToTerminalList = new ArrayList<>();
        for (Move m : curHighLights) {
            State s = new State(state).getNextState(m);
            if (Logic.gameOver(s)) {
                turnsToTerminalList.add("0");
            } else turnsToTerminalList.add(Database.turnsToTerminal(state.getTurn(), s));
        }
        return turnsToTerminalList;
    }

    public State getState() {
        return state;
    }

    public int getPlayerInstance(int team) {
        if (team == PLAYER1) {
            return playerRedInstance;
        } else {
            return playerBlackInstance;
        }
    }

    public void setPlayerInstance(int team, int playerInstance) {
        if (team == PLAYER1) {
            playerRedInstance = playerInstance;
        } else {
            playerBlackInstance = playerInstance;
        }
        int oldMode = mode;
        this.mode = setMode(playerRedInstance, playerBlackInstance);
        instantiateAI(team);
        if (state.getTurn() == team && playerInstance != HUMAN && mode != AI_VS_AI) {
            doAITurn();
        } else if (state.getTurn() != team && oldMode == AI_VS_AI && mode != AI_VS_AI) {
            doAITurn();
        }
        showNavButtons();
        playArea.update(this);
    }

    // Opens the review pane
    private void reviewGame() {
        Stage newStage = new Stage();
        newStage.setScene(new Scene(new ReviewPane(primaryStage, this), 550, Globals.HEIGHT - 50));
        newStage.initModality(Modality.APPLICATION_MODAL);
        newStage.initOwner(window);
        newStage.setOnCloseRequest(Event::consume);
        newStage.show();
    }

    // Sets the mode based on the red and black player types
    private int setMode(int playerRedInstance, int playerBlackInstance) {
        if (playerRedInstance == HUMAN && playerBlackInstance == HUMAN) {
            return HUMAN_VS_HUMAN;
        } else if (playerRedInstance != HUMAN ^ playerBlackInstance != HUMAN) {
            return HUMAN_VS_AI;
        } else {
            return AI_VS_AI;
        }
    }

    public BoardPiece getSelected() {
        return selected;
    }

    public void setSelected(BoardPiece piece) {
        deselect();
        selected = piece;
        highlightMoves(piece.getRow(), piece.getCol(), piece.getTeam(), true);
    }

    public int getTurnNo() {
        return turnNo;
    }

    public void setTurnNo(int turnNo) {
        this.turnNo = turnNo;
    }

    public int getScoreLimit() {
        return state.getScoreLimit();
    }

    public int getMode() {
        return mode;
    }

    public int getTime(int team) {
        if (team == PLAYER1) return redTime;
        else return blackTime;
    }

    public boolean getOverwriteDB() {
        return overwriteDB;
    }

    public void setOverwriteDB(boolean overwrite) {
        this.overwriteDB = overwrite;
    }

    public void setPlayerCalcTime(int team, int time) {
        if (team == PLAYER1)
            redTime = time;
        else
            blackTime = time;
    }

    public PlayArea getPlayArea() {
        return playArea;
    }

    public ArrayList<StateAndMove> getPreviousStates() {
        return previousStates;
    }

    public void setPreviousStates(ArrayList<StateAndMove> stateAndMoves) {
        this.previousStates = stateAndMoves;
    }

    public Window getWindow() {
        return window;
    }

    public boolean getFFTAllowInteraction() {
        return fftUserInteraction;
    }

    public InteractiveState getInteractiveState() {
        return gameSpecifics.interactiveState;
    }

    public FFT getCurrFFT() {
        return FFTManager.currFFT;
    }

}
