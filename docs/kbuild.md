# `kbuild` In `ktools-java`

The Java workspace currently uses the shared `kbuild` command model rather than
a checked-in Java-local `kbuild/` tree.

## Current Status

- the workspace expects `kbuild` on `PATH`
- batch orchestration and build flow should stay compatible with the shared
  `kbuild` repo
- the Java workspace pages document the current usage model, not a separate
  implementation
