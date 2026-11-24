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

## Bilinen sınırlar

- Aynı ad veya forma numarası iki istekte aynı anda gönderilirse kontrolü ikisi de geçebilir; ikinci kayıt
  veritabanı kısıtına takılır. Bu durumun 409'a çevrilmesi #13'te. Aynı adla iki sezon için de geçerlidir.
- Sezona takım ekleme ve fikstür üretme sezon satırını kilitler (`SELECT … FOR UPDATE`); aynı sezona gelen
  eşzamanlı istekler sırayla işlenir. Takım sınırı ve tekillik atlanamaz, fikstür iki kez üretilemez ve fikstür
  üretilirken sezona takım eklenemez.
- Liste sıralaması veritabanının sıralama kuralını (collation) kullanır; Türkçe `Collator` ile sıralama yoktur.
