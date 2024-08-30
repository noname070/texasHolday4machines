package ru.noname070.pockerroom.server.messages;

import java.util.HashMap;

import ru.noname070.pockerroom.game.commons.Action;

public class InfoMessage implements IMessage {
    private final Action action;

    public InfoMessage(Action action) {
        this.action = action;
    }

    @Override
    public String build() {
        return gson.toJson(new HashMap<>() {
            {
                put("type", "info");
                put("action", action.name());
            }
        });
    }

}
