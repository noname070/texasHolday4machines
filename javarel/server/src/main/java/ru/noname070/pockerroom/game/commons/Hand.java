package ru.noname070.pockerroom.game.cards;

import java.util.Arrays;
import java.util.List;

import ru.noname070.pockerroom.game.util.Pair;

public class Hand {
    private final Card c1;
    private final Card c2;

    public Hand(Card c1, Card c2) {
        this.c1 = c1;
        this.c2 = c2;
    }

    public Pair<Card, Card> getCards() {
        return new Pair<>(c1, c2);
    }

    public List<Card> asList() {
        return Arrays.asList(c1, c2);
    }

}
