package ru.noname070.pockerroom.game.cards;

import java.util.List;
import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Collections;

public class Card implements Comparable<Card> {
    private final Rank rank;
    private final Suit suit;

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public Rank getRank() {
        return this.rank;
    }

    public Suit getSuit() {
        return this.suit;
    }

    public static List<Card> newDeck() {
        List<Card> deck = Arrays.stream(Rank.values())
                .flatMap(rank -> Arrays.stream(Suit.values())
                        .map(suit -> new Card(rank, suit)))
                .collect(Collectors.toList());

        Collections.shuffle(deck);
        return deck;
    }

    @Override
    public int compareTo(Card o) {
        return this.rank.compareTo(o.getRank());
    }

    @Override
    public String toString() {
        return String.format("[%s %s]", this.rank.name(), this.suit.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Card card = (Card) o;
        return rank == card.rank && suit == card.suit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, suit);
    }

}
