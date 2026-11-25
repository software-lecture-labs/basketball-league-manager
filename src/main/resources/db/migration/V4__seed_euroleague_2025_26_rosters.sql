-- =============================================================================
-- V4: EuroLeague 2025-26 kulüplerinin 1 Ocak 2026 tarihindeki kadrolarını ekle.
--
-- Veri sürümü : euroleague-2025-26-rosters-v1
-- Referans    : 1 Ocak 2026. Her takım için kaynak, bu tarihten önceki son Wikipedia sürümüdür
--               (sabit sürüm bağlantıları takım bloklarının üstünde).
-- Kaynak      : Takım makalelerinin "Current roster" tablosu; Barcelona, Fenerbahçe, Partizan,
--               Real Madrid ve Virtus Bologna için tabloyu taşıyan "2025-26 ... season" makalesi.
-- Alınma      : 25 Kasım 2026
-- Kapsam      : V2'deki 20 kulüp, toplam 332 oyuncu.
--
-- Kurallar (ADR-0001 veri kapsamı):
-- - Yalnızca ad, soyad, forma numarası ve pozisyon. Doğum tarihi, uyruk, boy, kilo eklenmez.
-- - Adlar kaynaktaki yazımıyla ve diakritiklerle saklanır.
-- - Pozisyon: tekil etiketler aynen alınır (PG, SG, SF, PF, C). "X/Y" biçiminde ilk rol alınır
--   (PG/SG -> PG, SG/SF -> SG). Geniş etiketler orta pozisyona eşlenir:
--   G -> SG, F -> SF, GF -> SF, FC -> PF, CG -> SG.
-- - Forma numarası: Armando Bacot (Fenerbahçe) kaynakta "00". jersey_number sayı olduğu için "00"
--   ile "0" ayrılamaz; 0 olarak saklanır (takımda 0 numara yok).
-- - Takım kimliği adla bulunur; sonda her kulübün kadrosu olduğu denetlenir.
-- - Bu bir demo sezonu verisidir; transferler uygulamada yapılır, gerçek transferler izlenmez.
-- =============================================================================

-- Anadolu Efes: Anadolu Efes S.K., 31 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330468681
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Shane', 'Larkin', 0, 'SG'),
    ('Rodrigue', 'Beaubois', 1, 'SG'),
    ('Şehmus', 'Hazer', 2, 'SG'),
    ('Jordan', 'Loyd', 3, 'SG'),
    ('Saben', 'Lee', 5, 'SG'),
    ('Nick', 'Weiler-Babb', 8, 'SG'),
    ('Georgios', 'Papagiannis', 9, 'C'),
    ('Isaïa', 'Cordinier', 10, 'SF'),
    ('Rolands', 'Šmits', 11, 'PF'),
    ('Brice', 'Dessert', 13, 'C'),
    ('PJ', 'Dozier', 15, 'SF'),
    ('Vincent', 'Poirier', 17, 'C'),
    ('Burak Can', 'Yıldızlı', 19, 'PF'),
    ('Cole', 'Swider', 21, 'SF'),
    ('Sarper David', 'Mutaf', 23, 'SF'),
    ('Ercan', 'Osmani', 24, 'PF'),
    ('Erkan', 'Yılmaz', 33, 'SF'),
    ('Kai', 'Jones', 88, 'C')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Anadolu Efes';

-- AS Monaco: AS Monaco Basket, 11 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1326875087
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Élie', 'Okobo', 0, 'SG'),
    ('Jaron', 'Blossomgame', 4, 'SF'),
    ('Yoan', 'Makoundou', 5, 'PF'),
    ('David', 'Michineau', 6, 'PG'),
    ('Daniel', 'Theis', 10, 'PF'),
    ('Alpha', 'Diallo', 11, 'SF'),
    ('Kevarrius', 'Hayes', 13, 'C'),
    ('Terry', 'Tarpey', 22, 'SF'),
    ('Juhann', 'Begarin', 23, 'SG'),
    ('Nemanja', 'Nedović', 26, 'SG'),
    ('Matthew', 'Strazel', 32, 'SG'),
    ('Nikola', 'Mirotić', 33, 'PF'),
    ('Cory', 'Joseph', 34, 'PG'),
    ('Mike', 'James', 55, 'SG')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'AS Monaco';

