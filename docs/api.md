# REST API sözleşmesi

Bütün yollar `/api` altındadır (ör. `http://localhost:8080/basketball-league/api/teams`). İstek ve yanıt gövdeleri
`application/json`, hata gövdeleri `application/problem+json` biçimindedir ([ADR-0006](adr/0006-hata-yonetimi-ve-loglama.md)).

## Ortak kurallar

- **Kimlikler** sayısaldır (`id`, `bigint`).
- **Listeler sayfalıdır:** `?page=0&size=20`. `page` 0'dan başlar; `size` 1–100 arasıdır, varsayılan 20.
  Aralık dışı değer veya sıfırdan küçük/sıfır `id` `400 VALIDATION_FAILED` döner.
- **Sayfa yanıtı:**

  ```json
  { "items": [ ... ], "page": 0, "size": 20, "totalItems": 18 }
  ```

- **Hata yanıtı** (Problem Details). `code` kalıcıdır, istemci buna göre karar verebilir; `detail` Türkçe ve
  kullanıcıya gösterilebilir. Doğrulama hatalarında `errors` alan listesini verir:

  ```json
  {
    "type": "about:blank",
    "title": "Bad Request",
    "status": 400,
    "detail": "İstekte geçersiz alanlar var.",
    "code": "VALIDATION_FAILED",
    "errors": [ { "field": "jerseyNumber", "message": "99 veya daha küçük olmalıdır" } ]
  }
  ```

## Takımlar

### `GET /teams`

Takımları ada göre sıralı, sayfalı listeler.

```json
200 OK
{
  "items": [ { "id": 1, "name": "Anadolu Efes", "city": "İstanbul" } ],
  "page": 0, "size": 20, "totalItems": 1
}
```

### `GET /teams/{id}`

```json
200 OK
{ "id": 1, "name": "Anadolu Efes", "city": "İstanbul" }
```

| Durum | `code` |
| --- | --- |
| 404 | `TEAM_NOT_FOUND` |

### `GET /teams/{id}/seasons`

Takımın yer aldığı sezonları başlangıç tarihine göre yeniden eskiye sıralı bir dizi olarak döner.

```json
[
  { "id": 1, "name": "2026-27 Sezonu", "startDate": "2026-10-01", "status": "ACTIVE" }
]
```

| Durum | `code` |
| --- | --- |
| 404 | `TEAM_NOT_FOUND` |

### `GET /teams/{id}/seasons/{seasonId}/overview`

Takımın seçilen sezondaki puan durumu satırını ve en fazla beş son onaylanmış maçını döner.
Maçlar hafta numarasına göre yeniden eskiye sıralıdır; yalnızca sonucu onaylanmış maçlar dahil edilir.

```json
{
  "season": { "id": 1, "name": "2026-27 Sezonu", "startDate": "2026-10-01", "status": "ACTIVE" },
  "summary": {
    "rank": 2, "teamId": 7, "teamName": "Anadolu Efes", "played": 5, "won": 3, "lost": 2,
    "pointsFor": 420, "pointsAgainst": 399, "pointDiff": 21, "leaguePoints": 8
  },
  "lastFiveGames": [
    {
      "id": 10, "roundNumber": 5,
      "homeTeam": { "id": 7, "name": "Anadolu Efes", "city": "İstanbul" },
      "awayTeam": { "id": 9, "name": "Fenerbahçe", "city": "İstanbul" },
      "homeScore": 84, "awayScore": 80
    }
  ]
}
```

| Durum | `code` |
| --- | --- |
| 404 | `TEAM_NOT_FOUND` |
| 404 | `SEASON_NOT_FOUND` |
| 404 | `TEAM_NOT_IN_SEASON` |

### `POST /teams`

```json
{ "name": "Anadolu Efes", "city": "İstanbul" }
```

```text
201 Created
Location: http://localhost:8080/basketball-league/api/teams/1
```

```json
{ "id": 1, "name": "Anadolu Efes", "city": "İstanbul" }
```

| Alan | Kural |
| --- | --- |
| `name` | zorunlu, en fazla 100 karakter, takımlar arasında tekil (baştaki/sondaki boşluk atılır) |
| `city` | zorunlu, en fazla 100 karakter |

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | alan kuralı ihlali |
| 400 | `MALFORMED_REQUEST` | gövde yok veya JSON olarak okunamıyor |
| 409 | `TEAM_NAME_TAKEN` | aynı adda takım var |

