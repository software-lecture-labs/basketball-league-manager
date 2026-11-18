---
title: ADR-0002-Jakarta EE 11 Web Profile ve Payara Micro Seçimi
status: Kabul Edildi
date: 2026-11-18
author: Mustafa Budak
deciders: Ömer Ünlü, Osman Balcı, Mustafa Budak
---

# ADR-0002: Jakarta EE 11 Web Profile ve Payara Micro Seçimi

## Bağlam

ADR-0001'de uygulamanın mimarisi **Katmanlı Monolit** olarak belirlenmiş ve uygulamanın tek bir WAR (Web Application Archive)
paketi halinde bir uygulama sunucusu üzerinde çalıştırılacağı kararlaştırılmıştır. Bu ADR, söz konusu tek dağıtım biriminin
hangi teknoloji yığınına (standart spesifikasyon) dayanacağını, hangi çalışma zamanı sunucusunda (runtime) çalışacağını ve
bu kararın kurumsal yazılım geliştirme prensipleriyle gerekçesini tanımlar.

Proje, Doğuş Üniversitesi *Java Tabanlı Kurumsal Yazılım Geliştirme* dersi kapsamında 3 kişilik tek bir ekip tarafından,
kısıtlı dönem takvimi içinde ve ücretli bulut altyapısı/lisans bütçesi olmadan yürütülmektedir. Uygulama; lig yönetimi,
fikstür oluşturma, skor kaydı, transferler ve puan durumu hesaplama gibi iş süreçlerini yönetmektedir.

Bu süreçlerin gerektirdiği teknik ihtiyaçlar şunlardır:
1. **Veri Bütünlüğü ve Transaction Yönetimi:** Maç onaylama, puan durumu güncelleme ve transfer işlemleri birden fazla
   tabloyu ve kaydı aynı anda etkiler. Bu işlemlerin ACID standartlarında, bildirimsel (declarative) transaction
   yönetimiyle (`@Transactional`) korunması gerekir.
2. **Standart REST API Desteği:** İstemci tarafındaki arayüzün (HTML/CSS/JS) sunucuyla tutarlı ve standart bir biçimde
   haberleşebilmesi için JAX-RS / Jakarta REST standartlarına ihtiyaç vardır.
3. **Kalıcılık ve Doğrulama:** Veritabanı etkileşimleri için güçlü bir ORM (JPA) ve gelen isteklerin sınırda
   doğrulanması için Bean Validation standardı gereklidir.
4. **Bağımsızlık ve Test Edilebilirlik:** Saf iş kurallarının (örneğin lig fikstürünü üreten `FixtureGenerator`)
   sunucu ve veritabanı altyapısından bağımsız olarak saniyeler içinde birim testlerle sınanabilmesi gerekir.
5. **Düşük İşletim Maliyeti ve Hızlı Geliştirme Döngüsü:** Ekip üyelerinin kişisel bilgisayarlarında karmaşık sunucu
   kurulumlarıyla (domain, node, admin console yapılandırması vb.) vakit kaybetmemesi ve CI ortamında (GitHub Actions)
   derleme/test sürelerinin kısa kalması esastır.

---

## Karar

**Uygulama geliştirme standardı olarak Jakarta EE 11 Web Profile, çalıştırma ortamı (runtime) olarak Payara Micro 7.x ve temel dil sürümü olarak Java 21 LTS seçilmiştir.**

### 1. Neden Jakarta EE 11 Web Profile?

Jakarta EE 11, kurumsal Java geliştirme dünyasında Eclipse Foundation tarafından yönetilen açık ve tarafsız endüstri
standardıdır. Bu projede Full Profile yerine **Web Profile** spesifikasyon kümesi seçilmiştir.

* **Odaklanmış ve Modern Spesifikasyon Kümesi:** Web Profile; Full Profile içinde yer alan ve modern hafif web
  uygulamalarında gereksinim duyulmayan eski teknolojileri (SOAP/JAX-WS, JMS, RMI/IIOP, EJB Entity Beans vb.) barındırmaz.
  Projemizin ihtiyaç duyduğu çekirdek bileşenlerin tamamını standart olarak sunar:
  - **CDI 4.1 (Jakarta Contexts and Dependency Injection):** Gevşek bağlı, tip güvenli bileşen yönetimi ve yaşam döngüsü.
  - **Jakarta REST 4.0 (JAX-RS):** Anotasyon tabanlı RESTful API geliştirme (`@Path`, `@GET`, `@POST`, `ExceptionMapper`).
  - **Jakarta Persistence 3.2 (JPA):** Standart nesne-ilişkisel eşleme (`@Entity`, `@Table`) ve JTA entegrasyonu.
  - **Jakarta Validation 3.1 (Bean Validation):** Sınırda girdi doğrulama (`@NotNull`, `@Size`, `@Min`).
  - **Jakarta Transactions 2.0 (JTA):** Bildirimsel işlem sınırları (`@Transactional`).
  - **Jakarta JSON Processing 2.1 & JSON Binding 3.0:** RFC 9457 Problem Details ve API modelleri için JSON serileştirme.