-- ASVEL: ASVEL Basket, 30 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330276334
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Zac', 'Seljaas', 1, 'SF'),
    ('Adam', 'Atamna', 2, 'SF'),
    ('Shaquille', 'Harrison', 3, 'SG'),
    ('Thomas', 'Heurtel', 7, 'PG'),
    ('Melvin', 'Ajinça', 8, 'SF'),
    ('Bodian', 'Massa', 10, 'C'),
    ('Edwin', 'Jackson', 11, 'SG'),
    ('Nando', 'de Colo', 12, 'SG'),
    ('David', 'Lighty', 23, 'SF'),
    ('Mbaye', 'Ndiaye', 24, 'PF'),
    ('Glynn', 'Watson Jr.', 30, 'PG'),
    ('Bastien', 'Vautier', 32, 'C'),
    ('Marvyn', 'Wade', 88, 'SG'),
    ('Armel', 'Traoré', 94, 'SF')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'ASVEL';

-- Barcelona: 2025–26 FC Barcelona Bàsquet season, 31 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330360976
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Kevin', 'Punter', 0, 'SG'),
    ('Juani', 'Marcos', 2, 'PG'),
    ('Myles', 'Cale', 3, 'SF'),
    ('Miles', 'Norris', 5, 'PF'),
    ('Jan', 'Veselý', 6, 'PF'),
    ('Darío', 'Brizuela', 8, 'SG'),
    ('Tomáš', 'Satoranský', 13, 'PG'),
    ('Willy', 'Hernangómez', 14, 'C'),
    ('Juan', 'Núñez', 17, 'PG'),
    ('Youssoupha', 'Fall', 19, 'C'),
    ('Nicolás', 'Laprovíttola', 20, 'SG'),
    ('Will', 'Clyburn', 21, 'SF'),
    ('Tornike', 'Shengelia', 23, 'PF'),
    ('Sayon', 'Keita', 41, 'C'),
    ('Joel', 'Parra', 44, 'SF')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Barcelona';

-- Baskonia: Saski Baskonia, 24 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1329270589
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Markus', 'Howard', 0, 'SG'),
    ('Mamadi', 'Diakite', 1, 'C'),
    ('Kobi', 'Simmons', 2, 'PG'),
    ('Markquis', 'Nowell', 3, 'PG'),
    ('Rafa', 'Villar', 4, 'PG'),
    ('Eugene', 'Omoruyi', 5, 'PF'),
    ('Rodions', 'Kurucs', 7, 'PF'),
    ('Tadas', 'Sedekerskis', 8, 'PF'),
    ('Timothé', 'Luwawu-Cabarrot', 9, 'SF'),
    ('Matteo', 'Spagnolo', 10, 'SG'),
    ('Trent', 'Forrest', 11, 'PG'),
    ('Gytis', 'Radzevičius', 17, 'SF'),
    ('Khalifa', 'Diop', 18, 'C'),
    ('Clément', 'Frisch', 25, 'SF'),
    ('Stefan', 'Joksimović', 77, 'SG')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Baskonia';

-- Bayern Munich: FC Bayern Munich (basketball), 30 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330213351
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Oscar', 'da Silva', 1, 'PF'),
    ('Xavier', 'Rathan-Mayes', 4, 'SG'),
    ('Niels', 'Giffey', 5, 'SF'),
    ('Johannes', 'Voigtmann', 7, 'PF'),
    ('Leon', 'Kratzer', 8, 'C'),
    ('Justinian', 'Jessup', 10, 'SG'),
    ('Vladimir', 'Lučić', 11, 'SF'),
    ('Aleksa', 'Radanov', 12, 'SF'),
    ('Andreas', 'Obst', 13, 'SG'),
    ('Stefan', 'Jović', 16, 'PG'),
    ('Elias', 'Harris', 20, 'PF'),
    ('Justus', 'Hollatz', 21, 'PG'),
    ('Isiaha', 'Mike', 22, 'SF'),
    ('Spencer', 'Dinwiddie', 26, 'SG'),
    ('Rokas', 'Jokubaitis', 31, 'PG'),
    ('Wenyen', 'Gabriel', 32, 'C'),
    ('David', 'McCormack', 33, 'C'),
    ('Kamar', 'Baldwin', 44, 'SG')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Bayern Munich';

