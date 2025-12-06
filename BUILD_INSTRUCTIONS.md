# SynapseGuard VPN - Build Talimatları

Bu doküman, SynapseGuard VPN APK dosyasını oluşturmak için gerekli tüm adımları içermektedir.

---

## Hızlı Başlangıç (Debug APK)

### Yöntem 1: GitHub Actions ile (Önerilen)

1. GitHub repository'ye gidin
2. **Actions** sekmesine tıklayın
3. **Android Build** workflow'unu seçin
4. **Run workflow** butonuna tıklayın
5. Build type olarak `debug` seçin
6. Build tamamlandığında **Artifacts** bölümünden `app-debug.apk` dosyasını indirin

### Yöntem 2: Lokal Build

```bash
# Proje dizinine gidin
cd SynapseGuardVPN

# Debug APK oluşturun
./gradlew assembleDebug

# APK dosyası burada oluşacak:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## Release APK (Play Store için)

### Adım 1: Keystore Oluşturma

Keystore, APK'yı imzalamak için kullanılan dijital sertifikadır. **Bu dosyayı KAYBETMEYİN** - Play Store'a yüklenen tüm güncellemeler aynı keystore ile imzalanmalıdır.

```bash
# Keystore oluşturma komutu
keytool -genkey -v -keystore synapseguard-release.keystore \
  -alias synapseguard \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=SynapseGuard, OU=Mobile, O=BCI, L=Istanbul, ST=Istanbul, C=TR"
```

**Örnek değerler (kendi şifrelerinizi kullanın):**
- Store Password: `SynapseGuard2024!`
- Key Alias: `synapseguard`
- Key Password: `SynapseGuard2024!`

### Adım 2: GitHub Secrets Ayarlama

Repository Settings > Secrets and variables > Actions > New repository secret

| Secret Adı | Değer |
|------------|-------|
| `RELEASE_KEYSTORE_BASE64` | Keystore dosyasının base64 hali (aşağıya bakın) |
| `RELEASE_STORE_PASSWORD` | Keystore şifresi |
| `RELEASE_KEY_ALIAS` | Key alias (örn: `synapseguard`) |
| `RELEASE_KEY_PASSWORD` | Key şifresi |

**Keystore'u Base64'e dönüştürme:**

```bash
# macOS/Linux
base64 -i synapseguard-release.keystore | tr -d '\n' > keystore_base64.txt

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("synapseguard-release.keystore")) > keystore_base64.txt
```

`keystore_base64.txt` içeriğini `RELEASE_KEYSTORE_BASE64` secret'ına yapıştırın.

### Adım 3: Release Build Çalıştırma

1. GitHub Actions > Android Build > Run workflow
2. Build type: `release` seçin
3. Build tamamlandığında `app-release.apk` dosyasını indirin

---

## Lokal Release Build

### gradle.properties Yapılandırması

Proje kök dizininde `gradle.properties` dosyasına ekleyin:

```properties
releaseStoreFile=/path/to/synapseguard-release.keystore
releaseStorePassword=YOUR_STORE_PASSWORD
releaseKeyAlias=synapseguard
releaseKeyPassword=YOUR_KEY_PASSWORD
```

### Build Komutu

```bash
./gradlew assembleRelease

# APK konumu:
# app/build/outputs/apk/release/app-release.apk
```

---

## Testleri Çalıştırma

```bash
# Tüm testleri çalıştır
./gradlew test

# Sadece app modülü testleri
./gradlew :app:test

# Sadece vpn-service modülü testleri
./gradlew :vpn-service:test

# Test raporu konumu:
# app/build/reports/tests/testDebugUnitTest/index.html
```

---

## APK Analizi

### APK Boyutu Kontrolü

```bash
./gradlew :app:analyzeReleaseBundle
```

### Lint Kontrolü

```bash
./gradlew lint
```

---

## Troubleshooting

### Hata: "SDK location not found"

`local.properties` dosyası oluşturun:
```properties
sdk.dir=/path/to/Android/Sdk
```

### Hata: "Keystore file not found"

Keystore dosya yolunun doğru olduğundan emin olun. Mutlak yol kullanın.

### Hata: "Build tools not found"

Android Studio > SDK Manager > SDK Tools > Android SDK Build-Tools yükleyin.

---

## CI/CD Pipeline

Bu proje GitHub Actions ile otomatik build yapılandırılmıştır.

| Trigger | Build Type | Açıklama |
|---------|------------|----------|
| Push to `claude/*` | Debug | Otomatik debug build |
| Push to `work`, `main` | Debug | Otomatik debug build |
| Manual dispatch | Seçilebilir | Debug veya Release |

---

## Güvenlik Notları

1. **Keystore dosyasını** repository'ye COMMIT ETMEYİN
2. **Şifreleri** kod içinde saklamayın
3. GitHub Secrets kullanarak CI/CD'de güvenli şekilde saklayın
4. Keystore dosyasının **yedeğini alın** ve güvenli bir yerde saklayın

---

## Dosya Yapısı

```
SynapseGuardVPN/
├── app/
│   └── build/outputs/apk/
│       ├── debug/app-debug.apk
│       └── release/app-release.apk
├── gradle.properties          # Signing config (lokal)
├── synapseguard-release.keystore  # (gitignore'da - commit etmeyin!)
└── .github/workflows/
    └── android-release.yml    # CI/CD yapılandırması
```

---

*Son güncelleme: 6 Aralık 2025*
