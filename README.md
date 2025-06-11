# ChatGPT API Destekli Xray Test Case Otomatik Üretim Aracı

Bu proje, **Java** dili kullanılarak geliştirilmiştir.
Amaç; **Jira** üzerinde belirlenen "Story", "Improvement", "Change" tipi issue'ları (kod içerisinde başka issue tipleri için değişiklik yapılabilir) GPT API destekli özel bir asistan yardımıyla analiz edip, **Xray** formatına uygun **Test Case** üretmek ve otomatik olarak **Jira + Xray** sistemine kaydetmektir.

## Özellikler

✅ Jira üzerinde belirlenen issue'lardan test case üretir
✅ GPT (Özel eğitilmiş Xray Asistanı) kullanarak test case içeriklerini üretir
✅ Test case'leri otomatik olarak Jira/Xray üzerinde **Test** issue olarak açar
✅ Test steps alanını doldurur
✅ Kaynak issue ile test case arasında link oluşturur (traceability)
✅ Üretilen test case'lere özel etiket (label) ekler (`CreatedByChatGPT`)

---

## Gereksinimler

* Java 17 veya üzeri
* Maven yapı aracı
* [Unirest](http://kong.github.io/unirest-java/) HTTP istemcisi (proje kodunda kullanılmış)
* Jira Cloud veya Jira Server (Xray eklentisi aktif olmalı)
* Jira kullanıcı adı (email) + API Token
* OpenAI API Key (GPT-4o destekli, Xray test case yazmaya özel eğitilmiş bir asistan için ayarlanmış olmalı)
* Assistant ID (GPT üzerinde fine-tuned / özel prompt konfigürasyonuna sahip)

---

## Kurulum

### 1️️ Projeyi Klonla

```bash
git clone https://github.com/gulecgzm/ai-testcase-generation.git
cd ai-testcase-generation
```

### 2️️ Bağımlılıkları yükle

```bash
./mvnw clean install
```

### 3️️ Yapılandırma

`TestCaseGeneration` sınıfındaki aşağıdaki sabitleri doldurmanız gerekmektedir:

```java
private static final String JIRA_URL = "https://yourjiradomain.com";
private static final String JIRA_USERNAME = "yourjirausername@example.com";
private static final String JIRA_API_TOKEN = "yourjiraapikey";
private static final String FIX_VERSION_VALUE = "1.0"; // Zorunlu alan için kullanılacak versiyon değeri
```

Ayrıca aşağıdaki kısımdaki OpenAI ayarlarını da doldurmanız gerekmektedir:

```java
String apiKey = "your_openai_api_key";
message.put("assistant_id", "your_assistant_id");
```

### 4️️ Kullanım öncesi gereklilikler

✅ Proje kullanılmadan önce:

* Kendi Jira kullanıcı adı ve API token'ınızı girin
* Kendi OpenAI API Key ve assistant ID’nizi girin
* GPT Asistanınızın **XRAY formatına uygun Test Case üretecek şekilde eğitilmiş olduğundan emin olun**
* Jira projenizde **Xray Test issue type = "Test"** olarak tanımlanmış olmalıdır

---

## Kullanım

1️️ `TestCaseGeneration.GPTIntegration()` metodunu çalıştırın (örnek olarak bir main metodu veya test sınıfı üzerinden çağırabilirsiniz):

```java
public static void main(String[] args) {
    TestCaseGeneration.GPTIntegration();
}
```

2️️ JQL sorgusu içinde hangi issue'lara test case yazdırmak istediğinizi belirleyin:

```java
String jqlQuery = "project = ABC AND issuetype in (Story, Improvement, Change) AND status = 'Done'";
```

3️️ Program şu adımları gerçekleştirir:

* Jira üzerinden ilgili issue'ları çeker
* Her bir issue'nun description alanını GPT'ye gönderir
* GPT'den gelen test case bilgilerini **summary**, **description**, **steps** şeklinde işler
* Jira üzerinde **Test** issue oluşturur
* Test steps bilgilerini ekler
* Orijinal issue ile Test issue arasında link kurar (link type id `10006`)

---

## Akış Şeması

```text
Jira Issue (Story / Improvement / Change)
        |
        v
GPT API üzerinden Test Case üretimi
        |
        v
Jira Xray "Test" issue oluşturma
        |
        v
Test Steps ekleme
        |
        v
Kaynak Issue <----> Test Issue linkleme
```

---

## Örnek Konsol Çıktısı

```text
Processing issue: ABC-123
GPT Response:
{
    "summary": "Kullanıcı Giriş Testi",
    "description": "Kullanıcı giriş senaryosu için test açıklaması",
    "steps": [...]
}

Test issue created: ABC-456
Issues linked successfully.
Test steps added successfully.
```

---

## Önemli Notlar

* GPT’den dönen `summary` alanı uzunluğu 255 karakter ile sınırlandırılmıştır
* Test adımları (`steps`) şu formatta beklenmektedir:

```json
{
  "steps": [
    {
      "action": "Butona tıkla",
      "data": "Geçerli kullanıcı adı ve şifre",
      "result": "Başarılı giriş"
    }
  ]
}
```

* Bağlantı kurma sırasında kullanılan issue link type id (`10006`) Jira’nızda **Tests** veya benzeri bir link type olarak yapılandırılmış olmalıdır (bu Jira üzerinde özelleştirilebilir, gerekirse link type id değiştirilebilir)

---

**Hatırlatma:** Bu proje, Xray için özel olarak **Test Case üretmeye eğitilmiş bir GPT asistanı** gerektirir. Genel GPT-4 API ile deneme yaparsanız Xray formatına uygun olmayan veya hatalı cevaplar alabilirsiniz.

```
```
