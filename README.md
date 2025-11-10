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
  - Shield icon in circular connection button
  - Status-based color changes
  - Connection state animations
- ✅ Server selection screen with AI-optimized suggestions
  - 9 servers across Europe, Americas, Asia-Pacific, and Middle East
  - Real latency and load indicators
  - Flag emojis for countries
- ✅ Settings screen with security features
- ✅ **Split Tunneling Screen** (NEW!)
  - Per-app VPN bypass configuration
  - Installed apps list with icons
  - Toggle switches for each app
  - Search functionality
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

### VPN Protocol Support
- ✅ **WireGuard protocol implementation** (Functional tunnel with packet forwarding)
  - UDP channel communication
  - Handshake protocol (ready for native library integration)
  - Real-time packet forwarding
  - Statistics tracking
  - Note: Encryption layer ready for WireGuard-Android library integration
- 🔄 OpenVPN protocol implementation (framework ready)
- 🔄 V2Ray protocol implementation (framework ready)

### Security Features
- ✅ **Split Tunneling** (per-app VPN routing with addDisallowedApplication)
- ✅ **Kill Switch** (system-level traffic blocking with VpnService.Builder.setBlocking)
- ✅ **DNS Leak Protection** (custom DNS servers routed through tunnel)
- ✅ **Traffic Statistics** (real-time upload/download monitoring)
- ✅ **Foreground Service** (persistent notification with connection status)
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

### Version 0.6 (Current - Full UI Complete) ✅
- [x] Animated Splash Screen
- [x] Enhanced Home Screen with shield icon
- [x] Statistics Screen with metrics visualization
- [x] Server Selection Screen (9 servers)
- [x] Settings Screen
- [x] **Split Tunneling Screen** (NEW)
- [x] 6-screen navigation system
- [x] Dark theme with BCI-optimized colors
- [x] Custom logo and branding

### Version 1.0 (Current - Core VPN Functional) ✅
- [x] **WireGuard protocol implementation**
- [x] **Basic VPN connectivity with tunnel establishment**
- [x] **Real-time connection statistics**
- [x] **Kill Switch backend**
- [x] **Split Tunneling backend** (full integration)
- [x] **DNS Leak Protection**
- [x] **Foreground service with notification**
- [x] **Traffic monitoring and speed calculation**
- [ ] Server latency testing (UI ready, backend pending)

### Version 1.1
- [ ] OpenVPN support (handler framework ready)
- [ ] V2Ray support (handler framework ready)
- [ ] WireGuard native library integration (for production encryption)
- [ ] Persistent VPN settings with DataStore
- [ ] Server selection persistence

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
