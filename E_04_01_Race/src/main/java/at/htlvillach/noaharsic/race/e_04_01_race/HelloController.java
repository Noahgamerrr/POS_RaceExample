package at.htlvillach.noaharsic.race.e_04_01_race;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.Phaser;

public class HelloController implements Initializable {
    public StackPane spRaceLines;
    public GridPane gpPlayers;

    public Node[] cars = new Node[4];
    public Button btnAdd;
    public Button btnRemove;
    public Button btnStart;
    public VBox vbControls;

    private int playing = 2;

    private Thread rhaser;

    private void drawRoad() {
        try {
            Image road = new Image(HelloApplication.class.getResource("assets/raceRoad.png").openStream());
            ImageView roadView = new ImageView(road);
            spRaceLines.getChildren().add(roadView);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void drawCars() {
        try {
            for (int i = 1; i <= 4; i++) {
                String id = "car" +i;
                String imageStr = "assets/cars/" +id +".png";
                Image car = new Image(HelloApplication.class.getResource(imageStr).openStream());
                ImageView carView = new ImageView(car);
                carView.setId(id);
                carView.setTranslateX(10);
                carView.setTranslateY(42 * (i - 1) + 60);
                if (i > 2) carView.styleProperty().setValue("visibility: hidden;");
                cars[i - 1] = carView;
            }
            spRaceLines.getChildren().addAll(cars);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        spRaceLines.alignmentProperty().setValue(Pos.TOP_LEFT);
        btnRemove.setDisable(true);
        drawRoad();
        drawCars();
    }

    public void addPlayer(ActionEvent actionEvent) {
        btnRemove.setDisable(false);
        cars[playing].styleProperty().setValue("visibility: visible;");
        playing++;
        TextField tf = new TextField();
        Label l = new Label("Teilnehmer " + playing);
        GridPane.setMargin(l, new Insets(0, 0, 0, 5));
        gpPlayers.addRow((playing - 1), tf, l);
        gpPlayers.getRowConstraints().add(new RowConstraints(30));
        if (playing == 4) btnAdd.setDisable(true);
    }

    public void removePlayer(ActionEvent actionEvent) {
        btnAdd.setDisable(false);
        playing--;
        cars[playing].styleProperty().setValue("visibility: hidden;");
        gpPlayers.getChildren().removeIf(node -> GridPane.getRowIndex(node) == playing);
        gpPlayers.getRowConstraints().remove(playing);
        if (playing == 2) btnRemove.setDisable(true);
    }

    public void startGame(ActionEvent actionEvent) {
        rhaser = new Thread(new Rhaser(playing, cars));
        rhaser.start();
        vbControls.setDisable(true);
    }

    public void resetCars() {
        for (int i = 0; i < cars.length; i++) cars[i].setTranslateX(0);
    }

    class Rhaser implements Runnable{
        Phaser phaser = new Phaser();
        int players;
        Node[] cars;

        public Rhaser(int players, Node[] cars) {
            this.players = players;
            this.cars = cars;
        }

        @Override
        public void run() {
            for (int i = 0; i < players; i++) {
                Thread rt = new Thread(new RacerThread(phaser, cars[i]));
                rt.start();
            }
        }
    }

    class RacerThread implements Runnable {
        Phaser phaser;
        Node car;

        public RacerThread(Phaser phaser, Node car) {
            this.phaser = phaser;
            this.car = car;
            phaser.register();
        }
        private void runRound() {
            Random r = new Random();
            double drivingTime = r.nextDouble(1.5, 4);
            Platform.runLater(() -> car.setTranslateX(0));
            TranslateTransition translate = new TranslateTransition();
            translate.setNode(car);
            translate.setByX(675);
            translate.setDuration(Duration.seconds(drivingTime));
            Platform.runLater(translate::play);
            try {
                Thread.sleep((long) (drivingTime * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (phaser.getPhase() == 4) {
                resetCars();
                phaser.arriveAndDeregister();
                vbControls.setDisable(false);
            } else {
                phaser.arriveAndAwaitAdvance();
                runRound();
            }
        }

        @Override
        public void run() {
            runRound();
        }
    }
}