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

## Bilinen sınırlar

- Aynı ad veya forma numarası iki istekte aynı anda gönderilirse kontrolü ikisi de geçebilir; ikinci kayıt
  veritabanı kısıtına takılır. Bu durumun 409'a çevrilmesi #13'te.
- Liste sıralaması veritabanının sıralama kuralını (collation) kullanır; Türkçe `Collator` ile sıralama yoktur.
