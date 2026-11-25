# Mimari test planı

Bu plan, [ADR-0001: Katmanlı Monolit Mimari](../adr/0001-katmanli-mimari.md) kararının nasıl doğrulanacağını açıklar.
Kontroller ilgili özellik çalışmalarında uygulanacaktır; burada listelenmeleri tamamlandıkları anlamına gelmez.

## Planlanan kontroller

Aşağıdaki kontroller planlanmıştır; bu belge test veya üretim kodu eklemez.
Paket adı, public sınıflara erişimi engellemez. ArchUnit, bytecode üzerinden bağımlılıkları denetler;
yalnızca derlemenin geçmesi mimari uyum kanıtı değildir. [ders notu]

| Kural | Kim korur? | Planlanan kural / kanıt |
| --- | --- | --- |
| Resource repository veya entity'ye erişmez | ArchUnit | `resource_does_not_touch_repository_or_entity` |
| Repository yalnızca entity paketine bağımlıdır | ArchUnit | `repository_only_sees_entity` |
| Entity üst katmanlara bağımlı değildir | ArchUnit | `entity_does_not_see_upper_layers` |
| DTO üst katmanlara ve entity'ye bağımlı değildir | ArchUnit | `dto_is_a_pure_contract` |
| Domain yalnızca JDK kullanır | ArchUnit | `domain_is_framework_free` |
| `@Transactional` yalnızca service içindedir | ArchUnit | `transactional_only_in_service` |
| Field injection yasaktır | ArchUnit | `no_field_injection` |
| Entity'de public setter yasaktır | ArchUnit | `entities_have_no_public_setters` |
| REST entity döndürmez veya kabul etmez | İmza incelemesi ve resource testleri | DTO istek/yanıt sözleşmeleri |
| Maç onayı kısmen kalıcı olamaz | Transaction entegrasyon testi | Hata sonrası onay işlemindeki kayıtlar değişmemeli |
| Puan durumu yalnızca onaylı maçlardan hesaplanır | Birim ve entegrasyon testleri | Sezon, durum ve toplam doğrulamaları |
| Transfer bir bütün olarak tamamlanır | Transaction entegrasyon testi | Kayıt hatasında oyuncunun takımı değişmemeli |

## Kuralların doğrulanması

ArchUnit kuralı, geçici bir yasak bağımlılıkla başarısız olmalı; ihlal kaldırıldığında yeniden geçmelidir. [ders notu]
Boş sınıf kümesini denetleyen kurallar varsayılan olarak başarısız olur. Henüz oluşmamış paketler için genel bir
sessiz geçiş ayarı açılmaz; kuralların devreye alınma zamanı belirlenir.

## Transaction doğrulaması

Maç onayında geçersiz durum veya tutarsız skor reddedilmeli; maçın durumu ve hesaplanan puan durumu değişmemelidir.
Aynı maçın iki ayrı kopyasıyla yapılan güncellemelerde `@Version` çakışması doğrulanmalıdır.
`FINISHED` durumundan başlayan onay başarısızsa maç bu durumda kalmalıdır; ayrı puan durumu satırları güncellenmez.
Zaten `CONFIRMED` olan maça tekrar onay isteği reddedilirken mevcut onay ve toplamlar korunmalıdır.

Çok kayıtlı rollback senaryosu transferde sınanır: oyuncunun takımı değiştirildikten sonra transfer kaydının
oluşturulması başarısız olduğunda eski takım korunmalı ve transfer kaydı oluşmamalıdır.

Rollback doğrulamasında gerçek servis transaction sınırı etkin olmalı; hata sonrasındaki kayıtlar yeni bir
persistence context üzerinden okunmalıdır. Elle açılan bir `RESOURCE_LOCAL` transaction yalnızca o testteki
işlem davranışını gösterir; Payara'nın servis üzerindeki `@Transactional` sınırını etkinleştirdiğini kanıtlamaz.

## Puan durumu doğrulaması

- Yalnızca istenen sezondaki `CONFIRMED` maçlar hesaba katılır; diğer durumlar ve sezonlar sonucu etkilemez.
- Oynanan maç, galibiyet, mağlubiyet, atılan ve yenilen sayılar ev/deplasman ve uzatmalı maç örnekleriyle doğrulanır.
- `PointsPolicy` puan hesabı ve eşitlik bozma sırası birim testleriyle sınanır; maçı olmayan takımlar sıfırla görünür.
- İkili averajda yalnızca ilgili takımların birbirleriyle oynadığı onaylanmış maçlar kullanılır.
- Başarılı onaydan sonraki okumada maç bir kez hesaba katılır; tekrar onay reddedilir ve toplamlar değişmez.
- Aynı takımın farklı maçlarının başarılı eşzamanlı onaylarından sonra iki maç da toplamda yer alır.

Sorgu maliyeti temsili sezon verisiyle ölçülür; veri büyüklüğü ve ölçüm sonucu kaydedilir.
Bu ölçüm, sorgu tasarımı ve indeksler değerlendirilmeden ayrı bir özet tabloya geçiş gerekçesi sayılmaz.

## Test seviyeleri ve mevcut CI durumu

Birim testleri iş davranışını, repository entegrasyon testleri migration ve sorguları, ArchUnit yapısal kuralları sınar.
Mockito gerçek JPA/CDI davranışını, ArchUnit hesapların doğruluğunu kanıtlamaz.
Payara üzerinden HTTP kontrolü dağıtımı doğrular; sınır değer testlerinin yerini almaz.

Mevcut [CI iş akışı] `verify` çalıştırır ve JaCoCo raporunu saklar. ArchUnit ve Testcontainers/Failsafe henüz yoktur;
yeşil build planın tamamlandığı anlamına gelmez. Yeni kontroller ilgili özellik PR'larında eklenecektir.

[CI iş akışı]: ../../.github/workflows/build.yml
[ders notu]:
  https://github.com/software-lecture-labs/study-notes/blob/d33d07147fb440a76ac89944a5655e2021073da7/java-chapter-09.md
