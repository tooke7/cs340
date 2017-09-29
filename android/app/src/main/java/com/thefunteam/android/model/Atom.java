package com.thefunteam.android.model;

import android.support.v7.app.AppCompatActivity;

import java.util.Observable;

public class Atom extends Observable {
    private static Atom ourInstance = new Atom();

    public static Atom getInstance() {
        return ourInstance;
    }

    private Model model = new Model();

    private Atom() {
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
        setChanged();
        notifyObservers();
    }
}