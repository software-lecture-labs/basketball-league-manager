---
title: ADR-0006-Hata Yönetimi ve Loglama Politikası
status: Önerildi
date: 2026-11-13
author: Ömer Ünlü
deciders: Ömer Ünlü, Osman Balcı, Mustafa Budak
---

# ADR-0006: Hata Yönetimi ve Loglama Politikası

## Bağlam

ADR-0001, iş kurallarının HTTP'den bağımsız kalmasını ve hataların HTTP yanıtına yalnızca `exception` paketinde
çevrilmesini öngörür; hata yanıtının biçimini ise ayrı bir sözleşmeye bırakır. Bu belge o sözleşmedir.
İlk REST uç noktaları (takım ve oyuncu) yazılmadan önce karara bağlanması gerekir; aksi hâlde her uç nokta
kendi hata biçimini üretir.

Şu an hatalar için ortak bir davranış yoktur. Yakalanmayan her istisna Payara'nın varsayılan yanıtına,
çoğunlukla 500 durum koduna ve sunucuya özgü bir gövdeye dönüşür. Bu durum şu sorunları doğurur:

- İstemci hatası (eksik alan, bulunmayan takım, onaylanmış maçı yeniden onaylama) sunucu hatası gibi görünür.
- İstisna mesajı, SQL hatası veya kısıt adı istemciye ulaşabilir.
- Arayüz, hatanın türünü metin ayrıştırmadan anlayamaz.
- Aynı hata farklı katmanlarda tekrar tekrar loglanabilir veya hiç loglanmayabilir.

Kısıtlar: mevcut [DomainException] `RuntimeException` tabanından türer ve `domain` paketi yalnızca JDK kullanır.
Service katmanı HTTP yanıtı üretmez. Arayüz Türkçedir; mevcut domain hata mesajları İngilizcedir.

## Karar

**Tüm hata yanıtlarını, `exception` paketindeki JAX-RS `ExceptionMapper`'larıyla RFC 9457 Problem Details
biçiminde üreteceğiz.** [1] [2]

### Durum kodları ve log seviyeleri

| Durum | HTTP | `code` | Log seviyesi |
| --- | --- | --- | --- |
| Bean Validation ihlali (istek parametresi veya gövdesi) | 400 | `VALIDATION_FAILED` | DEBUG |
| Okunamayan istek gövdesi (bozuk veya boş JSON) | 400 | `MALFORMED_REQUEST` | DEBUG |
| Kayıt bulunamadı | 404 | ilgili kod, ör. `TEAM_NOT_FOUND` | DEBUG |
| JAX-RS'in ürettiği diğer istemci hataları (bilinmeyen yol, desteklenmeyen metot veya medya tipi) | kendi kodu korunur | `HTTP_<durum>`, ör. `HTTP_405` | DEBUG |
| Yanıt değerinin Bean Validation ihlali (sunucu hatası; Jakarta REST kuralı) [2] | 500 | `INTERNAL_ERROR` | ERROR, stack trace ile |
| İş kuralı ihlali (`DomainException`) | 409 | ilgili kod, ör. `INVALID_FIXTURE_INPUT` | INFO |
| Eşzamanlı güncelleme çakışması (optimistic lock) | 409 | `CONCURRENT_UPDATE` | INFO |
| Veri bütünlüğü ihlali (unique, foreign key) | 409 | `DATA_CONFLICT` | WARN |
| Beklenmeyen hata | 500 | `INTERNAL_ERROR` | ERROR, stack trace ile |

İş kuralı ihlallerinin tamamı 409 döner. 422 ayrımı yapılmaz (gerekçe alternatifler tablosunda).

### Yanıt gövdesi

`Content-Type: application/problem+json`. Alanlar:

| Alan | İçerik |
| --- | --- |
| `type` | Her zaman `about:blank`. |
| `title` | HTTP durum ifadesi (`Conflict`, `Not Found` …). RFC 9457, `about:blank` ile bunu önerir. [1] |
| `status` | HTTP durum kodu; yanıtın durum koduyla aynı. |
| `detail` | Kullanıcıya gösterilecek Türkçe açıklama; istisna mesajından üretilmez (aşağıda). |
| `code` | Makinenin okuyacağı sabit hata kodu (`UPPER_SNAKE_CASE`). Arayüz hata türünü buradan ayırt eder. |
| `errors` | Yalnızca 400 `VALIDATION_FAILED` yanıtında: `[{ "field": ..., "message": ... }]`. |

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Fikstür için birbirinden farklı en az iki takım gerekir.",
  "code": "INVALID_FIXTURE_INPUT"
}
```

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "İstekte geçersiz alanlar var.",
  "code": "VALIDATION_FAILED",
  "errors": [
    { "field": "name", "message": "boş bırakılamaz" },
    { "field": "players[1].jerseyNumber", "message": "99 veya daha küçük olmalıdır" }
  ]
}
```

`instance` alanı kullanılmaz. RFC 9457'de bu alan isteğin yolunu değil, hatanın kendisini tanımlar. [1]
Her hata için ayrı kimlik üretip saklamak bu projenin ihtiyacı değildir.

