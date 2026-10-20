# NetSentinel Privacy Policy (in-app reference copy)

This is the source text rendered by the in-app **Privacy & Data** screen.

**What is collected:** NetSentinel collects data only about networks and devices you actively
scan or capture on, and only when you initiate that action (starting a scan, starting a packet
capture, enabling the defensive monitor). This includes: IP addresses, hostnames (when
resolvable), MAC addresses (when exposed by the OS), response times, packet header metadata
(addresses/ports/protocol/size, and plain-text DNS query names), and companion-agent pairing/audit
records.

**What is never collected:** packet payload contents, HTTP bodies, passwords, session cookies,
authentication tokens, or any other application data. The Packet Capture Lab's parser is
structurally incapable of reading beyond the transport header plus a DNS query name — see
`capture/PacketParser.kt`.

**Where data is stored:** exclusively in this app's private local Room database on your device
(`netsentinel.db`), inside Android's app-sandboxed storage. `android:allowBackup="false"` and
explicit data-extraction-rule exclusions mean this data is not included in Android's cloud or
local device-transfer backups either.

**What leaves the device:** nothing, automatically. Data leaves only when you explicitly use an
Export/Share action (CSV/JSON export from Device Inventory), which hands the file to Android's
system share sheet — you choose the destination.

**Deletion:** the Privacy & Data screen's "Delete everything" button permanently removes all
locally stored devices, capture sessions, network-change events, and audit log entries.
Uninstalling the app also removes all of its data per standard Android app-sandbox behavior.

**Third parties:** NetSentinel makes no network calls to any Anthropic, Google Analytics,
advertising, or other third-party service. The only network traffic it initiates is the scan
probes and DNS/reachability checks you trigger yourself, targeted only at your own local subnet.
