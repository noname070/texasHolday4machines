package ru.noname070.pockerroom.server.messages;

import java.util.HashMap;

import lombok.Getter;
import ru.noname070.pockerroom.game.Player;
import ru.noname070.pockerroom.game.commons.Action;

public class BetMessage extends ActionMessage {
    @Getter private final int ammount; 

    public BetMessage(Player player, int ammount) {
        super(player, Action.BET);
        this.ammount = 0;
    }

    public BetMessage(Player player, Action action, int ammount) { // for call/raise/all_in
        super(player, action);
        this.ammount = 0;
    }

    @Override
    public String build() {
        return gson.toJson(new HashMap<>(){{
            put("player", player.getName());
            put("action", action.name());
            put("ammount", ammount);
        }});
    }
    
}