### Hata kodları ve açıklama metinleri

1. Domain'deki her hata türü, `domain` paketindeki bir hata kodu enum'undan bir değer taşır.
   Kod, hata nesnesi oluşturulurken verilir.
2. Kayıt bulunamadığında servis, `DomainException`'dan türeyen ayrı bir "bulunamadı" hatası fırlatır.
   JAX-RS en yakın üst türün eşleyicisini seçtiği için bu tür 404, diğer domain hataları 409 döner. [2]
3. `detail` metni `exception` paketindeki tek bir katalogdan, hata koduna göre alınır. Katalog enum üzerinde
   `switch` ifadesiyle yazılır; yeni bir kodun metni eksikse kod derlenmez.
4. İstisna mesajı (`getMessage()`) istemciye gönderilmez; yalnızca sunucu loguna yazılır. Mevcut İngilizce
   domain mesajları bu yüzden değiştirilmez.
5. Doğrulama mesajları sunucunun yerel ayarına bırakılmaz; Türkçe olarak proje içinde tanımlanır.
   `field`, istek gövdesindeki alan yoludur; Java metot ve parametre adları gönderilmez.
6. Veri bütünlüğü ihlalinde kısıt adı ve SQL mesajı istemciye gönderilmez. İş kuralı olarak bilinen çakışmalar
   (ör. takımda dolu forma numarası) servis tarafından önceden denetlenip kendi koduyla döner; veritabanı kısıtı
   eşzamanlı isteklere karşı son güvencedir.

Hata yönetimi kodunun ilk sürümü yalnızca mevcut hata türünün kodunu (`INVALID_FIXTURE_INPUT`) ekler.
Diğer kodlar ilgili özellikle birlikte gelir.

### Kim loglar?

- Hata yalnızca eşleyicide, bir kez ve tablodaki seviyede loglanır. Service ve resource katmanları hatayı
  loglayıp yeniden fırlatmaz; aynı hatanın birden fazla log satırı üretmesi böylece önlenir.
- Stack trace yalnızca 500 yanıtlarında yazılır. 4xx yanıtlarında tek satır yeterlidir.
- Loglama için SLF4J API ile `slf4j-jdk14` bağlayıcısı kullanılır; loglar Payara'nın `java.util.logging`
  altyapısına, yani `server.log` dosyasına düşer. Bağımlılıklar hata yönetimi kodunu ekleyen PR'da gelir.
- `System.out`, `System.err` ve `printStackTrace` kullanılmaz (mimari testlerle korunur).
- Log ve yanıt metinleri ortamdan bağımsızdır; "Docker'ı başlatın" gibi yerel geliştirme talimatı içermez.

### Sarılmış istisnalar

Optimistic lock veya kısıt ihlali commit sırasında oluşursa, eşleyiciye doğrudan değil, transaction yöneticisinin
veya `@Transactional` interceptor'ının istisnasına sarılmış olarak ulaşabilir. Hangi türle sarıldığı sunucuya
bağlıdır ve spesifikasyonda tek tip tanımlanmamıştır. Eşleyiciler bu nedenle istisnanın sebep (`cause`) zincirine
bakar. Payara'daki gerçek davranış, ilgili entegrasyon testleriyle (planlanan ADR-0005 kapsamı) doğrulanacaktır.

## Değerlendirilen Alternatifler

| **Alternatif** | **Neden seçilmedi?** |
| --- | --- |
| Serbest biçimli JSON (ör. `{ "error": "..." }`) | Her uç nokta farklı alanlar üretmeye yatkındır; standart bir biçim olmadığı için istemci hata türünü güvenilir şekilde ayırt edemez. |
| Her hatayı 500 (veya hepsini 400) döndürmek | İstemci hatası ile sunucu hatası ayrılmaz; izleme ve kullanıcıya yönlendirme imkânsızlaşır. |
| Resource metotlarında `try/catch` ile yanıt üretmek | Her uç noktada tekrar eder; unutulan bir `catch` 500 sızdırır. ADR-0001'e göre resource yalnızca servisi çağırır. |
| Service'in HTTP durum kodu veya `Response` döndürmesi | ADR-0001'deki "iş kuralı HTTP'den bağımsızdır" ilkesiyle çelişir. |
| İş kuralı ihlallerinde 422 | 422 içeriği anlaşılan ama işlenemeyen istek içindir. Kurallarımızın çoğu kaynağın mevcut durumuyla çakışmadır (ör. onaylanmış maç). Her kural için 409/422 ayrımı yapmak ek karar yükü getirir; tek kod tutarlı ve test edilebilir. |
| `type` için projeye özel, erişilebilir URI'ler | Her hata türü için yayınlanmış bir belge sayfası gerekir; bu kapsamda karşılığı yoktur. Aynı ayrımı `code` alanı sağlar. |
| `detail` olarak istisna mesajını göndermek | Teknik ayrıntı, SQL veya kısıt adı istemciye sızabilir; dili ve içeriği kontrol edilemez. |
| `instance` olarak istek yolunu göndermek | Yaygın bir kullanım olsa da RFC 9457'deki anlamı (hatanın kendisini tanımlama) karşılamaz; yol bilgisi istemcide zaten vardır. |
| Loglama için doğrudan `java.util.logging` veya `System.Logger` | Ek bağımlılık gerektirmez ve Payara'nın log altyapısına doğrudan yazar; teknik olarak yeterlidir. SLF4J ise Java kütüphanelerinde yaygın kullanılan bir loglama arayüzüdür; `slf4j-jdk14` ile aynı Payara altyapısına yazdığı için bu avantajdan bir şey kaybedilmez. Dersin örnek projesindeki (farm-java-lab-northwind, ADR-0002) karar da budur; ekip tek bir örneği izler. Bedeli iki küçük bağımlılıktır. |

