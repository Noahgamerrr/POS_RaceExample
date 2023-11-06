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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HelloController implements Initializable {
    public StackPane spRaceLines;
    public GridPane gpPlayers;

    public Node[] cars = new Node[4];
    public Button btnAdd;
    public Button btnRemove;
    public Button btnStart;
    public VBox vbControls;

    private final BlockingQueue<OnMainExecutable> execOnApplicationThread = new LinkedBlockingDeque<>();

    private int playing = 2;

    private Thread rhaser;

    private static class StopRunningException extends Exception {

    }

    private static interface OnMainExecutable {
        void run() throws StopRunningException;
    }

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
                String id = "car" + i;
                String imageStr = "assets/cars/" + id + ".png";
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

    String[] getPlayerNames() {
        return gpPlayers.getChildren().stream()
                .filter(node -> node instanceof TextField)
                .map(node -> (TextField) node)
                .map(TextField::getText)
                .toArray(String[]::new);
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

    class Rhaser implements Runnable {
        Phaser phaser = new Phaser();
        int players;
        List<AtomicReference<Double[]>> lapTimes = new ArrayList<>(4);
        Node[] cars;

        public Rhaser(int players, Node[] cars) {
            this.players = players;
            this.cars = cars;
            for (int i = 0; i < players; i++) {
                lapTimes.add(new AtomicReference<>(new Double[5]));
            }
        }

        private void onFinished() {
            // Show scoreboard
            vbControls.setDisable(true);

            // Hide everything behind a white rectangle
            StackPane sp = new StackPane();
            sp.setStyle("-fx-background-color: white;");
            sp.setPrefSize(800, 600);

            // Scoreboard
            GridPane gpScoreboard = new GridPane();
            gpScoreboard.setHgap(10);
            gpScoreboard.setVgap(10);

            // Scoreboard: Header
            Label lHeader = new Label("Platzierung");
            lHeader.setStyle("-fx-font-weight: bold;");
            gpScoreboard.add(lHeader, 0, 0);
            lHeader = new Label("Teilnehmer");
            lHeader.setStyle("-fx-font-weight: bold;");
            gpScoreboard.add(lHeader, 1, 0);
            lHeader = new Label("Zeit");
            lHeader.setStyle("-fx-font-weight: bold;");
            gpScoreboard.add(lHeader, 2, 0);

            // For each lap
            for (int lap = 0; lap < 5; lap++) {
                // Create lap header
                Label l = new Label("Runde " + (lap + 1));
                l.setStyle("-fx-font-weight: bold;");
                gpScoreboard.add(l, lap + 3, 0);
            }

            // Gesamtzeit
            lHeader = new Label("Gesamtzeit");
            lHeader.setStyle("-fx-font-weight: bold;");
            gpScoreboard.add(lHeader, 8, 0);

            String[] playerNames = getPlayerNames();

            // Scoreboard: Rows
            for (int i = 0; i < players; i++) {
                double totalTime = Arrays.stream(lapTimes.get(i).get()).reduce(0.0, Double::sum).doubleValue();

                // Get placement
                int betterThanThisCar = lapTimes.stream().map(AtomicReference::get).map((lt) -> (Arrays.stream(lt).reduce(0.0, Double::sum) < totalTime ? 1 : 0)).reduce(0, Integer::sum);

                Label l = new Label((betterThanThisCar + 1) + ".");
                gpScoreboard.add(l, 0, i + 1);
                if (betterThanThisCar == 0)
                    // Underline
                    l.setStyle("-fx-underline: true;");
                l = new Label(playerNames[i]);
                gpScoreboard.add(l, 1, i + 1);
                if (betterThanThisCar == 0)
                    l.setStyle("-fx-underline: true;");
                l = new Label();
                gpScoreboard.add(l, 2, i + 1);
                if (betterThanThisCar == 0)
                    l.setStyle("-fx-underline: true;");


                // For each lap
                for (int lap = 0; lap < 5; lap++) {
                    final int finalLap = lap;
                    l = new Label();
                    gpScoreboard.add(l, lap + 3, i + 1);
                    double lapTime = lapTimes.get(i).get()[lap];
                    boolean wasBestLap = lapTimes.stream().map(AtomicReference::get).map((lts) -> lts[finalLap]).min(Double::compare).orElse(0.0) == lapTime;
                    if (lapTime != 0) {
                        l.setText(String.format("%.2f", lapTime));
                        if (wasBestLap)
                            l.setStyle("-fx-font-weight: bold;");
                        // If this lap is from the winner, also underline
                        if (betterThanThisCar == 0)
                            l.setStyle(l.getStyle() + "-fx-underline: true;");

                    }
                }

                l = new Label();
                gpScoreboard.add(l, 8, i + 1);
                l.setText(String.format("%.2f", totalTime));
                if (betterThanThisCar == 0)
                    l.setStyle("-fx-underline: true; -fx-font-weight: bold;");
            }

            // Scoreboard: Buttons
            Button btnClose = new Button("Schließen");
            btnClose.setOnAction(actionEvent -> {
                vbControls.setDisable(false);
                execOnApplicationThread.add(() -> {
                    spRaceLines.getChildren().remove(sp);
                });
                resetCars();
            });
            // Set absolute position of button
            StackPane.setAlignment(btnClose, Pos.BOTTOM_RIGHT);

            // Scoreboard: Add to StackPane
            sp.getChildren().add(gpScoreboard);
            sp.getChildren().add(btnClose);
            execOnApplicationThread.add(() -> {
                spRaceLines.getChildren().add(sp);
            });
        }

        @Override
        public void run() {
            Thread[] racers = new Thread[players];
            for (int i = 0; i < players; i++) {
                racers[i] = new Thread(new RacerThread(phaser, cars[i], lapTimes.get(i), this::onFinished));
                racers[i].start();
            }
            final AtomicBoolean running = new AtomicBoolean(true);
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    OnMainExecutable r = execOnApplicationThread.take();
                    Platform.runLater(() -> {
                        try {
                            r.run();
                        } catch (StopRunningException e) {
                            running.set(false);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < players; i++) {
                try {
                    racers[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class RacerThread implements Runnable {
        Phaser phaser;
        Node car;
        AtomicReference<Double[]> carLapTimes;
        private Runnable onFinished;

        public RacerThread(Phaser phaser, Node car, AtomicReference<Double[]> carLapTimes, Runnable onFinished) {
            this.phaser = phaser;
            this.car = car;
            this.carLapTimes = carLapTimes;
            this.onFinished = onFinished;
            phaser.register();
        }

        private void runRound() {
            Random r = new Random();
            double drivingTime = r.nextDouble(1.5, 2.5);
            carLapTimes.get()[phaser.getPhase()] = drivingTime;
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
                phaser.arriveAndDeregister();
                if (phaser.isTerminated()) {
                    onFinished.run();
                }
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