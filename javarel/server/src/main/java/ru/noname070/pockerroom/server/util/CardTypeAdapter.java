package ru.noname070.pockerroom.server.util;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import ru.noname070.pockerroom.game.commons.Card;
import ru.noname070.pockerroom.game.commons.Rank;
import ru.noname070.pockerroom.game.commons.Suit;

public class CardTypeAdapter extends TypeAdapter<Card> {

    @Override
    public void write(JsonWriter out, Card card) throws IOException {
        out.value(card.toString());
    }

    @Override
    public Card read(JsonReader in) throws IOException {
        String[] token = in.nextName().split(" ");
        return new Card(Rank.valueOf(token[0]), Suit.valueOf(token[1]));
    }

}
