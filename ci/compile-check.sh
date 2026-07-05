#!/bin/sh
# Type-check the BlackBerry application against the compile-only API stubs in
# ci/stubs/ using a stock JDK, producing .class files under build/ci-classes.
#
# This verifies the source COMPILES and type-checks; it does NOT produce a .cod
# (that needs rapc + net_rim_api.jar via build.sh). The stubs are for the
# compiler only and are never given to rapc.
#
# Usage:  sh ci/compile-check.sh
set -e

OUT=build/ci-classes
LIST=build/ci-sources.txt

rm -rf "$OUT"
mkdir -p "$OUT"

find ci/stubs src -name '*.java' > "$LIST"
echo "Type-checking $(wc -l < "$LIST" | tr -d ' ') source files against ci/stubs ..."

# Plain javac (no --release): default source/target works on JDK 8 through 25.
javac -d "$OUT" @"$LIST"

echo "OK: application type-checks. Classes in $OUT (verification only, not a .cod)."
