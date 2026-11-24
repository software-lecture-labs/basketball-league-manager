-- V3: Create seasons and games tables.
-- Standing table is intentionally omitted: standings are computed from confirmed matches (ADR-0001).

SET lock_timeout = '15s';

CREATE TABLE seasons (
    id   bigint GENERATED ALWAYS AS IDENTITY,
    name varchar(100) NOT NULL,

    CONSTRAINT pk_seasons PRIMARY KEY (id),
    CONSTRAINT uq_seasons_name UNIQUE (name)
);

CREATE TABLE games (
    id           bigint GENERATED ALWAYS AS IDENTITY,
    season_id    bigint      NOT NULL,
    home_team_id bigint      NOT NULL,
    away_team_id bigint      NOT NULL,
    round_number integer     NOT NULL,
    home_score   integer,
    away_score   integer,
    status       varchar(20) NOT NULL DEFAULT 'SCHEDULED',
    version      bigint      NOT NULL DEFAULT 0,

    CONSTRAINT pk_games PRIMARY KEY (id),
    CONSTRAINT fk_games_season FOREIGN KEY (season_id) REFERENCES seasons (id) ON DELETE CASCADE,
    CONSTRAINT fk_games_home_team FOREIGN KEY (home_team_id) REFERENCES teams (id) ON DELETE RESTRICT,
    CONSTRAINT fk_games_away_team FOREIGN KEY (away_team_id) REFERENCES teams (id) ON DELETE RESTRICT,
    CONSTRAINT ck_games_different_teams CHECK (home_team_id <> away_team_id),
    CONSTRAINT ck_games_status CHECK (status IN ('SCHEDULED', 'LIVE', 'FINISHED', 'CONFIRMED'))
);

CREATE INDEX idx_games_season_status ON games (season_id, status);
