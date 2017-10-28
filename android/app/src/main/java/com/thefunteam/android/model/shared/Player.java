package com.thefunteam.android.model.shared;

import java.util.List;

public class Player {
    public final String username;
    public final List<Route> routes;
    public final List<TrainType> trainCards;
    public final List<DestinationCard> destCards;
    public int score;
    public int trainsLeft;
    public boolean currentPlayer;

    public Player(String username, List<Route> routes, List<TrainType> trainCards,
                  List<DestinationCard> destCards, int score, int trainsLeft,
                  boolean currentPlayer) {
        this.username = username;
        this.routes = routes;
        this.trainCards = trainCards;
        this.destCards = destCards;
        this.score = score;
        this.trainsLeft = trainsLeft;
        this.currentPlayer = currentPlayer;
    }

    public String getUsername() {
        return username;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<TrainType> getTrainCards() {
        return trainCards;
    }

    public List<DestinationCard> getDestCards() {
        return destCards;
    }

    public int getScore() {
        return score;
    }

    public int getTrainsLeft() {
        return trainsLeft;
    }

    public boolean isCurrentPlayer() {
        return currentPlayer;
    }
}