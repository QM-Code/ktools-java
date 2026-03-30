# Java Conventions Refactor

## Mission

Refactor `ktools-java/` so that `kcli` and `ktrace` preserve the shared ktools
behavior while reading like deliberate Java libraries instead of C++ APIs
ported into Java syntax.

Assume a fresh agent should perform a full audit and complete refactor pass
across both components.

## Scope

This brief applies to:

- `ktools-java/kcli/`
- `ktools-java/ktrace/`
- `ktools-java/README.md`

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `README.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- local docs that describe parser or trace behavior
- the matching C++ docs and tests for the same behavior

There is no existing `projects/updates.md` in this workspace at the time this
brief was written.
If substantial work lands, create one only if it is clearly useful.

## Core Principle

Preserve semantics, not foreign API texture.

Preserve:

- the parser contract
- inline-root semantics
- alias behavior
- trace selector semantics
- help and error behavior
- demo contract behavior

Do not preserve:

- C++-style naming that sounds unnatural in Java
- raw transport concepts in the public API if Java has a better abstraction
- utility-class-heavy architecture just because the C++ code split that way
- collection exposure that ignores Java interfaces

## Assignment Model

A fresh agent should assume:

- both `kcli` and `ktrace` need full public API review
- internal code organization is in scope
- docs and tests are part of the job
- compatibility matters at the behavior level, not at the naming level

## Public API Refactor Goals

`kcli` should look like a Java parser library, not a transliterated C++ one.

Prefer:

- Java-idiomatic method names
- obvious value/config objects where they improve clarity
- `List`, `Map`, and interface-shaped returns over concrete collection leakage
- explicit exception choices that make sense for Java callers
- class and package structure that aligns with responsibility

`ktrace` should look like a Java tracing/logging library.

Prefer:

- Java-style configuration APIs
- clear separation between library-facing trace-source types and runtime logger
  types
- names that read naturally in Java call sites
- public value/config types that read as Java objects instead of translated
  structs

## Internal Refactor Goals

Review and refactor:

- giant utility classes
- static helper sprawl
- translated control flow that ignores Java object boundaries
- mutable data bags with unclear ownership
- package structure that mirrors C++ file layout instead of Java package logic

Prefer:

- cohesive classes
- package-private boundaries for internal details
- clear value-model objects
- internal layering that reflects Java responsibility, not C++ file history

## Demo, Test, And Docs Expectations

- demos are contract checks
- bootstrap, SDK, and executable ownership should be explicit
- docs must point at the current file/package structure directly
- important parity cases must be tested explicitly

## Validation

Use the local workspace and component docs to determine the exact Java build and
test flow.
At minimum, the final validation should include:

- documented `kbuild` flows
- Java library tests
- any demo validation flow documented by the component
- direct demo smoke runs for documented entrypoints

## Done When

- a Java reviewer would find the public APIs natural
- remaining non-Java public API texture is explicitly justified
- internal package/class structure is coherent
- demos and docs are explicit and easy to follow
- key parity behaviors are covered directly
- validation passes

## Final Checklist

- read all required docs
- audit the entire public API surface
- identify port-shaped naming and structure
- refactor toward Java conventions without changing behavior
- update tests and docs to match
- validate the workspace
