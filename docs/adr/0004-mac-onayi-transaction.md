---
title: ADR-0004-Maç Sonucu, Onay Transaction'ı ve Eşzamanlı Değişiklikler
status: Kabul Edildi
date: 2026-11-25
author: Ömer Ünlü
deciders: Ömer Ünlü, Osman Balcı, Mustafa Budak
---

# ADR-0004: Maç Sonucu, Onay Transaction'ı ve Eşzamanlı Değişiklikler

## Bağlam

ADR-0001 maç onayının akışını tanımlar: servis maçı, çeyrek skorlarını ve oyuncu istatistiklerini yükler,
`Game.confirm()` tutarlılığı denetler, maç `CONFIRMED` olur ve puan durumu onaylı maçlardan hesaplanır.
Kayıt modeli, kilitleme ve geri alma ayrıntısı ayrı bir karara bırakılmıştır. ADR-0003 maç durumlarını
(`SCHEDULED → LIVE → FINISHED → CONFIRMED`) ve `Game`'in `@Version` taşıdığını belirler.

ADR-0001'in incelemesinde (PR #5) şu risk not edildi: onay sırasında çeyrek skorları ve oyuncu istatistikleri
okunur. Bunlar ayrı satırlarda değişirse `Game`'in sürümü artmaz ve maç eski verilerle onaylanabilir.

Cevaplanması gereken sorular:

