package ru.noname070.pockerroom;

import ru.noname070.pockerroom.db.DBHandler;
import ru.noname070.pockerroom.game.Player;
import ru.noname070.pockerroom.game.TexasHolday;
import ru.noname070.pockerroom.game.util.Pair;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * кринжатина ебаная, тут немного сгенерировано говнокода, но в целом норм
 * 
 * подклбчаться на ws://host/game
 * 
 * TODO очень много надо дописать в Game и в Player что бы соглаосвать это и их.
 */
@ServerEndpoint("/game")
public class PockerServer {

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private TexasHolday game;

    @OnOpen
    public void onOpen(Session session) {
        Pair<String, String> tokenAndName = getTokenAndName(session.getQueryString());

        if (tokenAndName.getFirst() != null && isValidToken(tokenAndName.getFirst())) {
            System.out.println("New connection with valid token: " + session.getId());
            sessions.add(session);
            players.put(tokenAndName.getFirst(),
                    new Player(tokenAndName.getSecond(), tokenAndName.getFirst(), session));
        } else {
            try {
                session.getAsyncRemote().sendText("ERROR: your token incorrect");
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
                    if (keyValue[0] == "token") {
                        token = keyValue[1];
                    } else if (keyValue[0] == "name") {
                        name = keyValue[1];
                    }
                }
            }
        }
        return (token != name && token != "" && name != "") ? new Pair<>(token, name) : null;
    }

    private boolean isValidToken(String token) {
        if (token != null && !token.isEmpty()) {
            return DBHandler.getInstance().isUserTokenExists(token);
        }
        return false;
    }

    /**
     * TODO надо дописать Player.getGameState() -> game.getPublicState()
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        String[] parts = message.split(" ");
        String command = parts[0];

        switch (command) {
            case "READY":
                String name = parts[1];
                String token = UUID.randomUUID().toString();
                Player player = new Player(name, token, session);
                players.put(token, player);
                session.getAsyncRemote().sendText("AUTH_SUCCESS " + token);
                if (players.size() == Config.NUM_PLAYERS) {
                    startGame(); // TODO а мы тут не застрянем?
                }
                break;
            case "BET":
            case "FOLD":
            case "CALL":
                String playerToken = parts[1];
                Player currentPlayer = players.get(playerToken);
                if (currentPlayer != null) {
                    handleGameAction(command, currentPlayer, parts);
                } else {
                    session.getAsyncRemote().sendText("ERROR Unauthorized");
                }
                break;
            default:
                session.getAsyncRemote().sendText("ERROR Unknown command");
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Connection closed: " + session.getId());
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("Error: " + throwable.getMessage());
    }

    /**
     * TODO тут вообще все надо дописать. связать Player, TexasHolday и все еще отправлять туда сюда.
     * 
     * из идей - передавать игре пару Player : action и там решать че кто делает
     *
     */
    private void handleGameAction(String action, Player player, String[] parts) {
        switch (action) {
            case "BET":
                int betAmount = Integer.parseInt(parts[2]);
                player.bet(betAmount);
                // TODO а если не хватает бабок.
                break;
            case "FOLD":
                player.fold();
                break;
            case "CALL":
                // TODO
                break;
        }
        broadcast(String.format("ACTION %s %s", player.getName(), action));
    }

    private void broadcast(String message) {
        sessions.forEach(
                session -> session.getAsyncRemote().sendText(message));
    }

    private void startGame() {
        game = new TexasHolday(players.values().toArray(new Player[0]));
        game.newGame();
        broadcast("GAME_STARTED");
    }
}