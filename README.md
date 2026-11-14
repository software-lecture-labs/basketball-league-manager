#  Basketball League Manager

EuroLeague takımları ve oyuncularından esinlenen demo verileri kullanılarak geliştirilen, basketbol ligi yönetim uygulaması.

Basketball League Manager; takım ve oyuncu yönetimi, otomatik fikstür oluşturma, maç sonuçlarının kaydedilmesi, puan durumunun hesaplanması, oyuncu transferleri ve istatistik liderlerinin takip edilmesini sağlayan web tabanlı bir uygulamadır.

> *Önemli:* Projede kullanılan takım ve oyuncu isimleri gerçek EuroLeague takımlarına ve oyuncularına aittir. Ancak uygulamadaki maç skorları ve istatistikler demo verileridir. Resmî EuroLeague sonuçlarını veya istatistiklerini temsil etmez. Projede EuroLeague'e ait resmî logolar kullanılmamaktadır.

---

## 1. Proje Tanımı

Basketball League Manager, basketbol ligi organizasyonunu dijital ortamda yönetmek amacıyla geliştirilen bir web uygulamasıdır.

Uygulama, EuroLeague'den alınan takım ve oyuncu isimleriyle oluşturulan demo veriler üzerinden çalışır. Fikstür oluşturma, maç sonuçlarını kaydetme ve puan durumu hesaplama işlemlerini otomatikleştirir.

Projenin temel amacı, basketbol ligi yönetim süreçlerini merkezi bir sistem üzerinden gerçekleştirmek ve verilerin tutarlı şekilde saklanmasını sağlamaktır.

## 2. Proje Özellikleri

### 2.1. Takım Yönetimi
- Takım oluşturma ve takım bilgilerini yönetme.
- Takım bilgilerini görüntüleme ve güncelleme.
- Takım kadrolarını takip etme.

### 2.2. Oyuncu Yönetimi
- Oyuncu kayıtlarını yönetme.
- Oyuncunun ad, soyad, forma numarası ve pozisyon bilgilerini saklama.
- Oyuncuları takımlarla ilişkilendirme.
- Oyuncu bilgilerini güncelleme.

### 2.3. Otomatik Fikstür Oluşturma
- Çift devreli lig fikstürü oluşturma.
- Her takımın diğer takımlarla iki kez karşılaşmasını sağlama.
- İç saha ve deplasman karşılaşmalarını belirleme.
- Takım sayısına göre dinamik fikstür oluşturma.

Fikstür sistemi sabit bir takım sayısına bağlı değildir.

### 2.4. Maç Yönetimi
- Fikstür üzerinden oluşturulan maçları yönetme.
- Dört çeyrek için ayrı skor girişi yapma.
- Uzatma skorlarını kaydetme.
- Toplam maç skorunu otomatik hesaplama.
- Maç sonucunu onaylama.
- Onaylanan sonuçları puan durumuna yansıtma.

*Maç skoru hesaplama:*

Maçın toplam skoru, dört çeyrekte ve varsa uzatma periyotlarında elde edilen sayıların toplamıdır.

### 2.5. Puan Durumu

Puan durumu, onaylanmış maç sonuçlarına göre otomatik hesaplanır.

*Puanlama sistemi:*

| Maç Sonucu | Kazanılan Puan |
|---|---:|
| Galibiyet | 2 |
| Mağlubiyet | 1 |

*Sıralama kriterleri:*

Takımlar aşağıdaki öncelik sırasına göre sıralanır:

1. Toplam puan.
2. İkili averaj.
3. Genel averaj.
4. Atılan toplam sayı.
5. Takım adı (alfabetik sıralama).

Bu sıralama zinciri projenin kendi lig kurallarıdır. Resmî EuroLeague puanlama sistemiyle aynı değildir.

### 2.6. Hafif Canlı Skor
- Devam eden maçların skorlarını görüntüleme.
- Çeyrek skorlarını takip etme.
- Maçın mevcut durumunu görüntüleme.

### 2.7. Oyuncu Transfer Yönetimi
- Oyuncuların takımlar arasındaki transferlerini yönetme.
- Oyuncunun takım bağlantısını güncelleme.
- Transfer kayıtlarını takip etme.

### 2.8. İstatistik Liderleri

Oyuncu istatistikleri maç verilerinden hesaplanır.

Projenin mevcut istatistik kapsamı yalnızca oyuncuların attığı sayılardır.

*PPG (Points Per Game):*

Oyuncunun maç başına attığı sayı ortalamasıdır.

Hesaplama:

