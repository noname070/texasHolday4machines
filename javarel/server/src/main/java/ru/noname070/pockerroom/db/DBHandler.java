package ru.noname070.pockerroom.db;

import java.sql.*;
import java.util.List;

import ru.noname070.pockerroom.Config;

public class DBHandler {
    private static DBHandler instance;
    private Connection connection;

    private DBHandler() {
        try {
            DriverManager.getConnection(
                    String.format("jdbc:postgresql://%s:%s/%s", 
                            Config.DB_HOST, 
                            Config.DB_PORT,
                            Config.DB_TABLE),
                    Config.DB_USER, Config.DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static DBHandler getInstance() {
        return instance == null ? instance = new DBHandler() : instance;
    }

    public boolean isUserTokenExists(String token) {
        String query = "SELECT COUNT(*) FROM users WHERE token = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // TODO
    public void updateAfterGame(int gameId, int winnerId, int potSize, List<UserAction> actions) {
        try {
            connection.setAutoCommit(false);

            String insertGameQuery = "INSERT INTO games (id, pot_size, winner_id) VALUES (?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertGameQuery)) {
                ps.setInt(1, gameId);
                ps.setInt(2, potSize);
                ps.setInt(3, winnerId);
                ps.executeUpdate();
            }

            String insertActionQuery = "INSERT INTO user_actions (game_id, user_id, bet_amount) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertActionQuery)) {
                for (UserAction action : actions) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, action.getUserId());
                    ps.setInt(4, action.getBetAmount());
                    ps.addBatch(); 
                }
                ps.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}