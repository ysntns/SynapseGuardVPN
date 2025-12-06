# SynapseGuard VPN 1.1.0

## Highlights
- Added CI- and Gradle-friendly release signing configuration that can read keystore credentials from `gradle.properties` or environment variables.
- Improved release signing validation with explicit missing-property diagnostics and CI guardrails when secrets are absent.
- Improved onboarding with explicit VPN and notification permission prompts on the home screen.
- Persisted split tunneling selections using DataStore so bypassed apps survive restarts.
- Stabilized VPN service state propagation so UI surfaces live connection status and traffic metrics from the foreground service.

## Türkçe Özeti
- CI ve Gradle için uygun, `gradle.properties` veya ortam değişkenlerinden anahtar bilgilerini okuyabilen release imzalama yapısı eklendi.
- Eksik bilgileri açıkça raporlayan doğrulama ve CI ortamı için korumalarla imzalama süreci güçlendirildi.
- Ana ekranda VPN ve bildirim izinleri için net izin istemleriyle ilk kullanım akışı iyileştirildi.
- Bölünmüş tünel (split tunneling) seçimleri DataStore üzerinde saklanarak yeniden başlatmalardan sonra da korunuyor.
- Ön plan VPN servisi bağlantı durumu ve trafik istatistiklerini uygulamaya canlı olarak ileterek kullanıcıya anlık geri bildirim sağlıyor.

## Privacy
The app requests VPN permission to create the secure tunnel and POST_NOTIFICATIONS (Android 13+) to surface important connection status updates. No personal data is collected beyond what is required to operate the VPN connection and user-selected preferences.
