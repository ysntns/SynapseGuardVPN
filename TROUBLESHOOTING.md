# SynapseGuard VPN - Troubleshooting Guide

## 🔴 Frozen/Black Screen Issues

### Problem: App shows frozen or black screen
**FIXED in commit `e06ef14`**

**Root Cause:** Missing UI components (`ServerInfoCard` and `StatsCard`) caused the HomeScreen to crash silently.

**Solution:** The missing components have been added. Pull the latest code:
```bash
git pull origin claude/android-vpn-app-setup-011CUzP5w1WPFipsUeX97DWE
```

---

## 🛠️ Common Issues & Solutions

### 1. **Build Errors**

#### Missing dependencies
```bash
./gradlew clean build
# or
gradle wrapper
./gradlew clean build
```

#### Sync issues
In Android Studio:
- File → Invalidate Caches / Restart
- Sync Project with Gradle Files

### 2. **Runtime Crashes**

#### Check Logcat for errors:
```bash
adb logcat | grep "SynapseGuard"
```

Common crash patterns:
- `ClassNotFoundException`: Missing class/file
- `InflationException`: Missing Compose component
- `NoSuchMethodException`: Reflection error in VpnRepositoryImpl

#### VPN Service not starting:
Check AndroidManifest.xml has:
```xml
<service
    android:name="com.synapseguard.vpn.service.core.VpnConnectionService"
    android:exported="false"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:foregroundServiceType="specialUse">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

### 3. **VPN Not Connecting**

#### Permission not granted:
The app will request VPN permission on first connection. If denied:
1. Go to Android Settings → Apps → SynapseGuard VPN
2. Enable "VPN" permission manually
3. Retry connection

#### Service crashes:
Check Timber logs for:
```
VpnConnectionService: Failed to establish VPN interface
```

This usually means:
- VPN permission not granted
- Another VPN is running
- Device doesn't support VPN

### 4. **UI Not Updating**

#### State not flowing:
The VPN state flows through:
```
VpnConnectionService (StateFlow)
    ↓
VpnRepositoryImpl (monitoring via reflection)
    ↓
HomeViewModel (collectAsState)
    ↓
HomeScreen (UI)
```

If state isn't updating, check:
1. VpnConnectionService is running: `adb shell dumpsys activity services | grep VpnConnection`
2. Logs show state transitions: `adb logcat | grep "VpnConnectionService"`

---

## 📱 Testing the App

### 1. **Installation**
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. **Manual Testing Checklist**
- [ ] App launches without crash
- [ ] Splash screen appears (2 seconds)
- [ ] HomeScreen shows with "TAP TO CONNECT" button
- [ ] Tapping Connect button requests VPN permission
- [ ] After granting permission, connection starts (yellow ring)
- [ ] After ~2s, connection completes (green ring)
- [ ] Stats card appears showing download/upload
- [ ] Tapping Disconnect button stops VPN
- [ ] Navigation to Servers/Settings/Stats works

### 3. **Verify VPN Tunnel**
When connected, check if VPN is active:
```bash
# Check VPN interface
adb shell ifconfig | grep tun

# Check VPN service
adb shell dumpsys connectivity | grep VPN

# Check traffic routing
adb shell ip route
```

You should see:
- `tun0` interface created
- VPN service listed as active
- Routes showing traffic through 10.8.0.2

---

## 🐛 Debug Mode

### Enable Verbose Logging

In `SynapseGuardApplication.kt`, Timber is already configured for debug builds.

View logs:
```bash
adb logcat -s "SynapseGuard" "VpnConnectionService" "HomeViewModel"
```

### Key Log Messages

**Successful connection flow:**
```
D/MainActivity: VPN permission granted
D/VpnRepositoryImpl: Starting VPN connection to Germany - Frankfurt
D/VpnConnectionService: Building VPN interface...
D/VpnConnectionService: VPN interface established successfully
D/WireGuardHandler: WireGuard: Connected successfully
D/VpnRepositoryImpl: VPN connected successfully
```

**Error flow:**
```
W/VpnRepositoryImpl: VPN permission check failed
E/VpnConnectionService: Failed to establish VPN interface
```

---

## 🚀 Current Working Status

✅ **Fully Functional (v1.0):**
- UI: All 6 screens (Splash, Home, Servers, Settings, Stats, Split Tunneling)
- VPN Service: Tunnel establishment with VpnService.Builder
- Kill Switch: setBlocking(true) implementation
- Split Tunneling: addDisallowedApplication() support
- DNS Protection: Custom DNS servers (1.1.1.1, 1.0.0.1)
- Statistics: Real-time byte counting
- Notifications: Foreground service with status

⚠️ **Simulated (Prototype):**
- Encryption: WireGuard handshake and packet encryption
  - Real tunnel exists
  - Real packet forwarding
  - Encryption layer simulated (ready for native library)

---

## 📞 Need More Help?

### Report Issues
If you encounter bugs not listed here:

1. **Collect logs:**
   ```bash
   adb logcat > crash_log.txt
   ```

2. **Check git status:**
   ```bash
   git log --oneline -5
   git status
   ```

3. **Describe the issue:**
   - What screen/action caused the issue?
   - Error messages shown?
   - Device/Android version?
   - Logs showing the error?

---

## ✨ Latest Fixes

### Commit `e06ef14` (Latest)
- **Fixed:** Frozen screen on HomeScreen
- **Added:** ServerInfoCard component
- **Added:** StatsCard component
- **Result:** App now launches and displays properly

### Commit `f67a4f0`
- **Added:** Core VPN functionality
- **Added:** WireGuard protocol handler
- **Added:** Real traffic statistics
- **Added:** Kill Switch & Split Tunneling backend

### Commit `0eca1cc`
- **Updated:** README documentation
- **Marked:** Version 1.0 features as complete
