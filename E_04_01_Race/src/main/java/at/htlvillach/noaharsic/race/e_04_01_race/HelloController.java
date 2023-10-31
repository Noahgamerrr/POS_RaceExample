package at.htlvillach.noaharsic.race.e_04_01_race;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    public StackPane spRaceLines;
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    private void drawCar() {
        try {
            Image car = new Image(HelloApplication.class.getResource("assets/cars/car1.png").openStream());
            ImageView carView = new ImageView(car);
            spRaceLines.getChildren().add(carView);
        } catch (IOException e) {
            System.err.println(e);
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        drawCar();
    }
}