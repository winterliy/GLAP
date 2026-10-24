# GLAP1 reference-based upgrade design

## Intent

Grow GLAP1 from three gameplay switches into a small, recognizable server
customization layer while keeping Paper plugin compatibility and vanilla-like
defaults. Keep GLAP-specific policy in Kotlin and keep the Java patches that
connect Minecraft/Paper code to GLAP small.

## Current state

GLAP currently has `glap.yml` switches for Enderman block pickup, Creeper block
damage, and farmland trampling. `/glap reload` reads the file, `/glap version`
checks the latest release, and startup checks for updates. The Java patch points
read mutable static booleans. The update-check command completes its future by
calling the sender from the worker thread.

## Reference lessons

- [Purpur](https://github.com/PurpurMC/Purpur) treats gameplay customization as
  a first-class fork feature and exposes many entity, block, and server options.
- [PlazmaBukkit](https://github.com/PlazmaMC/PlazmaBukkit) combines configurable
  gameplay with stability/performance work and privacy controls around
  diagnostics. Its broader optimizations should not be copied without evidence
  that they improve GLAP.
- [Folia](https://github.com/PaperMC/Folia) gains parallel ticking through a
  region ownership model, scheduler APIs, and strict plugin thread-safety rules.
  Its README explicitly warns that the model breaks many Paper plugins.
- [Spigot/CraftBukkit](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/spigot/browse)
  remains the compatibility baseline for the Bukkit-facing behavior.

## Approaches considered

1. **Copy broad fork configuration.** This would add many options quickly, but
   it would make GLAP a shallow Purpur clone and create a large patch-maintenance
   burden.
2. **Adopt Folia's regionized ticking.** This would be a major server rewrite,
   require region/entity schedulers and thread checks throughout the API, and
   break many existing Paper plugins. It does not fit a safe first upgrade.
3. **Build a curated GLAP feature and safety layer (selected).** Add a few
   concrete options inspired by Purpur, add operational/privacy controls
   inspired by Plazma, and apply Folia's safe-state lessons to GLAP-owned data
   without claiming Folia support. Preserve Paper/Spigot behavior by default.

## Scope

### Configuration and Kotlin core

- Add a Kotlin-owned immutable settings model, parsed and validated before it is
  published to the running server.
- Keep a small Java bridge for patched Java sources. Replace direct mutable
  fields with read-only accessors backed by an atomic snapshot, so reload cannot
  expose a partly updated configuration to ticking threads.
- Add `config-version`, preserve comments/default insertion, back up the old
  file before a schema migration, and leave the active configuration untouched
  when parsing or validation fails.
- Keep Paper-facing entry points in Java where required by the current Eclipse
  compiler setup; keep configuration policy and update-check logic in Kotlin.

### Gameplay feature pack

Retain the current three options and add these opt-in settings, all defaulting
to Paper/vanilla behavior:

- `gameplay.allow-water-placement-in-end: false`
- `gameplay.dispensers-place-anvils: false`
- `gameplay.allow-infinity-and-mending-together: false`

Each setting gets a focused Java patch at the behavior point, a Korean comment
in `glap.yml`, and README documentation. Do not import Purpur's full option
surface.

### Operations and privacy

- Add `/glap status` to show the GLAP version, config version, and whether
  update checks and Paper metrics are enabled.
- Add `updates.enabled` and `privacy.paper-metrics-enabled` settings. Both retain
  current Paper/GLAP behavior by default; operators can opt out. When update
  checks are disabled, both automatic checks and `/glap version`'s remote
  request stop; the command still shows the local version and release link.
  Update checks remain bounded and run off the tick thread.
- `privacy.paper-metrics-enabled` controls only Paper's built-in
  `PaperMetrics.startMetrics()` collection. It does not claim to disable Spark,
  JMX, operating-system telemetry, or plugin metrics. It takes effect on server
  restart. `/glap status` shows both the configured and currently active values
  and marks when a restart is needed.
- Make `/glap reload` report validation errors clearly and publish a complete
  settings snapshot only after the whole file succeeds.
- Marshal `/glap version`'s result from the HTTP worker back onto the Paper
  server thread before messaging the sender; the worker must not access the
  sender. Do not advertise Folia compatibility unless a supported
  server-owned task-dispatch path is implemented and checked.

### Performance and compatibility

- Apply Folia's lesson about shared mutable state to GLAP-owned settings and
  caches; do not add regionized ticking, mark plugins Folia-supported, or change
  Paper's scheduler contract.
- Do not copy performance patches from Plazma/Pufferfish without a measured
  GLAP bottleneck and a demonstrated improvement.
- Keep Bukkit/Paper APIs and plugin loading behavior unchanged.

## Non-goals

- Full Folia support or regionized multithreading.
- A full Purpur/Plazma configuration clone.
- Changing existing defaults, disabling Paper metrics or GLAP update checks by
  default, or adding unmeasured performance tweaks.

## Acceptance criteria

- Existing three settings retain their behavior and reload semantics.
- New settings are documented, default to Paper behavior, and each maps to one
  narrowly scoped patch.
- Invalid YAML or an unsupported config version cannot partially alter live
  settings; migrations preserve a recoverable backup.
- `/glap status` accurately reports effective settings and restart-only options.
- Kotlin implementation remains behind Java-compatible entry points for
  Minecraft/Paper patches.
- The project builds against the pinned Paper version and the README describes
  the new options and the lack of Folia support.
