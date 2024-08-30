package ru.noname070.pockerroom.game;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;
import lombok.extern.java.Log;
import ru.noname070.pockerroom.Config;
import ru.noname070.pockerroom.game.commons.*;
import ru.noname070.pockerroom.util.CycleIterator;

@Log
public class TexasHolday {

    private final List<Player> players;
    private final Iterator<Player> dealers;
    @Getter
    private final List<Card> board;
    private Map<Player, Integer> bets;
    private Iterator<Card> deck;
    private int maxbet;

    private final Gson gson = new GsonBuilder().create();

    public TexasHolday(Player... players) {
        this.players = Arrays.asList(players);
        this.dealers = new CycleIterator<>(players);
        this.board = new ArrayList<>();
    }

    public Iterator<Player> getPlayerIterator() {
        return new CycleIterator<>(players);
    }

    public Map<String, Object> getPublicState() {
        Map<String, Object> state = new HashMap<>();

        state.put("boardCards", board.stream()
                .map(Card::toString)
                .collect(Collectors.joining(";")));

        Map<String, Object> playerStates = new HashMap<>();
        for (Player player : players) {
            Map<String, Object> playerState = new HashMap<>();
            playerState.put("currentBet", bets.getOrDefault(player, 0));
            playerState.put("remainingCapital", player.getCapital());
            playerState.put("isFolded", player.isFolded());
            playerStates.put(player.getName(), playerState);
        }
        state.put("players", playerStates);
        state.put("maxBet", maxbet);

        return state;
    }

    public void newGame() {
        this.deck = Card.newDeck().iterator();
        for (Player p : this.players) {
            p.reciveCards(deck.next(), deck.next());
            // p.sen("Your cards: " + p.getHand().asList().toString());
            p.sendMessage(Action.INFO, new HashMap<>() {
                {
                    put("personalCards", p.getHand().asList());
                }
            });
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

    /**
     * метод реализует непосредственно игру.
     * по окончанию игры всем игрокам отправить сообщение кто победил и с какими
     * картами.
     */
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
            // System.out.printf("%s : %s\n", p.getName(), p.getHand().asList());
            log.info("%s : %s\n".formatted(p.getName(), p.getHand().asList()));
        });

        winners.get(0).sendMessage(Action.WIN);
    }

    /**
     * положить на стол N карт.
     */
    private void revealCards(int n) {
        for (int i = 0; i < n; i++) {
            this.board.add(this.deck.next());
        }
    }

    /**
     * момент уравнивания ставок
     * 
     * ROƒL если игрок некорректно сделает свои действия - он фолднится xd xd xd xd
     *
     */
    private void bettingRound() {
        bets.forEach((Player p, Integer n) -> {
            maxbet = Math.max(n, maxbet);
        });

        boolean allBetsMatched = false;
        while (!allBetsMatched) {
            allBetsMatched = true;
            for (Player p : this.players) {
                if (p.isFolded())
                    continue;
                if (bets.get(p) != maxbet) {
                    int amountToCall = maxbet - bets.get(p);
                    Action action = p.requestAction("CALL", amountToCall);
                    switch (action) {
                        case CALL:
                            if (p.getCapital() >= amountToCall) {
                                bets.put(p, maxbet);
                                p.decreaseCapital(amountToCall);
                            } else {
                                p.sendMessage(Action.ERR);
                                p.fold();
                            }
                            break;
                        case RAISE:
                            int raiseAmount = p.requestCallAmount();
                            if (p.getCapital() >= raiseAmount + amountToCall) {
                                maxbet += raiseAmount;
                                bets.put(p, maxbet);
                                p.decreaseCapital(amountToCall + raiseAmount);
                            } else {
                                p.sendMessage(Action.ERR);
                                p.fold();
                            }
                            break;
                        case FOLD:
                            p.fold();
                            break;
                        default:
                            p.sendMessage(Action.ERR);
                            p.fold();
                    }
                    allBetsMatched = false;
                }
            }
        }
    }

    /**
     * отсортировать комбинации по силе (учитывая карты на столе)
     */
    private List<Player> sortByComboPower() {
        return this.players
                .stream()
                .sorted((o1, o2) -> {
                    try {
                        return o1.compareTo(o2, board);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return 0;
                }).collect(Collectors.toList());
    }

    /**
     * отправить всем игрокам сообщение
     */
    private void broadcast(String message) {
        this.players.forEach(p -> p.sendMessage(Action.INFO, message));
    }

    public void placeBet(Player player, int betAmount) {
        if (player.getCapital() >= betAmount) {
            player.decreaseCapital(betAmount);
            bets.put(player, betAmount);
            maxbet = Math.max(maxbet, betAmount);

            broadcast(gson.toJson(new HashMap<>() {
                {
                    put("player", player.getName());
                    put("action", Action.BET);
                    put("amount", betAmount);
                }
            }));
        } else {
            player.sendMessage(Action.ERR);
        }
    }

    public void call(Player player) {
        int amountToCall = maxbet - bets.getOrDefault(player, 0);
        if (player.getCapital() >= amountToCall) {
            player.decreaseCapital(amountToCall);
            bets.put(player, maxbet);
            broadcast(String.format("%s called", player.getName()));
        } else {
            player.sendMessage(Action.ERR);
            player.fold();
        }
    }

    public void raise(Player player, int raiseAmount) {
        int amountToCall = maxbet - bets.getOrDefault(player, 0);
        if (player.getCapital() >= (amountToCall + raiseAmount)) {
            player.decreaseCapital(amountToCall + raiseAmount);
            maxbet += raiseAmount;
            bets.put(player, maxbet);
            broadcast(gson.toJson(new HashMap<>() {
                {
                    put("player", player.getName());
                    put("action", Action.RAISE);
                    put("amount", maxbet);
                }
            }));
        } else {
            player.sendMessage(Action.ERR);
            player.fold();
        }
    }

    public void fold(Player player) {
        player.fold();
    }

    public void check(Player player) {
        if (bets.getOrDefault(player, 0) == maxbet) {
            broadcast(gson.toJson(new HashMap<>() {
                {
                    put("player", player.getName());
                    put("action", Action.CHECK);
                }
            }));
        } else {
            player.sendMessage(Action.ERR);
        }
    }

    public void allIn(Player player) {
        int remainingCapital = player.getCapital();
        if (player.decreaseCapital(remainingCapital) <= 0)
            return; // фолд чела

        bets.put(player, bets.getOrDefault(player, 0) + remainingCapital);

        maxbet = Math.max(maxbet, bets.get(player));
        broadcast(gson.toJson(new HashMap<>() {
            {
                put("player", player.getName());
                put("action", Action.ALL_IN);
                put("amount", remainingCapital);
            }
        }));
    }

}