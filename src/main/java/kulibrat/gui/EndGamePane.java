package kulibrat.gui;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kulibrat.game.Controller;
import kulibrat.game.State;
import kulibrat.gui.menu.MenuPane;
import misc.Globals;

import static misc.Globals.*;


public class EndGamePane extends VBox {

    public EndGamePane(Stage primaryStage, int team, Controller cont) {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        Label label = new Label();
        if (team == PLAYER1) label.setText("Congratulations player Red!");
        else label.setText(("Congratulations player Black!"));

        label.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        getChildren().add(label);

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(40);
        Button menuBtn = new Button("Menu");
        menuBtn.setFont(Font.font("Verdana", 16));
        menuBtn.setOnMouseClicked(event -> {
            Stage stage = (Stage) getScene().getWindow();
            stage.close();
            primaryStage.setScene(new Scene(new MenuPane(),
                    Globals.WIDTH, Globals.HEIGHT));
        });
        menuBtn.setPrefWidth(150);
        hBox.getChildren().add(menuBtn);
        Button restartGameBtn = new Button("Restart Game");
        restartGameBtn.setFont(Font.font("Verdana", 16));
        restartGameBtn.setOnMouseClicked(event -> {
            Stage stage = (Stage) getScene().getWindow();
            stage.close();
            new Controller(primaryStage, cont.getPlayerInstance(PLAYER1),
                    cont.getPlayerInstance(PLAYER2), new State(), cont.getTime(PLAYER1), cont.getTime(PLAYER2), cont.getOverwriteDB());
        });
        restartGameBtn.setPrefWidth(150);
        hBox.getChildren().add(restartGameBtn);
        getChildren().add(hBox);

        Button reviewGameBtn = new Button("Review Game");
        reviewGameBtn.setFont(Font.font("Verdana", 16));
        reviewGameBtn.setOnMouseClicked(event -> {
            Stage stage = (Stage) getScene().getWindow();
            stage.close();
            Stage newStage = new Stage();
            newStage.setScene(new Scene(new ReviewPane(primaryStage, cont), 325, Globals.HEIGHT - 50));
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.initOwner(cont.getWindow());
            newStage.setOnCloseRequest(Event::consume);
            newStage.show();

        });
        reviewGameBtn.setPrefWidth(180);
        if (cont.getMode() == HUMAN_VS_AI)
            getChildren().add(reviewGameBtn);
    }
}