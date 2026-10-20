# NetSentinel

Android network reconnaissance & Wi-Fi security toolkit for **networks and devices you own or
are explicitly authorized to assess**. Built as a learning/diagnostics lab, not an attack tool.

> **Authorized use only.** Scanning, capturing packets on, or probing devices/networks you do not
> own or have explicit permission to test may be illegal in your jurisdiction. This app enforces
> local-subnet-only scanning and never performs ARP/DNS spoofing, credential interception, or
> remote control of unpaired devices — see [LIMITATIONS](#honest-limitations--whats-not-implemented)
> below for exactly what is and isn't real.

## Stack

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | 1.9.24 |
| UI | Jetpack Compose + Material 3 | Compose BOM 2024.06.00, Material3 1.2.1 |
| Architecture | MVVM, repository pattern, coroutines/Flow | kotlinx-coroutines 1.8.1 |
| Local storage | Room | 2.6.1 |
| Settings | DataStore Preferences | 1.1.1 |
| Build | Gradle Kotlin DSL + AGP | AGP 8.5.2, Gradle 8.7 |
| Min / Target SDK | Android 10 (API 29) / Android 14 (API 34) | |

No DI framework (Hilt/Dagger) is used — `NetSentinelApp` is a small, explicit service locator.
This keeps the object graph easy to audit for a security-focused codebase.

## Project structure

```
NetSentinel/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/netsentinel/app/
│       │   │   ├── NetSentinelApp.kt          # service locator
│       │   │   ├── MainActivity.kt            # Compose NavHost
│       │   │   ├── navigation/                # route definitions
│       │   │   ├── ui/
│       │   │   │   ├── theme/                 # cyberpunk Material3 theme
│       │   │   │   ├── common/                # shared Compose components, permission helpers
│       │   │   │   ├── dashboard/
│       │   │   │   ├── wifi/                  # Wi-Fi Analyzer
│       │   │   │   ├── scanner/               # Network Scanner
│       │   │   │   ├── inventory/             # Device Inventory + detail
│       │   │   │   ├── capture/                # Packet Capture Lab
│       │   │   │   ├── mitm/                  # MITM Security Lab
│       │   │   │   ├── reports/                # Security Reports
│       │   │   │   ├── privacy/                # Privacy & Data
│       │   │   │   └── control/                # Device Control
│       │   │   ├── data/
│       │   │   │   ├── network/                # WifiInfoProvider, NetworkUtils (subnet/CIDR math)
│       │   │   │   ├── scanner/                # NetworkScanner, HostResult
│       │   │   │   ├── db/                     # Room entities/DAOs/AppDatabase
│       │   │   │   └── repository/             # WifiRepository, DeviceRepository, CaptureRepository, MitmMonitorRepository
│       │   │   ├── capture/                    # PacketCaptureVpnService, PacketParser, models
│       │   │   ├── mitm/                       # MitmSimulator (educational), ArpMonitor (real defensive monitor)
│       │   │   ├── control/                    # Companion-agent pairing protocol + manager
│       │   │   └── export/                     # CSV/JSON exporters
│       │   └── res/                            # themes, strings, xml configs, launcher icon
│       ├── test/                               # JVM unit tests
│       └── androidTest/                        # instrumented tests (device/emulator required)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

## Setup & run

1. **Install Android Studio** (Koala/2024.1 or newer recommended) with an SDK 34 platform and a
   JDK 17 toolchain (Android Studio bundles one).
2. Open the `NetSentinel/` folder as a project (`File → Open`).
3. Let Gradle sync. If prompted, accept SDK license dialogs.
   - The wrapper jar binary isn't included in this deliverable (binary files don't travel well
     through this channel). Android Studio will offer to regenerate it on first sync; if it
     doesn't, run once from a terminal with a local Gradle install: `gradle wrapper --gradle-version 8.7`.
4. Select a device: a **physical Android 10+ phone** is strongly recommended for the Wi-Fi
   Analyzer, Network Scanner, and Packet Capture Lab — emulators have limited/virtualized Wi-Fi
   and VPN behavior that doesn't reflect real hardware.
5. Run the `app` configuration (▶). On first launch to each lab, Android will prompt for the
   relevant permission (Location for SSID/scan, Notifications for the capture foreground
   notification, and a system VPN consent dialog for the Packet Capture Lab).

## Debug APK build instructions

From Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**; the APK lands in
`app/build/outputs/apk/debug/app-debug.apk`.

From the command line (after the wrapper jar is regenerated per step 3 above):

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Running tests

```bash
./gradlew test                 # JVM unit tests (NetworkUtils, PacketParser, Exporters, Scanner, CompanionAgentManager)
./gradlew connectedAndroidTest # instrumented tests — requires a connected device/emulator
```

## Permissions requested, and why

| Permission | Why |
|---|---|
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Read connectivity/Wi-Fi status (not runtime-dangerous). |
| `CHANGE_WIFI_STATE` | Reserved for potential Wi-Fi-state interactions; not used to force reconnects today. |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Required **by Android itself** (not by this app's design) to read SSID/BSSID and Wi-Fi scan results, from API 26+. Requested only when you open the Wi-Fi Analyzer. |
| `NEARBY_WIFI_DEVICES` (API 33+) | Alternative to location for some Wi-Fi APIs on Android 13+; requested with `neverForLocation`. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` | Required to run the Packet Capture Lab's VPN service with a persistent, visible status notification while it's active. |
| `BIND_VPN_SERVICE` (system-granted) | Required by Android's `VpnService` API itself. |

No permission is requested at app launch — each is requested contextually on the screen that
needs it, with an in-UI explanation.

## Honest limitations / what's *not* implemented

This project takes "don't claim a feature works unless it's implemented and tested" seriously.
A few things are intentionally scoped down from a theoretical maximum, with the reasoning
documented in code comments at the relevant file:

- **Packet Capture Lab does not relay traffic back to the real network.** It establishes a real
  TUN interface and parses real packets routed through it, but forwarding captured IP packets
  back out (full userspace NAT/TCP relay) is a large, correctness-critical subsystem that's out
  of scope for this build. Practically: while capture is ON, other apps' traffic routed into the
  VPN pauses rather than reaching the internet. This is disclosed in the lab's own UI before you
  start a capture. See `capture/PacketCaptureVpnService.kt`.
- **MITM Learning Lab performs no real attacks.** The ARP/DNS spoofing "scenarios" are an
  in-memory, fictional three-node simulation with canned sample packets — nothing is ever sent on
  a real network. The one real, working piece is the **defensive monitor**, which watches your
  actual gateway IP/MAC (via `/proc/net/arp`, the same source the scanner uses) and DNS servers
  for unexplained changes. See `mitm/MitmSimulator.kt` vs `mitm/ArpMonitor.kt`.
- **Device Control ships a loopback (simulated) transport, not a real network protocol.** The
  pairing state machine, command allowlist, and audit log are real and tested
  (`control/CompanionAgentManager.kt`), but the transport that would carry commands to a real
  second device over TLS is deliberately not included — a half-built crypto transport is a
  liability, not a feature. `control/CompanionAgentProtocol.kt` documents exactly what a
  production transport (mTLS, Android Keystore-backed keys) would need.
- **IPv6 packets are not parsed** by the Packet Capture Lab in this build (reported as
  unparsed/OTHER); IPv4 is the full path.
- **The network scanner is best-effort.** Client isolation, host firewalls, VPNs, and Android's
  own background-network restrictions can and will hide real devices — the UI says this
  explicitly rather than implying a scan is exhaustive.

## Architecture notes

- **MVVM**: each screen has a `XyzViewModel` exposing `StateFlow`s; screens are stateless
  Composables that collect them. ViewModels are constructed via `viewModelFactory { initializer { } }`
  closures over the `NetSentinelApp` service locator — no DI framework.
- **Repository pattern**: `WifiRepository`, `DeviceRepository`, `CaptureRepository`,
  `MitmMonitorRepository` sit between ViewModels and the data sources (Android system APIs, Room,
  the capture VPN service), so ViewModels never touch `ConnectivityManager`/`WifiManager`/DAOs
  directly.
- **Coroutines/Flow** throughout: the scanner emits `Flow<ScanEvent>` with structured-concurrency
  cancellation (cancelling the collector cancels every in-flight probe), Room DAOs return `Flow`
  for reactive UI, and the Wi-Fi snapshot and defensive monitor are polling `Flow`s.
- **Pure-logic modules are framework-free on purpose**: `NetworkUtils` (subnet/CIDR math),
  `PacketParser` (byte-level header parsing), and the CSV/JSON `Exporters` have zero Android
  dependencies, which is what makes them directly unit-testable under plain JUnit without
  Robolectric.

## Data & privacy

See the in-app **Privacy & Data** screen for the full statement, enforced at the manifest level
(`android:allowBackup="false"`, explicit backup/transfer exclusion rules). Summary: everything
stays in the app's local Room database by default; nothing is uploaded anywhere without you
explicitly using an Export/Share action; packet payload, credentials, cookies, and tokens are
never stored because the parser never reads them in the first place.

See also [TROUBLESHOOTING.md](TROUBLESHOOTING.md) and [TEST_PLAN.md](TEST_PLAN.md).
