package ru.noname070.pockerroom.server.messages;

import java.util.HashMap;

import lombok.Getter;
import ru.noname070.pockerroom.game.Player;
import ru.noname070.pockerroom.game.commons.Action;

public class ActionMessage implements IMessage {
    @Getter protected final Action action;
    @Getter protected final Player player;

    public ActionMessage(Player player, Action action) {
        this.player = player;
        this.action = action;
    }

    @Override
    public String build() {
        return gson.toJson(new HashMap<>() {{
            put("type", "action");
            put("player", player.getName());
            put("action", action.name());
        }});
    }
    
}
