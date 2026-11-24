-- =============================================================================
-- V2: EuroLeague 2025-26 normal sezonundaki 20 kulübü başlangıç verisi olarak ekle.
--
-- Veri sürümü : euroleague-2025-26-teams-v1
-- Referans    : 2025-26 EuroLeague sezonunun (30 Eylül 2025 - 24 Mayıs 2026) normal sezon kulüpleri
-- Kaynak      : https://en.wikipedia.org/wiki/2025%E2%80%9326_EuroLeague ("Qualified teams" bölümü)
-- Alınma      : 20 Kasım 2026
--
-- Kurallar (ADR-0001 veri kapsamı):
-- - Yalnızca takım adı ve şehir; logo, renk, marka görseli yok.
-- - Ad, kaynaktaki "Qualified teams" listesinde yazıldığı gibi ve diakritiklerle saklanır.
--   Sponsorlu adlar sezon içinde değiştiği ve kaynakta tutarsız olduğu için kullanılmadı.
-- - Şehir, Türkçede yerleşik yazımıyla (Münih, Belgrad, Pire); yoksa yerel adıyla.
-- - Kadrolar bu migration'da yok; referans tarihi seçilince ayrı bir migration ile gelecek.
-- - Bu bir demo sezonu verisidir; skorlar ve fikstür uygulamada üretilir.
-- =============================================================================

INSERT INTO teams (name, city) VALUES
    ('Anadolu Efes',        'İstanbul'),
    ('AS Monaco',           'Monako'),
    ('ASVEL',               'Villeurbanne'),
    ('Barcelona',           'Barselona'),
    ('Baskonia',            'Vitoria-Gasteiz'),
    ('Bayern Munich',       'Münih'),
    ('Crvena zvezda',       'Belgrad'),
    ('Dubai Basketball',    'Dubai'),
    ('Fenerbahçe',          'İstanbul'),
    ('Hapoel IBI Tel Aviv', 'Tel Aviv'),
    ('Maccabi Tel Aviv',    'Tel Aviv'),
    ('Olimpia Milano',      'Milano'),
    ('Olympiacos',          'Pire'),
    ('Panathinaikos',       'Atina'),
    ('Paris Basketball',    'Paris'),
    ('Partizan',            'Belgrad'),
    ('Real Madrid',         'Madrid'),
    ('Valencia Basket',     'Valensiya'),
    ('Virtus Bologna',      'Bologna'),
    ('Žalgiris',            'Kaunas');
