package ru.noname070.pockerroom;

import java.util.Scanner;

import org.glassfish.tyrus.server.Server;

import ru.noname070.pockerroom.db.DBHandler;
import ru.noname070.pockerroom.server.PockerServer;

public class Main {
    public static void main(String[] args) {
        DBHandler.getInstance(); // чекаем коннект к бд

        Server server = new Server("localhost", Config.SERVER_PORT, "/", null, PockerServer.class);
        // уебище не implements java.lang.AutoCloseable
        try {
            server.start();
            System.out.println("WebSocket server started at ws://localhost:8080/game");

            try (Scanner scanner = new Scanner(System.in);) {
                while (true) {
                    if (scanner.hasNextLine())
                        if (scanner.nextLine().equals("q")) break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}