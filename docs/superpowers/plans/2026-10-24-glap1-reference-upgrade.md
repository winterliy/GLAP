# GLAP1 Reference Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand GLAP1 with safe, opt-in gameplay controls and operational/privacy settings while preserving Paper defaults and plugin compatibility.

**Architecture:** Kotlin owns an immutable validated configuration snapshot; a small Java bridge exposes it to patched Minecraft/Paper Java. Focused behavior patches consume that bridge, and `/glap` reports/reloads settings while update-check results return to the server thread.

**Tech Stack:** Java 25, Kotlin, Paper 26.3, Bukkit YAML configuration, Minecraft patch sources.

**Spec:** `docs/superpowers/specs/2026-10-24-glap1-reference-upgrade-design.md`

## Global Constraints

- Preserve vanilla/Paper defaults for every new gameplay option.
- Keep Paper/Spigot plugin behavior and scheduler contracts unchanged.
- Do not claim Folia support or add regionized ticking.
- Do not disable update checks or Paper metrics by default.
- Publish only a complete, validated configuration snapshot.
- User asked for implementation; do not add or run tests unless separately requested.

## Review Focus

- Invalid YAML or unsupported config versions must not change active settings; inspect the reload path for atomic publication.
- Existing `glap.yml` files must retain configured values and comments when defaults are inserted.
- Update fetching must not touch Bukkit senders from a worker thread; inspect executor handoff.
- Metrics status must distinguish configured and active states because activation is restart-only.
- New gameplay settings must be gated at the narrow behavior point and default to current Paper behavior.

---

### Task 1: Validated Kotlin configuration and Java bridge

**Files:**
- Create: `glap-server/src/main/java/dev/glap/GlapSettings.java`
- Create: `glap-server/src/main/java/dev/glap/GlapSettingsLoader.java`
- Create: `glap-server/src/main/kotlin/dev/glap/KotlinGlapSettingsLoader.kt`
- Create: `glap-server/src/main/resources/META-INF/services/dev.glap.GlapSettingsLoader`
- Modify: `glap-server/src/main/java/dev/glap/GlapConfig.java`
- Modify: `glap-server/src/main/java/dev/glap/GlapCommand.java`
- Modify: `glap-server/src/main/kotlin/dev/glap/GitHubUpdateChecker.kt`

**Interfaces:**
- `GlapSettings` is the immutable Java transport record populated only by the Kotlin-owned parser/validator; it contains the config version, all gameplay flags, `updatesEnabled`, and `paperMetricsEnabled`.
- `GlapSettingsLoader.load(File): GlapSettings` is the Java SPI implemented by `KotlinGlapSettingsLoader` and registered through `META-INF/services`.
- `GlapConfig` exposes static read-only accessors for each setting, `load(): boolean`, `settings(): GlapSettings`, `metricsEnabledAtStartup(): boolean`, and version/status formatting helpers.

- [x] Parse all settings into a candidate snapshot; reject malformed YAML, invalid value types, and config versions newer than supported before publication.
- [x] Preserve existing values, insert missing defaults/comments, and create a recoverable backup before rewriting an older config schema.
- [x] Publish the candidate with one atomic reference update; leave the previous snapshot active on any parse/validation failure.
- [x] Gate remote update checks on `updates.enabled`; continue showing local version and release URL when disabled.

### Task 2: Curated gameplay options

**Files:**
- Modify: `glap-server/minecraft-patches/sources/net/minecraft/world/item/BucketItem.java.patch`
- Modify: `glap-server/minecraft-patches/sources/net/minecraft/core/dispenser/DispenseItemBehavior.java.patch`
- Modify: `glap-server/minecraft-patches/sources/net/minecraft/world/item/enchantment/Enchantment.java.patch`
- Modify: `glap-server/src/main/java/dev/glap/GlapConfig.java`

**Interfaces:**
- Read the `allowWaterPlacementInEnd()`, `dispensersPlaceAnvils()`, and `allowInfinityAndMendingTogether()` bridge accessors from Task 1.

- [x] Permit water placement in the End only when configured, retaining vanilla behavior otherwise.
- [x] Add anvil placement to dispenser behavior only when configured, matching normal block-placement and item-consumption semantics.
- [x] Allow Infinity and Mending compatibility only when configured, preserving all other enchantment exclusivity checks.

### Task 3: Commands, update threading, and metrics privacy

**Files:**
- Modify: `glap-server/src/main/java/dev/glap/GlapCommand.java`
- Modify: `glap-server/src/main/java/dev/glap/GlapVersionFetcher.java`
- Modify: `glap-server/minecraft-patches/sources/net/minecraft/server/dedicated/DedicatedServer.java.patch`

**Interfaces:**
- `/glap status` reports config version, update-check state, configured metrics state, active-at-startup metrics state, and restart requirement.
- Use Paper's server executor to deliver `/glap version` results after asynchronous fetch completion.

- [x] Add `/glap status` and tab completion; retain `/glap`, `/glap version`, and `/glap reload` behavior.
- [x] Marshal async version-check results back to the server thread before accessing the sender.
- [x] Gate only `PaperMetrics.startMetrics()` with `privacy.paper-metrics-enabled` and report restart-only changes accurately.

### Task 4: User-facing documentation

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`

- [x] Document the complete generated `glap.yml`, defaults, reload/restart semantics, `/glap status`, update opt-out, Paper metrics scope, and explicit lack of Folia support.
- [x] Record the reference-inspired feature set without suggesting that upstream code or broad performance patches were copied.

### Task 5: Build integration review

**Files:**
- Inspect: generated Minecraft patch application/build configuration

- [x] Review the final diff for patch format, Java/Kotlin API consistency, defaults, and compatibility boundaries.
- [x] Run `./gradlew :glap-server:compileJava :glap-server:compileKotlin`; do not add or run tests.
