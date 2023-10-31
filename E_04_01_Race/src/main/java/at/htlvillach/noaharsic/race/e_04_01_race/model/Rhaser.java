package at.htlvillach.noaharsic.race.e_04_01_race.model;

import javafx.scene.Node;

import java.util.concurrent.Phaser;

public class Rhaser implements Runnable{
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