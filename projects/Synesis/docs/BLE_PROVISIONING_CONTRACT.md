# BLE WLAN Provisioning Contract

Agreed 2026-09-28 between Aurora (firmware, sensor controller `iot-rtd-sensor`,
NimBLE-Arduino) and Tommy (Synesis app, Android BLE central) for Alex' Auftrag
#888: set WLAN SSID + password on the sensor controller via Bluetooth. v1 scope:
SSID + password only, no other configuration.

## Device

- BLE peripheral, advertising name `iot-rtd-sensor` (same as LAN OTA hostname).
- Advertising (confirmed by firmware): service UUID as complete 128-bit UUID
  list in the main payload; the device name sits in the scan response. The
  app filters scans by service UUID.

## GATT layout

| Name     | UUID                                     | Properties      | Format                          |
|----------|------------------------------------------|-----------------|---------------------------------|
| Service  | `bbf1ab36-e36e-4b35-a238-e33e32684d8f`   | -               | -                               |
| SSID     | `e2ee8fe3-eda6-4b0c-8795-5d127191a83b`   | Write           | UTF-8, max 32 bytes             |
| Password | `64f853c5-3007-4986-8f0d-1442969bdf42`   | Write           | UTF-8, max 64 bytes             |
| Apply    | `15b55d7c-c1ec-4e21-ab83-56da84ca9dbf`   | Write           | 1 byte `0x01` = store (NVS) + reconnect |
| Status   | `ef3f6a85-2f63-490f-9803-bb29ba25cf64`   | Read + Notify   | UTF-8 status string (below)     |
| StoreInterval | `bdc0591c-2f3a-47c8-9d89-0288d52a6d0d` | Read + Write | uint16 little-endian, seconds |

Writes use write-with-response; the app writes sequentially and waits for each
response before the next write.

## Status grammar (read + notify)

- `idle`
- `connecting`
- `connected:<ssid>:<ipv4>` — SSID may contain `:`; the app takes the LAST
  colon-separated segment as the IP, so v1 guarantees IPv4 only.
- `failed:<code>` — stable code from the firmware enum
  `{auth, notfound, timeout, invalid}`, mapped from `WiFi.status()`; the app
  maps these to localized messages and shows unknown codes as-is.
  The app additionally reserves `internal` as a fallback for an uncategorized
  failure and maps it to a generic error message; the firmware never emits it.

Implementation note: the app parser tolerates a trailing NUL byte in status
payloads defensively (historic `NimBLECharacteristic::setValue(const char*)`
behavior appended the string terminator); the firmware must still emit clean,
NUL-free payloads.

The firmware validates byte lengths and rejects invalid input with `failed:`
instead of truncating or overflowing. `connecting` must terminate within 30 s
with either `connected:...` or a `failed:` status.

## Store interval (firmware v3+, added 2026-10-02)

- `StoreInterval` controls how often the controller writes a measurement into
  its database: uint16 little-endian seconds, allowed range 5–3600.
- A valid write is applied immediately and persisted in NVS (survives
  reboots); no Apply command is involved.
- Invalid writes (payload not exactly 2 bytes, or value outside 5–3600) are
  silently ignored. The app therefore verifies every write by reading the
  characteristic back and expects the previous value when the write was
  rejected.
- Independent from the on-device chart ring buffer, which keeps a fixed 60 s
  cadence regardless of this value — a shorter interval must not silently
  shrink the ring buffer's covered timespan.
- The characteristic is optional: on firmware without it (pre-v3) the app
  still provisions WLAN and disables the interval section.

## App flow

1. Scan, filter by service UUID / name `iot-rtd-sensor`.
2. Connect, discover services.
3. Read Status (current state, e.g. `connected:<ssid>:<ip>` after boot), and
   read StoreInterval if the characteristic is present (shows the current
   interval; optional, non-blocking).
4. Subscribe to Status notifications (CCCD).
5. Write SSID, then Password, then Apply `0x01`.
6. Wait for notifications: `connecting` then `connected:...` or `failed:...`.
7. If the link drops mid-flow, reconnect and read Status (fallback).
8. StoreInterval changes (independent of the WLAN flow): write the uint16
   value, then read it back and compare — equal means applied, different
   means the device ignored the write.

## Firmware requirements (confirmed 2026-09-28)

- MTU: firmware calls `NimBLEDevice::setMTU(247)`; the app requests MTU 247
  after connect. Characteristic `max_len` pinned to 32/64 bytes.
- Long writes: NimBLE-Arduino handles prepare/execute transparently in the
  host stack (code-secured; the empirical 64-B write test is part of the
  joint hardware test after the one-time USB reflash).
- Connection: no reboot on Apply; `connecting` is re-notified at most every
  3 s during the reconnect window (heartbeat). Connection parameters
  unchanged — to be revisited only if the live test shows drops.
- Open networks: an empty password is accepted (`WiFi.begin(ssid, "")`).

## Security model (v1)

No pairing/auth: BLE range (physical proximity) is the barrier, matching the
project threat model (the DB bridge is likewise unauthenticated,
reachability-based). Hardening (e.g. passkey pairing) is a possible later step.