PPG = Toplam Atılan Sayı / Oynanan Maç Sayısı

İstatistik liderleri, maç başına sayı ortalamasına göre belirlenir.

---

## 3. Lig Kuralları ve İşleyiş

Proje, EuroLeague organizasyonundaki takım ve oyuncu verilerini örnek alır. Ancak fikstür, puanlama ve sıralama işlemleri projenin kendi kurallarına göre gerçekleştirilir.

### 3.1. Lig Sistemi
- Çift devreli lig sistemi kullanılır.
- Her takım diğer takımlarla iki kez karşılaşır.
- Bir karşılaşma iç sahada, diğeri deplasmanda oynanır.
- Fikstür takım sayısına göre otomatik oluşturulur.

### 3.2. Maç Süresi ve Skor

Basketbol maçları dört çeyrek üzerinden takip edilir.

| Periyot | Normal Süre |
|---|---:|
| 1. Çeyrek | 10 dakika |
| 2. Çeyrek | 10 dakika |
| 3. Çeyrek | 10 dakika |
| 4. Çeyrek | 10 dakika |
| Uzatma | 5 dakika |

Normal süre sonunda skor eşitse uzatma oynanır. Eşitlik bozulana kadar uzatma periyotları devam eder.

### 3.3. Sayı Sistemi

| Atış Türü | Kazandırdığı Sayı |
|---|---:|
| Serbest atış | 1 |
| İki sayılık atış | 2 |
| Üç sayılık atış | 3 |

### 3.4. Puan Durumu Hesaplama

Puan durumu hesaplamasında yalnızca onaylanmış maçlar dikkate alınır.

Örnek:

| Takım | Oynanan | Galibiyet | Mağlubiyet | Puan |
|---|---:|---:|---:|---:|
| Takım A | 10 | 8 | 2 | 18 |
| Takım B | 10 | 7 | 3 | 17 |
| Takım C | 10 | 6 | 4 | 16 |

Puan eşitliği durumunda ikili averaj, genel averaj, atılan toplam sayı ve takım adı kriterleri uygulanır.

---

## 4. Kullanılan Teknolojiler

Proje, Java tabanlı katmanlı monolit mimarisi kullanılarak geliştirilmektedir.

| Teknoloji | Sürüm | Kullanım Alanı |
|---|---|---|
| Java | 21 | Ana programlama dili |
| Jakarta EE | 11 | Kurumsal uygulama altyapısı |
| Payara Micro | 7.2026.9 | Uygulama sunucusu |
| PostgreSQL | 18 | İlişkisel veritabanı |
| Maven | Maven Wrapper | Derleme ve bağımlılık yönetimi |
| JUnit | 5 | Test altyapısı |
| ArchUnit | 1.5.1 | Mimari kuralların test edilmesi |
| AssertJ | Proje bağımlılığı | Test doğrulamaları |
| Mockito | Proje bağımlılığı | Testlerde bağımlılıkların taklit edilmesi |
| JaCoCo | Proje bağımlılığı | Test kapsamı ölçümü |

### 4.1. Java 21
Uygulamanın temel programlama dilidir. İş mantığı, servisler ve veri işlemleri Java ile geliştirilir.

### 4.2. Jakarta EE 11
Sunucu tarafındaki uygulama bileşenlerinin ve REST API altyapısının geliştirilmesinde kullanılır.

### 4.3. Payara Micro 7.2026.9
Jakarta EE uygulamasının çalıştırılması ve WAR dosyasının dağıtılması için kullanılan uygulama sunucusudur.

### 4.4. PostgreSQL 18
Takım, oyuncu, fikstür, maç, skor, transfer ve istatistik verilerinin saklandığı ilişkisel veritabanıdır.

### 4.5. Maven Wrapper
Projenin derlenmesini ve bağımlılıkların yönetilmesini sağlar. Ayrı Maven kurulumu gerektirmez.

### 4.6. JUnit 5
Uygulamanın birim testlerinin gerçekleştirilmesinde kullanılır.

### 4.7. ArchUnit
Uygulamanın katmanlı mimari kurallarına uygunluğunu otomatik olarak test eder.

### 4.8. AssertJ ve Mockito
Testlerde doğrulama işlemleri ve bağımlılıkların taklit edilmesi için kullanılır.

### 4.9. JaCoCo
Testlerin kod kapsamını ölçmek için kullanılır.

### 4.10. Planlanan Teknolojiler

*Flyway:* Veritabanı şema değişikliklerinin sürümlendirilmesi ve migration işlemlerinin yönetilmesi için planlanmaktadır. Henüz mevcut proje bağımlılığı değildir.

