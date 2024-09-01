package ru.noname070.pockerroom.game;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;

import lombok.Getter;
import lombok.extern.java.Log;
import ru.noname070.pockerroom.Config;
import ru.noname070.pockerroom.game.commons.*;
import ru.noname070.pockerroom.server.util.Request;
import ru.noname070.pockerroom.util.CycleIterator;
import ru.noname070.pockerroom.util.Pair;

@Log
public class TexasHolday {

    private final List<Player> players;
    private final Iterator<Player> dealers;
    @Getter
    private final List<Card> board;
    private Map<Player, Integer> bets;
    private Iterator<Card> deck;
    private int maxbet = 0;
    private int bank = 0;

    // private final Gson gson = new GsonBuilder().create();

    public TexasHolday(Player... players) {
        this.players = Arrays.asList(players);
        this.dealers = new CycleIterator<>(players);
        this.board = new ArrayList<>();
    }

    public Iterator<Player> getPlayerIterator() {
        return new CycleIterator<>(players);
    }

    public Map<String, Object> getPublicState() {

        Map<String, Object> playerStates = new HashMap<>();
        for (Player player : players) {
            Map<String, Object> playerState = new HashMap<>();
            playerState.put("currentBet", bets.getOrDefault(player, 0));
            playerState.put("remainingCapital", player.getCapital());
            playerState.put("isFolded", player.isFolded());

            playerStates.put(player.getName(), playerState);
        }

        playerStates.put("maxBet", maxbet);

        return playerStates;
    }

    public void newGame() {
        this.deck = Card.newDeck().iterator();

        for (Player p : this.players) {
            p.reciveCards(deck.next(), deck.next());
            p.sendMessage(Request.builder()
                    .type("INFO")
                    .cards(p.getCards())
                    .build());
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

        broadcast(Request.builder()
                .type("INFO")
                .winner(winners.get(0).getName())
                .boardCards(board)
                .playerStates(getPublicState())
                .bank(maxbet)
                // блять а как с сайд потом ебаться пупуп
                .build());
    }

    /**
     * положить на стол N карт.
     */
    private void revealCards(int n) {
        for (int i = 0; i < n; i++) {
            this.board.add(this.deck.next());
        }
    }

    private Pair<Action, Integer> requestAction(Player p, Request r) {
        Action act = Action.CHECK;
        int bet = 0;
        p.getSession().getAsyncRemote().sendText(
                r.toJson(),
                new SendHandler() {

                    @Override
                    public void onResult(SendResult result) {
                        // ret = Action.valueOf(result);
                        /**
                         * TODO кароче не ебу
                         * сюда надо воткнуть хендлер что бы сразу получить ответ
                         * от игрока.
                         * 
                         * как сделать - 0 идей.
                         */
                    }

                });

        return new Pair<>(act, bet);
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
            for (Player p : this.players) {
                if (p.isFolded())
                    continue;
                if (bets.get(p) != maxbet) {
                    int amountToCall = maxbet - bets.get(p);
                    Pair<Action, Integer> actionNdbet = requestAction(p,
                            Request.builder()
                                    .type("request")
                                    .boardCards(board)
                                    .cards(p.getCards())
                                    .bank(this.bank)
                                    .playerStates(getPublicState())
                                    // .sidePod(0)
                                    .amount(amountToCall)
                                    .build());

                    switch (actionNdbet.getFirst()) {
                        case CALL:
                            if (p.getCapital() >= amountToCall) {
                                bets.put(p, maxbet);
                                p.decreaseCapital(amountToCall);
                            } else {
                                p.sendMessage(Request.builder()
                                        .error("oops no money  (( ( (( u got  folded ")
                                        .build());
                                p.fold();
                            }
                            break;
                        case RAISE:
                            int raiseAmount = actionNdbet.getSecond();
                            if (p.getCapital() >= raiseAmount + amountToCall) {
                                maxbet += raiseAmount;
                                bets.put(p, maxbet);
                                p.decreaseCapital(amountToCall + raiseAmount);
                            } else {
                                p.sendMessage(Request.builder()
                                        .error("oops no money  (( ( (( u got  folded ")
                                        .build());
                                p.fold();
                            }
                            break;
                        case FOLD:
                            p.fold();
                            break;
                        default:
                            p.sendMessage(Request.builder()
                                    .error("idk what r u means")
                                    .build());
                            p.fold();
                    }

                    allBetsMatched = bets.keySet().stream().allMatch(new Predicate<Player>() {
                        @Override
                        public boolean test(Player t) {
                            return p.isFolded() && (bets.get(p) == maxbet);
                        }
                    });
                }

                p.sendMessage(Request.builder()
                        .boardCards(board)
                        .bank(bank)
                        .cards(p.getCards())
                        .playerStates(getPublicState())
                        .build()

                );
                // TODO
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
    public void broadcast(Request r) {
        this.players.forEach(p -> r.send(p.getSession()));
    }

    public void placeBet(Player player, int betAmount) {
        if (player.getCapital() >= betAmount) {
            player.decreaseCapital(betAmount);
            bets.put(player, betAmount);
            maxbet = Math.max(maxbet, betAmount);

            broadcast(Request.builder()
                    .type("info")
                    .name(player.getName())
                    .action(Action.BET.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());
        } else {
            player.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant bet pypyp") // TODO
                    .build());
        }
    }

    public void call(Player player) {
        int amountToCall = maxbet - bets.getOrDefault(player, 0);
        if (player.getCapital() >= amountToCall) {
            player.decreaseCapital(amountToCall);
            bets.put(player, maxbet);

            broadcast(Request.builder()
                    .type("info")
                    .name(player.getName())
                    .action(Action.CALL.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());
        } else {
            player.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant bet pypyp") // TODO
                    .build());
            player.fold();
        }
    }

    public void raise(Player player, int raiseAmount) {
        int amountToCall = maxbet - bets.getOrDefault(player, 0);
        if (player.getCapital() >= (amountToCall + raiseAmount)) {
            player.decreaseCapital(amountToCall + raiseAmount);
            maxbet += raiseAmount;
            bets.put(player, maxbet);

            broadcast(Request.builder()
                    .type("info")
                    .name(player.getName())
                    .action(Action.RAISE.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());

        } else {
            player.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant raise pypyp") // TODO
                    .build());
            player.fold();
        }
    }

    public void fold(Player player) {
        broadcast(Request.builder()
                .type("info")
                .name(player.getName())
                .action(Action.FOLD.name())
                .boardCards(board)
                .bank(this.bank)
                .playerStates(getPublicState())
                .build());
        player.fold();
    }

    public void check(Player player) {
        if (bets.getOrDefault(player, 0) == maxbet) {
            broadcast(Request.builder()
                    .type("info")
                    .name(player.getName())
                    .action(Action.CHECK.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());
        } else {
            player.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant check pypyp") // TODO
                    .build());
        }
    }

    public void allIn(Player player) {
        int remainingCapital = player.getCapital();
        if (player.decreaseCapital(remainingCapital) <= 0)
            return; // фолд чела

        bets.put(player, bets.getOrDefault(player, 0) + remainingCapital);

        maxbet = Math.max(maxbet, bets.get(player));
        broadcast(Request.builder()
                .type("info")
                .name(player.getName())
                .action(Action.ALL_IN.name())
                .boardCards(board)
                .bank(this.bank)
                // .sidePod() // TODO блять надо реально че то с ним придумать
                .playerStates(getPublicState())
                .build());
    }

}