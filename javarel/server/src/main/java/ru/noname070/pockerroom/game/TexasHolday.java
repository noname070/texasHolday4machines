package ru.noname070.pockerroom.game;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.java.Log;
import ru.noname070.pockerroom.Config;
import ru.noname070.pockerroom.game.commons.*;
import ru.noname070.pockerroom.server.util.Request;
import ru.noname070.pockerroom.server.util.Response;
import ru.noname070.pockerroom.util.CycleIterator;

@Log
public class TexasHolday {

    private final List<Player> players;
    private final Iterator<Player> dealers;
    @Getter private final List<Card> board;
    private Map<Player, Integer> bets;
    private Map<Player, Integer> sidePods = new HashMap<>();
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
        for (Player p : players) {
            Map<String, Object> pState = new HashMap<>();
            pState.put("currentBet", bets.getOrDefault(p, 0));
            pState.put("remainingCapital", p.getCapital());
            pState.put("isFolded", p.isFolded());

            playerStates.put(p.getName(), pState);
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

        createSidePots();

        List<Player> winners = sortByComboPower();

        System.out.printf("Board : %s\n",
                this.board.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(" ")));

        winners.forEach((Player p) -> {
            // System.out.printf("%s : %s\n", p.getName(), p.getHand().asList());
            log.info("%s : %s\n".formatted(p.getName(), p.getHand().asList()));
        });

        try {
            distributeWinnings(winners);
        } catch (Exception e) {
            log.log(Level.WARNING, "distributing winnings err", e);
        }