Ad karşılaştırması büyük/küçük harfe duyarlıdır (veritabanındaki `UNIQUE` kısıtı ile aynı).

## Oyuncular

Oyuncu yalnızca ad, soyad, forma numarası ve pozisyon taşır ([ADR-0001](adr/0001-katmanli-mimari.md) veri kapsamı).
Pozisyon değerleri: `PG`, `SG`, `SF`, `PF`, `C`.

### `GET /teams/{id}/players`

Takımın oyuncularını forma numarasına göre sıralı, sayfalı listeler.

```json
200 OK
{
  "items": [ { "id": 7, "firstName": "Shane", "lastName": "Larkin", "jerseyNumber": 3, "position": "PG" } ],
  "page": 0, "size": 20, "totalItems": 1
}
```

| Durum | `code` |
| --- | --- |
| 404 | `TEAM_NOT_FOUND` |

### `POST /teams/{id}/players`

```json
{ "firstName": "Shane", "lastName": "Larkin", "jerseyNumber": 3, "position": "PG" }
```

```json
201 Created
{ "id": 7, "firstName": "Shane", "lastName": "Larkin", "jerseyNumber": 3, "position": "PG" }
```

Oyuncunun kendi adresi (`/players/{id}`) henüz olmadığı için `Location` başlığı gönderilmez.

| Alan | Kural |
| --- | --- |
| `firstName`, `lastName` | zorunlu, en fazla 100 karakter |
| `jerseyNumber` | zorunlu, 0–99, takım içinde tekil |
| `position` | zorunlu, `PG` \| `SG` \| `SF` \| `PF` \| `C` |

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | alan kuralı ihlali |
| 400 | `MALFORMED_REQUEST` | gövde yok veya JSON olarak okunamıyor |
| 404 | `TEAM_NOT_FOUND` | takım yok |
| 409 | `JERSEY_NUMBER_TAKEN` | forma numarası bu takımda kullanılıyor |

## Sezonlar

Sezon `DRAFT` durumunda oluşturulur ve takımlar yalnızca bu durumda eklenir. Fikstür üretilince sezon `ACTIVE`
olur ve takım listesi artık değişmez ([ADR-0003](adr/0003-entity-domain-modeli.md)). `status` değerleri:
`DRAFT`, `ACTIVE`, `FINISHED`. `startDate` ISO-8601 tarihtir (`YYYY-AA-GG`).

### `GET /seasons`

Sezonları başlangıç tarihine göre yeniden eskiye, sayfalı listeler.

```json
200 OK
{
  "items": [ { "id": 1, "name": "2026-27 Demo Sezonu", "startDate": "2026-10-01", "status": "DRAFT" } ],
  "page": 0, "size": 20, "totalItems": 1
}
```

### `GET /seasons/{id}`

```json
200 OK
{ "id": 1, "name": "2026-27 Demo Sezonu", "startDate": "2026-10-01", "status": "DRAFT" }
```

| Durum | `code` |
| --- | --- |
| 404 | `SEASON_NOT_FOUND` |

### `POST /seasons`

```json
{ "name": "2026-27 Demo Sezonu", "startDate": "2026-10-01" }
```

```text
201 Created
Location: http://localhost:8080/basketball-league/api/seasons/1
```

```json
{ "id": 1, "name": "2026-27 Demo Sezonu", "startDate": "2026-10-01", "status": "DRAFT" }
```

| Alan | Kural |
| --- | --- |
| `name` | zorunlu, en fazla 100 karakter, sezonlar arasında tekil (baştaki/sondaki boşluk atılır) |
| `startDate` | zorunlu, `YYYY-AA-GG` |

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | alan kuralı ihlali |
| 400 | `MALFORMED_REQUEST` | gövde yok, JSON olarak okunamıyor veya tarih geçersiz (ör. `2026-02-30`) |
| 409 | `SEASON_NAME_TAKEN` | aynı adda sezon var |

### `GET /seasons/{id}/teams`

Sezondaki takımları ada göre sıralı döner. Bir sezonda en fazla 20 takım olduğu için liste sayfalı değildir;
yanıt bir dizidir.

