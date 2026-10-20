# Manual Test Plan

Requirements: one Android phone (API 29+) with developer options/USB debugging enabled, a
personal Wi-Fi router you administer, and 1–2 other devices on that same network that you own or
have explicit authorization to test against (e.g. a laptop, a smart speaker).

## 1. Dashboard
- [ ] Launch the app. All 9 dashboard cards render with the cyberpunk dark theme (neon
      cyan/green/purple accents).
- [ ] "Wi-Fi Status" card reflects actual connection state; toggle Wi-Fi off/on in system
      settings and confirm the card updates on return to the screen.

## 2. Wi-Fi Analyzer
- [ ] With Location permission not yet granted, open Wi-Fi Analyzer: SSID/BSSID show as
      unavailable with a stated reason; no crash.
- [ ] Grant Location permission via the in-app prompt. SSID now shows your real network name
      (confirm it matches what's shown in Android's own Wi-Fi settings).
- [ ] Local IPv4, gateway, subnet/CIDR, and DNS servers all populate and match what you see in
      your router admin page / `ip addr` on another device on the same network.
- [ ] Link speed, signal (dBm), and frequency populate with plausible values (e.g. 2412–5825 MHz
      range for 2.4/5GHz).
- [ ] Turn off Wi-Fi entirely: screen reflects "not connected" without crashing.

## 3. Network Scanner
- [ ] Default start/end IP range is pre-filled and contained within your subnet.
- [ ] Attempt to edit the end IP to something clearly outside your subnet (e.g. `8.8.8.8`) and
      start a scan — it's rejected with an explanatory error, not attempted.
- [ ] Start a scan within your subnet. Progress bar and "scanned/total" update live.
- [ ] At least one known device (e.g. the phone's own gateway, or a laptop you started pinging
      from) appears in results with a plausible IP, response time, and discovery method.
- [ ] Cancel a scan mid-way — it stops promptly (within ~1-2s), doesn't hang the UI.
- [ ] Re-run the scan — previously found devices persist in Device Inventory and get a fresh
      "last seen" time; a device that's now off/unreachable is marked unresponsive, not deleted.

## 4. Device Inventory
- [ ] Search box filters by IP, hostname, MAC, and nickname.
- [ ] Open a device, set a nickname, save — it appears in the list and survives app restart.
- [ ] Export CSV and Export JSON both produce a file via the system share sheet; open the shared
      file and confirm it has correct, well-formed rows/fields for every known device.
- [ ] Remove a device from the detail screen — it disappears from the list.

## 5. Packet Capture Lab
- [ ] Read the scope disclosure card before starting; confirm it clearly states the no-forwarding
      limitation.
- [ ] Tap Start — the system VPN consent dialog appears (first time). Accept it.
- [ ] A persistent "Packet Capture Lab active" notification appears.
- [ ] Open another app that makes network calls (e.g. a browser) — some of its traffic triggers
      packets visible in the capture list with real-looking addresses/ports/sizes (note: per the
      documented limitation, that other app's requests themselves may stall/fail while capture
      is on — that's expected).
- [ ] Protocol filter chips (ALL/TCP/UDP/DNS/OTHER) correctly narrow the list.
- [ ] A DNS query (e.g. visiting a new domain in a browser) shows up with a plausible hostname
      in the DNS filter.
- [ ] Pause, then Resume — packet count stops incrementing while paused, resumes after.
- [ ] Stop — notification clears, stats are saved and visible on **Security Reports** as a
      session with correct packet/byte/protocol totals.
- [ ] Confirm no screen anywhere shows decoded HTTP bodies, passwords, or cookie values — only
      header-level metadata.

## 6. MITM Security Lab
- [ ] ARP and DNS spoofing walkthrough steps render with their educational notes; confirm the
      "simulation only, never executed on a real network" banner is visible.
- [ ] Tap "Check now" on the Defensive Monitor — completes without error.
- [ ] Change your router's DHCP-assigned gateway, or connect to a different Wi-Fi network
      entirely and back, then return to this screen — a `GATEWAY_IP_CHANGED` or `DNS_CHANGED`
      event appears in the log with correct before/after values.
- [ ] "Clear log" empties the event list.

## 7. Security Reports
- [ ] Device/session/event counts match what you saw in the other screens.
- [ ] Capture session list shows the session(s) created in step 5 with correct totals.

## 8. Device Control
- [ ] Request pairing with a simulated device by name — it appears as `REQUESTED`.
- [ ] Approve it — state becomes `ACTIVE`, allowlisted command buttons appear.
- [ ] Send `PING` — "Last result: OK: pong" appears; a corresponding audit log entry is added.
- [ ] Revoke pairing — command buttons disappear; sending a command via the API (not possible
      from UI once revoked, confirmed by code review) is rejected per
      `CompanionAgentManagerTest`.
- [ ] Audit log lists every pairing/command event in reverse-chronological order.

## 9. Privacy & Data
- [ ] Screen text accurately describes what is/isn't stored (spot-check against the actual Room
      schema in `data/db/`).
- [ ] "Delete everything" clears Device Inventory, Capture Sessions, and the MITM event log;
      confirm by revisiting those screens.

## 10. Permissions
- [ ] Deny Location permission permanently (via Android's "Don't ask again") — Wi-Fi Analyzer
      still functions (degraded: no SSID/BSSID) rather than crashing.
- [ ] Deny Notifications permission (Android 13+) — Packet Capture Lab still starts/stops
      correctly; only the visible notification is missing.
- [ ] Revoke the VPN permission from Android Settings while a capture is running — the app tears
      down cleanly (`onRevoke()`), no crash, state returns to Stopped.

## 11. Regression after device/OS variety (if available)
- [ ] Repeat sections 2–5 on at least one additional Android version (e.g. Android 10 and Android
      14) to confirm permission-model differences (API 26 location gate vs API 33 nearby-Wi-Fi,
      API 34 foreground-service types) are both handled.
