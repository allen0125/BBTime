#!/bin/sh
# Build bbtime.cod for BlackBerry OS 4.5+ using RIM's rapc compiler.
#
# Requirements:
#   - A BlackBerry JDE / Component Pack (>= 4.5) that provides `rapc` and
#     `net_rim_api.jar`. rapc is a RIM tool; it is NOT part of this repo and is
#     not available on a generic build box. Install a JDE or Component Pack and
#     point BB_JDE at its root.
#   - A 1.4-compatible JDK on PATH (rapc drives javac + preverify).
#
# Usage:
#   BB_JDE=/path/to/BlackBerry/JDE ./build.sh
#
# Output:
#   build/bbtime.cod   (+ .cso/.jar/.jad siblings) ready to load onto a device
#   or into the BlackBerry simulator.
set -e

: "${BB_JDE:?Set BB_JDE to your BlackBerry JDE / Component Pack root (must contain bin/rapc and lib/net_rim_api.jar)}"

RAPC="$BB_JDE/bin/rapc"
API="$BB_JDE/lib/net_rim_api.jar"
APP="bbtime"
OUT="build"

[ -x "$RAPC" ] || { echo "rapc not found or not executable at: $RAPC" >&2; exit 1; }
[ -f "$API" ]  || { echo "net_rim_api.jar not found at: $API" >&2; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT"

# Gather all sources (rapc wants explicit file arguments).
SOURCES=$(find src -name '*.java')
[ -n "$SOURCES" ] || { echo "no .java sources found under src/" >&2; exit 1; }

echo "Compiling with rapc ($BB_JDE) ..."
"$RAPC" \
  import="$API" \
  codename="$OUT/$APP" \
  -quiet \
  "$APP.rapc" \
  $SOURCES

echo "Built $OUT/$APP.cod"
echo "Install with the BlackBerry Desktop Manager (bbtime.alx) or load via the simulator."