## Sonuçlar

### Olumlu

- Bütün hatalar tek biçimde döner; arayüz `code` alanına göre davranır, metin ayrıştırmaz.
- İstemci hataları 4xx, sunucu hataları 5xx olarak ayrışır.
- Teknik ayrıntı istemciye sızmaz; hata ayrıntısı sunucu logunda kalır.
- Hata başına tek log satırı; seviyeler politika tablosundan okunabilir.
- Domain HTTP'den bağımsız kalır; durum kodu kararı tek pakette toplanır.

### Olumsuz

- Her yeni hata kodu için katalogda bir Türkçe metin yazmak gerekir (eksik metin derlemeyi durdurur).
- Domain hata türleri bir kod taşımak zorundadır; `DomainException` yapısı bu nedenle değişir.
- `title`, RFC önerisi gereği İngilizce HTTP ifadesidir; arayüz kullanıcıya `detail` metnini gösterir.
- JAX-RS kuralı gereği sorgu parametresi dönüştürülemezse (ör. `?page=abc`) çerçeve 404 üretir ve bu kod korunur;
  bu tür parametreler gerektiğinde ilgili uç noktada ayrıca doğrulanır. [2]

### Risk

| Risk | Nasıl ele alınacak? |
| --- | --- |
| Genel 500 eşleyicisinin JAX-RS'in kendi 404/405 hatalarını yutması | Bu hatalar için ayrı eşleyici durum kodunu korur; bilinmeyen yol testle denetlenir. |
| Bozuk JSON'un Payara'da beklenmeyen bir istisna türüyle gelmesi | Payara 7.2026.9'daki Jersey, okunamayan JSON'u `JsonbException` sebepli `ProcessingException`, boş gövdeyi `BadRequestException` olarak bildirir; ikisi için ayrı eşleyici vardır. Davranış ayrıca `curl` ile denenir. |
| Sarılmış transaction istisnalarının 500'e düşmesi | Sebep zinciri taranır; entegrasyon testiyle doğrulanır. |
| Katalog metinlerinin kodla uyumsuzlaşması | Metinler kodlarla aynı `switch` ifadesinde durur. |

## Doğrulama

| **Kural** | **Kim korur?** | **Test** |
| --- | --- | --- |
| Her eşleyici doğru durum kodu, `application/problem+json` ve beklenen `code` üretir | Birim testleri | Her eşleyici için ayrı test |
| `detail`, istisna mesajını veya SQL ayrıntısını içermez | Birim testleri | Mesajı bilinen bir istisnayla yanıt karşılaştırması |
| JAX-RS'in kendi hataları durum kodunu korur | Birim testi + Payara üzerinde `curl` | Bilinmeyen yol 404, desteklenmeyen metot 405 |
| Bean Validation ihlali alan listesiyle 400 döner | Birim testi + Payara üzerinde `curl` | Eksik alanlı istek |
| Optimistic lock ve kısıt ihlali 409 döner | Entegrasyon testleri (Testcontainers) | Planlanan ADR-0005 ve ilgili özellik çalışmaları |
| Eşleyiciler yalnızca `exception` paketinde; `exception` yalnızca `domain` ve `dto`'ya bağımlı | ArchUnit | Mimari testlerdeki katman kuralı |
| Standart akışlar kullanılmaz | ArchUnit | Standart akış kuralı |

## Kaynaklar

Erişim tarihi: **13 Kasım 2026**.

1. **IETF — [RFC 9457: Problem Details for HTTP APIs][1].** Alanların anlamı (bölüm 3.1), `about:blank` ile
   `title` kullanımı (bölüm 4.2.1), güvenlik değerlendirmesi (bölüm 5).
2. **Eclipse Foundation — [Jakarta RESTful Web Services 4.0][2].** `ExceptionMapper` seçimi ve parametre
   dönüştürme hatalarında üretilen durum kodları.
3. **Software Lecture Labs — [farm-java-lab-northwind, ADR-0002][3].** SLF4J + `slf4j-jdk14` loglama kararı ve
   `slf4j-simple`'ın Payara log altyapısını atlaması.

[1]: https://www.rfc-editor.org/rfc/rfc9457
[2]: https://jakarta.ee/specifications/restful-ws/4.0/jakarta-restful-ws-spec-4.0
[3]: https://github.com/software-lecture-labs/farm-java-lab-northwind/blob/main/docs/adr/0002-jakarta-web-profile.md
[DomainException]: ../../src/main/java/com/basketballleague/domain/DomainException.java
