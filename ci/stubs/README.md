# CI compile-only API stubs

These are **not** part of the application. They are minimal, empty-bodied
declarations of exactly the BlackBerry (`net.rim.*`) and MIDP
(`javax.microedition.io.*`) API surface that `src/` references, so that a stock
JDK can **type-check** the application in CI without RIM's proprietary
`net_rim_api.jar`.

- They let CI catch real compile errors (bad signatures, missing methods, type
  mismatches) that the grep-based guardrail cannot.
- They are **never** passed to `rapc` — `build.sh` only compiles `src/`, against
  the real `net_rim_api.jar`.
- Signatures mirror the members the app calls; they are not a complete or
  authoritative model of the RIM API. A green stub compile is a strong smoke
  test, not a guarantee that the real `.cod` links.