-- Crvena zvezda: KK Crvena zvezda, 31 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330516126
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Codi', 'Miller-McIntyre', 0, 'PG'),
    ('Uroš', 'Plavšić', 1, 'C'),
    ('Stefan', 'Miljenović', 2, 'SG'),
    ('Isaiah', 'Canaan', 3, 'SG'),
    ('Devonte''', 'Graham', 4, 'SG'),
    ('Aleksej', 'Nedeljković', 5, 'SF'),
    ('Jared', 'Butler', 6, 'PG'),
    ('Dejan', 'Davidovac', 7, 'SF'),
    ('Tyson', 'Carter', 11, 'SG'),
    ('Nikola', 'Kalinić', 12, 'SF'),
    ('Ognjen', 'Dobrić', 13, 'SF'),
    ('Jasiel', 'Rivero', 14, 'C'),
    ('Ebuka', 'Izundu', 15, 'C'),
    ('Lazar', 'Stojković', 17, 'PF'),
    ('Donatas', 'Motiejūnas', 20, 'C'),
    ('Joel', 'Bolomboy', 21, 'C'),
    ('Jordan', 'Nwora', 33, 'SF'),
    ('Semi', 'Ojeleye', 37, 'SF'),
    ('Ognjen', 'Radošić', 44, 'SG'),
    ('Chima', 'Moneke', 95, 'PF'),
    ('Yago', 'dos Santos', 99, 'PG')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Crvena zvezda';

-- Dubai Basketball: Dubai Basketball, 23 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1329097889
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Nate', 'Mason', 1, 'SG'),
    ('Nemanja', 'Dangubić', 2, 'SF'),
    ('Dwayne', 'Bacon', 3, 'SF'),
    ('Aleksa', 'Avramović', 4, 'SG'),
    ('Awudu', 'Abass', 5, 'SF'),
    ('Klemen', 'Prepelič', 7, 'SG'),
    ('Dāvis', 'Bertāns', 8, 'SF'),
    ('Boogie', 'Ellis', 9, 'SG'),
    ('Justin', 'Anderson', 10, 'SF'),
    ('Kosta', 'Kondić', 11, 'SG'),
    ('Džanan', 'Musa', 13, 'SF'),
    ('Mam', 'Jaiteh', 14, 'C'),
    ('Sertaç', 'Şanlı', 15, 'C'),
    ('Mfiondu', 'Kabengele', 17, 'C'),
    ('Taran', 'Armstrong', 22, 'PG'),
    ('McKinley', 'Wright IV', 25, 'PG'),
    ('Filip', 'Petrušev', 30, 'PF'),
    ('Matt', 'Ryan', 32, 'SF'),
    ('Kenan', 'Kamenjaš', 34, 'C')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Dubai Basketball';

-- Fenerbahçe: 2025–26 Fenerbahçe S.K. (basketball) season, 31 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330475500
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Armando', 'Bacot', 0, 'PF'),  -- kaynakta "00"
    ('Metecan', 'Birsen', 1, 'SF'),
    ('Wade', 'Baldwin IV', 2, 'SG'),
    ('Scottie', 'Wilbekin', 3, 'SG'),
    ('Nicolò', 'Melli', 4, 'PF'),
    ('Mert Emre', 'Ekşioğlu', 5, 'SG'),
    ('Talen', 'Horton-Tucker', 8, 'SG'),
    ('Melih', 'Mahmutoğlu', 10, 'SG'),
    ('Brandon', 'Boston Jr.', 11, 'SF'),
    ('Tarik', 'Biberović', 13, 'SF'),
    ('Onuralp', 'Bitim', 17, 'SF'),
    ('Mikael', 'Jantunen', 18, 'PF'),
    ('Devon', 'Hall', 20, 'SG'),
    ('Yiğit Hamza', 'Mestoğlu', 22, 'SF'),
    ('Artūrs', 'Žagars', 32, 'PG'),
    ('Jilson', 'Bango', 44, 'C'),
    ('Bonzie', 'Colson', 50, 'SF'),
    ('Khem', 'Birch', 92, 'C')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Fenerbahçe';

