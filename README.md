# bbtime

Network time synchronisation for **BlackBerry OS 4.5+** (the classic RIM Java
platform — not BB10/Android).

`bbtime` fetches authoritative time over **NTP, HTTP, HTTPS, or UDP (RFC 868
TIME)**, measures how far the device clock has drifted, and lets you sync with a
single key press. It can also run a once-a-day background check.

---

## Features

- **Four protocols**, selectable at runtime:

  | Protocol   | Transport | Port | Default server        | Accuracy |
  |------------|-----------|------|-----------------------|----------|
  | `NTP`      | UDP       | 123  | `pool.ntp.org`        | ~ms (RTT-corrected) |
  | `HTTP`     | TCP       | 80   | `http://www.google.com/`  | ~1s |
  | `HTTPS`    | TCP/TLS   | 443  | `https://www.google.com/` | ~1s |
  | `UDP-TIME` | UDP       | 37   | `time.nist.gov`       | ~1s |

- **One-key manual sync** — press **`S`** anywhere on the screen (except while
  editing the server field) or use *Menu → Sync now*.
- **Live clock** on screen in both local time and UTC.
- **Daily background check** — an optional resident daemon re-syncs once every
  24 hours if the last sync is stale.
- **Round-trip compensation** — NTP uses the full four-timestamp offset
  calculation; the one-second protocols are corrected by half the measured RTT.

---

## Build

Building requires a **BlackBerry JDE or Component Pack (≥ 4.5)**, which provides
the `rapc` compiler and `net_rim_api.jar`. These are RIM tools and are not part
of this repository (and cannot be installed on a generic Linux/macOS CI box).

```sh
BB_JDE=/path/to/BlackBerry/JDE ./build.sh
# -> build/bbtime.cod
```

`build.sh` compiles every `.java` under `src/` against `net_rim_api.jar` using
the module descriptor `bbtime.rapc`.

> **Windows + JDE step-by-step (中文):** see
> [`docs/BUILD-Windows-JDE.zh-CN.md`](docs/BUILD-Windows-JDE.zh-CN.md) — which
> JDE/JDK to use (JDE 4.5 needs **32-bit JDK 1.5**, *not* 1.7+), install order,
> building in the JDE IDE, wiring the autostart alternate entry point, simulator
> testing, and installing to a device. **Do not** feed `ci/stubs/` to `rapc`.

> **Note on the source language:** this is CLDC 1.1 / MIDP 2.0 (Java 1.3-level).
> No generics, autoboxing, reflection, `String.split`, or `SimpleDateFormat`.
> See [`CLAUDE.md`](CLAUDE.md) before editing.

---

## Continuous integration

`.github/workflows/build.yml` runs on every push/PR. It **verifies the source
only — it does not produce a `.cod`.** Producing a `.cod` needs RIM's proprietary
`rapc` + `net_rim_api.jar` (Windows binaries, including preverify) which cannot
run on hosted Linux runners, so the `.cod` is built locally with the BlackBerry
JDE on Windows (see [Build](#build)). CI has two jobs:

1. **CLDC guardrails** — `tools/check-cldc.sh` fails the build if any
   BlackBerry-incompatible construct (generics, `StringBuilder`, `String.split`,
   `SimpleDateFormat`, reflection, for-each, `enum`, annotations…) creeps into
   `src/`. Run it locally with `sh tools/check-cldc.sh`.

2. **Type-check (stub compile)** — compiles `src/` with a stock JDK against
   minimal API stubs in [`ci/stubs/`](ci/stubs) that declare exactly the
   `net.rim.*` / `javax.microedition.*` surface the app uses. This catches real
   compile errors (bad signatures, missing methods, type mismatches) the grep
   guardrail can't. Run it locally with `sh ci/compile-check.sh`. It emits
   `.class` files for verification only — **not** a `.cod`, and the stubs are
   never given to `rapc`.

---

## Install

**Via BlackBerry Desktop Manager (device):** point Application Loader at
[`bbtime.alx`](bbtime.alx) (it references `bbtime.cod` and targets `Java=1.45` =
OS 4.5). Ensure `bbtime.cod` sits next to the `.alx`.

**Via the simulator:** copy `build/bbtime.cod` into the simulator's
`simulator/` directory (or use the JDE's *Run* button) and boot it.

---

## Usage

1. Launch **bbtime**.
2. Pick a **Protocol** and, if needed, edit the **Server** (URL for HTTP/HTTPS,
   hostname for NTP/UDP).
3. Choose a **Transport** (see below).
4. Press **`S`** to sync now.

The status area shows the source used, the correct time, the measured offset
(e.g. *"device 1.4s slow"*), the round-trip, and the apply result.

### Transport

BlackBerry connection strings often need a transport suffix. Pick the one that
matches how the device reaches the internet:

| Transport   | Suffix              | Use when |
|-------------|---------------------|----------|
| Auto        | *(none)*            | Let the OS choose (works on many carrier data plans) |
| Direct TCP  | `;deviceside=true`  | APN-configured direct TCP/UDP; usually needed for NTP/UDP-TIME |
| Wi-Fi       | `;interface=wifi`   | Device on Wi-Fi |
| BIS/MDS     | `;deviceside=false` | Via BlackBerry Internet/Enterprise Service |

If a sync times out, try a different transport — this is the most common cause
of failure on real devices.

---

## Daily background sync

The daemon (`com.bbtime.bg.DailyCheckDaemon`) runs headless, checks 60 seconds
after boot, then every 24 hours, and re-syncs if the last sync is older than a
day and the **Daily auto-sync** checkbox is enabled.

To wire it up, add an **alternate entry point** to the project so the OS starts
it at boot. In the BlackBerry JDE, on the project's properties:

- **Application → Alternate entry point:** add one for `com.bbtime.TimeSyncApp`
  with **Application argument** `autostart`.
- Mark that entry point **Auto-run on startup = Yes** and
  **Do not display application icon** (it has no UI).

`TimeSyncApp.main` routes the `autostart` argument to the daemon; launching the
icon normally opens the GUI. Both share one persistent settings store, so
whatever you select in the UI is what the daemon uses.

---

## Setting the clock (and when it fails)

`bbtime` sets the system clock with the RIM API
**`net.rim.device.api.system.Device.setDateTime(long utcMillis)`** — a `static
boolean` available since BlackBerry API 3.6.0, i.e. on **OS 4.5+**. So the one-key
sync really does change the device time; it does not just display it.

Two things gate it on a **real device** (neither applies on the simulator):

1. **Code signing.** `Device.setDateTime` is a *controlled* API. Unsigned, it
   throws `ControlledAccessException`; the build must be signed with RIM keys.
2. **Automatic network time.** If **Options → Date/Time** has time set to update
   automatically, the OS overrides your change — switch it to **Manual** first.
   An enterprise **IT policy** can also block time changes.

When the set fails for any of these, the app doesn't fake success: it reports it
on the status line and pops up the exact correct time so you can set it by hand.
`Device.setTimeZone(...)` is only 4.6.0+, so on a 4.5 target the **time zone** is
left to the user; `bbtime` sets the absolute instant (UTC), which is what matters.

---

## Project layout

```
src/com/bbtime/
├── TimeSyncApp.java        entry point (GUI vs. autostart daemon)
├── ui/TimeSyncScreen.java  main screen, 'S'-key sync, live clock
├── net/                    NTP / HTTP(S) / UDP-TIME sources + Date parser
├── core/                   sync manager, persistence, clock seam, formatting
└── bg/DailyCheckDaemon.java  resident 24h checker
```

See [`CLAUDE.md`](CLAUDE.md) for architecture and platform constraints.
