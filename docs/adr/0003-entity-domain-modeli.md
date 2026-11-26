---
title: ADR-0003-Entity'lerin Domain Nesnesi Olarak Kullanılması ve Durum Makineleri
status: Kabul Edildi
date: 2026-11-24
author: Ömer Ünlü
deciders: Ömer Ünlü, Osman Balcı, Mustafa Budak
---

# ADR-0003: Entity'lerin Domain Nesnesi Olarak Kullanılması ve Durum Makineleri

## Bağlam

ADR-0001'e göre entity veriyi ve o veriyi değiştiren iş davranışını birlikte taşır. Entity eşlemeleri, tablo
alanları ve kimlik tipi ise ayrı bir çalışmaya bırakılmıştır. `Team` ve `Player` bu yaklaşımla yazıldı: public
setter yoktur, durum `rename()` ve `transferTo()` gibi metotlarla değişir.

Dilim 2 ile durumu olan ilk nesneler geliyor:

- **Sezon:** Takımlar eklenir. Fikstür üretilince sezon başlar ve takım listesi artık değişmemelidir.
- **Maç:** `SCHEDULED → LIVE → FINISHED → CONFIRMED`. Onaylanmış maçın skoru değişmemelidir (ADR-0001, maç onayı).

Cevaplanması gereken sorular:

- İş kuralları nerede durur: entity'de mi, servislerde mi, ayrı bir domain modelinde mi?
- Durumlar ve geçişler nasıl temsil edilir ve veritabanında nasıl saklanır?
- Sezon, takım ve maç birbirine nesne olarak mı, kimlikle mi bağlanır?

Dersin örneğindeki ADR-0004, domain sınıflarını Jakarta tiplerinden tamamen ayırır. JPA kullanıldığında da ayrı
bir persistence modeli ve mapper gerekeceğini not eder. [2] Bu projede ADR-0001 farklı bir sınır çizmiştir:
framework'ten bağımsız kalan paket `domain`'dir, `entity` paketinde JPA bağımlılığı kabul edilir. Bu belge o
sınırın sonuçlarını netleştirir.

## Karar

**JPA entity'lerini domain nesnesi olarak kullanmaya devam edeceğiz. Durumları `domain` paketindeki enum'larla
temsil edecek, geçişleri entity'nin niyet bildiren metotlarından yapacağız.**

### Kurallar entity'de, saf kurallar `domain`'de

- Entity'de public setter yoktur. JPA için `protected` boş constructor bulunur. Public constructor zorunlu
  alanların hepsini doğrular.
- Durum yalnızca iş dilindeki metotlarla değişir: `Season.addTeam()`, `Season.activate()`, `Game.start()`.
  Dilim 3'te `Game.recordQuarter()`, `finish()` ve `confirm()` eklenir.
- Kural ihlalinde `DomainException` alt sınıfı fırlatılır. HTTP karşılığı 409'dur (ADR-0006).
- Veritabanına bakmayı gerektiren kurallar (aynı adda sezon, var olmayan takım) servistedir. Veritabanı kısıtı
  eşzamanlı isteklere karşı son güvencedir.
- Durum enum'ları ve geçiş tabloları JDK dışında bağımlılığı olmayan `domain` paketindedir. Böylece geçişler
  veritabanı olmadan test edilir ve geçiş hatası (`InvalidGameStatusTransitionException`) durum tiplerini
  `entity` paketine bağımlı olmadan taşıyabilir.

### Durum makineleri

Geçerli geçişler enum'un içinde, tek bir `switch` ifadesinde tanımlıdır. Dersin Bölüm 04'teki `OrderStatus`
örneğiyle aynı yaklaşımdır. [1] `default` dalı yoktur; yeni bir durum eklenip geçişleri yazılmazsa kod
derlenmez.

| Maç durumu  | Geçebileceği durum |
| ----------- | ------------------ |
| `SCHEDULED` | `LIVE`             |
| `LIVE`      | `FINISHED`         |
| `FINISHED`  | `CONFIRMED`        |
| `CONFIRMED` | yok (son durum)    |

