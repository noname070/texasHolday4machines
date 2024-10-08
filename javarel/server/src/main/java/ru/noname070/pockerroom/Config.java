package ru.noname070.pockerroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import lombok.extern.java.Log;

@Log
public class Config {
    public final static int SERVER_PORT = 7777;
    public final static int NUM_PLAYERS = 5; // количество игроков за столом
    public final static int SMALL_BLIND = 15; // малый блайнд
    public final static int START_CAPITAL = 1000; // защекойнов на старт
    public final static int PLAYER_TIMEOUT = 30; // 30 сек ожидания от игрока. TODO ТЕСТОВАЯ ТЕМА, ПОТОМ ПОМЕНЯТЬ

    // насрал.
    public static String DB_PORT;
    public static String DB_USER;
    public static String DB_PASSWORD;
    public static String DB_TABLE;
    public static String DB_HOST;

    static {
        try (BufferedReader configFile = new BufferedReader(
                new InputStreamReader(
                        Thread.currentThread()
                                .getContextClassLoader()
                                .getResourceAsStream("application.properties")));) {
            Map<String, String> config = configFile
                    .lines()
                    .map(s -> s.split("=", 2))
                    .collect(
                            Collectors.toMap(
                                    parts -> parts[0],
                                    parts -> parts[1],
                                    (oldValue, newValue) -> newValue,
                                    HashMap::new));

            DB_HOST = config.getOrDefault("db_host", "localhost");
            DB_PORT = config.getOrDefault("db_port", "5432");
            DB_USER = config.getOrDefault("db_user", "root");
            DB_PASSWORD = config.getOrDefault("db_password", "root");
            DB_TABLE = config.getOrDefault("db_table", "");

        } catch (IOException e) {
            log.log(Level.CONFIG, "config setup err", e);
            System.exit(1);
        }
    }

}
