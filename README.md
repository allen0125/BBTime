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

> **Note on the source language:** this is CLDC 1.1 / MIDP 2.0 (Java 1.3-level).
> No generics, autoboxing, reflection, `String.split`, or `SimpleDateFormat`.
> See [`CLAUDE.md`](CLAUDE.md) before editing.

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

## Limitation: setting the clock

**Stock BlackBerry OS (4.5–7.x) has no public API for a third-party app to set
the system clock** — the device gets time from the carrier network (NITZ). So
`bbtime` cannot silently overwrite the clock on an unmodified OS.

What it does instead: it computes the exact correct time and the drift, displays
them, and — when the clock is off by more than a couple of seconds — pops up the
precise value to enter under **Options → Date/Time**.

`ClockSetter.apply(...)` is the single, documented seam where a signed or
privileged build with a native time-set capability can plug in an actual setter
and have the one-key sync become fully automatic. The app never fakes success:
the status line always reports honestly whether the clock was changed or must be
set manually.

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