        broadcast(Request.builder()
                .type("gameInfo")
                .winner(winners.get(0).getName())
                .boardCards(board)
                .playerStates(getPublicState())
                .bank(maxbet)
                .sidePods(sidePods)
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

    /**
     * момент уравнивания ставок
     * 
     * ROƒL если игрок некорректно сделает свои действия - он фолднится xd
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

                if (bets.get(p).equals(maxbet))
                    continue;

                int amountToCall = maxbet - bets.get(p);

                p.setWaitingForAction(true);
                p.sendMessage(Request.builder()
                        .type("playerAction")
                        .action("") // TODO значит что идет запрос действия игрока. к о с т ы л ь
                        .amount(amountToCall)
                        .bank(bank)
                        .boardCards(board)
                        .cards(p.getCards())
                        .sidePods(sidePods)
                        .playerStates(getPublicState())
                        .build());

                while (p.isWaitingForAction()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                allBetsMatched = bets
                        .keySet()
                        .stream()
                        .allMatch(
                                new Predicate<Player>() {
                                    @Override
                                    public boolean test(Player t) {
                                        return p.isFolded() && (bets.get(p) == maxbet);
                                    }
                                });
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
                        log.log(Level.WARNING, "compare combo power", e);

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

    private void createSidePots() {
        List<Map.Entry<Player, Integer>> sortedBets = bets.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());

        int accumulatedPot = 0;
        int previousBet = 0;

        for (int i = 0; i < sortedBets.size(); i++) {
            Player currentPlayer = sortedBets.get(i).getKey();
            int currentBet = sortedBets.get(i).getValue();

            if (currentBet > previousBet) {
                int sidePot = (currentBet - previousBet) * (sortedBets.size() - i);
                sidePods.put(currentPlayer, sidePods.getOrDefault(currentPlayer, 0) + sidePot);
                accumulatedPot += sidePot;
                previousBet = currentBet;
            }
        }

        this.bank = accumulatedPot;
    }

    private void distributeWinnings(List<Player> winners) {
        Map<Player, Integer> winnings = winners.stream()
                .collect(Collectors.toMap(player -> player, player -> 0));

        int totalPot = bets.entrySet().stream()
                .filter(entry -> winners.contains(entry.getKey()))
                .mapToInt(entry -> {
                    Player player = entry.getKey();
                    int playerWinnings = Math.min(entry.getValue(), sidePods.getOrDefault(player, 0));
                    winnings.merge(player, playerWinnings, Integer::sum);
                    return playerWinnings;
                })
                .sum();

        winnings.forEach((player, amount) -> player.setCapital(player.getCapital() + amount));
        sidePods.replaceAll((player, value) -> 0);
        this.bank = totalPot;
    }

    /**
     * bet handler
     */
    public void betHandler(Player p, int betAmount) {
        if (p.getCapital() >= betAmount) {
            p.decreaseCapital(betAmount);
            bets.put(p, betAmount);
            maxbet = Math.max(maxbet, betAmount);

            broadcast(Request.builder()
                    .type("playerAction")
                    .name(p.getName())
                    .action(Action.BET.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());
        } else {
            p.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant bet pypyp") // TODO
                    .build());
        }
    }

    /**
     * call handler
     */
    public void callHandler(Player p) {
        int amountToCall = maxbet - bets.getOrDefault(p, 0);
        if (p.getCapital() >= amountToCall) {
            p.decreaseCapital(amountToCall);
            bets.put(p, maxbet);

            broadcast(Request.builder()
                    .type("playerAction")
                    .name(p.getName())
                    .action(Action.CALL.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());
        } else {
            p.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant bet pypyp") // TODO
                    .build());
            p.fold();
        }
    }

    /**
     * raise handler
     */
    public void raiseHandler(Player p, int raiseAmount) {
        int amountToCall = maxbet - bets.getOrDefault(p, 0);
        if (p.getCapital() >= (amountToCall + raiseAmount)) {
            p.decreaseCapital(amountToCall + raiseAmount);
            maxbet += raiseAmount;
            bets.put(p, maxbet);

            broadcast(Request.builder()
                    .type("playerAction")
                    .name(p.getName())
                    .action(Action.RAISE.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());

        } else {
            p.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant raise pypyp") // TODO
                    .build());
            p.fold();
        }
    }

    /**
     * fold handler
     */
    public void foldHandler(Player p) {
        broadcast(Request.builder()
                .type("playerAction")
                .name(p.getName())
                .action(Action.FOLD.name())
                .boardCards(board)
                .bank(this.bank)
                .playerStates(getPublicState())
                .build());
        p.fold();
    }

    /**
     * check handler
     */
    public void checkHandler(Player p) {
        if (bets.getOrDefault(p, 0) == maxbet) {
            broadcast(Request.builder()
                    .type("playerAction")
                    .name(p.getName())
                    .action(Action.CHECK.name())
                    .boardCards(board)
                    .bank(this.bank)
                    .playerStates(getPublicState())
                    .build());
        } else {
            p.sendMessage(Request.builder()
                    .type("err")
                    .error("u cant check pykpykp") // TODO
                    .build());
        }
    }

    /**
     * all in handler
     */
    public void allInHandler(Player p) {
        int remainingCapital = p.getCapital();
        if (p.decreaseCapital(remainingCapital) <= 0)
            return; // фолд чела

        bets.put(p, bets.getOrDefault(p, 0) + remainingCapital);

        maxbet = Math.max(maxbet, bets.get(p));
        broadcast(Request.builder()
                .type("playerAction")
                .name(p.getName())
                .action(Action.ALL_IN.name())
                .boardCards(board)
                .bank(this.bank)
                .sidePods(sidePods)
                .playerStates(getPublicState())
                .build());
    }

    /**
     * handle player action
     */
    public void handleGameAction(Player p, String message) {
        Response r = new Response(message);
        if (!r.getType().equals("playerAction"))
            return;

        try {
            Map<String, Object> data = r.getData();
            switch (Action.valueOf((String) data.get("action"))) {
                case BET:
                    betHandler(p, (Integer) data.get("amount"));
                    break;
                case RAISE:
                    raiseHandler(p, (Integer) data.get("amount"));
                    break;
                case CALL:
                    callHandler(p);
                    break;
                case FOLD:
                    foldHandler(p);
                    break;
                case CHECK:
                    checkHandler(p);
                    break;
                case ALL_IN:
                    allInHandler(p);
                    break;
                default:
                    p.sendMessage(Request.builder()
                            .type("err")
                            .error("Unknown action")
                            .build());
                    break;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "player action handler err", e);
            p.sendMessage(Request.builder()
                    .type("err")
                    .error(e.getMessage())
                    .build());
        }
    }

}