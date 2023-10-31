package at.htlvillach.noaharsic.race.e_04_01_race.model;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.Random;
import java.util.concurrent.Phaser;

public class RacerThread implements Runnable {
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
        car.setTranslateX(0);
        TranslateTransition translate = new TranslateTransition();
        translate.setNode(car);
        translate.setByX(675);
        translate.setDuration(Duration.seconds(drivingTime));
        translate.play();
        try {
            Thread.sleep((long) (drivingTime * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (phaser.getPhase() != 4) {
            phaser.arriveAndAwaitAdvance();
            runRound();
        } else phaser.arriveAndDeregister();
    }

    @Override
    public void run() {
        runRound();
    }
}
