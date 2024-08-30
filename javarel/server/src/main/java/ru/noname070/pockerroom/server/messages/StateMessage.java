package ru.noname070.pockerroom.server.messages;

import java.util.HashMap;
import java.util.Map;

import ru.noname070.pockerroom.game.commons.Action;

public class StateMessage implements IMessage {

    private final Map<String, Object> publicGameState;

    public StateMessage(Map<String, Object> publicGameState) {
        this.publicGameState = publicGameState;
    }

    @Override
    public String build() {
        return gson.toJson(new HashMap<>(){{
            put("type", Action.INFO);
            put("data", publicGameState);
        }});
    }

}
