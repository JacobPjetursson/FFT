package kulibrat.gui.board;


import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import kulibrat.game.Controller;
import kulibrat.game.Node;

import java.net.URL;
import java.util.ArrayList;

import static misc.Globals.*;


public class Player extends VBox {
    private int team;
    private GridPane gridPaneBoard;
    private ArrayList<BoardPiece> pieces;
    private int clickMode;
    private int pieceRadius;
    private int type;
    private Label typeLabel;
    private Button swapBtn;

    public Player(int team, Controller cont, int tileSize, int pieceRadius, int clickMode) {
        this.team = team;
        this.clickMode = clickMode;
        this.pieceRadius = pieceRadius;
        pieces = new ArrayList<>();
        type = cont.getPlayerInstance(team);
        setAlignment(Pos.CENTER);
        setSpacing(tileSize / 6);
        setStyle("-fx-background-color: rgb(255, 255, 255);");
        int amount_of_pieces = 4;
        gridPaneBoard = new GridPane();
        gridPaneBoard.setAlignment(Pos.CENTER);
        gridPaneBoard.setPrefSize(tileSize * 4, tileSize);
        gridPaneBoard.setMaxWidth(tileSize * 4);
        gridPaneBoard.setBorder(new Border(new BorderStroke(getColor(),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        ColumnConstraints column = new ColumnConstraints(tileSize);
        for (int i = 0; i < 4; i++) {
            gridPaneBoard.getColumnConstraints().add(column);
        }
        for (int i = 0; i < amount_of_pieces; i++) {
            BoardPiece bp = new BoardPiece(team, cont, pieceRadius, clickMode);
            pieces.add(bp);
            gridPaneBoard.add(pieceBox(bp), i, 0);
        }
        URL urlRed = this.getClass().getClassLoader().getResource("playerIconRed.png");
        URL urlBlack = this.getClass().getClassLoader().getResource("playerIconBlack.png");
        Image img = (team == PLAYER1) ? new Image(urlRed.toExternalForm()) :
                new Image(urlBlack.toExternalForm());
        ImageView imgView = new ImageView(img);

        imgView.setPreserveRatio(true);
        imgView.setFitHeight(tileSize);
        imgView.setFitWidth(tileSize);
        BorderPane imgPane = new BorderPane();
        imgPane.setCenter(imgView);

        GridPane gridPaneDisplay = new GridPane();
        gridPaneDisplay.setAlignment(Pos.CENTER);
        gridPaneDisplay.setPrefSize((tileSize * 4) / 3, tileSize);
        gridPaneDisplay.setMaxWidth((tileSize * 4) / 3);

        typeLabel = new Label();
        setTypeLabelText(type);
        typeLabel.setFont(Font.font("Verdana", tileSize / 3));

        // swap player button
        swapBtn = new Button("Swap");
        swapBtn.setFont(Font.font("Verdana", 16));
        ColumnConstraints column1 = new ColumnConstraints((tileSize * 4) / 3);
        for (int i = 0; i < 3; i++) {
            gridPaneDisplay.getColumnConstraints().add(column1);
        }
        gridPaneDisplay.add(swapBtn, 0, 0);
        gridPaneDisplay.add(imgPane, 1, 0);
        gridPaneDisplay.add(typeLabel, 2, 0);
        getChildren().add(gridPaneBoard);
        if (clickMode == CLICK_DEFAULT) {
            if (team == PLAYER1) getChildren().add(1, gridPaneDisplay);
            else getChildren().add(0, gridPaneDisplay);
        }

    }

    public void update(Controller cont, Node node) {
        if (gridPaneBoard.getChildren().size() > node.getUnplaced(team)) {
            if (cont.getSelected() != null) { //Human turn
                BoardPiece bp = cont.getSelected();
                pieces.remove(bp);
                gridPaneBoard.getChildren().remove(bp.getParent());
            } else {
                HBox parent = (HBox) gridPaneBoard.getChildren().get(0);
                BoardPiece bp = (BoardPiece) parent.getChildren().get(0);
                pieces.remove(bp);
                gridPaneBoard.getChildren().remove(parent);
            }
        } else if (gridPaneBoard.getChildren().size() < node.getUnplaced(team)) {
            for (int i = 0; i < 4; i++) {
                boolean occupied = false;
                for (javafx.scene.Node n : gridPaneBoard.getChildren()) {
                    if (GridPane.getColumnIndex(n) == i) {
                        occupied = true;
                        break;
                    }
                }
                if (!occupied) {
                    BoardPiece bp = new BoardPiece(team, cont, pieceRadius, clickMode);
                    pieces.add(bp);
                    gridPaneBoard.add(pieceBox(bp), i, 0);
                    break;
                }
            }
        }
    }

    public ArrayList<BoardPiece> getPieces() {
        return pieces;
    }

    private Color getColor() {
        if (team == PLAYER1) return Color.RED;
        else return Color.BLACK;
    }

    public int getTeam() {
        return team;
    }

    public void setTypeLabelText(int type) {
        typeLabel.setText((type == HUMAN) ? "Human" : (type == MINIMAX) ? "Minimax" :
                (type == LOOKUP_TABLE) ? "Lookup\n Table" :
                        (type == MONTE_CARLO) ? "MCTS" : "FFT");
    }

    public Button getSwapBtn() {
        return swapBtn;
    }

    private HBox pieceBox(BoardPiece piece) {
        HBox box = new HBox(piece);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public Label getTypeLabel() {
        return typeLabel;
    }
}
