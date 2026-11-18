-- =============================================================================
-- V2: Seed teams from the existing EuroLeague database.
--
-- Data source  : Local PostgreSQL "EuroLeague" database (manually entered data)
-- Reference date (roster snapshot): TODO: Kadroların temsil ettiği tarih
-- Data exported on                : TODO: Verinin EuroLeague DB'den alındığı tarih
--
-- Only name and city are imported. Standing columns (mac_sayisi, galibiyet,
-- maglubiyet, etc.) are intentionally excluded: standings are computed from
-- confirmed matches (ADR-0001).
--
-- No player data exists in the source database; players will be seeded in a
-- future migration when roster data is available.
-- =============================================================================

-- ── Teams (20 EuroLeague 2024-25 season teams) ──────────────────────────────

INSERT INTO teams (name, city) VALUES
    ('Anadolu Efes',              'İstanbul'),
    ('ASVEL',                     'Villeurbanne'),
    ('Baskonia',                  'Vitoria-Gasteiz'),
    ('Beşiktaş',                  'İstanbul'),
    ('Crvena Zvezda',             'Beograd'),
    ('Dubai Basketball',          'Dubai'),
    ('EA7 Emporio Armani Milano', 'Milano'),
    ('FC Barcelona',              'Barcelona'),
    ('FC Bayern Munich',          'München'),
    ('Fenerbahçe Beko',           'İstanbul'),
    ('Hapoel Tel Aviv',           'Tel Aviv'),
    ('Maccabi Tel Aviv',          'Tel Aviv'),
    ('Olympiacos',                'Pireas'),
    ('Panathinaikos',             'Atina'),
    ('Paris Basketball',          'Paris'),
    ('Partizan',                  'Beograd'),
    ('Real Madrid',               'Madrid'),
    ('Valencia Basket',           'Valencia'),
    ('Virtus Bologna',            'Bologna'),
    ('Zalgiris Kaunas',           'Kaunas');