| Sezon durumu | Geçebileceği durum | Takım eklenebilir mi? |
| ------------ | ------------------ | --------------------- |
| `DRAFT`      | `ACTIVE`           | Evet                  |
| `ACTIVE`     | `FINISHED`         | Hayır                 |
| `FINISHED`   | yok (son durum)    | Hayır                 |

- Geçersiz maç geçişi (ör. `SCHEDULED → CONFIRMED`) `INVALID_GAME_STATUS_TRANSITION` ile reddedilir.
- Fikstürü üretmek sezonu `ACTIVE` yapar. Bu yüzden fikstürü ikinci kez üretmek ve başlamış sezona takım eklemek
  aynı kuralla, `SEASON_ALREADY_STARTED` ile reddedilir.
- Bir sezon en az 2 takımla başlar. Bir sezonda en fazla 20 takım olur (`Season.MAX_TEAMS`).

### Durumların saklanması

Durumlar `varchar` sütunda, enum adıyla (`@Enumerated(EnumType.STRING)`) saklanır. Sütundaki `CHECK` kısıtı
izin verilen değerleri sınırlar. Bu, V1'deki `players.position` sütunuyla aynı yöntemdir.

### Nesne sınırları

| Referans          | Nasıl kurulur?                                                       | Neden?                                                                                                     |
| ----------------- | -------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| Season → takımlar | `season_teams` tablosunda takım kimlikleri (`Set<Long>`)             | Üyelik sezonun parçasıdır ve sezonun kurallarıyla (yalnızca `DRAFT`, en fazla 20) değişir. Takım nesnesi yüklenmez. |
| Game → Season     | `season_id` kimliği                                                  | Maçın kendi yaşam döngüsü var. Sezonu okumak 380 maçı yüklememeli.                                          |
| Game → takımlar   | `home_team_id`, `away_team_id` kimlikleri                            | Fikstür üreticisi kimliklerle çalışır (`Pairing`). Takım adını gerektiren okumalar tek sorguyla yapılır.     |
| Season → maçlar   | tutulmaz; sorgudur (`GameRepository`)                                | `season.getGames().add(...)` gibi bir yol maç kurallarını atlatır.                                          |

Bu tercih Bölüm 04'teki "aggregate'ler arası referans kimlikle kurulur" ilkesini izler. [1] `Player → Team`
bağlantısı mevcut koddaki gibi nesne referansı olarak kalır, çünkü oyuncu takımının kadrosunun parçasıdır.

Maç, canlı skor girişinde eşzamanlı güncellemeleri ayırt etmek için `@Version` sütunu taşır. Sürümün nasıl
kullanılacağı ADR-0004'te belirlenmiştir.

## Değerlendirilen Alternatifler

| **Alternatif**                                                  | **Neden seçilmedi?**                                                                                                                                                                         |
| --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| JPA'sız domain modeli + ayrı persistence modeli ve mapper [2]   | Her nesne iki kez yazılır ve aradaki dönüşüm sürekli bakım ister. ADR-0001 entity'de JPA bağımlılığını kabul etti. Saf algoritmalar (fikstür, durum geçişleri) zaten `domain`'de, JPA'sız test ediliyor. |
| Anemik entity, kurallar serviste                                | Kurallar servislere dağılır ve tekrarlanır. Entity bir setter çağrısıyla geçersiz duruma sokulabilir.                                                                                         |
| Her durum için ayrı sınıf (State tasarım kalıbı)                | 4 durum ve durum başına tek geçiş var. Enum içindeki tablo aynı güvenceyi daha az kodla verir. Bölüm 04 de kalıbı durum sayısı arttığında önerir. [1]                                         |
| Durumu `String` sabitlerle veya `boolean` bayraklarla tutmak    | `live && confirmed` gibi anlamsız birleşimler mümkün olur ve derleyici bunları yakalamaz.                                                                                                       |
| Durumlar için referans tablosu (`game_status`)                  | Yalnızca PostgreSQL kullanıyoruz ve değerler kodla birlikte değişiyor. `CHECK` yeterli. Referans tablo, her okumada ek bir join ve ayrı seed verisi getirir.                                   |
| PostgreSQL `ENUM` tipi                                          | Değer eklemek `ALTER TYPE` gerektirir ve JPA tarafında özel dönüştürücü ister. `varchar` + `CHECK` daha sade.                                                                                 |
| `EnumType.ORDINAL`                                              | Enum'daki sıra değişirse saklanan sayıların anlamı sessizce bozulur.                                                                                                                         |
| Maç ve sezonda `@ManyToOne` nesne referansları                  | Bir maçı yüklemek sezonu ve iki takımı da getirmeye yatkındır. Fikstür üreticisinin kimlik listesiyle ayrıca dönüşüm gerekir.                                                                  |

