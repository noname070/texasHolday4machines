package ru.noname070.pockerroom.game;

import ru.noname070.pockerroom.Config;
import ru.noname070.pockerroom.game.util.Pair;
import ru.noname070.pockerroom.game.commons.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class Player {
    private final String name;
    private final String token;
    private final Session session;
    private boolean folded;
    private int capital = Config.START_CAPITAL;
    private Hand hand;

    public Player(String name, String token, Session session) {
        this.name = name;
        this.token = token;
        this.session = session;
    }

    public String getName() {
        return this.name;
    }

    public String getToken() {
        return this.token;
    }

    public int getCapital() {
        return this.capital;
    }

    public Hand getHand() {
        return this.hand;
    }

    public void reciveCards(Card c1, Card c2) {
        this.hand = new Hand(c1, c2);
    }

    public Pair<Card, Card> getCards() {
        return this.hand.getCards();
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

    public void fold() {
        this.hand = null;
        this.folded = true;
    }

    public boolean hasFolded() {
        return this.folded;
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Action requestAction(String defaultAction, int amountToCall) {
        sendMessage(String.format("REQUEST_ACTION %s %s", defaultAction, amountToCall));

        CountDownLatch latch = new CountDownLatch(1);
        final Action[] action = new Action[1];

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                action[0] = Action.valueOf(message.trim().toUpperCase());
                latch.countDown();
            }
        });

        try {
            latch.await(Config.PLAYER_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return action[0] != null ? action[0] : Action.FOLD;
    }

    // TODO
    public int requestRaiseAmount() {
        sendMessage("REQUEST_RAISE_AMOUNT");

        CountDownLatch latch = new CountDownLatch(1);
        final int[] raiseAmount = new int[1];

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    raiseAmount[0] = Integer.parseInt(message.trim());
                } catch (NumberFormatException e) {
                    raiseAmount[0] = 0;
                }
                latch.countDown();
            }
        });

        try {
            latch.await(Config.PLAYER_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return raiseAmount[0] > 0 ? raiseAmount[0] : 0;
    }

    public Pair<Combo, List<Card>> getMaxCombo(List<Card> board) {
        List<Card> allCards = new ArrayList<>(this.hand.asList()) {{
            addAll(board);
        }};
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