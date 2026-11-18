-- V1: Create teams and players tables.
-- Standing table is intentionally omitted: standings are computed from confirmed matches (ADR-0001).
-- Players hold only name, jersey number and position; no further personal data is stored (ADR-0001).
-- ON DELETE RESTRICT: a team that still has players cannot be deleted by mistake.

SET lock_timeout = '15s';

CREATE TABLE teams (
    id    bigint GENERATED ALWAYS AS IDENTITY,
    name  varchar(100) NOT NULL,
    city  varchar(100) NOT NULL,

    CONSTRAINT pk_teams PRIMARY KEY (id),
    CONSTRAINT uq_teams_name UNIQUE (name)
);

CREATE TABLE players (
    id             bigint GENERATED ALWAYS AS IDENTITY,
    team_id        bigint       NOT NULL,
    first_name     varchar(100) NOT NULL,
    last_name      varchar(100) NOT NULL,
    jersey_number  smallint     NOT NULL,
    position       varchar(2)   NOT NULL,

    CONSTRAINT pk_players PRIMARY KEY (id),
    CONSTRAINT fk_players_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
    CONSTRAINT ck_players_jersey_number CHECK (jersey_number BETWEEN 0 AND 99),
    CONSTRAINT ck_players_position CHECK (position IN ('PG', 'SG', 'SF', 'PF', 'C')),
    -- The unique index starts with team_id, so it also serves team_id lookups; no separate index needed.
    CONSTRAINT uq_players_team_jersey UNIQUE (team_id, jersey_number)
);
