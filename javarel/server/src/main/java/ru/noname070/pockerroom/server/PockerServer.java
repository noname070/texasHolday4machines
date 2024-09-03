package ru.noname070.pockerroom.server;

import ru.noname070.pockerroom.db.DBHandler;
import ru.noname070.pockerroom.game.Player;
import ru.noname070.pockerroom.game.TexasHolday;
import ru.noname070.pockerroom.server.util.Request;
import ru.noname070.pockerroom.util.CycleIterator;
import ru.noname070.pockerroom.util.Pair;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import lombok.Getter;
import lombok.extern.java.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

/**
 * кринжатина ебаная, тут немного сгенерировано говнокода, но в целом норм
 * 
 * подклбчаться на ws://host/
 * 
 */
@Log
@ServerEndpoint("/game")
public class PockerServer {

    @Getter
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private Iterator<Player> playerIter;
    private final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private TexasHolday game;

    @OnOpen
    public void onOpen(Session session) {
        Pair<String, String> tokenAndName = getTokenAndName(session.getQueryString());

        if (tokenAndName != null && isValidToken(tokenAndName.getFirst())) {
            log.info("New connection with valid token: " + session.getId());
            session.getAsyncRemote().sendText("OK");
            sessions.add(session);
            Player player = new Player(tokenAndName.getSecond(), tokenAndName.getFirst(), session);
            players.put(tokenAndName.getFirst(), player);

            // костыль но похуй
            playerIter = new CycleIterator<>(players.values());

        } else {
            try {
                session.getAsyncRemote().sendText("ERROR: token incorrect");
                session.close();
                System.out.println("Connection closed due to invalid token: " + session.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Pair<String, String> getTokenAndName(String queryString) {
        String token = "";
        String name = "";
        if (queryString != null) {
            for (String param : queryString.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    if (keyValue[0].equals("token")) {
                        token = keyValue[1];
                    } else if (keyValue[0].equals("name")) {
                        name = keyValue[1];
                    }
                }
            }
        }
        return (!token.isEmpty() && !name.isEmpty()) ? new Pair<>(token, name) : null;
    }

    private boolean isValidToken(String token) {
        return DBHandler.getInstance().isUserTokenExists(token);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        Player player = players.values().stream()
                .filter(p -> p.getSession().equals(session))
                .findFirst()
                .orElse(null);

        log.log(Level.INFO, "New message from session " + session.getId() + " : " + message);
        if (player != null) {
            game.handleGameAction(player, message);
            player.setWaitingForAction(false);
        } else {
            session.getAsyncRemote().sendText(Request.builder()
                    .type("err")
                    .error("can`t handle yr response")
                    .build().toJson());
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.info("Session %s closed".formatted(session.getId()));
        sessions.remove(session);
        players.values().removeIf(p -> {
            if (p.getSession().equals(session)) {
                log.info("Player %s left".formatted(p.getName()));
                return true;
            }
            return false;
        });
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.log(Level.WARNING, "Err with session:" + session, throwable);
    }

    public void initGame() {
        game = new TexasHolday(
            playerIter,
            players.values().toArray(new Player[0]));
    }

    public void startGame() {
        game.broadcast(Request.builder()
                .type("action")
                .action("GAME_START")
                .build());

        log.info("Game started");
        game.newGame();
    }
}