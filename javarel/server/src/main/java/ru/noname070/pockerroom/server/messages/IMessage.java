package ru.noname070.pockerroom.server.messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public interface IMessage {
    static final Gson gson = new GsonBuilder().create();
    
    String build();

}