*SLF4J:* ADR-0006 kapsamında loglama altyapısı için kararlaştırılmıştır. Ancak henüz pom.xml dosyasına eklenmemiştir.

---

## 5. Mimari Yapı

Proje, katmanlı monolit (Layered Monolith) mimarisi kullanılarak tasarlanmıştır.

Katmanlı monolit yaklaşımında uygulama tek bir dağıtılabilir uygulama olarak çalışır. Bununla birlikte kod yapısı sorumluluklarına göre farklı katmanlara ayrılır.

Bu yaklaşımın amaçları:

- Kodun düzenli ve anlaşılır olmasını sağlamak.
- İş mantığını diğer katmanlardan ayırmak.
- Katmanlar arasındaki bağımlılıkları kontrol etmek.
- Test edilebilirliği artırmak.
- Bakım ve geliştirme işlemlerini kolaylaştırmak.

Mimari kararlar docs/adr/ klasöründe bulunan Architecture Decision Records (ADR) belgelerinde açıklanmaktadır.

| ADR | Konu |
|---|---|
| ADR-0001 | Proje kapsamı ve temel kararlar |
| ADR-0006 | Loglama ve hata yönetimi |
| ADR-0008 | Katmanlı monolit mimarisi |

ADR belgeleri, mimari tercihlerin nedenlerini ve uygulama üzerindeki etkilerini açıklar.

---

## 6. Veritabanı ve Veri Yönetimi

Uygulama PostgreSQL 18 ilişkisel veritabanını kullanır.

Veritabanında aşağıdaki temel veri grupları yönetilir:

- Takımlar.
- Oyuncular.
- Fikstür ve maçlar.
- Çeyrek skorları.
- Oyuncu maç istatistikleri.
- Transfer kayıtları.

### 6.1. Puan Durumu Hesaplaması

Puan durumu, onaylanmış maç kayıtlarından hesaplanır.

Hesaplamada:

- Galibiyet ve mağlubiyet sayıları belirlenir.
- Galibiyete 2, mağlubiyete 1 puan verilir.
- Atılan ve yenilen toplam sayılar hesaplanır.
- Genel averaj bulunur.
- Eşitlik durumunda ikili averaj uygulanır.
- Belirlenen sıralama kriterleri kullanılır.

### 6.2. Oyuncu İstatistikleri

Oyuncu maç istatistikleri üzerinden toplam atılan sayılar hesaplanır.

Maç başına sayı ortalaması (PPG), oyuncunun toplam sayı değerinin oynadığı maç sayısına bölünmesiyle elde edilir.

### 6.3. Veri Kaynağı

Projede kullanılan takım ve oyuncu isimleri EuroLeague verilerinden yararlanılarak hazırlanan demo verileridir.

| Bilgi | Açıklama |
|---|---|
| Veri kaynağı | EuroLeague takım ve oyuncu verileri |
| Veri türü | Demo verisi |
| Referans sezonu | Henüz belirlenmedi |
| Veri alınma tarihi | Seed PR sonrasında eklenecek |
| Maç sonuçları | Demo sonuçları |
| Resmî sonuç durumu | Resmî EuroLeague sonuçları değildir |

Demo veriler, uygulamanın işlevlerini test etmek ve basketbol ligi yönetim süreçlerini göstermek amacıyla kullanılmaktadır.

---

## 7. Loglama ve Hata Yönetimi

Uygulamada hata yanıtlarının standart bir yapıda sunulması hedeflenmektedir.

ADR-0006 kapsamında loglama ve hata yönetimi kararları belirlenmiştir.

| HTTP Durum Kodu | Açıklama |
|---|---|
| 400 Bad Request | Geçersiz veya hatalı istek |
| 404 Not Found | İstenen kaydın bulunamaması |
| 409 Conflict | Veri veya işlem çakışması |
| 500 Internal Server Error | Sunucu tarafında beklenmeyen hata |

Hata yanıtlarında Problem Details yaklaşımı kullanılır.

Loglama altyapısı için SLF4J kararlaştırılmıştır; bağımlılığın projeye eklenmesi henüz tamamlanmamıştır.

---

## 8. Testler

Projenin doğruluğunu ve mimari kurallara uygunluğunu kontrol etmek için otomatik testlerden yararlanılır.

Kullanılan test araçları:

- JUnit 5
- ArchUnit
- AssertJ
- Mockito
- JaCoCo

### Testleri Çalıştırma

*Windows (PowerShell):*

powershell
.\mvnw.cmd -B clean verify