-- Hapoel IBI Tel Aviv: Hapoel Tel Aviv B.C., 24 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1329265139
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Johnathan', 'Motley', 0, 'PF'),
    ('Chris', 'Jones', 1, 'PG'),
    ('Antonio', 'Blakeney', 2, 'SG'),
    ('Elijah', 'Bryant', 3, 'SF'),
    ('Itay', 'Segev', 6, 'PF'),
    ('Guy', 'Palatin', 9, 'SG'),
    ('Bar', 'Timor', 10, 'SG'),
    ('Tyler', 'Ennis', 11, 'PG'),
    ('Oz', 'Blayzer', 14, 'SF'),
    ('Collin', 'Malcolm', 17, 'SF'),
    ('Tai', 'Odiase', 21, 'C'),
    ('Vasilije', 'Micić', 22, 'PG'),
    ('Ish', 'Wainright', 24, 'SF'),
    ('Daniel', 'Oturu', 25, 'C'),
    ('Yam', 'Madar', 26, 'PG'),
    ('Yonatan', 'Rabinowitz', 30, 'PG'),
    ('Tomer', 'Ginat', 41, 'PF'),
    ('Bruno', 'Caboclo', 51, 'PF'),
    ('Yiftach', 'Ziv', 99, 'PG')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Hapoel IBI Tel Aviv';

-- Maccabi Tel Aviv: Maccabi Tel Aviv B.C., 17 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1328002158
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Gabriel', 'Lundberg', 0, 'SG'),
    ('Jaylen', 'Hoard', 1, 'PF'),
    ('Jimmy', 'Clark III', 2, 'SG'),
    ('Márcio', 'Santos', 3, 'C'),
    ('Gur', 'Lavy', 4, 'SF'),
    ('Lonnie', 'Walker IV', 8, 'SG'),
    ('Roman', 'Sorkin', 9, 'PF'),
    ('Oshae', 'Brissett', 10, 'SF'),
    ('Will', 'Rayman', 11, 'SF'),
    ('John', 'DiBartolomeo', 12, 'SG'),
    ('Jeff', 'Dowtin Jr.', 21, 'PG'),
    ('T. J.', 'Leaf', 22, 'PF'),
    ('Zach', 'Hankins', 35, 'C'),
    ('Tamir', 'Blatt', 45, 'PG')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Maccabi Tel Aviv';

-- Olimpia Milano: Olimpia Milano, 23 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1329099232
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Nico', 'Mannion', 1, 'PG'),
    ('Lorenzo', 'Brown', 2, 'PG'),
    ('Quinn', 'Ellis', 3, 'PG'),
    ('Devin', 'Booker', 6, 'PF'),
    ('Stefano', 'Tonut', 7, 'SG'),
    ('Leandro', 'Bolmaro', 10, 'SF'),
    ('Armoni', 'Brooks', 12, 'SG'),
    ('Zach', 'LeDay', 16, 'PF'),
    ('Giampaolo', 'Ricci', 17, 'SF'),
    ('Diego', 'Flaccadori', 21, 'SG'),
    ('Marko', 'Gudurić', 23, 'SF'),
    ('Ousmane', 'Diop', 25, 'C'),
    ('Shavon', 'Shields', 31, 'SF'),
    ('Josh', 'Nebo', 32, 'C'),
    ('Leonardo', 'Totè', 35, 'PF'),
    ('Bryant', 'Dunston', 42, 'C'),
    ('Nate', 'Sestina', 77, 'PF')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Olimpia Milano';

-- Olympiacos: Olympiacos B.C., 30 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330334922
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Thomas', 'Walkup', 0, 'PG'),
    ('Frank', 'Ntilikina', 1, 'PG'),
    ('Keenan', 'Evans', 2, 'PG'),
    ('Tyson', 'Ward', 3, 'SG'),
    ('Giannoulis', 'Larentzakis', 5, 'SG'),
    ('Moustapha', 'Fall', 10, 'C'),
    ('Monté', 'Morris', 11, 'PG'),
    ('Sasha', 'Vezenkov', 14, 'PF'),
    ('Kostas', 'Papanikolaou', 16, 'SF'),
    ('Omiros', 'Netzipoglou', 21, 'SG'),
    ('Tyler', 'Dorsey', 22, 'SG'),
    ('Alec', 'Peters', 25, 'SF'),
    ('Nikola', 'Milutinov', 33, 'C'),
    ('Kostas', 'Antetokounmpo', 37, 'C'),
    ('Donta', 'Hall', 45, 'C'),
    ('Shaquielle', 'McKissic', 77, 'SG'),
    ('Evan', 'Fournier', 94, 'SG')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Olympiacos';

