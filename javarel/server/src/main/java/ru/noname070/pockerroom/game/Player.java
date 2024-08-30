package ru.noname070.pockerroom.game;

import ru.noname070.pockerroom.Config;
import ru.noname070.pockerroom.game.commons.*;
import ru.noname070.pockerroom.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;

@Getter

public class Player {
    private static final Gson gson = new GsonBuilder().create();
    private final String name;
    private final String token;
    private final Session session;
    private boolean folded = false;
    private int capital = Config.START_CAPITAL;
    private Hand hand;

    public Player(String name, String token, Session session) {
        this.name = name;
        this.token = token;
        this.session = session;
    }

    public void reciveCards(Card c1, Card c2) {
        this.hand = new Hand(c1, c2);
    }

    public Pair<Card, Card> getCards() {
        return this.hand.getCards();
    }

    public void sendMessage(Action type, Map<String, Object> data) {
        try {
            session.getBasicRemote().sendText(gson.toJson(new HashMap<>() {
                {
                    put("type", type.name());
                    put("data", data);
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Action type, String data) {
        try {
            session.getBasicRemote().sendText(gson.toJson(new HashMap<>() {
                {
                    put("type", type.name());
                    put("data", data);
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Action type) {
        try {
            session.getBasicRemote().sendText(gson.toJson(new HashMap<>() {
                {
                    put("type", type.name());
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fold() {
        this.hand = null;
        this.folded = true;
    }

    public boolean hasFolded() {
        return this.folded;
    }

    public Pair<Integer, Action> bet(int n) {
        if (capital >= n) {
            capital -= n;
            return new Pair<>(n, Action.BET);
        } else {
            int remainingCapital = capital;
            capital = 0;
            return new Pair<>(remainingCapital, Action.SIDEPOT);
        }
    }

    public Pair<Integer, Action> raise(int n) {
        if (capital > n) {
            capital -= n;
            return new Pair<>(n, Action.RAISE);
        } else {
            int remainingCapital = capital;
            capital = 0;
            return new Pair<>(remainingCapital, Action.SIDEPOT);
        }
    }

    public Pair<Integer, Action> allIn() {
        int remainingCapital = capital;
        capital = 0;
        return new Pair<>(remainingCapital, Action.ALL_IN);
    }

    public void check() {
        return; // а че бубнеть то
    }

    public void call(int amount) {
        if (capital >= amount) {
            capital -= amount;
            sendMessage(Action.CALL);
        } else {
            sendMessage(Action.ERR);
        }
    }

    public Action requestAction(String defaultAction, int amountToCall) {
        sendMessage(Action.REQUEST, new HashMap<>() {{
            put("amount", amountToCall);
            put("player", name);
        }});
    
        final Action[] action = new Action[1];
        final CountDownLatch latch = new CountDownLatch(1);
    
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    @SuppressWarnings("unchecked") // gson base
                    Map<String, String> response = gson.fromJson(message, Map.class);
                    action[0] = Action.valueOf(((String) response.get("type")).trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    sendMessage(Action.ERR, Collections.singletonMap("message", "Invalid action. Please choose CALL, RAISE, or FOLD."));
                } finally {
                    latch.countDown();
                }
            }
        });
    
        try {
            latch.await(Config.PLAYER_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        return action[0] != null ? action[0] : Action.FOLD;
    }
    

    public int requestCallAmount() {
        sendMessage(Action.CALL);

        final int[] raiseAmount = new int[1];
        final CountDownLatch latch = new CountDownLatch(1);

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    raiseAmount[0] = Integer.parseInt(message.trim());
                } catch (NumberFormatException e) {
                    sendMessage(Action.ERR);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await(Config.PLAYER_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return raiseAmount[0];
    }

    public int decreaseCapital(int amount) {
        if (amount <= capital) {
            return capital -= amount;
        } else {
            sendMessage(Action.ERR);
            return 0;
        }
    }

    public Pair<Combo, List<Card>> getMaxCombo(List<Card> board) {
        List<Card> allCards = new ArrayList<>(this.hand.asList()) {
            {
                addAll(board);
            }
        };
        return new Pair<>(Combo.findCombo(allCards), allCards);
    }

    public int compareTo(Player o, List<Card> board) throws Exception {
        Pair<Combo, List<Card>> thisCombo = this.getMaxCombo(board);
        Pair<Combo, List<Card>> otherCombo = o.getMaxCombo(board);

        int comboComparison = thisCombo.getFirst().compareTo(otherCombo.getFirst());

        if (comboComparison != 0) {
            return comboComparison;
        }

        List<Card> thisComboCards = thisCombo.getSecond();
        List<Card> otherComboCards = otherCombo.getSecond();

        Collections.sort(thisComboCards, Collections.reverseOrder());
        Collections.sort(otherComboCards, Collections.reverseOrder());

        for (int i = 0; i < thisComboCards.size(); i++) {
            int cardComparison = thisComboCards.get(i).compareTo(otherComboCards.get(i));
            if (cardComparison != 0) {
                return cardComparison;
            }
        }

        List<Card> thisRemainingCards = new ArrayList<>(board);
        thisRemainingCards.addAll(this.hand.asList());
        thisRemainingCards.removeAll(thisComboCards);

        List<Card> otherRemainingCards = new ArrayList<>(board);
        otherRemainingCards.addAll(o.getHand().asList());
        otherRemainingCards.removeAll(otherComboCards);

        Collections.sort(thisRemainingCards, Collections.reverseOrder());
        Collections.sort(otherRemainingCards, Collections.reverseOrder());

        for (int i = 0; i < Math.min(thisRemainingCards.size(), otherRemainingCards.size()); i++) {
            int kickerComparison = thisRemainingCards.get(i).compareTo(otherRemainingCards.get(i));
            if (kickerComparison != 0) {
                return kickerComparison;
            }
        }

        throw new Exception(
                String.format(
                        "чзх у челов одинаковые карты: %s:%s, %s:%s, board:%s",
                        this.getName(),
                        this.getHand()
                                .asList()
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" ")),
                        o.getName(),
                        o.getHand()
                                .asList()
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" ")),
                        board.stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" "))));
    }
}