*macOS / Linux:*

bash
./mvnw -B clean verify


Bu komut proje derlemesini, testleri ve doğrulama aşamalarını çalıştırır.

ArchUnit testleri, katmanlar arasındaki bağımlılıkların belirlenen mimari kurallara uygunluğunu kontrol eder.

JaCoCo test kapsamı raporlarının oluşturulmasında kullanılır.

---

## 9. Sistem Gereksinimleri

| Yazılım | Sürüm |
|---|---|
| Java Development Kit | 21 |
| PostgreSQL | 18 |
| Payara Micro | 7.2026.9 |
| İşletim Sistemi | Windows / macOS / Linux |

*Not:* Maven ayrıca kurulmak zorunda değildir. Projede Maven Wrapper bulunmaktadır.

---

## 10. Kurulum ve Çalıştırma

### 10.1. Projeyi Klonlama

bash
git clone <repository-url>
cd basketball-league-manager


### 10.2. PostgreSQL Veritabanı Oluşturma

PostgreSQL 18 kurulumunu tamamladıktan sonra veritabanı kullanıcısını ve veritabanını oluşturun.

sql
CREATE USER basketball WITH PASSWORD 'your_local_database_password';

CREATE DATABASE basketball_league OWNER basketball;


### 10.3. Projeyi Derleme

*Windows (PowerShell):*

powershell
.\mvnw.cmd clean package


*macOS / Linux:*

bash
./mvnw clean package


Derleme tamamlandığında WAR dosyası target klasöründe oluşturulur.

### 10.4. Veritabanı Bağlantı Ayarları

Uygulamanın veritabanına bağlanabilmesi için aşağıdaki ortam değişkenleri tanımlanmalıdır.

| Değişken | Örnek |
|---|---|
| DB_URL | jdbc:postgresql://localhost:5432/basketball_league |
| DB_USER | basketball |
| DB_PASSWORD | Yerel veritabanı şifreniz |

*Windows (PowerShell):*

powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/basketball_league"
$env:DB_USER="basketball"
$env:DB_PASSWORD="your_local_database_password"


*macOS / Linux:*

bash
export DB_URL="jdbc:postgresql://localhost:5432/basketball_league"
export DB_USER="basketball"
export DB_PASSWORD="your_local_database_password"


Gerçek veritabanı şifreleri kaynak koduna eklenmemelidir.

### 10.5. Uygulamayı Başlatma

Payara Micro 7.2026.9 JAR dosyasının hazır olduğundan emin olun.

*Windows (PowerShell):*

powershell
java -jar <payara-micro.jar> --deploy target\basketball-league.war --contextroot /basketball-league --nocluster


*macOS / Linux:*

bash
java -jar <payara-micro.jar> --deploy target/basketball-league.war --contextroot /basketball-league --nocluster


<payara-micro.jar> yerine Payara Micro JAR dosyasının gerçek adı veya tam yolu yazılmalıdır.

---

## 11. API Kontrolü

Uygulama başlatıldıktan sonra aşağıdaki endpoint üzerinden API'nin çalışması kontrol edilebilir.

*İstek:*

http
GET http://localhost:8080/basketball-league/api/ping


*Beklenen yanıt örneği:*

json
{
  "status": "UP",
  "timestamp": "2026-11-14T19:00:00Z"
}


Yanıttaki timestamp alanı isteğin işlendiği zamanı temsil eder. Gerçek değer uygulama tarafından oluşturulur.

---

## 12. Proje Ekibi

| Ekip Üyesi | GitHub Kullanıcı Adı |
|---|---|
| Ömer | OmerCN1 |
| Osman | OsmanBlc |
| Mustafa | Mustafabdk7 |

---

## 13. Mimari Karar Kayıtları (ADR)

Projenin mimari ve teknik kararları docs/adr/ klasöründe belgelenmektedir.

İlgili karar belgeleri:

- [ADR-0001](docs/adr/ADR-0001.md) – Proje kapsamı ve temel kararlar
- [ADR-0006](docs/adr/ADR-0006.md) – Loglama ve hata yönetimi
- [ADR-0008](docs/adr/ADR-0008.md) – Katmanlı monolit mimarisi

---

## 14. Lisans ve Kullanım

Bu proje eğitim ve yazılım geliştirme amaçlı hazırlanmıştır.

Kullanılan takım ve oyuncu isimleri demo verilerinde yer almakta olup uygulama resmî EuroLeague ürünü değildir. EuroLeague markası ve ilgili isimler kendi hak sahiplerine aittir.
