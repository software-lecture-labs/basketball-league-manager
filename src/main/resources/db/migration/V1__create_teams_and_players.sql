-- V1: Create teams and players tables.
-- Standing table is intentionally omitted: standings are computed from confirmed matches (ADR-0001).

CREATE TABLE teams (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        varchar(100) NOT NULL UNIQUE,
    city        varchar(100) NOT NULL
);

CREATE TABLE players (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    team_id        bigint REFERENCES teams (id),
    first_name     varchar(100) NOT NULL,
    last_name      varchar(100) NOT NULL,
    jersey_number  smallint CHECK (jersey_number BETWEEN 0 AND 99),
    position       varchar(2) CHECK (position IN ('PG', 'SG', 'SF', 'PF', 'C')),
    birth_date     date,

    UNIQUE (team_id, jersey_number)
);

CREATE INDEX idx_players_team_id ON players (team_id);
