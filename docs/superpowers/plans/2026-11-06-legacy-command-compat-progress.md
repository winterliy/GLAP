# Legacy command compatibility progress

Plan: `docs/superpowers/plans/2026-11-06-legacy-command-compat.md`

## Task 1 — translator and `/blockdata`

- Status: complete; committed and pushed as `8b337ecff`.
- Red/green: focused suite first failed to compile because the translator was missing; after implementation, `./gradlew :glap-server:test --tests dev.glap.GlapRuntimeFindingsTestSuite --no-configuration-cache` passed.
- Packaging: `./gradlew :glap-server:assemble --no-configuration-cache` and `./gradlew :glap-server:createPaperclipJar --no-configuration-cache` passed.
- Runtime: isolated GLAP 26.3 Paperclip server started with EULA accepted. `forceload add 0 0`, `setblock 0 100 0 chest`, legacy `blockdata 0 100 0 {CustomName:"Smoke Test"}`, and modern `data get block 0 100 0` succeeded; retrieved block data contained `CustomName: "Smoke Test"`. Server stopped cleanly.
- Reviewer decision: first release is an intentionally scoped useful subset, documented as such. `/xp` omitted target maps to self only for entity command sources; console/RCON retain their normal missing-target error.

## Task 2 — `/entitydata`

- Status: implementation and local verification complete; feature commit/push pending.
- Red/green: focused tests first failed because `entitydata` was not recognized; after implementation, `./gradlew :glap-server:test --tests dev.glap.GlapRuntimeFindingsTestSuite --no-configuration-cache` passed.
- Packaging: `./gradlew :glap-server:createPaperclipJar --no-configuration-cache` passed.
- Runtime: isolated server accepted `summon pig 0 100 0 {CustomName:"Old Pig"}`, translated `entitydata @e[type=minecraft:pig,limit=1] {CustomName:"Legacy Bacon"}`, and `data get entity @e[type=minecraft:pig,limit=1] CustomName` returned `"Legacy Bacon"`. Server stopped cleanly.
