from collections import Counter
from enum import Enum 
from typing import List, Optional, Tuple
import random

Actions = Enum("Actions", ["WAIT_GAME", "BET", "CALL", "CHECK", "FOLD", "RAISE", "RETURN", "SHOW", "WIN", "SIDEPOT"])
Combo = Enum("Combo", ["HIGHEST_CARD", "PAIR", "TWO_PAIR", "THREE", "STRAIGHT", "FLUSH", "FULL_HOUSE", "FOUR", "STRAIGHT_FLUSH", "ROYAL_FLUSH"])
Suit = Enum("Suit", ["HEARTS", "DIAMONDS", "CLUBS", "SPADES"]) # ♥ ♦ ♣ ♠H
Rank = Enum("Rank", ["2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"])

class Card():
    def __init__(self, rank: Rank, suit: Suit):
        self.rank = rank
        self.suit = suit
        
    def __str__(self):
        return f"{self.rank.name}{self.suit.name}"

    def __eq__(self, c):
        return self.rank == c.rank

    def __lt__(self, c):
        return self.rank.value < c.rank.value
    
    def __hash__(self) -> int:
        return hash(self.__str__())

class Player:
    def __init__(self, name) -> None:
        self.name = name
        self.capital = 0 # TODO
        self.hand: List[Card] = []
        self.status: Actions = Actions.WAIT_GAME

    def get_name(self) -> str:
        return self.name

    def receive_card(self, new_card: Card) -> None:
        if isinstance(new_card, Card):
            self.hand.append(new_card)

    def show_hand(self) -> List[Card]:
        return self.hand

    def bet(self, n: int) -> int:
        if n > self.capital:
            return self.capital # TODO +SIDEPOT
        self.capital -= n
        return n
    
    # кринж ебаный
    def get_max_combo(self, l: List[Card]) -> Tuple[Combo, List[Card]]:
        all_cards = self.hand + l

        def is_flush(cards: List[Card]) -> Optional[List[Card]]:
            suit_counts = Counter(card.suit for card in cards)
            flush_suit = next((suit for suit, count in suit_counts.items() if count >= 5), None)
            return sorted([card for card in cards if card.suit == flush_suit], reverse=True)[:5] if flush_suit else []
        
        def is_straight(cards: List[Card]) -> Optional[List[Card]]:
            seen_ranks = set()
            unique_cards = []
            for card in sorted(cards, key=lambda card: card.rank.value, reverse=True):
                if card.rank not in seen_ranks:
                    seen_ranks.add(card.rank)
                    unique_cards.append(card)

            for i in range(len(unique_cards) - 4):
                if unique_cards[i].rank.value - unique_cards[i + 4].rank.value == 4:
                    return unique_cards[i:i + 5]
            return []
        
        def rank_counts(cards: List[Card]) -> Counter:
            return Counter(card.rank for card in cards)

        def get_combinations(cards: List[Card]) -> Tuple[Combo, List[Card]]:
            flush_cards = is_flush(cards)
            straight_cards = is_straight(cards)
            rank_count = rank_counts(cards)

            if flush_cards and straight_cards:
                flush_ranks = set(card.rank for card in flush_cards)
                if set(straight_cards).issubset(flush_ranks):
                    if flush_ranks == {Rank.A, Rank.K, Rank.Q, Rank.J, Rank["10"]}:
                        return Combo.ROYAL_FLUSH, flush_cards
                    return Combo.STRAIGHT_FLUSH, flush_cards[:5]

            if 4 in rank_count.values():
                four_of_a_kind = [card for card in cards if rank_count[card.rank] == 4]
                kicker = max([card for card in cards if rank_count[card.rank] != 4], key=lambda c: c.rank.value)
                return Combo.FOUR, four_of_a_kind + [kicker]

            if 3 in rank_count.values() and 2 in rank_count.values():
                three_of_a_kind = [card for card in cards if rank_count[card.rank] == 3]
                pair = [card for card in cards if rank_count[card.rank] == 2]
                return Combo.FULL_HOUSE, three_of_a_kind + pair[:2]

            if flush_cards:
                return Combo.FLUSH, flush_cards[:5]

            if straight_cards:
                return Combo.STRAIGHT, straight_cards

            if 3 in rank_count.values():
                three_of_a_kind = [card for card in cards if rank_count[card.rank] == 3]
                kickers = sorted([card for card in cards if rank_count[card.rank] != 3], key=lambda c: c.rank.value, reverse=True)[:2]
                return Combo.THREE, three_of_a_kind + kickers

            pairs = sorted([rank for rank, count in rank_count.items() if count == 2], key=lambda rank: rank.value, reverse=True)
            if len(pairs) >= 2:
                two_pairs = [card for card in cards if card.rank in pairs[:2]]
                kicker = max([card for card in cards if card.rank not in pairs[:2]], key=lambda c: c.rank.value)
                return Combo.TWO_PAIR, two_pairs + [kicker]

            if len(pairs) == 1:
                pair = [card for card in cards if card.rank == pairs[0]]
                kickers = sorted([card for card in cards if card.rank != pairs[0]], key=lambda c: c.rank.value, reverse=True)[:3]
                return Combo.PAIR, pair + kickers

            sorted_cards = sorted(cards, key=lambda card: card.rank.value, reverse=True)[:5]
            return Combo.HIGHEST_CARD, sorted_cards
    
        return get_combinations(all_cards)

def get_probability(cards: List[Card], board: List[Card]): pass # TODO расчитать текущую вероятность выиграша

def compare_hands(p1: Player, p2: Player, board: List[Card]) -> int:
    c1, h1 = p1.get_max_combo(board)
    c2, h2 = p2.get_max_combo(board)

    if c1.value > c2.value:
        return 1
    elif c1.value < c2.value:
        return -1
    else:
        # kickers 
        h1_sorted = sorted(h1, key=lambda card: card.rank.value, reverse=True)
        h2_sorted = sorted(h2, key=lambda card: card.rank.value, reverse=True)

        for card1, card2 in zip(h1_sorted, h2_sorted):
            if card1.rank.value > card2.rank.value:
                return 1
            elif card1.rank.value < card2.rank.value:
                return -1

        raise Exception("чзх у чела карты один в один")
        return 0 # lmao wtf

class Deck():
    def __init__(self):
        self.cards: List[Card] = [Card(v, s) for v in list(Rank) for s in list(Suit)]                
        random.shuffle(self.cards)

    def get_card(self):
        return self.cards.pop()