```json
200 OK
[ { "id": 1, "name": "Anadolu Efes", "city": "İstanbul" } ]
```

| Durum | `code` |
| --- | --- |
| 404 | `SEASON_NOT_FOUND` |

### `POST /seasons/{id}/teams`

Var olan bir takımı sezona ekler.

```json
{ "teamId": 1 }
```

```json
201 Created
{ "id": 1, "name": "Anadolu Efes", "city": "İstanbul" }
```

Sezon üyeliğinin kendi adresi olmadığı için `Location` başlığı gönderilmez.

| Alan | Kural |
| --- | --- |
| `teamId` | zorunlu, 0'dan büyük |

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | alan kuralı ihlali |
| 400 | `MALFORMED_REQUEST` | gövde yok veya JSON olarak okunamıyor |
| 404 | `SEASON_NOT_FOUND` | sezon yok |
| 404 | `TEAM_NOT_FOUND` | takım yok |
| 409 | `SEASON_ALREADY_STARTED` | sezon `DRAFT` değil (fikstürü üretilmiş) |
| 409 | `TEAM_ALREADY_IN_SEASON` | takım sezonda zaten var |
| 409 | `SEASON_FULL` | sezonda 20 takım var |

## Fikstür

Fikstür sezonun takımlarından çift devreli olarak üretilir ([ADR-0008](adr/0008-fikstur-algoritmasi.md)):
çift sayıda `n` takımda `2(n − 1)` tur ve `n(n − 1)` maç. Takım sayısı tekse her turda bir takım bay geçer.
Fikstürü üretmek sezonu `ACTIVE` yapar; aynı sezon için ikinci kez üretilemez.

### `GET /seasons/{id}/fixtures`

Sezonun turlarını tur numarasına göre sıralı döner. Bir sezonda en fazla 20 takım olduğu için en fazla 38 tur
ve 380 maç vardır; yanıt sayfalı değildir. Fikstürü üretilmemiş sezonda `rounds` boştur.

```json
200 OK
{
  "seasonId": 1,
  "rounds": [
    {
      "number": 1,
      "games": [
        {
          "id": 10,
          "homeTeam": { "id": 1, "name": "Anadolu Efes", "city": "İstanbul" },
          "awayTeam": { "id": 2, "name": "Fenerbahçe", "city": "İstanbul" },
          "status": "SCHEDULED"
        }
      ],
      "byeTeam": { "id": 3, "name": "Žalgiris", "city": "Kaunas" }
    }
  ]
}
```

- Bir turdaki maçlar maç kimliğine göre sıralıdır; ilk yazılan takım ev sahibidir.
- `byeTeam` yalnızca o turda bay geçen takım varsa (takım sayısı tekse) bulunur.
- `status` değerleri: `SCHEDULED`, `LIVE`, `FINISHED`, `CONFIRMED`.

| Durum | `code` |
| --- | --- |
| 404 | `SEASON_NOT_FOUND` |

### `POST /seasons/{id}/fixtures`

Gövde almaz. Fikstürü üretir, sezonu `ACTIVE` yapar ve üretilen fikstürü `GET` ile aynı biçimde döner.

```text
201 Created
Location: http://localhost:8080/basketball-league/api/seasons/1/fixtures
```

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 404 | `SEASON_NOT_FOUND` | sezon yok |
| 409 | `INVALID_FIXTURE_INPUT` | sezonda 2'den az takım var |
| 409 | `SEASON_ALREADY_STARTED` | fikstür zaten üretilmiş |

## Maçlar

> Kararlar [ADR-0004](adr/0004-mac-onayi-transaction.md)'tedir.

Maç `SCHEDULED → LIVE → FINISHED → CONFIRMED` akışını izler. Bitmiş maç onaydan önce yeniden açılabilir
(`FINISHED → LIVE`); onaylanmış maç değişmez. Çeyrek skorları ve oyuncu sayıları maçın parçasıdır.

