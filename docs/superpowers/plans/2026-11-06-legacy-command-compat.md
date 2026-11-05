# Legacy 1.12.2 Command Compatibility Implementation Plan

> **For agentic workers:** Use `superpowers:executing-plans` to implement this plan task by task. Each task has its own RED→GREEN test cycle and commit/push.

**Goal:** Translate a useful initial set of Minecraft 1.12.2 command forms into Minecraft 26.3 commands.

**Architecture:** A Kotlin translator recognizes a legacy root and supported argument shape at the central `Commands.performCommand` execution boundary. It returns a rewritten command string, which the existing Brigadier dispatcher reparses with the same command source. Unknown and malformed forms remain unchanged.

**Tech Stack:** Java, Kotlin, Brigadier, Paperweight, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-06-legacy-command-compat-design.md`

## Global Constraints

- Do not intercept or emulate the Minecraft network protocol.
- Keep modern dispatcher permissions, command events, and validation authoritative.
- Preserve SNBT and quoted argument text, including embedded whitespace.
- Do not claim numeric legacy ID, historical item damage, or old NBT schema support.
- Commit and push each independently tested command feature to `feature/legacy-command-compat`.
- Publish GLAP 3.1.0 only after all planned command features and runtime checks pass.

## Review Focus

- SNBT with nested compounds/lists and spaces must remain one unchanged argument.
- Quotes and escaped quotes must not corrupt token boundaries.
- Incomplete or malformed legacy input must not execute a partial rewrite.
- Bukkit preprocess changes and cancellations must happen before translation.
- Reparse must retain the original command source and current permission checks.

---

### Task 1: Translation engine and `/blockdata`

**Files:**
- Create: `glap-server/src/main/kotlin/dev/glap/LegacyCommandCompatibility.kt`
- Modify: `glap-server/src/minecraft/java/net/minecraft/commands/Commands.java` via a Minecraft source patch
- Create: `glap-server/src/test/kotlin/dev/glap/LegacyCommandCompatibilityTest.kt`
- Modify: `glap-server/src/test/java/dev/glap/GlapRuntimeFindingsTestSuite.java`

**Interface:** `LegacyCommandCompatibility.translate(input: String, sourceHasEntity: Boolean = false): String?`; null means no supported rewrite. The parser records top-level argument spans while treating quoted strings and balanced `{}`, `[]` as nested.

- [x] Add tests for `/blockdata` rewrite, nested SNBT with spaces, malformed nesting, and unchanged unknown input.
- [x] Run the focused suite and observe expected failures.
- [x] Implement the minimal tokenizer, rewrite, and central dispatch reparse.
- [x] Run the focused suite and verify it passes.
- [x] Build the server and perform a console smoke test for block data merge.
- [x] Commit with the required co-author trailers and push the feature branch.

### Task 2: `/entitydata`

**Files:**
- Modify: `LegacyCommandCompatibility.kt`
- Modify: `LegacyCommandCompatibilityTest.kt`

**Consumes:** Task 1 `translate(input: String): String?` and token spans.
**Produces:** `/entitydata <target> <snbt>` → `/data merge entity <target> <snbt>`.

- [x] Add target and SNBT preservation tests, including selectors.
- [x] Run RED, implement, and run GREEN.
- [x] Run the focused suite and runtime smoke test.
- [x] Commit with trailers and push the feature branch.

### Task 3: `/xp`

**Files:**
- Modify: `LegacyCommandCompatibility.kt`
- Modify: `LegacyCommandCompatibilityTest.kt`

**Produces:** Legacy point/level forms map to `/experience add`. An omitted target maps to `@s` only when the original command source is an entity; console/RCON with no entity retains current Brigadier error behavior.

- [x] Add tests for points, `L` levels, negative amounts, selectors, and omitted targets.
- [x] Run RED, implement, and run GREEN.
- [x] Verify emitted syntax reaches current dispatcher (explicit target with no players returns “No player was found”; console omitted-target input reports incomplete `experience add`).
- [x] Commit with trailers and push the feature branch.

### Task 4: `/effect`

**Files:**
- Modify: `LegacyCommandCompatibility.kt`
- Modify: `LegacyCommandCompatibilityTest.kt`

**Produces:** Legacy clear and give forms map to the current `/effect clear` and `/effect give` forms. Numeric effect IDs remain unsupported.

- [x] Add tests for clear, duration/amplifier/particle options, zero-duration clearing, and infinite duration.
- [x] Run RED, implement, and run GREEN.
- [x] Confirm emitted command forms match the current dispatcher command tree.
- [x] Commit with trailers and push the feature branch.

### Task 5: `/replaceitem` limited syntax

**Files:**
- Modify: `LegacyCommandCompatibility.kt`
- Modify: `LegacyCommandCompatibilityTest.kt`

**Produces:** Entity/block forms map to `/item replace`; convert `slot.*` to current slot names. Translate only named item IDs without legacy damage or NBT suffixes.

- [x] Add tests for entity, block, count, and slot normalization, plus unchanged unsupported metadata/NBT forms.
- [x] Run RED, implement, and run GREEN.
- [x] Verify emitted forms against the current dispatcher in an isolated runtime smoke test.
- [ ] Commit with trailers and push the feature branch.

### Task 6: Release and documentation

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, and `gradle.properties`

- [ ] Document supported forms and unsupported numeric IDs/NBT migrations.
- [ ] Bump `glapVersion` to `3.1.0` and add release notes.
- [ ] Run `applyAllPatches`, focused tests, and the Paperclip build.
- [ ] Run final isolated-server smoke tests and inspect the release manifest.
- [ ] Commit and push the release preparation.
- [ ] Merge the tested feature branch to `26.3`, tag `v3.1.0`, and verify the GitHub release asset and workflow.