## Sonuçlar

### Olumlu

- Her kural tek yerde durur. Entity ve durum tabloları veritabanı olmadan birim test edilir.
- Geçişler tek bir tabloda okunur. Yeni durum eklenince eksik geçiş derlemede ortaya çıkar.
- Fikstür üreticisinin çıktısı (`Pairing`) doğrudan `Game`'e dönüşür.
- Sezon okunurken maçlar, maç okunurken sezon yüklenmez.

### Olumsuz

- Entity'ler JPA anotasyonları taşır. Kalıcı alanlar `final` olamaz (JPA §2.1). [3]
- Birim testleri eşleme hatalarını göremez. Takım entity'sinde bu yaşandı ve Payara'da ortaya çıktı.
- Kimlikle referans, takım adı gereken yanıtlarda ayrı bir sorgu ister. Bu sorgu döngüde değil, tek seferde yapılır.

### Risk

| Risk                                            | Nasıl ele alınacak?                                                                                            |
| ----------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| Entity eşlemesinin migration ile ayrışması       | Eşleme PR'larında sütun adları migration ile karşılaştırılır. Testcontainers geldiğinde (Dilim 3) eşleme entegrasyon testiyle doğrulanır. |
| Bitmiş maçın hatalı skorunu düzeltme ihtiyacı   | Tabloda `FINISHED → LIVE` yoktur. ADR-0004 bu geçişi `reopen()` ile ekler.                                       |
| Aynı anda iki fikstür üretme isteği             | Fikstür üretimi ve takım ekleme sezon satırını kilitleyerek çalışır. Ayrıntı fikstür API'sinin PR'ındadır.        |

## Doğrulama

| **Kural**                                              | **Kim korur?**  | **ArchUnit kuralı / Test**                |
| ------------------------------------------------------ | --------------- | ----------------------------------------- |
| Entity'lerde public setter yok                         | ArchUnit        | `ENTITIES_HAVE_NO_PUBLIC_SETTERS`         |
| Entity alanları `final` değil                          | ArchUnit        | `ENTITY_FIELDS_ARE_NOT_FINAL`             |
| Durum tabloları framework'ten bağımsız                 | ArchUnit        | `DOMAIN_IS_FRAMEWORK_FREE`                |
| Kural ihlalleri tek tabandan türer                     | ArchUnit        | `DOMAIN_EXCEPTIONS_SHARE_ONE_BASE`        |
| Her durum çifti için geçişe izin verilir veya verilmez | Birim testi     | `GameStatusTest`, `SeasonStatusTest`      |
| Sezon ve maç kuralları                                 | Birim testi     | `SeasonTest`, `GameTest`                  |
| Eşleme ile şemanın uyumu                               | Entegrasyon     | Dilim 3, Testcontainers                   |

## Kaynaklar

Erişim tarihi: **24 Kasım 2026**.

1. **Software Lecture Labs — [Bölüm 04: Aggregate Sınırları, State Machine ve Domain Exception'lar][1].**
   Aggregate'ler arası kimlikle referans, enum ile durum makinesi.
2. **Software Lecture Labs — [ADR-0004: Domain Modeli ve Paylaşılan Çekirdek Kuralları][2].**
   Framework'ten bağımsız domain ve JPA ile ayrı persistence modeli notu.
3. **Eclipse Foundation — [Jakarta Persistence 3.2][3].** Kalıcı alanlar (§2.1), `@Enumerated`, `@Version`.

[1]: https://github.com/software-lecture-labs/study-notes/blob/11579c0e428956babe6bccd8d37ce73084986e8e/java-chapter-04.md
[2]: https://github.com/software-lecture-labs/study-notes/blob/11579c0e428956babe6bccd8d37ce73084986e8e/docs/adr/ADR-0004.md
[3]: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2
