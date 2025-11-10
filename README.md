# SynapseGuard VPN

Professional Android VPN application with multi-protocol support (WireGuard, OpenVPN, V2Ray)

![Min SDK](https://img.shields.io/badge/Min%20SDK-26-blue)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-purple)
![Gradle](https://img.shields.io/badge/Gradle-8.2-green)

## Overview

SynapseGuard VPN is a modern, secure VPN application for Android built with the latest Android development practices. It features a clean architecture, modern UI with Jetpack Compose, and support for multiple VPN protocols.

## Features

### Current UI & UX
- ✅ Modern Material3 UI with Jetpack Compose
- ✅ Animated Splash Screen with BCI-optimized branding
- ✅ Statistics Screen with real-time metrics
  - Circular speed gauge
  - Download/upload speed visualization
  - Data usage graphs (30-day history)
  - BCI Neural Latency monitoring
  - Interactive speed test
- ✅ Enhanced Home Screen with connection management
- ✅ Server selection screen with AI-optimized suggestions
- ✅ Settings screen with security features
- ✅ Dark theme with cyan accents (#00D9FF)
- ✅ Custom logo and branding assets

### Architecture & Development
- ✅ MVVM Architecture with Clean Architecture
- ✅ Hilt Dependency Injection
- ✅ Room Database for local storage
- ✅ DataStore for preferences
- ✅ Retrofit for network operations
- ✅ Coroutines & Flow for async operations
- ✅ Navigation Compose with multi-screen flow

### Planned Protocol Support
- 🔄 WireGuard protocol implementation
- 🔄 OpenVPN protocol implementation
- 🔄 V2Ray protocol implementation

### Planned Security Features
- 🔄 Kill Switch (system-level traffic blocking)
- 🔄 Split Tunneling (per-app VPN routing)
- 🔄 DNS Leak Protection
- 🔄 Auto-connect on startup
- 🔄 Always-on VPN support

## Tech Stack

### Core
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle 8.2 with Kotlin DSL

### Architecture & Patterns
- **Architecture**: Clean Architecture (Data, Domain, Presentation)
- **Design Pattern**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt (Dagger)

### Libraries
- **UI**: Jetpack Compose + Material3
- **Navigation**: Navigation Compose
- **Async**: Kotlin Coroutines + Flow
- **Network**: Retrofit + OkHttp
- **Local Storage**: Room Database + DataStore
- **Logging**: Timber

## Project Structure

```
app/                    # Main application module
├── data/              # Data layer (repositories, local/remote data sources)
├── domain/            # Domain layer (models, use cases, repository interfaces)
├── presentation/      # Presentation layer (UI, ViewModels, navigation)
└── di/                # Dependency injection modules

vpn-service/           # VPN service module
├── core/              # Core VPN service
├── wireguard/         # WireGuard implementation
├── openvpn/           # OpenVPN implementation
└── v2ray/             # V2Ray implementation
```

For detailed project structure, see [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35

### Setup
1. Clone the repository
   ```bash
   git clone https://github.com/your-username/SynapseGuardVPN.git
   cd SynapseGuardVPN
   ```

2. Open in Android Studio
   - File → Open → Select the project directory

3. Build the project
   ```bash
   ./gradlew build
   ```

4. Run on device/emulator
   ```bash
   ./gradlew installDebug
   ```

## Development

### Building
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Roadmap

### Version 0.5 (Current - UI Complete) ✅
- [x] Animated Splash Screen
- [x] Enhanced Home Screen with connection UI
- [x] Statistics Screen with metrics visualization
- [x] Server Selection Screen
- [x] Settings Screen
- [x] Navigation system
- [x] Dark theme with BCI-optimized colors
- [x] Custom logo and branding

### Version 1.0 (In Progress)
- [ ] WireGuard protocol implementation
- [ ] Basic VPN connectivity
- [ ] Real-time connection statistics
- [ ] Server latency testing

### Version 1.1
- [ ] OpenVPN support
- [ ] Kill Switch implementation
- [ ] Split Tunneling
- [ ] DNS Leak Protection

### Version 2.0
- [ ] V2Ray support
- [ ] Advanced routing options
- [ ] Per-app VPN configuration
- [ ] AI-enhanced server selection

## License

[License to be determined]

## Acknowledgments

- Built with modern Android development best practices
- Inspired by the need for secure, open-source VPN solutions
- Thanks to the Android and Kotlin communities