-- Panathinaikos: Panathinaikos B.C., 30 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330303491
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('T. J.', 'Shorts', 0, 'PG'),
    ('Panagiotis', 'Kalaitzakis', 5, 'SF'),
    ('Cedi', 'Osman', 6, 'SF'),
    ('Richaun', 'Holmes', 8, 'C'),
    ('Kostas', 'Sloukas', 10, 'SG'),
    ('Nikos', 'Rogkavopoulos', 17, 'SF'),
    ('Alexandros', 'Samodurov', 20, 'PF'),
    ('Jerian', 'Grant', 22, 'SG'),
    ('Ioannis', 'Kouzeloglou', 24, 'PF'),
    ('Kendrick', 'Nunn', 25, 'SG'),
    ('Mathias', 'Lessort', 26, 'C'),
    ('Vassilis', 'Toliopoulos', 27, 'SG'),
    ('Kenneth', 'Faried', 35, 'C'),
    ('Marius', 'Grigonis', 40, 'SF'),
    ('Juancho', 'Hernangómez', 41, 'SF'),
    ('Dinos', 'Mitoglou', 44, 'PF'),
    ('Ömer', 'Yurtseven', 77, 'C')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Panathinaikos';

-- Paris Basketball: Paris Basketball, 23 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1329099843
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Nadir', 'Hifi', 2, 'SG'),
    ('Léopold', 'Cavalière', 4, 'PF'),
    ('Justin', 'Robinson', 5, 'PG'),
    ('Sebastián', 'Herrera', 7, 'SG'),
    ('Jared', 'Rhoden', 8, 'SF'),
    ('Lamar', 'Stevens', 9, 'SF'),
    ('Joël', 'Ayayi', 11, 'SG'),
    ('Allan', 'Dokossi', 13, 'C'),
    ('Momo', 'Faye', 15, 'C'),
    ('Enzo', 'Shahrvin', 18, 'C'),
    ('Jeremy', 'Morgan', 20, 'SF'),
    ('Ismaël', 'Bako', 21, 'C'),
    ('Amath', 'M''Baye', 22, 'PF'),
    ('Yakuba', 'Ouattara', 24, 'SF'),
    ('Daulton', 'Hommes', 34, 'PF'),
    ('Derek', 'Willis', 35, 'PF')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Paris Basketball';

-- Partizan: 2025–26 KK Partizan season, 31 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330526132
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Carlik', 'Jones', 1, 'PG'),
    ('Shake', 'Milton', 2, 'SG'),
    ('Miikka', 'Muurinen', 3, 'SF'),
    ('Duane', 'Washington Jr.', 4, 'SG'),
    ('Dylan', 'Osetkowski', 5, 'PF'),
    ('Mario', 'Nakić', 7, 'SF'),
    ('Mitar', 'Bošnjaković', 8, 'SF'),
    ('Vanja', 'Marinković', 9, 'SF'),
    ('Uroš', 'Mijailović', 10, 'SG'),
    ('Aleksej', 'Pokuševski', 11, 'PF'),
    ('Sterling', 'Brown', 12, 'SF'),
    ('Cameron', 'Payne', 15, 'SG'),
    ('Isaac', 'Bonga', 17, 'SF'),
    ('Arijan', 'Lakić', 19, 'SG'),
    ('Đorđe', 'Šekularac', 21, 'SG'),
    ('Jabari', 'Parker', 22, 'PF'),
    ('Luca', 'Illeș', 23, 'PG'),
    ('Bruno', 'Fernando', 24, 'C'),
    ('Nick', 'Calathes', 33, 'PG'),
    ('Uroš', 'Danilović', 44, 'SF'),
    ('Tyrique', 'Jones', 88, 'C')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Partizan';

-- Real Madrid: 2025–26 Real Madrid Baloncesto season, 30 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1330346876
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Trey', 'Lyles', 0, 'PF'),
    ('David', 'Krämer', 1, 'SG'),
    ('Alberto', 'Abalde', 6, 'SF'),
    ('Facundo', 'Campazzo', 7, 'PG'),
    ('Chuma', 'Okeke', 8, 'PF'),
    ('Gabriele', 'Procida', 9, 'SF'),
    ('Mario', 'Hezonja', 11, 'SF'),
    ('Théo', 'Maledon', 12, 'SG'),
    ('Izan', 'Almansa', 13, 'PF'),
    ('Gabriel', 'Deck', 14, 'SF'),
    ('Usman', 'Garuba', 16, 'PF'),
    ('Edy', 'Tavares', 22, 'C'),
    ('Sergio', 'Llull', 23, 'SG'),
    ('Andrés', 'Feliz', 24, 'PG'),
    ('Alex', 'Len', 25, 'C')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Real Madrid';

