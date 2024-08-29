DROP TABLE IF EXISTS users              CASCADE;
DROP TABLE IF EXISTS user_metrics       CASCADE;
DROP TABLE IF EXISTS games              CASCADE;
DROP TABLE IF EXISTS user_actions       CASCADE;

CREATE TABLE users
(
    id                  SERIAL          PRIMARY KEY,
    username            VARCHAR(255)    NOT NULL UNIQUE,
    token               VARCHAR(255)    NOT NULL UNIQUE,
    score               REAL            NOT NULL DEFAULT 0.0
);

-- обновлять после каждой игры
CREATE TABLE games
(
    id                  SERIAL          PRIMARY KEY,
    pot_size            INT             NOT NULL,
    winner_id           INT             NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (winner_id) REFERENCES users(id)
);

-- обновлять после каждой игры
CREATE TABLE user_actions
(
    id                  SERIAL          PRIMARY KEY,
    game_id             INT             NOT NULL,
    user_id             INT             NOT NULL,
    bet_amount          INT             NOT NULL DEFAULT 0,
    action_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP, 
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_metrics 
(
    id                  SERIAL          PRIMARY KEY,
    game_id             INT             NOT NULL,
    user_id             INT             NOT NULL,
    win_rate            REAL            NOT NULL,
    average_profit      REAL            NOT NULL,
    win_to_bet_ratio    REAL            NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- трига что бы считать скор пользователей прям в бд
CREATE OR REPLACE FUNCTION update_user_score() RETURNS TRIGGER AS $$
DECLARE
    total_games INT;
    total_wins INT;
    total_profit REAL;
    total_bets REAL;
    win_rate REAL;
    avg_profit_per_game REAL;
    win_to_bet_ratio REAL;
    adaptability_score REAL;
BEGIN
    SELECT COUNT(DISTINCT game_id) INTO total_games FROM user_actions WHERE user_id = NEW.user_id;
    SELECT COUNT(*) INTO total_wins FROM games WHERE winner_id = NEW.user_id;
    SELECT COALESCE(SUM(bet_amount), 0) INTO total_profit FROM user_actions WHERE user_id = NEW.user_id;
    SELECT COALESCE(SUM(bet_amount), 0) INTO total_bets FROM user_actions WHERE user_id = NEW.user_id;

    win_rate := (total_wins::REAL / total_games);
    -- TODO надо учитывать только игры где не проиграл
    avg_profit_per_game := (total_profit / total_games);
    -- TODO подумать.........
    win_to_bet_ratio := (total_wins::REAL / total_bets);

    SELECT COALESCE(AVG(win_ratio), 0) INTO adaptability_score
    FROM (
        SELECT opponent_id, 
               COUNT(*) AS games_against_opponent,
               SUM(CASE WHEN winner_id = NEW.user_id THEN 1 ELSE 0 END)::REAL / COUNT(*) AS win_ratio
        FROM games g
        JOIN user_actions ua ON g.id = ua.game_id
        WHERE ua.user_id = NEW.user_id
        GROUP BY opponent_id
    ) AS opponent_stats;

    UPDATE users
    SET score = 
     (win_rate * 0.5) +
     (avg_profit_per_game * 0.5) + 
     -- (win_to_bet_ratio * 0.5) + 
     (adaptability_score * 0.5) -- ВОТ ТУТ НАДО ПОДУМАТЬ КАК ИХ ВСЕ СЛОЖИТЬ
    WHERE id = NEW.user_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_user_actions_after_insert
AFTER INSERT ON user_actions
FOR EACH ROW
EXECUTE FUNCTION update_user_score();

CREATE TRIGGER tr_games_after_insert
AFTER INSERT ON games
FOR EACH ROW
EXECUTE FUNCTION update_user_score();