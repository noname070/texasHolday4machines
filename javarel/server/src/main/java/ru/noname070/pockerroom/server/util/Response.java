package ru.noname070.pockerroom.server.util;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ru.noname070.pockerroom.game.commons.Card;

public class Response {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Card.class, new CardTypeAdapter())
            .disableHtmlEscaping()
            .create();

    // TODO какую нибудь штуку красивую что бы распаковывала все ключи
    // пупупупупупупупуу
    @SuppressWarnings("unchecked")
    public static Map<String, Object> get(String raw) {
        return gson.fromJson(raw, HashMap.class);
    }

}
