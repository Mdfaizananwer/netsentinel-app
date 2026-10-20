# Troubleshooting

### Gradle sync fails with "wrapper jar not found" or similar
The binary `gradle-wrapper.jar` isn't included in this source deliverable. Open the project in
Android Studio and accept its offer to regenerate the wrapper, or run (with any local Gradle
install) `gradle wrapper --gradle-version 8.7` from the project root once, then re-sync.

### `SSID` / `BSSID` show "Unavailable"
1. Make sure you granted Location permission when the Wi-Fi Analyzer prompted for it.
2. Make sure the device's **system Location setting** (Settings → Location) is turned on —
   Android withholds SSID/BSSID if it's off, independent of the app's own permission, and the app
   cannot detect or override this.
3. Confirm you're actually connected to Wi-Fi (not just nearby) — `isWifiConnected` must be true.

### Network Scanner finds few or no devices
This is expected on many real networks, not necessarily a bug:
- **Client/AP isolation** (common on guest networks, some mesh systems, and most public Wi-Fi)
  stops devices from reaching each other even though they share an SSID — the scanner can't see
  what it can't reach.
- Devices with their own firewalls, or that are asleep / in a deep power-save state, may not
  respond to any of the scanner's probes.
- Check **Dashboard → Wi-Fi Status** first to confirm your detected subnet looks right; a wrong
  subnet (e.g. a VPN active system-wide) will scan the wrong range.

### Packet Capture Lab: other apps lose internet while capturing
This is the documented limitation, not a bug: this build is capture/metadata-only and does not
relay traffic back out to the internet (see README → Honest limitations). Press **Stop** to tear
down the VPN interface and restore normal connectivity immediately.

### Packet Capture Lab: "Failed to establish VPN interface"
- Another VPN app (or this app's own prior session) may still hold the VPN slot — only one
  VPN/VpnService can be active system-wide. Fully stop other VPN apps and retry.
- On some OEM skins, a device policy (MDM/work profile) can block `VpnService` entirely; this
  will surface the same error and isn't something the app can route around.

### Build fails on `ksp` (Room annotation processing)
Make sure the KSP plugin version in the root `build.gradle.kts`
(`com.google.devtools.ksp:1.9.24-1.0.20`) matches your Kotlin version if you change the Kotlin
version — KSP releases are coupled to a specific Kotlin version.

### App crashes immediately on launch
Check Logcat for the actual stack trace first (`adb logcat | grep -i netsentinel`). The most
common cause after a fresh clone is a stale/partial Gradle sync — try **File → Invalidate Caches
/ Restart** in Android Studio, then a clean rebuild (`./gradlew clean assembleDebug`).

### `adb install` fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
You likely have an older signature of the app installed. Uninstall first:
`adb uninstall com.netsentinel.app.debug`, then reinstall.
