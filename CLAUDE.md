# CLAUDE.md

Guidance for working in this repository.

## What this is

`bbtime` is a network time-synchronisation application for **BlackBerry OS 4.5 and
later** (the classic RIM Java platform, not Android-era BB10). It fetches
authoritative time over several protocols, computes the offset from the device
clock, and drives a one-key "sync now" action plus an optional once-a-day
background check.

Supported time protocols:

- **NTP** — RFC 5905 over UDP/123 (most accurate; uses full T1..T4 offset math)
- **HTTP** — `Date` response header over TCP/80
- **HTTPS** — `Date` response header over TLS/443
- **UDP-TIME** — RFC 868 TIME protocol over UDP/37 (4-byte seconds since 1900)

## Platform constraints (read before editing code)

This is **CLDC 1.1 / MIDP 2.0 + RIM APIs**, compiled with `rapc`. The source
language is Java 1.3-level. The following are **NOT available** and must not be
introduced:

- Generics, enhanced `for`, autoboxing, varargs, annotations, enums
- `java.lang.reflect` (CLDC has no reflection beyond `Class.forName`)
- `String.split(...)`, `String.format(...)`, `StringBuilder`
- `java.text.SimpleDateFormat` (parse/format dates manually via `Calendar`)

Use `Vector`/`Hashtable` (raw), `StringBuffer`, explicit `new Long(...)`, and
indexed loops. Network I/O must run off the UI event thread.

### The clock-setting limitation

Stock BlackBerry OS (4.5–7.x) exposes **no public API for a third-party app to
set the system clock** — the platform sources time from the carrier (NITZ). So
`ClockSetter.apply(...)` returns `UNSUPPORTED` by design, and the UI falls back
to displaying the exact correct time and the measured offset so the user can
adjust it manually. `ClockSetter` is the single integration seam where a
signed/privileged build could implement a native setter. Do not fake success.

## Architecture

```
com.bbtime
├── TimeSyncApp        entry point; GUI vs. "autostart" background mode
├── ui/
│   └── TimeSyncScreen main screen; 'S' key = sync now; live clock; settings
├── net/
│   ├── TimeSource     interface: query() -> TimeResult
│   ├── TimeResult     UTC time + offset + round-trip
│   ├── NtpTimeSource  NTP (UDP/123)
│   ├── UdpTimeSource  RFC 868 TIME (UDP/37)
│   ├── HttpTimeSource HTTP/HTTPS Date header
│   ├── HttpDateParser manual RFC 1123 date parser
│   └── Watchdog       closes a hung datagram connection after a timeout
├── core/
│   ├── TimeSyncManager  picks source, runs query, applies, persists
│   ├── SyncOutcome      result of a sync attempt
│   ├── ClockSetter      best-effort clock write (see limitation above)
│   ├── SyncStore        PersistentStore-backed settings + last-sync state
│   └── TimeFmt          Calendar-based date/time formatting
└── bg/
    └── DailyCheckDaemon resident background app; 24h check loop
```

## Build

`rapc` (from a BlackBerry JDE / Component Pack >= 4.5) is required; it is not
part of this repo and is not installable on a plain CI box.

```sh
BB_JDE=/path/to/BlackBerry/JDE ./build.sh   # produces build/bbtime.cod
```

There is no unit-test harness — the platform APIs (`Connector`,
`PersistentStore`, `net.rim.*`) only exist on-device or in the RIM simulator.
Validate protocol/parsing logic by reading it carefully; validate end-to-end in
the BlackBerry simulator or on a device.

## Conventions

- Keep everything Java 1.3-compatible (see constraints above).
- Network code returns UTC milliseconds since the Unix epoch everywhere.
- Never block the UI event thread on I/O; use a worker `Thread` + `invokeLater`.