| İşlem | Uç nokta | Durum geçişi | Yapılabildiği durum |
| --- | --- | --- | --- |
| Başlat | `POST /games/{id}/start` | `SCHEDULED → LIVE` | `SCHEDULED` |
| Çeyrekleri kaydet | `PUT /games/{id}/quarters` | yok | `LIVE` |
| Oyuncu sayılarını kaydet | `PUT /games/{id}/stats` | yok | `LIVE`, `FINISHED` |
| Bitir | `POST /games/{id}/finish` | `LIVE → FINISHED` | `LIVE` |
| Yeniden aç | `POST /games/{id}/reopen` | `FINISHED → LIVE` | `FINISHED` |
| Onayla | `POST /games/{id}/confirm` | `FINISHED → CONFIRMED` | `FINISHED` |

**Sürüm:** Maçı değiştiren her istek, yöneticinin son okuduğu `version` değerini gövdede taşır. Maç o arada başka
bir istekle değiştiyse 409 `CONCURRENT_UPDATE` döner; istemci maçı yeniden okuyup tekrar dener. Başarılı her
değişiklik `version`'ı artırır ve güncel maçı döner.

### `GET /games/{id}`

```json
200 OK
{
  "id": 10,
  "seasonId": 1,
  "roundNumber": 1,
  "homeTeam": { "id": 1, "name": "Anadolu Efes", "city": "İstanbul" },
  "awayTeam": { "id": 9, "name": "Fenerbahçe", "city": "İstanbul" },
  "status": "FINISHED",
  "homeScore": 84,
  "awayScore": 80,
  "quarters": [
    { "quarterNo": 1, "homePoints": 22, "awayPoints": 18 },
    { "quarterNo": 2, "homePoints": 20, "awayPoints": 21 },
    { "quarterNo": 3, "homePoints": 19, "awayPoints": 20 },
    { "quarterNo": 4, "homePoints": 23, "awayPoints": 21 }
  ],
  "playerPoints": [
    { "playerId": 7, "teamId": 1, "points": 21 },
    { "playerId": 40, "teamId": 9, "points": 17 }
  ],
  "version": 6
}
```

- `homeScore` ve `awayScore` yalnızca `FINISHED` ve `CONFIRMED` maçta bulunur; çeyrek toplamlarından hesaplanır,
  istemci göndermez.
- `playerPoints[].teamId` oyuncunun sayının girildiği andaki takımıdır; sonradan yapılan transfer onu değiştirmez.
- Örnekte `playerPoints` kısaltılmıştır; onay için her takımın oyuncu sayıları toplamı takım skoruna eşit olmalıdır.

| Durum | `code` |
| --- | --- |
| 404 | `GAME_NOT_FOUND` |

### `POST /games/{id}/start`, `/finish`, `/reopen`, `/confirm`

```json
{ "version": 6 }
```

```text
200 OK   (güncel maç, GET /games/{id} biçiminde)
```

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | `version` yok veya negatif |
| 404 | `GAME_NOT_FOUND` | maç yok |
| 409 | `INVALID_GAME_STATUS_TRANSITION` | geçiş tablodaki durumdan yapılmıyor (ör. `SCHEDULED` maçı onaylamak) |
| 409 | `INVALID_QUARTER_SCORE` | `finish` veya `confirm`: 4'ten az çeyrek var |
| 409 | `OVERTIME_REQUIRED` | `finish`: skor eşit; uzatma çeyreği girilmeli |
| 409 | `SCORE_MISMATCH` | `confirm`: çeyrek toplamları veya bir takımın oyuncu sayıları toplamı skora eşit değil |
| 409 | `CONCURRENT_UPDATE` | `version` güncel değil veya aynı anda başka bir değişiklik kaydedildi |

### `PUT /games/{id}/quarters`

Maçın bütün çeyreklerini bir arada değiştirir; gönderilmeyen çeyrek silinir.

```json
{
  "version": 3,
  "quarters": [
    { "quarterNo": 1, "homePoints": 22, "awayPoints": 18 },
    { "quarterNo": 2, "homePoints": 20, "awayPoints": 21 }
  ]
}
```

| Alan | Kural |
| --- | --- |
| `version` | zorunlu, 0 veya büyük |
| `quarters` | zorunlu, en fazla 10 çeyrek; `quarterNo` 1'den başlar ve boşluksuz artar; sayılar 0–100 |
| Uzatma | 5. ve sonraki çeyrek yalnızca önceki çeyrekler sonunda skor eşitse girilebilir |

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | alan eksik, sayı negatif veya 100'den büyük, 10'dan fazla çeyrek |
| 404 | `GAME_NOT_FOUND` | maç yok |
| 409 | `INVALID_GAME_STATUS_TRANSITION` | maç `LIVE` değil |
| 409 | `INVALID_QUARTER_SCORE` | çeyrek sırası boşluklu veya tekrarlı, gereksiz uzatma |
| 409 | `CONCURRENT_UPDATE` | `version` güncel değil |

