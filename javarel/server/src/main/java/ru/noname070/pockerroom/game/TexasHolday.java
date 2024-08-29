package ru.noname070.pockerroom.game;

import java.util.*;
import java.util.stream.Collectors;

import ru.noname070.pockerroom.Config;
import ru.noname070.pockerroom.game.commons.*;
import ru.noname070.pockerroom.game.util.CycleIterator;

public class TexasHolday {

    private final List<Player> players;
    private final Iterator<Player> dealers;
    private final List<Card> board;
    private Map<Player, Integer> bets;
    private Iterator<Card> deck;

    private int maxbet;

    public TexasHolday(Player... players) {
        this.players = Arrays.asList(players);
        this.dealers = new CycleIterator<>(players);
        this.board = new ArrayList<>();
    }

    public void newGame() {
        this.deck = Card.newDeck().iterator();
        for (Player p : this.players) {
            p.reciveCards(deck.next(), deck.next());
            p.sendMessage("Your cards: " + p.getHand().asList().toString());
        }

        Player dealer = dealers.next();
        bets = new HashMap<>() {
            {
                put(dealer, 0);
                put(players.get((players.indexOf(dealer) + 1) % players.size()), Config.SMALL_BLIND);
                put(players.get((players.indexOf(dealer) + 2) % players.size()), Config.SMALL_BLIND * 2);
            }
        };

        playRound();
    }

    private void playRound() {
        bettingRound();

        revealCards(3);
        bettingRound();

        revealCards(1);
        bettingRound();

        revealCards(1);
        bettingRound();

        List<Player> winners = sortByComboPower();

        System.out.printf("Board : %s\n",
                this.board.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(" ")));

        winners.forEach((Player p) -> {
            System.out.printf("%s : %s\n", p.getName(), p.getHand().asList());
            p.sendMessage("Your final hand: " + p.getHand().asList().toString());
        });
    }

    private void revealCards(int n) {
        for (int i = 0; i < n; i++) {
            this.board.add(this.deck.next());
        }
        broadcastBoard();
    }

    private void bettingRound() {
        bets.forEach((Player p, Integer n) -> {
            maxbet = Math.max(n, maxbet);
        });


        // TODO может переписать на switch/case?
        while (!bets.values().stream().allMatch(x -> x == maxbet)) {
            for (Player p : this.players) {
                if (bets.get(p) != maxbet) {
                    int amountToCall = maxbet - bets.get(p);
                    Action action = p.requestAction("CALL", amountToCall);
                    if (action == Action.CALL) {
                        bets.put(p, bets.get(p) + amountToCall);
                    } else if (action == Action.RAISE) {
                        int raiseAmount = p.requestRaiseAmount();
                        maxbet += raiseAmount;
                        bets.put(p, bets.get(p) + amountToCall + raiseAmount);
                        broadcast("Player " + p.getName() + " raised to " + maxbet);
                    } else if (action == Action.FOLD) {
                        bets.remove(p);
                        p.fold();
                    }
                }
            }
        }
    }

    private List<Player> sortByComboPower() {
        return this.players
                .stream()
                .sorted((o1, o2) -> {
                    try {
                        return o1.compareTo(o2, board);
                    } catch (Throwable e) {
                        e.printStackTrace(); // Обработка ошибок
                    }
                    return 0;
                }).collect(Collectors.toList());
    }

    private void broadcastBoard() {
        String boardState = "Board: " + board.stream().map(Object::toString).collect(Collectors.joining(" "));
        broadcast(boardState);
    }

    private void broadcast(String message) {
        for (Player p : this.players) {
            p.sendMessage(message);
        }
    }
}