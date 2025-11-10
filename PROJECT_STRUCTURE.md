# SynapseGuard VPN - Project Structure

## Overview
Professional Android VPN application with multi-protocol support (WireGuard, OpenVPN, V2Ray) built with modern Android development practices.

## Technology Stack

### Core
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle 8.2 with Kotlin DSL

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **Architecture**: Clean Architecture (Data, Domain, Presentation layers)
- **Dependency Injection**: Hilt (Dagger)

### Libraries
- **UI**: Jetpack Compose with Material3
- **Navigation**: Navigation Compose
- **Async**: Coroutines & Flow
- **Network**: Retrofit + OkHttp
- **Local Storage**: Room Database + DataStore (Preferences)
- **Logging**: Timber

## Project Structure

```
SynapseGuardVPN/
├── app/                                    # Main application module
│   ├── src/main/
│   │   ├── java/com/synapseguard/vpn/
│   │   │   ├── data/                      # Data layer
│   │   │   │   ├── local/                 # Local data sources
│   │   │   │   │   ├── dao/               # Room DAOs
│   │   │   │   │   ├── entity/            # Room entities
│   │   │   │   │   └── VpnDatabase.kt     # Room database
│   │   │   │   ├── remote/                # Remote data sources
│   │   │   │   │   ├── dto/               # Data Transfer Objects
│   │   │   │   │   └── VpnApiService.kt   # Retrofit API
│   │   │   │   └── repository/            # Repository implementations
│   │   │   │
│   │   │   ├── domain/                    # Domain layer (business logic)
│   │   │   │   ├── model/                 # Domain models
│   │   │   │   ├── repository/            # Repository interfaces
│   │   │   │   └── usecase/               # Use cases
│   │   │   │
│   │   │   ├── presentation/              # Presentation layer
│   │   │   │   ├── home/                  # Home screen (main VPN control)
│   │   │   │   ├── servers/               # Server selection screen
│   │   │   │   ├── settings/              # Settings screen
│   │   │   │   ├── navigation/            # Navigation setup
│   │   │   │   ├── theme/                 # Compose theme
│   │   │   │   └── MainActivity.kt        # Main activity
│   │   │   │
│   │   │   ├── di/                        # Dependency Injection modules
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   │   └── DatabaseModule.kt
│   │   │   │
│   │   │   └── SynapseGuardApplication.kt # Application class
│   │   │
│   │   ├── res/                           # Resources
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── themes.xml
│   │   │   │   └── colors.xml
│   │   │   └── xml/
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   ├── build.gradle.kts                   # App module build file
│   └── proguard-rules.pro
│
├── vpn-service/                           # VPN service module
│   ├── src/main/
│   │   ├── java/com/synapseguard/vpn/service/
│   │   │   ├── core/                      # Core VPN service
│   │   │   │   ├── VpnConnectionService.kt
│   │   │   │   └── VpnProtocolHandler.kt
│   │   │   ├── wireguard/                 # WireGuard implementation
│   │   │   ├── openvpn/                   # OpenVPN implementation
│   │   │   └── v2ray/                     # V2Ray implementation
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   ├── build.gradle.kts                   # Service module build file
│   ├── proguard-rules.pro
│   └── consumer-rules.pro
│
├── build.gradle.kts                       # Root build file
├── settings.gradle.kts                    # Project settings
├── gradle.properties                      # Gradle properties
└── gradle/wrapper/                        # Gradle wrapper

```

## Module Description

### App Module
Main application module containing:
- UI (Jetpack Compose)
- ViewModels
- Navigation
- Dependency Injection setup
- Data & Domain layers

### VPN-Service Module
Dedicated module for VPN functionality:
- Core VPN service implementation
- Protocol handlers (WireGuard, OpenVPN, V2Ray)
- Low-level VPN operations

## Clean Architecture Layers

### Data Layer
- **Entities**: Room database entities
- **DTOs**: Network data transfer objects
- **DAOs**: Data Access Objects for Room
- **API Services**: Retrofit interfaces
- **Repositories**: Implementation of domain repository interfaces

### Domain Layer
- **Models**: Core business models
- **Repository Interfaces**: Contracts for data operations
- **Use Cases**: Single-responsibility business logic units

### Presentation Layer
- **Screens**: Composable UI screens
- **ViewModels**: UI state management
- **Navigation**: App navigation graph
- **Theme**: Material3 theming

## Key Features Prepared

1. **Multi-Protocol Support**
   - WireGuard (modern, fast)
   - OpenVPN (widely compatible)
   - V2Ray (advanced routing)

2. **Security Features**
   - Kill Switch (planned)
   - Split Tunneling (planned)
   - Custom DNS

3. **User Experience**
   - Modern Material3 UI
   - Real-time connection stats
   - Server selection
   - Settings management

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35

### Building the Project
```bash
./gradlew build
```

### Running the App
```bash
./gradlew installDebug
```

## Implementation Status

### ✅ Completed
- Project structure setup
- Gradle configuration
- Clean Architecture layers
- Hilt dependency injection
- Room database setup
- Retrofit API setup
- DataStore for settings
- Basic UI screens (Home, Servers, Settings)
- ViewModels with state management
- Navigation setup
- VPN service foundation

### 🔄 In Progress / TODO
- VPN protocol implementations (WireGuard, OpenVPN, V2Ray)
- Server list API integration
- Kill Switch implementation
- Split Tunneling implementation
- Connection statistics tracking
- Notification system
- Server latency testing
- App icons and branding
- Unit tests
- Integration tests

## Development Guidelines

### Code Style
- Follow Kotlin official coding conventions
- Use meaningful variable names
- Add documentation for public APIs

### Commit Messages
- Use conventional commits format
- Be descriptive about changes

### Testing
- Write unit tests for ViewModels and Use Cases
- Write integration tests for Repositories
- Write UI tests for critical user flows

## License
[To be determined]

## Contributors
[To be added]