* **Java 21 LTS Uyumu:** Jakarta EE 11, taban Java sürümü olarak Java 21 LTS'i zorunlu kılar. Bu sayede Java'nın
  modern dil özellikleri (Records, Pattern Matching for switch, Sealed Classes, Sequenced Collections) kurumsal
  katmanlarda ve DTO modellerinde doğal olarak kullanılabilir.
* **İnce Dağıtım Birimi (Thin WAR):** `pom.xml` dosyasında sadece `jakarta.jakartaee-web-api` (11.0.0) bağımlılığı
  `provided` kapsamıyla (scope) eklenir. Framework kodları WAR içine gömülmez. Sonuç olarak üretilen `basketball-league.war`
  dosyası 1 MB'ın altındadır; derleme saniyeler içinde tamamlanır ve ağ/disk maliyeti minimuma iner.
* **Vendor Lock-in Riskini Önleme:** Kod, Spring Boot gibi tescilli bir çatının özel sınıflarına (`org.springframework.*`)
  değil, resmi standart arayüzlere (`jakarta.*`) bağımlıdır. İleride çalışma ortamı WildFly, Open Liberty veya TomEE
  olarak değiştirilmek istense dahi uygulama kodu değişmeden çalışmaya devam eder.

### 2. Neden Payara Micro?

Payara Micro, GlassFish'in kurumsal ve kararlı çekirdeğinden türetilmiş, mikro-hizmet ve konteyner çağı için optimize
edilmiş hafif (lightweight) ve gömülebilir bir Jakarta EE Web Profile çalışma zamanıdır.

* **Kurulumsuz ve Kendi Kendini Barındıran Yapı:** Geleneksel GlassFish veya WildFly gibi sistem seviyesinde sunucu
  kurulumu, servis tanımlaması veya yönetim konsolu gerektirmez. Tek bir çalıştırılabilir JAR (`payara-micro.jar`)
  üzerinden tek satırlık bir terminal komutuyla WAR ayağa kaldırılır.
* **`@DataSourceDefinition` ve Çevre Değişkeni Desteği:** Payara Micro, `DataSourceConfig.java` içinde tanımlanan
  standard `@DataSourceDefinition` anotasyonunda `${ENV=DB_URL}`, `${ENV=DB_USER}` ve `${ENV=DB_PASSWORD}` sözdizimini
  yerel olarak çözümler. Bu sayede veritabanı parolaları ve bağlantı adresleri kaynak koda gömülmez; Twelve-Factor App
  metodolojisine uygun olarak işletim sistemi çevre değişkenlerinden (environment variables) güvenle okunur.
* **Kutudan Çıkan JTA ve Bağlantı Havuzu:** Tomcat gibi yalın Servlet konteynerlerinde transaction yönetimi ve
  bağlantı havuzu için HikariCP, Bitronix, Atomikos gibi harici kütüphaneleri WAR içine ekleyip elle konfigüre etmek
  gerekirken; Payara Micro kurumsal düzeyde JTA motoru ve JDBC bağlantı havuzunu hazır sunar.
* **Düşük Bellek Kullanımı ve Hızlı Başlatma:** Geleneksel sunucuların 1–2 GB RAM ihtiyacı ve dakikaları bulan açılış
  süresine karşılık, Payara Micro 200–300 MB bellek tüketimiyle 3–5 saniye içinde dağıtımı tamamlar. Ekip üyelerinin
  geliştirme makinelerinde ve CI (GitHub Actions) hatlarında hızlı çalışır.

---

## Değerlendirilen Alternatifler

Aşağıdaki karşılaştırma, dönem projesinin ölçeği, ekibin ders hedefleri ve kurumsal yazılım standartları gözetilerek
yapılmıştır:

| Alternatif | Güçlü Yönleri | Neden Seçilmedi? |
| :--- | :--- | :--- |
| **Spring Boot 3.x** | Çok geniş topluluk, zengin kütüphane ekosistemi, popüler hazır entegrasyonlar. | Dersin temel amacı olan açık Jakarta EE standartlarını değil, Spring'in tescilli (proprietary) çatısını zorlar. Kod Spring sınıflarına sıkı bağlanır; "Fat JAR" modeli yüzlerce megabaytlık bağımlılığı taşır ve `pom.xml` karmaşıklığını artırır. |
| **Quarkus / Micronaut** | Süper hızlı açılış süresi, GraalVM native imaj desteği, çok düşük bellek. | Tam bir Jakarta EE Web Profile sunucusu değildir. Derleme anı kod zenginleştirmesi (build-time augmentation) kullanır; bazı standart CDI/JPA dinamik davranışları standart dışı kısıtlamalara tabidir. Akademik ders müfredatıyla birebir örtüşmez. |
| **Apache Tomcat / Jetty (Yalın Servlet)** | Çok hafif, sadece HTTP ve Servlet motoru barındırır. | Yalnızca Servlet konteyneridir. CDI, JAX-RS, JPA, Bean Validation ve JTA sunmaz. Bu yetenekleri kazandırmak için Jersey, Weld/OpenWebBeans, Hibernate ve JTA yöneticisinin tek tek WAR içine eklenmesi gerekir ("kendi sunucunu kendin yap" maliyeti ve uyumsuzluk riski). |
| **WildFly / GlassFish (Full Server)** | Eksiksiz yönetim konsolları, geniş kurumsal kümeleme yetenekleri. | Hantal ve ağırdır. Geliştiricilerin işletim sistemine sunucu kurmasını, port çakışmalarını yönetmesini ve XML konfigürasyonlarıyla uğraşmasını gerektirir. Küçük bir ekip için gereksiz bilişsel ve işletimsel yük oluşturur. |

