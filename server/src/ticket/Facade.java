package ticket;

import shared.*;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static shared.TrainType.any;
import static shared.TurnState.*;

public class Facade {
    private static Object run(State.Swapper swapper, String key, boolean getByUsername) {
        State m;
        try {
            m = State.swap(swapper);
        } catch (BadJuju e) {
            return e.toMap();
        }
        String sessionId;
        if (getByUsername) {
            sessionId = m.getNewestSession(key);
        } else {
            sessionId = key;
        }
        return success(m, sessionId);
    }

    private static Object run(State.Swapper swapper, String sessionId) {
        return run(swapper, sessionId, false);
    }

    private static ClientModel success(State m, String sessionId) {
        return m.getClientModel(sessionId);
    }

    public static Object register(String username, String password) {
        return run((state) -> {
            state.checkUsernameAvailable(username);

            Object[] path = new Object[] {"users", username};
            User u = new User(username, password, path);
            state = state.commit(u);
            return createSession(state, username);
        }, username, true);
    }

    public static Object login(String username, String password) {
        return run((state) -> {
            state.authenticate(username, password);
            return createSession(state, username);
        }, username, true);
    }

    public static Object create(String sessionId) {
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkNoGame(sessionId);

            String gameId = UUID.randomUUID().toString();
            Object[] path = {"games", gameId};
            Game g = new Game(gameId, sessionId, false, path)
                    .addHistory(state, sessionId, "created the game");
            Session s = state.getSession(sessionId).setGameId(gameId);
            return state.commit(g, s);
        }, sessionId);
    }

    public static Object join(String sessionId, String gameId) {
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkNoGame(sessionId);
            state.checkGameAvailable(sessionId, gameId);

            Game game = state.getGame(gameId)
                        .addSessionId(sessionId)
                        .addHistory(state, sessionId, "joined the game");

            Session ses = state.getSession(sessionId).setGameId(gameId);
            return state.commit(game, ses);
        }, sessionId);
    }

    public static Object leave(String sessionId){
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkHasGame(sessionId);
            state.checkStarted(state.getGameBySession(sessionId), false);

            Session session = state.getSession(sessionId);
            Game game = state.getGameBySession(sessionId);
            state = state.commit(session.setGameId(null));
            if (game.getSessionIds().size() == 1) {
                return state.delete(game);
            } else {
                game = game.removeSessionId(sessionId)
                           .addHistory(state, sessionId, "left the game");
                return state.commit(game);
            }
        }, sessionId);
    }

    public static Object start(String sessionId){
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkHasGame(sessionId);
            Game game = state.getGameBySession(sessionId);
            state.checkStarted(game, false);
            state.checkEnoughPlayers(game);

            game = game.addHistory(state, sessionId, "started the game")
                       .start();
            for (Session ses : game.getSessions(state)) {
                for (int i = 0; i < 4; i++) {
                    ses = ses.giveTrain(game.topTrain());
                    game = game.drawCard("trainDeck");
                }
                for (int i = 0; i < 3; i++) {
                    ses = ses.givePendingDest(game.topDest());
                    game = game.drawCard("destDeck");
                }
                ses = ses.setTurnState(init);
                state = state.commit(ses);
            }
            game = game.fillFaceUp();
            return state.commit(game);
        }, sessionId);
    }

    public static Object chat(String sessionId, String message){
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkHasGame(sessionId);

            String name = state.getUserBySessionId(sessionId).getName();
            Game game = state.getGameBySession(sessionId)
                        .sendMessage(name + ": " + message)
                        .addHistory(state, sessionId, "sent a message");

            return state.commit(game);
        }, sessionId);
    }

    public static Object drawDest(String sessionId) {
        return run((state) -> {
            state.checkTurnState(sessionId, beginning);
            Session ses = state.getSession(sessionId);
            Game game = state.getGame(ses.getGameId());
            state.checkDestDeckNotEmpty(game);

            int nCards = Math.min(3, game.getDestDeck().size());
            for (int i = 0; i < nCards; i++) {
                ses = ses.givePendingDest(game.topDest());
                game = game.drawCard("destDeck");
            }
            ses = ses.setTurnState(returnDest);
            game = game.addHistory(state, sessionId,
                    "drew destination cards");
            return state.commit(game, ses);
        }, sessionId);
    }

    public static Object returnDest(String sessionId, DestinationCard[] cards) {
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkTurnState(sessionId, init, returnDest);
            Session ses = state.getSession(sessionId);
            state.checkHasPending(ses, cards);
            int max = ses.getTurnState().maxReturnCards();
            if (cards.length > max) {
                throw new BadJuju("You may return at most " + max + " card(s)");
            }

            ses = ses.returnCards(cards);
            Game game = state.getGame(ses.getGameId())
                       .discard(cards)
                       .addHistory(state, sessionId, "returned " + cards.length +
                               " destination card(s)");
            state = state.commit(ses, game);
            state = updatePoints(state, game, sessionId);
            return endTurn(state, sessionId);

        }, sessionId);
    }

    public static Object drawTrain(String sessionId) {
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkTurnState(sessionId, beginning, drawTrain);
            Session ses = state.getSession(sessionId);
            Game game = state.getGame(ses.getGameId());
            state.checkTrainDeckNotEmpty(game);

            TrainType card = game.topTrain();
            ses = ses.giveTrain(card);
            game = game.drawTrainCard()
                       .addHistory(state, sessionId, "drew a train card");
            state = state.commit(ses, game);

            if (ses.getTurnState().equals(beginning) && game.canDrawAgain()) {
                return state.commit(ses.setTurnState(drawTrain));
            }
            return endTurn(state, sessionId);
        }, sessionId);
    }

    public static Object drawFaceupTrain(String sessionId, int index) {
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkTurnState(sessionId, beginning, drawTrain);
            Session ses = state.getSession(sessionId);
            Game game = state.getGame(ses.getGameId());
            state.checkValidFaceupIndex(game, index);
            TrainType card = game.getFaceUpDeck().get(index);
            if (ses.getTurnState().equals(drawTrain) && card.equals(any)) {
                throw new BadJuju("Can't take locomotive on second draw");
            }

            game = game.discardFaceUp(index).addHistory(state, sessionId,
                    "took a face up " + card.cardName() + " card");
            ses = ses.giveTrain(card);
            state = state.commit(game, ses);

            if (ses.getTurnState().equals(beginning) && !card.equals(any)
                    && game.canDrawAgain()) {
                return state.commit(ses.setTurnState(drawTrain));
            }
            return endTurn(state, sessionId);
        }, sessionId);
    }

    public static Object build(String sessionId, Route route,
                               List<TrainType> cards) {
        return run((state) -> {
            state.authenticate(sessionId);
            state.checkTurnState(sessionId, beginning);
            Session ses = state.getSession(sessionId);
            Game game = state.getGame(ses.getGameId());
            state.checkClaimable(game, route);
            state.checkCanClaim(ses, route, cards);

            game = game.claim(route, cards)
                       .addHistory(state, sessionId, "claimed a route");
            ses = ses.claim(route, cards);
            if (game.getTurnsLeft() == 666 && ses.getTrainsLeft() < 3) {
                game = game.setLastRound();
            }
            state = state.commit(game, ses);
            state = updatePoints(state, game, sessionId);
            return endTurn(state, sessionId);
        }, sessionId);
    }

    public static Object state(String sessionId) {
        State s = State.getState();
        try {
            s.authenticate(sessionId);
        } catch (BadJuju e) {
            return e.toMap();
        }
        return success(s, sessionId);
    }

    public static Object clear() {
        // For testing purposes. Hopefully none of our users find out
        // about this endpoint.
        State.swap((state) -> new State());
        return new HashMap();
    }

    private static State createSession(State s, String username) {
        String id = UUID.randomUUID().toString();
        Object[] path = {"sessions", id};
        return s.commit(new Session(id, username, path),
                        s.getUser(username).addSessionId(id));
    }

    private static State endTurn(State s, String sessionId) {
        Session ses = s.getSession(sessionId);
        TurnState oldState = ses.getTurnState();

        ses = ses.setTurnState(waiting);
        s = s.commit(ses);
        Game game = s.getGame(ses.getGameId());

        if (oldState.equals(init)) {
            final State sf = s;
            if (game.getSessionIds().stream().allMatch(sid ->
                    sf.getSession(sid).getTurnState().equals(waiting))) {
                ses = s.getSession(game.getSessionIds().get(0));
                s = s.commit(ses.setTurnState(beginning));
            }
        } else {
            if (game.getTurnsLeft() != 666) {
                game = game.decrementTurnsLeft();
                s = s.commit(game);
            }
            if (game.getTurnsLeft() > 0) {
                List<String> sids = game.getSessionIds();
                int nextIndex = (sids.indexOf(sessionId) + 1) % sids.size();
                Session next = s.getSession(sids.get(nextIndex))
                                .setTurnState(beginning);
                s = s.commit(next);
            }
        }
        return s;
    }

    private static State updatePoints(State s, Game g, String sessionId) {
        final State sf = s;
        int longestRouteLength = g.getSessionIds().stream().mapToInt((sid) ->
                sf.getSession(sid).getLongestRouteLength()).max().orElse(0);
        for (String sid : g.getSessionIds()) {
            boolean cur = sessionId.equals(sid);
            s = s.commit(s.getSession(sid).updatePoints(cur, longestRouteLength));
        }
        return s;
    }
}