### `PUT /games/{id}/stats`

Maçın bütün oyuncu sayılarını bir arada değiştirir; gönderilmeyen oyuncunun sayısı silinir. Oyuncunun takımı
sunucuda belirlenir.

```json
{
  "version": 5,
  "players": [
    { "playerId": 7, "points": 21 },
    { "playerId": 40, "points": 17 }
  ]
}
```

| Alan | Kural |
| --- | --- |
| `version` | zorunlu, 0 veya büyük |
| `players` | zorunlu, en fazla 40 oyuncu; her oyuncu bir kez; `points` 0–200 |

| Durum | `code` | Ne zaman |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | alan eksik, sayı negatif veya 200'den büyük, aynı oyuncu iki kez, 40'tan fazla oyuncu |
| 404 | `GAME_NOT_FOUND` | maç yok |
| 409 | `INVALID_GAME_STATUS_TRANSITION` | maç `LIVE` veya `FINISHED` değil |
| 409 | `PLAYER_NOT_IN_GAME` | oyuncu yok ya da maçın iki takımından birinde değil |
| 409 | `CONCURRENT_UPDATE` | `version` güncel değil |

Negatif sayı ve tekrarlanan oyuncu önce istek doğrulamasında 400 döner. Aynı kurallar `Game` içinde de denetlenir
(409 `INVALID_PLAYER_POINTS`, ADR-0004); bu kod yalnızca doğrulamayı atlayan bir çağrıda görülür.

## Puan durumu

Puan durumu saklanmaz; her okumada sezonun yalnızca `CONFIRMED` maçlarından hesaplanır
([ADR-0001](adr/0001-katmanli-mimari.md)). Galibiyet 2, mağlubiyet 1 puandır.

### `GET /seasons/{id}/standings`

Sezonun bütün takımlarını sıralı döner. Onaylı maçı olmayan takım sıfırlarla listelenir. Bir sezonda en fazla
20 takım olduğu için yanıt sayfalı değildir; bir dizidir.

Sıralama: lig puanı → ikili averaj (aralarındaki maçlar) → genel averaj (`pointDiff`) → atılan sayı → takım adı
(Türk alfabesine göre).

```json
200 OK
[
  {
    "rank": 1, "teamId": 1, "teamName": "Anadolu Efes",
    "played": 2, "won": 2, "lost": 0, "pointsFor": 170, "pointsAgainst": 130, "pointDiff": 40, "leaguePoints": 4
  }
]
```

| Durum | `code` |
| --- | --- |
| 404 | `SEASON_NOT_FOUND` |

Maç sonucu girişi ve onay ([ADR-0004](adr/0004-mac-onayi-transaction.md)) gelene kadar onaylı maç olmadığı için
bütün satırlar sıfırdır.

## Bilinen sınırlar

- Aynı ad veya forma numarası iki istekte aynı anda gönderilirse kontrolü ikisi de geçebilir; ikinci kayıt
  veritabanı kısıtına takılır ve 409 `DATA_CONFLICT` döner (kurala özel kod yerine). Aynı adla iki sezon için de
  geçerlidir.
- Sezona takım ekleme ve fikstür üretme sezon satırını kilitler (`SELECT … FOR UPDATE`); aynı sezona gelen
  eşzamanlı istekler sırayla işlenir. Takım sınırı ve tekillik atlanamaz, fikstür iki kez üretilemez ve fikstür
  üretilirken sezona takım eklenemez.
- Liste sıralaması veritabanının sıralama kuralını (collation) kullanır; Türkçe `Collator` ile sıralama yoktur.
  Puan durumundaki son eşitlik bozma adımı (takım adı) bunun istisnasıdır; Türkçe `Collator` kullanır.
- Üç veya daha fazla takım eşit puanlıysa ikili averaj takımlar ikişer ikişer karşılaştırılarak uygulanır; mini lig
  hesaplanmaz (kapsam dışı).
