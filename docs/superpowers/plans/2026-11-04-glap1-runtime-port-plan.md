# GLAP1 runtime port and Paper sync plan

## Sequence

1. [x] Pin Paper `paperCommit` to `5050b0f3339ab37730744c604939996bf2e0e628`, apply patches, and resolve the upstream Checkstyle context change.
2. [x] Implement a compact typed command DSL and migrate `/glap`, preserving permissions and completions.
3. [x] Define the main-thread boundary and add client-only entity spawn, movement, metadata, equipment, and removal APIs.
4. [x] Separate GLAP API interfaces from the single-version server/NMS implementation without importing unused multi-version/reflection machinery.
5. [x] Make GLAP the sole source of truth for Kode and publish both artifacts in build and release workflows. Freeze the standalone Kode repository.
6. [x] Read Paperweight, paperweight-examples, PaperTestServer, Mache, DataConverter, and patch-roulette sources.
7. Compile and build both jars; inspect outputs and final diffs. No test suite or runtime server session was run.

## Current baseline

- GLAP branch: `feature/glap1-runtime-port-and-paper-sync` in `/private/tmp/glap1-port`.
- Starting GLAP commit: `14246414610dc2029d8031e637877d954dc00098`.
- Paper target: `5050b0f3339ab37730744c604939996bf2e0e628`.
- Exact forward distance from the corrected existing pin `1b9bf5c8e9cd8afa33117fd74d592247191bbb4a`: 26 commits.
- Kode packaging: a GLAP-owned companion plugin artifact, versioned and released with the GLAP server to preserve Paper plugin classloader dependency semantics.