---

## Kararın Sonuçları

### Olumlu Sonuçlar (Kazançlar)
1. **Temiz ve Yalın Bağımlılık Ağacı:** `pom.xml` dosyasında yalnızca API arayüzü (`jakarta.jakartaee-web-api`) ve
   test kütüphaneleri (JUnit 5, AssertJ, Mockito, ArchUnit) bulunur. Üretim bağımlılıklarında sürüm çakışması (jar hell)
   yaşanmaz.
2. **Geliştirici Bağımsızlığı:** Kod herhangi bir işletim sisteminde (Windows, macOS, Linux) ve CI ortamında sadece
   Java 21 ve Maven ile derlenir; WAR dosyası taşınabilirdir.
3. **Mimarinin Korunması:** ADR-0001 gereğince `domain` katmanı sıfır framework bağımlılığıyla korunurken
   (`DOMAIN_IS_FRAMEWORK_FREE` kuralı), altyapı servisleri (`service`, `resource`, `repository`) standart anotasyonlarla
   güvenle geliştirilir.
4. **Hızlı CI/CD Hatları:** GitHub Actions üzerinde saniyeler içinde derleme, ArchUnit mimari doğrulaması ve birim testler
   koşulabilir.

### Olumsuzluklar ve Kısıtlar (Trade-offs & Mitigations)
1. **Ortam Değişkenleri Yönetimi:** Payara Micro `.env` dosyasını otomatik olarak ayrıştırmaz.
   * *Azaltma:* Değişkenlerin kabukta (PowerShell / Bash) nasıl yükleneceği `README.md` dosyasında belgelenmiştir. Eksik
     değişken durumunda uygulama güvenli bir şekilde ayağa kalkmayıp 404 dönerek veri tabanına tanımsız erişimi engeller.
2. **Topluluk Kaynak Yoğunluğu:** İnternetteki genel forumlarda Spring Boot örnekleri Jakarta EE 11'e kıyasla daha
   fazladır.
   * *Azaltma:* Eclipse Foundation'ın resmi şartnameleri, Java EE / Jakarta EE dokümantasyonu ve dersin referans
     uygulaması (`farm-java-lab-northwind`, ADR-0002) kılavuz olarak kullanılır.

---

## Doğrulama Stratejisi

Bu kararın koddaki yansımaları ve kuralları aşağıdaki yöntemlerle doğrulanır:

1. **ArchUnit Mimarisi Testleri (`ArchitectureTest.java`):**
   - `DOMAIN_IS_FRAMEWORK_FREE`: `com.basketballleague.domain..` paketi altındaki hiçbir sınıfın `jakarta..`, `javax..`
     veya `java.sql..` paketlerine bağımlı olamayacağı derleme aşamasında doğrulanır.
   - `TRANSACTIONAL_ONLY_IN_SERVICE`: `@Transactional` anotasyonunun yalnızca Service katmanında yer alabileceği
     garanti edilir.
   - `NO_FIELD_INJECTION`: CDI bağımlılık enjeksiyonunun alan üzerinden değil, yapıcı metot (constructor injection)
     üzerinden yapılması zorunlu tutulur.
2. **Dağıtım ve Sağlık Kontrolü:**
   - Derlenen `target/basketball-league.war` paketinin Payara Micro üzerinde dağıtımı sonrasında `GET /api/ping`
     çağrısının 200 OK ve geçerli `PingResponse` modeli döndürdüğü doğrulanır.

---

## Kaynaklar

1. **Eclipse Foundation — [Jakarta EE 11 Platform Specification][1].**
2. **Eclipse Foundation — [Jakarta Contexts and Dependency Injection (CDI) 4.1][2].**
3. **Eclipse Foundation — [Jakarta RESTful Web Services 4.0][3].**
4. **Eclipse Foundation — [Jakarta Persistence 3.2][4].**
5. **Payara Services — [Payara Micro Documentation][5].**
6. **ADR-0001: Katmanlı Monolit Mimari.**
7. **ADR-0006: Hata Yönetimi ve Loglama Politikası.**

[1]: https://jakarta.ee/specifications/platform/11/
[2]: https://jakarta.ee/specifications/cdi/4.1/
[3]: https://jakarta.ee/specifications/restful-ws/4.0/
[4]: https://jakarta.ee/specifications/persistence/3.2/
[5]: https://docs.payara.fish/community/docs/documentation/payara-micro/