-- Valencia Basket: Valencia Basket, 23 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1329100188
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Brancou', 'Badio', 0, 'SG'),
    ('Kameron', 'Taylor', 1, 'SF'),
    ('Josep', 'Puerto', 2, 'SF'),
    ('Nate', 'Reuvers', 3, 'PF'),
    ('Jaime', 'Pradilla', 4, 'PF'),
    ('Sergio', 'de Larrea', 5, 'PG'),
    ('Xabier', 'López-Arostegui', 6, 'SF'),
    ('Braxton', 'Key', 7, 'SF'),
    ('Jean', 'Montero', 8, 'PG'),
    ('Omari', 'Moore', 10, 'SG'),
    ('Neal', 'Sako', 12, 'C'),
    ('Darius', 'Thompson', 13, 'PG'),
    ('Matt', 'Costello', 24, 'PF'),
    ('Isaac', 'Nogués', 32, 'SG'),
    ('Yankuba', 'Sima', 77, 'C')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Valencia Basket';

-- Virtus Bologna: 2025–26 Virtus Bologna season, 16 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1327928193
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Luca', 'Vildoza', 1, 'SG'),
    ('Carsen', 'Edwards', 3, 'SG'),
    ('Alessandro', 'Pajola', 6, 'PG'),
    ('Saliou', 'Niang', 7, 'SF'),
    ('Matteo', 'Accorsi', 8, 'SG'),
    ('Alen', 'Smailagić', 9, 'PF'),
    ('Brandon', 'Taylor', 11, 'PG'),
    ('Derrick', 'Alston Jr.', 21, 'SF'),
    ('Abramo', 'Canka', 22, 'SF'),
    ('Daniel', 'Hackett', 23, 'SG'),
    ('Leo', 'Menalo', 24, 'PF'),
    ('Matt', 'Morgan', 30, 'SG'),
    ('Aliou', 'Diarra', 31, 'C'),
    ('Karim', 'Jallow', 34, 'SF'),
    ('Mouhamet', 'Diouf', 35, 'C'),
    ('Nicola', 'Akele', 45, 'SF')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Virtus Bologna';

-- Žalgiris: BC Žalgiris, 27 Aralık 2025
-- https://en.wikipedia.org/w/index.php?oldid=1329765392
INSERT INTO players (team_id, first_name, last_name, jersey_number, position)
SELECT t.id, p.first_name, p.last_name, p.jersey_number, p.position
FROM teams t
CROSS JOIN (VALUES
    ('Nigel', 'Williams-Goss', 1, 'PG'),
    ('Sylvain', 'Francisco', 3, 'PG'),
    ('Moses', 'Wright', 7, 'C'),
    ('Ignas', 'Brazdeikis', 8, 'SF'),
    ('Dovydas', 'Giedraitis', 9, 'SG'),
    ('Ąžuolas', 'Tubelis', 10, 'PF'),
    ('Maodo', 'Lô', 12, 'SG'),
    ('Dustin', 'Sleva', 14, 'PF'),
    ('Laurynas', 'Birutis', 15, 'C'),
    ('Mantas', 'Rubštavičius', 17, 'SF'),
    ('Arnas', 'Butkevičius', 51, 'SF'),
    ('Deividas', 'Sirvydis', 91, 'SF'),
    ('Edgaras', 'Ulanovas', 92, 'SF')
) AS p (first_name, last_name, jersey_number, position)
WHERE t.name = 'Žalgiris';

-- Bir takım adı V2 ile uyuşmazsa INSERT sessizce sıfır satır ekler; bunu yakalamak için denetim.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM teams t WHERE NOT EXISTS (SELECT 1 FROM players p WHERE p.team_id = t.id)) THEN
        RAISE EXCEPTION 'V4: kadrosu eklenmemiş takım var';
    END IF;
END $$;