- Çeyrek skorları ve oyuncu sayıları `Game`'in parçası mı, ayrı nesneler mi?
- Maç skoru saklanacak mı, her okumada çeyreklerden mi hesaplanacak?
- Berabere biten normal süre, uzatma ve bitmiş maçta hatalı skorun düzeltilmesi (#30) nasıl ele alınacak?
- Onay sırasında eşzamanlı bir değişiklik nasıl fark edilecek: kilitle mi, sürümle mi?

## Karar

**Çeyrek skorlarını ve oyuncu sayılarını `Game`'in parçası (value object) olarak tutacağız. Maçta yapılan her
değişiklik maçın sürümünü artıracak. Onay tek bir `@Transactional` işlemde, istemcinin gördüğü sürümle
yapılacak.**

### Maçın parçaları

`Game`, kendi kimliği olmayan iki değer listesi taşır. Bölüm 12'deki sipariş kalemleri gibi, maçla birlikte
yüklenir, maçla birlikte yazılır ve maç olmadan anlam taşımaz. [1]

| Değer nesnesi                                | Tablo                   | Birincil anahtar          | Kural                                                    |
| -------------------------------------------- | ----------------------- | ------------------------- | -------------------------------------------------------- |
| `QuarterScore(quarterNo, homePoints, awayPoints)` | `game_quarter_scores` | `(game_id, quarter_no)`   | `quarter_no` 1'den başlar ve boşluksuz artar; 5 ve sonrası uzatma; sayılar `>= 0` |
| `PlayerPoints(playerId, teamId, points)`     | `player_game_stats`     | `(game_id, player_id)`    | `teamId` maçın iki takımından biri; sayı `>= 0`          |

- `PlayerPoints.teamId`, oyuncunun **sayının girildiği andaki** takımıdır. Sonradan yapılan transfer eski maçların
  istatistiğini değiştirmez.
- Oyuncunun o takımda olduğunu servis denetler; bütün oyuncular tek sorguyla yüklenir (N+1 yok).
- Liste her kayıtta bütün olarak değiştirilir (`PUT`). Bir maçta en fazla birkaç çeyrek ve birkaç düzine oyuncu
  satırı olduğu için bu sade ve yeterlidir.

### Skor ve maçın akışı

Maç skoru (`games.home_score`, `games.away_score`) kullanıcıdan ayrıca alınmaz. `finish()` çeyrek toplamlarından
yazar. Toplamlar `Math.addExact` ile hesaplanır; taşma sessizce geçmez.

| Metot                   | Geçiş               | Kural                                                                                                   |
| ----------------------- | ------------------- | ------------------------------------------------------------------------------------------------------- |
| `start()`               | `SCHEDULED → LIVE`  | Mevcut.                                                                                                 |
| `recordQuarters(...)`   | `LIVE` içinde       | Çeyrekler sıralı. 4. çeyrekten sonra eşitlik yoksa uzatma girilemez.                                     |
| `recordPlayerPoints(...)` | `LIVE` veya `FINISHED` | Oyuncu sayıları maç bittikten sonra da girilebilir.                                                  |
| `finish()`              | `LIVE → FINISHED`   | En az 4 çeyrek var ve skor eşit değil. Eşitse uzatma gerekir. Skor çeyreklerden yazılır.                 |
| `reopen()`              | `FINISHED → LIVE`   | Hatalı girilmiş skor onaydan önce düzeltilebilir (#30). Saklanan skor temizlenir.                        |
| `confirm(version)`      | `FINISHED → CONFIRMED` | Her takımın oyuncu sayıları toplamı takım skoruna eşit. Sonrasında hiçbir değişiklik kabul edilmez.  |

`reopen()`, ADR-0003'teki geçiş tablosuna `FINISHED → LIVE` geçişini ekler. `CONFIRMED` son durum olarak kalır.

### Eşzamanlı değişiklikler: sürüm, kilit değil

1. Maçı değiştiren her kullanım senaryosu (çeyrek, oyuncu sayıları, `finish`, `reopen`, `confirm`) maçı
   `LockModeType.OPTIMISTIC_FORCE_INCREMENT` ile yükler. Yalnızca değer listesi değişse bile `games.version`
   artar. [3] PR #5'teki "ayrı satır değişir, sürüm artmaz" riski böylece ortadan kalkar.
2. Maç okuma yanıtı `version` alanını içerir. Onay isteği yöneticinin gördüğü sürümü gönderir:
   `POST /games/{id}/confirm` gövdesinde `{ "version": 7 }`.
3. Servis bu sürümü maçın güncel sürümüyle karşılaştırır. Farklıysa yönetici artık ekranda olmayan bir veriyi
   onaylamak üzeredir: 409 `CONCURRENT_UPDATE`.
4. Karşılaştırmadan sonra başka bir istek araya girerse commit sırasındaki sürüm denetimi
   `OptimisticLockException` üretir. Bu da 409 `CONCURRENT_UPDATE` olur (ADR-0006, #13).

### Transaction ve geri alma

- `GameService.confirm` tek bir `@Transactional` (`REQUIRED`) metottur. Yükleme, doğrulama ve durum değişikliği
  aynı işlemde yapılır. [2]
- Kural ihlalleri `DomainException` alt sınıfıdır (`RuntimeException`). Fırlatılınca JTA işlemi geri alır;
  serviste `try/catch` ile elle geri alma yapılmaz.
- Puan durumu tablosu yoktur (ADR-0001). Geri alınan bir onaydan sonra maç `FINISHED` kalır ve puan durumu
  sorgusu onu saymaz.

### Hata kodları

| Durum                                             | HTTP | `code`                           |
| ------------------------------------------------- | ---- | -------------------------------- |
| Maç bulunamadı                                    | 404  | `GAME_NOT_FOUND`                 |
| Geçersiz geçiş (ör. `SCHEDULED` maçı onaylamak)   | 409  | `INVALID_GAME_STATUS_TRANSITION` |
| Çeyrek sırası, negatif sayı, gereksiz uzatma      | 409  | `INVALID_QUARTER_SCORE`          |
| Normal süre berabere bitti, uzatma girilmedi      | 409  | `OVERTIME_REQUIRED`              |
| Oyuncu maçın iki takımından birinde değil         | 409  | `PLAYER_NOT_IN_GAME`             |
| Oyuncu sayısı kaydı geçersiz (negatif, tekrar)    | 409  | `INVALID_PLAYER_POINTS`          |
| Çeyrek toplamı veya oyuncu sayıları skora uymuyor | 409  | `SCORE_MISMATCH`                 |
| Gönderilen sürüm güncel değil / commit'te çakışma | 409  | `CONCURRENT_UPDATE`              |

Uç noktaların istek ve yanıt örnekleri sonuç girişi PR'ında (3.4a) `docs/api.md`'ye yazılır.

## Değerlendirilen Alternatifler

| **Alternatif**                                                      | **Neden seçilmedi?**                                                                                                                                                         |
| ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Çeyrek skoru ve istatistiği ayrı entity'ler (kendi repository'leri) | PR #5'teki riskin kaynağı: ayrı satırlar değişince maçın sürümü artmaz. Kurallar (sıra, uzatma, toplam) iki nesneye bölünür.                                                   |
| Onayda maç satırını `PESSIMISTIC_WRITE` ile kilitlemek              | Yalnızca çeyrek ve istatistik yazan bütün işlemler de aynı satırı kilitlerse işe yarar. Yöneticinin ekranda gördüğü eski veriyi yine fark etmez. Canlı skordaki iyimser yaklaşımla iki ayrı strateji oluşur. |
| Yalnızca sunucu tarafında `@Version` (istemci sürüm göndermez)      | Aynı anda çalışan iki işlemi ayırır, ama yönetici sayfayı açtıktan sonra yapılan değişikliği görmeden onaylayabilir.                                                           |
| Sürümü `ETag` / `If-Match` başlığıyla taşımak [4]                   | HTTP açısından doğru yol, ama her maç yanıtına `ETag` ve 412/428 kodları gerekir; ADR-0006 çakışmayı 409 ile tanımlar. Gerekirse yeni bir ADR ile yeniden değerlendirilir.    |
| Maç skorunu saklamamak, her okumada çeyreklerden hesaplamak          | Puan durumu sorgusu her maç için çeyrek tablosunu toplamak zorunda kalır. Onaylı maçın skoru zaten değişmez.                                                                  |
| Maç skorunu kullanıcının ayrıca girmesi                              | Aynı bilginin iki kaynağı olur; çeyrek toplamıyla uyuşmazlık yeni bir hata türü doğurur.                                                                                        |
| Bitmiş maçta düzeltme yok (`FINISHED → LIVE` yok)                   | Veri girişindeki bir yanlışlık ancak veritabanına elle müdahaleyle düzeltilebilir.                                                                                             |
| Onay hatasında serviste `try/catch` ile elle geri alma              | JTA bunu zaten yapar; elle geri alma unutulabilir ve hata yutulabilir (ADR-0001).                                                                                             |

## Sonuçlar

### Olumlu

- Maçla ilgili bütün kurallar tek nesnede; birim testleri veritabanı istemez.
- Çeyrek veya oyuncu sayısı değişince sürüm her zaman artar. Eski veriyle onay, hem eşzamanlı istekte hem de
  bayat ekranda yakalanır.
- Kilit beklemesi yok. Canlı skor girişi ile onay aynı iyimser yaklaşımı kullanır.
- Puan durumu sorgusu yalnızca `games` tablosunun skor sütunlarını okur.

### Olumsuz

- Maçı değiştiren her işlem bir sürüm artışı yazar; aynı maça hızlı ardışık girişlerde ikinci istek 409 alır ve
  yeniden denemesi gerekir.
- Değer listesinin bütün olarak değiştirilmesi, değişmeyen satırları da yeniden yazar.
- Arayüz, onay isteğinde son okuduğu sürümü göndermek zorundadır.

### Risk

| Risk                                                         | Nasıl ele alınacak?                                                                                 |
| ------------------------------------------------------------ | --------------------------------------------------------------------------------------------------- |
| `OPTIMISTIC_FORCE_INCREMENT`'in EclipseLink'te beklendiği gibi çalışmaması | Entegrasyon testi: yalnızca oyuncu sayısını değiştiren işlemden sonra `games.version` artmış olmalı. |
| Sarılmış `OptimisticLockException`'ın 500'e düşmesi          | #13'teki eşleyici sebep zincirine bakar; entegrasyon testiyle doğrulanır.                            |
| Toplamlarda taşma                                            | `Math.addExact`; `Integer.MAX_VALUE` sınır testi.                                                     |

## Doğrulama

| **Kural**                                                            | **Kim korur?**          | **ArchUnit kuralı / Test**                                   |
| -------------------------------------------------------------------- | ----------------------- | ------------------------------------------------------------ |
| Çeyrek sırası, uzatma, `finish`, `reopen`, `confirm` kuralları        | Birim testi             | `GameTest`, `QuarterScoreTest`                               |
| Oyuncu sayıları toplamı takım skoruna eşit değilse onay reddedilir    | Birim testi             | `GameTest`                                                   |
| `@Transactional` yalnızca servis katmanında                           | ArchUnit                | `TRANSACTIONAL_ONLY_IN_SERVICE`                              |
| Hata verdirilen onaydan sonra maç `FINISHED`, puan durumu saymıyor   | Entegrasyon (Testcontainers) | Onay rollback testi (#28)                               |
| Oyuncu sayısı değişince maçın sürümü artıyor                          | Entegrasyon             | Sürüm artışı testi                                           |
| İki kopya: ikinci onay veya değişiklik 409 alıyor                     | Entegrasyon             | Çakışma testi (iki ayrı `EntityManager`)                     |

## Kaynaklar

Erişim tarihi: **25 Kasım 2026**.

1. **Software Lecture Labs — [Bölüm 12: Persistence Boundary, JDBC ve Flyway][1].** "Aggregate sınırı =
   transaction sınırı", sipariş kalemlerinin siparişle birlikte yazılması, kayıp güncelleme ve optimistic locking.
2. **Eclipse Foundation — [Jakarta Transactions 2.0, bölüm 3.7][2].** `REQUIRED` ve istisna türüne göre geri alma.
3. **Eclipse Foundation — [Jakarta Persistence 3.2][3].** Optimistic locking, `@Version`, `LockModeType.OPTIMISTIC_FORCE_INCREMENT`.
4. **Software Lecture Labs — [Bölüm 13: HTTP Boundary, DTO, Validasyon ve Hata Sözleşmeleri][4].**
   `ETag` / `If-Match` ile kayıp güncellemenin önlenmesi (bölüm soruları).

[1]: https://github.com/software-lecture-labs/study-notes/blob/11579c0e428956babe6bccd8d37ce73084986e8e/java-chapter-12.md
[2]: https://jakarta.ee/specifications/transactions/2.0/jakarta-transactions-spec-2.0.html
[3]: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2
[4]: https://github.com/software-lecture-labs/study-notes/blob/11579c0e428956babe6bccd8d37ce73084986e8e/java-chapter-13.md
