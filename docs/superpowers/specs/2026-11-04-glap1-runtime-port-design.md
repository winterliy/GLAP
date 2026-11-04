# GLAP1 runtime port design

## Goal

Adapt selected logic from Kommand, Tap, paper-sample-complex, paper-sample-lib, kotlin-plugin, and Kode into GLAP1, then rebase the GLAP Paper baseline from `1b9bf5c8e` to Paper `5050b0f3339ab37730744c604939996bf2e0e628` (exactly 26 commits ahead).

## Design decisions

- Keep the existing Paperweight patchset architecture. Do not copy upstream Paper sources into the repository.
- Follow the port order requested by the user: (1) a typed command DSL based on Kommand concepts, (2) narrowly scoped packet and fake-entity APIs inspired by Tap, and (3) API/NMS implementation separation informed by the sample projects.
- GLAP is a conventional single-main-thread server. Bukkit/world/entity access and packet sends that touch a live player/entity are main-thread-only. Public entry points either assert the server thread or provide an explicit `execute`/`submit` handoff; asynchronous work may compute immutable data only and must hand results back to the server thread.
- Keep Java-facing Paper/NMS integration in Java and GLAP internal logic in Kotlin, using a Java API boundary where Paper or Java code must call into Kotlin.
- Keep API and implementation separated; implement the single supported Minecraft version directly rather than importing Tap's broad multi-version/reflection machinery.
- Treat Kode as a GLAP-owned runtime-provider plugin artifact, preserving `provides: [Kotlin]` and plugin classloader dependency semantics. Build and release it beside the server jar from the GLAP repository; the standalone `winterliy/kode` repository is frozen. Do not embed the plugin in the server jar because Paper plugin dependency resolution requires a plugin provider.
- Preserve existing GLAP behavior and keep Paper patches small. Do not port unrelated helpers, Folia support, unsafe async Bukkit access, or known flawed fake-projectile range logic.

## Proposed surface

- Kommand concepts: command tree, typed argument nodes, permissions, execution handlers, suggestions, and usage/help generated from the tree. Keep command registration compatible with GLAP's current Bukkit command registration unless Paper's Brigadier surface yields a smaller and safer bridge.
- Tap concepts: API-level fake entity identity/state and lifecycle (spawn, metadata/equipment update, teleport, remove), backed by a version-specific packet adapter. Scope initial implementation to features GLAP needs and document which state must be mutated on the main thread.
- API/core/NMS split: stable public API interfaces in GLAP API, generic lifecycle/validation in core, direct version-specific packet construction in the server implementation. Avoid reflection where the pinned implementation is known.
- Kode: shared Kotlin stdlib/reflection, coroutines, and serialization runtime provided by a plugin with hard dependency/classloader visibility; fail startup if any required runtime package is missing; no application database framework.

## Validation

- Regenerate/rebase Paperweight patches against the selected Paper commit and resolve all conflicts.
- Compile the API and server distribution, verify command registration and Kode artifact wiring, and inspect the final patch diff for accidental upstream churn.
- Do not claim runtime behavior has been verified unless the server is launched and the relevant API paths are exercised.

## Exclusions

Do not use `regions`, `twipe`, `travis-ci-buildtools`, `over-immersion`, or `time-kill`. Do not attempt Folia compatibility. Do not port old repo versions verbatim; adapt only useful logic.
