package ru.noname070.pockerroom.server.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Builder;
import ru.noname070.pockerroom.game.Player;
import ru.noname070.pockerroom.game.commons.Card;

@Builder
public class Request {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Card.class, new CardTypeAdapter())
            .disableHtmlEscaping()
            .create();

    private String type;
    private String name;
    private List<Card> boardCards;
    private List<Card> cards;
    private Map<String, Object> playerStates;
    private String error;
    private Integer amount;
    private String winner;
    private Integer bank;
    private String action;
    private Integer bankAmount;
    private Map<Player, Integer> sidePods;

    public void send(Session session) {
        session.getAsyncRemote().sendText(toJson());
    }

    public String toJson() {
        Map<String, Object> data = new HashMap<>();

        if (this.type != null) {
            data.put("type", type);
        }

        if (this.name != null) {
            data.put("name", name);
        }

        if (this.playerStates != null) {
            data.put("playerStates", playerStates);
        }

        if (this.boardCards != null) {
            data.put("boardCards", boardCards);
        }

        if (this.cards != null) {
            data.put("cards", cards);
        }

        if (this.error != null) {
            data.put("error", error);
        }

        if (this.amount != null) {
            data.put("amount", amount);
        }

        if (this.winner != null) {
            data.put("winner", winner);
        }

        if (this.bank != null) {
            data.put("bank", bank);
        }

        if (this.cards != null) {
            data.put("cards", cards);
        }

        if (this.action != null) {
            data.put("action", action);
        }

        if (this.bankAmount != null) {
            data.put("bankAmount", bankAmount);
        }

        if (this.sidePods != null) {
            data.put("sidePods", sidePods);
        }

        return gson.toJson(new HashMap<>() {
            {
                put("type", type);
                put("data", data);
            }
        });
    }
}
