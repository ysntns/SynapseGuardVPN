# SynapseGuard VPN

Cross-platform VPN application built with React Native featuring multi-protocol support (WireGuard, OpenVPN, V2Ray)

![React Native](https://img.shields.io/badge/React%20Native-0.76-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue)
![Platform](https://img.shields.io/badge/Platform-Android-green)

## Overview

SynapseGuard VPN is a modern, secure VPN application built with React Native for cross-platform support. It features a clean architecture, beautiful dark-themed UI, and support for multiple VPN protocols through native modules.

## Features

### UI & UX
- ✅ Modern dark theme with cyan accents (#00D9FF)
- ✅ Animated Splash Screen
- ✅ Statistics Screen with real-time metrics
  - Download/upload speed visualization
  - Data usage tracking
  - Session duration
- ✅ Home Screen with circular connection button
  - Animated connection states
  - Real-time status display
- ✅ Server selection screen
  - 9 servers across Europe, Americas, Asia-Pacific
  - Real latency and load indicators
  - Search and sort functionality
- ✅ Settings screen with security features
- ✅ Split Tunneling Screen
  - Per-app VPN bypass configuration
  - Toggle switches for each app
  - Search functionality

### Architecture
- ✅ Clean Architecture with TypeScript
- ✅ Zustand for state management
- ✅ React Navigation for routing
- ✅ Native Module bridge for VPN functionality
- ✅ Type-safe codebase

### VPN Protocol Support (Android)
- ✅ **WireGuard protocol** (via native module)
- 🔄 OpenVPN protocol (framework ready)
- 🔄 V2Ray protocol (framework ready)

### Security Features
- ✅ **Kill Switch** (system-level traffic blocking)
- ✅ **Split Tunneling** (per-app VPN routing)
- ✅ **DNS Leak Protection** (custom DNS servers)
- ✅ **Traffic Statistics** (real-time monitoring)
- ✅ **Foreground Service** (persistent notification)

## Tech Stack

### Frontend (React Native)
- **Framework**: React Native 0.76
- **Language**: TypeScript
- **State Management**: Zustand
- **Navigation**: React Navigation 7
- **Animations**: React Native Reanimated
- **Icons**: React Native Vector Icons

### Native (Android)
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **VPN Service**: Android VpnService API

## Project Structure

```
SynapseGuardVPN/
├── src/
│   ├── components/       # Reusable UI components
│   │   ├── CircularConnectionButton.tsx
│   │   ├── ServerListItem.tsx
│   │   ├── StatsCard.tsx
│   │   └── StatusCard.tsx
│   ├── screens/          # App screens
│   │   ├── splash/
│   │   ├── home/
│   │   ├── servers/
│   │   ├── settings/
│   │   ├── stats/
│   │   └── splittunnel/
│   ├── navigation/       # React Navigation setup
│   ├── stores/           # Zustand state stores
│   ├── services/         # Native module bridges
│   ├── hooks/            # Custom React hooks
│   ├── types/            # TypeScript type definitions
│   └── theme/            # Colors, typography, spacing
├── android/
│   └── app/src/main/java/com/synapseguardvpn/
│       └── vpn/          # VPN Native Module (Kotlin)
├── App.tsx
├── index.js
└── package.json
```

## Getting Started

### Prerequisites
- Node.js 18+
- npm or yarn
- Android Studio (for Android development)
- JDK 17

### Installation

1. Clone the repository
   ```bash
   git clone https://github.com/your-username/SynapseGuardVPN.git
   cd SynapseGuardVPN
   ```

2. Install dependencies
   ```bash
   npm install
   ```

3. Start Metro bundler
   ```bash
   npm start
   ```

4. Run on Android
   ```bash
   npm run android
   ```

## Development

### Available Scripts

```bash
# Start Metro bundler
npm start

# Run on Android
npm run android

# Run tests
npm test

# Lint code
npm run lint

# Clean Android build
npm run clean
```

### Building for Release

```bash
cd android
./gradlew assembleRelease
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Roadmap

### Version 1.0 (Current) ✅
- [x] React Native project setup with TypeScript
- [x] Complete UI implementation (6 screens)
- [x] Zustand state management
- [x] React Navigation setup
- [x] VPN Native Module bridge (Kotlin)
- [x] VpnService implementation
- [x] Kill Switch, Split Tunneling, DNS protection

### Version 1.1
- [ ] Real WireGuard encryption integration
- [ ] Server latency testing
- [ ] Persistent settings with MMKV
- [ ] Connection history

### Version 2.0
- [ ] iOS support
- [ ] OpenVPN protocol
- [ ] V2Ray protocol
- [ ] In-app purchases

## License

MIT License - see LICENSE file for details

## Acknowledgments

- Built with React Native and modern development practices
- Native VPN implementation using Android VpnService
- Inspired by the need for secure, open-source VPN solutions
