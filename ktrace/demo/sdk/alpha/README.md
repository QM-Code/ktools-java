# Alpha Demo SDK

Exists for CI and as a minimal reference for integrating an SDK add-on with the
Java `ktrace` SDK.

This SDK demonstrates the library-side pattern:

- expose `getTraceLogger()`
- build a shared `ktrace.TraceLogger("alpha")` with local channels
- emit with `getTraceLogger().trace(...)` and `getTraceLogger().info/warn/error(...)`
