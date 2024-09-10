package ru.noname070.pockerroom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.glassfish.tyrus.server.Server;

import lombok.extern.java.Log;
import ru.noname070.pockerroom.db.DBHandler;
import ru.noname070.pockerroom.server.PockerServer;

@Log
public class Main {
    public static void main(String[] args) {
        DBHandler.getInstance(); // чекаем коннект к бд

        Server server = new Server("localhost", Config.SERVER_PORT, "/", null, PockerServer.class);
        // уебище не implements java.lang.AutoCloseable
        try {
            server.start();
            System.out.println("WebSocket server started at ws://localhost:8080/game");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while (true) {
                    if ((line = reader.readLine()) != null && line.equals("q")) break; 
                }
            }
        } catch (Exception e) {
            log.log(Level.OFF, "exception with server", e);
        } finally {
            try {
                server.stop();
            } catch (Exception e) {
                log.log(Level.OFF, "exception when stopping the server, bb+250", e);
                System.exit(1);
            }
        }
    }
}