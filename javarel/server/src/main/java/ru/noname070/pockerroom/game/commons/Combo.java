package ru.noname070.pockerroom.game.cards;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum Combo {
    HIGHEST_CARD,
    PAIR,
    TWO_PAIR,
    THREE,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR,
    STRAIGHT_FLUSH,
    ROYAL_FLUSH;

    public static Combo findCombo(List<Card> cards) {
        Collections.sort(cards);

        Map<Rank, Long> rankCounts = cards.stream()
            .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
        Map<Suit, Long> suitCounts = cards.stream()
            .collect(Collectors.groupingBy(Card::getSuit, Collectors.counting()));

        boolean isFlush = suitCounts.values().stream().anyMatch(count -> count >= 5);
        boolean isStraight = isStraight(cards);
        boolean isRoyal = isRoyal(cards);

        if (isFlush && isStraight && isRoyal) {
            return ROYAL_FLUSH;
        } else if (isFlush && isStraight) {
            return STRAIGHT_FLUSH;
        } else if (rankCounts.containsValue(4L)) {
            return FOUR;
        } else if (rankCounts.containsValue(3L) && rankCounts.containsValue(2L)) {
            return FULL_HOUSE;
        } else if (isFlush) {
            return FLUSH;
        } else if (isStraight) {
            return STRAIGHT;
        } else if (rankCounts.containsValue(3L)) {
            return THREE;
        } else if (rankCounts.values().stream().filter(count -> count == 2).count() == 2) {
            return TWO_PAIR;
        } else if (rankCounts.containsValue(2L)) {
            return PAIR;
        } else {
            return HIGHEST_CARD;
        }
    }

    private static boolean isStraight(List<Card> cards) {
        List<Integer> ranks = cards.stream()
            .map(card -> card.getRank().ordinal())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        for (int i = 0; i <= ranks.size() - 5; i++) {
            if (ranks.get(i + 4) - ranks.get(i) == 4) {
                return true;
            }
        }

        if (ranks.contains(Rank.ACE.ordinal()) &&
            ranks.contains(Rank.TWO.ordinal()) &&
            ranks.contains(Rank.THREE.ordinal()) &&
            ranks.contains(Rank.FOUR.ordinal()) &&
            ranks.contains(Rank.FIVE.ordinal())) {
            return true;
        }

        return false;
    }

    private static boolean isRoyal(List<Card> cards) {
        Set<Rank> royalRanks = EnumSet.of(Rank.ACE, Rank.KING, Rank.QUEEN, Rank.JACK, Rank.TEN);
        Set<Rank> cardRanks = cards.stream()
            .map(Card::getRank)
            .collect(Collectors.toSet());

        return cardRanks.containsAll(royalRanks);
    }
}