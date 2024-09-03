package ru.noname070.pockerroom.server;

import ru.noname070.pockerroom.Config;
import ru.noname070.pockerroom.db.DBHandler;
import ru.noname070.pockerroom.game.Player;
import ru.noname070.pockerroom.game.TexasHolday;
import ru.noname070.pockerroom.server.util.Request;
import ru.noname070.pockerroom.util.CycleIterator;
import ru.noname070.pockerroom.util.Pair;

import javax.websocket.Session;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.ServerEndpoint;

import lombok.extern.java.Log;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * кринжатина ебаная, тут немного сгенерировано говнокода, но в целом норм
 * 
 * подклбчаться на ws://host/
 * 
 */
@Log
@ServerEndpoint("/game")
public class PockerServer {

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private Iterator<Player> playerIter;
    private final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private TexasHolday game;

    @OnOpen
    public void onOpen(Session session) {
        Pair<String, String> tokenAndName = getTokenAndName(session.getQueryString());

        if (tokenAndName != null && isValidToken(tokenAndName.getFirst())) {
            log.info("New connection with valid token: " + session.getId());
            session.getAsyncRemote().sendText(Request.builder()
                    .type("authOK")
                    .build()
                    .toJson());
            sessions.add(session);
            Player player = new Player(tokenAndName.getSecond(), tokenAndName.getFirst(), session);
            players.put(tokenAndName.getFirst(), player);

            {
                if (players.size() == Config.NUM_PLAYERS) {
                    playerIter = new CycleIterator<>(players.values());
                    initGame();
                }
            }

        } else {
            try {
                session.getAsyncRemote().sendText(Request
                        .builder()
                        .type("authERR")
                        .error("incorrect token")
                        .build().toJson());
                session.close();
                log.log(Level.INFO, "Connection closed due to invalid token %s", session.getId());
            } catch (Exception e) {
                log.log(Level.WARNING, "", e);
            }
        }
    }

    private Pair<String, String> getTokenAndName(String qString) {
        if (qString == null)
            return null;
        Map<String, String> qParams = Arrays.stream(qString.split("&"))
                .map(p -> p.split("="))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

        String token = qParams.get("token");
        String name = qParams.get("name");

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

        log.log(Level.INFO, "New message from session %s : ".formatted(session.getId(), message));
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
                .type("gameInfo")
                .action("GAME_START")
                .build());

        log.info("Game started");
        game.newGame();
    }
}