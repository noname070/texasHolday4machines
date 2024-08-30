package ru.noname070.pockerroom.server;

import ru.noname070.pockerroom.db.DBHandler;
import ru.noname070.pockerroom.game.Player;
import ru.noname070.pockerroom.game.TexasHolday;
import ru.noname070.pockerroom.game.commons.Action;
import ru.noname070.pockerroom.server.messages.IMessage;
import ru.noname070.pockerroom.server.messages.InfoMessage;
import ru.noname070.pockerroom.server.messages.StateMessage;
import ru.noname070.pockerroom.util.CycleIterator;
import ru.noname070.pockerroom.util.Pair;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import lombok.extern.java.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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
    private final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private final CycleIterator<Player> playerIterator = new CycleIterator<>(players.values());
    private TexasHolday game;

    @OnOpen
    public void onOpen(Session session) {
        Pair<String, String> tokenAndName = getTokenAndName(session.getQueryString());

        if (tokenAndName != null && isValidToken(tokenAndName.getFirst())) {
            System.out.println("New connection with valid token: " + session.getId());
            session.getAsyncRemote().sendText("OK");
            sessions.add(session);
            Player player = new Player(tokenAndName.getSecond(), tokenAndName.getFirst(), session);
            players.put(tokenAndName.getFirst(), player);

            if (players.size() >= 2) {
                startGame();
            }
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

        if (player != null) {
            handleGameAction(session, message);
            sendMessage(session, new InfoMessage(Action.OK));
        } else {
            sendMessage(session, new InfoMessage(Action.ERR));
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Connection closed: " + session.getId());
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
        log.log(null, "Err with session:" + session, throwable);
    }

    /**
     * метод должен принимать от игрока одно из enum Action действий и обрабатывать
     * его.
     * введи при этом порядок очереди игроков, которые должны совершать свой ход -
     * он начинается от диллера. можешь использовать util.CycleIterator для учета
     * ходов.
     * при этом в конце выполнения действия игроку должно вернуться
     * InfoMessage(Action.OK/ERR). Если err он должен заного сделать запрос.
     * всем игрокам должны высылаться обновленное состояние StateMessage с
     * информацией о ставках и картах на столе;
     *
     */
    private void handleGameAction(Session session, String message) {
        Player p = players.values().stream()
                .filter(pl -> pl.getSession().equals(session))
                .findFirst()
                .orElse(null);

        if (p == null) {
            sendMessage(session, new InfoMessage(Action.ERR));
            return;
        }

        String[] parts = message.split(" ");
        Action action;
        try {
            action = Action.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            sendMessage(session, new InfoMessage(Action.ERR));
            return;
        }

        switch (action) {
            case BET: {
                int amount = 0;
                try {
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    sendMessage(session, new InfoMessage(Action.ERR));
                    return;
                }
                game.placeBet(p, amount);
            }
            case CALL:
                game.call(p);
            case RAISE: {
                int amount = 0;
                try {
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    sendMessage(session, new InfoMessage(Action.ERR));
                    return;
                }
                game.raise(p, amount);
            }
            case FOLD: {
                game.fold(p);
            }
            case CHECK: {
                game.check(p);
            }
            case ALL_IN: {
                game.allIn(p);
            }

            default:
                broadcast(new StateMessage(game.getPublicState()));
        }
        Player nextPlayer = playerIterator.next();
        sendMessage(nextPlayer.getSession(), new InfoMessage(Action.YOUR_TURN));

    }

    private void broadcast(IMessage message) {
        sessions.forEach(session -> sendMessage(session, message));
    }

    private void sendMessage(Session session, IMessage message) {
        session.getAsyncRemote().sendText(message.build());
    }

    private void startGame() {
        game = new TexasHolday(players.values().toArray(new Player[0]));
        broadcast(new InfoMessage(Action.GAME_START));
        game.newGame();
    }
}