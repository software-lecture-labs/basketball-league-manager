-- V4: Add score columns to games and create game_quarter_scores and player_game_stats tables (Slice 3.1, ADR-0004).
-- Standings table is intentionally omitted; standings are computed from confirmed games (ADR-0001).
-- games.home_score and games.away_score stay NULL while SCHEDULED or LIVE (and when reopened to LIVE),
-- and store the sum of quarter points once finish() completes (ADR-0004).
-- player_game_stats.team_id records the player's team on game day so later transfers do not alter past game stats.
-- ON DELETE CASCADE: deleting a game removes its quarter scores and player stats.
-- ON DELETE RESTRICT: players and teams referenced by recorded game stats cannot be deleted by mistake.

SET lock_timeout = '15s';

ALTER TABLE games
    ADD COLUMN home_score integer,
    ADD COLUMN away_score integer,
    ADD CONSTRAINT ck_games_home_score CHECK (home_score >= 0),
    ADD CONSTRAINT ck_games_away_score CHECK (away_score >= 0),
    ADD CONSTRAINT ck_games_scores_paired CHECK ((home_score IS NULL) = (away_score IS NULL));

CREATE TABLE game_quarter_scores (
    game_id      bigint   NOT NULL,
    quarter_no   integer  NOT NULL,
    home_points  integer  NOT NULL,
    away_points  integer  NOT NULL,

    -- The primary key starts with game_id, so it also serves game_id lookups; no separate index needed.
    CONSTRAINT pk_game_quarter_scores PRIMARY KEY (game_id, quarter_no),
    CONSTRAINT fk_game_quarter_scores_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE,
    CONSTRAINT ck_game_quarter_scores_quarter_no CHECK (quarter_no >= 1),
    CONSTRAINT ck_game_quarter_scores_home_points CHECK (home_points >= 0),
    CONSTRAINT ck_game_quarter_scores_away_points CHECK (away_points >= 0)
);

CREATE TABLE player_game_stats (
    game_id    bigint   NOT NULL,
    player_id  bigint   NOT NULL,
    team_id    bigint   NOT NULL,
    points     integer  NOT NULL,

    CONSTRAINT pk_player_game_stats PRIMARY KEY (game_id, player_id),
    CONSTRAINT fk_player_game_stats_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_game_stats_player FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE RESTRICT,
    CONSTRAINT fk_player_game_stats_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
    CONSTRAINT ck_player_game_stats_points CHECK (points >= 0)
);

CREATE INDEX idx_player_game_stats_player_id ON player_game_stats (player_id);
CREATE INDEX idx_player_game_stats_team_id ON player_game_stats (team_id);
