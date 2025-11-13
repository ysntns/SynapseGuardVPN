# Developer Notes / Geliştirici Notları

## Gradle Wrapper Sorunları / Gradle Wrapper Issues

### Problem: gradlew Dosyası Bulunamıyor

Eğer `./gradlew` komutunu çalıştırdığınızda "command not found" hatası alıyorsanız, gradlew script dosyaları silinmiş olabilir.

**English**: If you get "command not found" when running `./gradlew`, the gradle wrapper scripts may have been deleted.

### Çözüm 1: Git'ten Geri Al / Solution 1: Restore from Git

Gradle wrapper dosyalarını git'ten geri alın:

```bash
# Tüm gradle dosyalarını geri al / Restore all gradle files
git checkout HEAD -- gradlew gradlew.bat gradle/

# Executable permission ver / Make executable
chmod +x gradlew

# Test et / Test it
./gradlew --version
```

### Çözüm 2: Fresh Clone / Solution 2: Fresh Clone

Repo'yu yeniden clone edin:

```bash
cd ~/AndroidStudioProjects/
rm -rf SynapseGuardVPN
git clone https://github.com/ysntns/SynapseGuardVPN.git
cd SynapseGuardVPN
git checkout claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE

# SDK location ayarla / Set SDK location
echo "sdk.dir=/home/y/Android/Sdk" > local.properties

# Build yap / Build it
./gradlew assembleDebug
```

### Çözüm 3: Manuel İndirme / Solution 3: Manual Download

Eğer git checkout çalışmazsa, dosyaları GitHub'dan manuel indirebilirsiniz:

```bash
# gradlew script (Linux/Mac)
curl -o gradlew https://raw.githubusercontent.com/ysntns/SynapseGuardVPN/claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE/gradlew
chmod +x gradlew

# gradlew.bat (Windows)
curl -o gradlew.bat https://raw.githubusercontent.com/ysntns/SynapseGuardVPN/claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE/gradlew.bat

# gradle-wrapper.jar
curl -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/ysntns/SynapseGuardVPN/claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE/gradle/wrapper/gradle-wrapper.jar

# gradle-wrapper.properties
curl -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/ysntns/SynapseGuardVPN/claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE/gradle/wrapper/gradle-wrapper.properties
```

### Çözüm 4: Gradle Daemon ile Build / Solution 4: Using Android Studio

Android Studio kullanıyorsanız:

1. **File → Sync Project with Gradle Files**
2. Android Studio otomatik olarak gradle wrapper'ı yeniden oluşturur
3. Veya Android Studio'nun built-in gradle'ini kullanabilirsiniz

### ⚠️ Önemli Notlar / Important Notes

#### Gradle Wrapper Neden Önemli? / Why is Gradle Wrapper Important?

Gradle Wrapper şunları sağlar:
- **Consistent builds**: Herkes aynı Gradle versiyonunu kullanır
- **No installation needed**: Sistem genelinde gradle kurulumu gerektirmez
- **Version control**: Gradle versiyonu proje ile birlikte versiyon kontrolünde
- **CI/CD friendly**: Continuous Integration ortamlarında kolay kullanım

#### Dosyalar / Files

```
gradlew                         # Linux/Mac script (executable)
gradlew.bat                     # Windows script
gradle/wrapper/
├── gradle-wrapper.jar         # Wrapper executable (~43KB)
└── gradle-wrapper.properties  # Gradle version config
```

#### Git'te İzlenmesi Gereken Dosyalar / Files to Track in Git

✅ **Commit edilmeli / Should be committed**:
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

❌ **Commit edilmemeli / Should NOT be committed** (.gitignore):
- `.gradle/` (cache directory)
- `local.properties` (SDK location, machine-specific)
- `*.iml` (IDE files)
- `build/` (build outputs)

## Android SDK Yapılandırması / Android SDK Configuration

### local.properties Dosyası

Bu dosya **asla commit edilmemeli** çünkü her makinede farklı olabilir.

```properties
# Linux/Mac
sdk.dir=/home/YOUR_USERNAME/Android/Sdk

# Windows
sdk.dir=C\\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

**Your path** (for user 'y'):
```bash
echo "sdk.dir=/home/y/Android/Sdk" > local.properties
```

## Build Komutları / Build Commands

### Debug Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Install to Device
```bash
./gradlew installDebug
```

### Run Tests
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests (requires device/emulator)
```

### Clean Build
```bash
./gradlew clean
./gradlew clean assembleDebug
```

### Check Dependencies
```bash
./gradlew dependencies
./gradlew app:dependencies
```

## Sorun Giderme / Troubleshooting

### 1. "SDK location not found"
```bash
# Solution
echo "sdk.dir=/home/y/Android/Sdk" > local.properties
```

### 2. "gradlew: command not found"
```bash
# Solution 1: Git'ten geri al
git checkout HEAD -- gradlew
chmod +x gradlew

# Solution 2: Fresh clone yap
```

### 3. "Could not find or load main class org.gradle.wrapper.GradleWrapperMain"
```bash
# Gradle wrapper jar eksik
git checkout HEAD -- gradle/wrapper/gradle-wrapper.jar
```

### 4. Build Fails - Out of Memory
```bash
# gradle.properties dosyasına ekle:
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### 5. Deprecated API Warnings
Bu uyarılar normal ve derlemeyi engellemez. Gelecek versiyonlarda güncellenecek.

```
w: file:///.../ServersScreen.kt:48:17 'launch: Job' is deprecated. This API...
```

## Geliştirme Ortamı / Development Environment

### Gereksinimler / Requirements
- **Android Studio**: Hedgehog (2023.1.1) veya daha yeni
- **JDK**: 17
- **Android SDK**: 35
- **Minimum SDK**: 26
- **Gradle**: 8.2 (wrapper üzerinden)
- **Kotlin**: 1.9.20

### İlk Kurulum / Initial Setup
```bash
# 1. Clone repo
git clone https://github.com/ysntns/SynapseGuardVPN.git
cd SynapseGuardVPN
git checkout claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE

# 2. SDK location
echo "sdk.dir=/home/y/Android/Sdk" > local.properties

# 3. Build
./gradlew assembleDebug

# 4. Install
./gradlew installDebug
```

## Google Play Billing Test

Production'da Google Play Billing test etmek için:

1. **Google Play Console'da ürünleri tanımla**:
   - `synapseguard_basic_monthly`
   - `synapseguard_premium_monthly`
   - `synapseguard_enterprise_monthly`

2. **Test lisansı ekle**: Play Console → License Testing

3. **Test hesabıyla test et**: Real payment yapmadan test edebilirsiniz

## CI/CD Notları

GitHub Actions veya başka CI/CD kullanıyorsanız:

```yaml
# .github/workflows/android.yml example
- name: Make gradlew executable
  run: chmod +x gradlew

- name: Build with Gradle
  run: ./gradlew assembleDebug
```

## Yardım / Help

Daha fazla bilgi için:
- [README.md](README.md) - Proje genel bakış
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - Kod yapısı
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Detaylı sorun giderme

---

**Son Güncelleme / Last Updated**: 2025-01-13
**Branch**: `claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE`
**Build System**: Gradle 8.2 with Kotlin DSL
