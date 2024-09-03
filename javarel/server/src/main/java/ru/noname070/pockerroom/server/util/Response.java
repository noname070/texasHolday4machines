package ru.noname070.pockerroom.server.util;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;
import ru.noname070.pockerroom.game.commons.Card;

public class Response {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Card.class, new CardTypeAdapter())
            .disableHtmlEscaping()
            .create();

    @Getter private final String type;
    @Getter private final Map<String, Object> data;

    public Response(String raw) {
        Map<String, Object> r = get(raw);
        this.type = (String) r.getOrDefault("type", "err");
        this.data = (Map<String, Object>) r.getOrDefault("data", null);
    }

    public static Map<String, Object> get(String raw) {
        return gson.fromJson(raw, HashMap.class);
    }

}
