# Changelog

## Unreleased

- Add opt-in End water placement, dispenser anvil placement, and Infinity plus Mending compatibility settings.
- Add atomic configuration reload, schema versioning with migration backups, `/glap status`, update-check opt-out, and Paper Metrics opt-out.
- Marshal `/glap version` responses back to the Paper server thread.

## [1.1.3] - 2026-10-24

### Changed
- Maintenance release with no server changes.

## [1.1.2] - 2026-10-24

### Fixed
- Fix the release workflow: make `printGlapVersion` work with the Gradle configuration cache.

### Changed
- Log a warning on startup when the update checker cannot be loaded, so it is not confused with a failed GitHub lookup.

## [1.1.1] - 2026-10-24

The `v1.1.1` tag was pushed, but its release build failed, so no jar was published. Use 1.1.2.

### Fixed
- Fix the CI build: track `gradle-wrapper.jar` and drop the `.gitkeep` that paperweight read as a patch.
- Fix Eclipse compiler (VS Code Java) errors in Minecraft and Paper sources by spelling out generic types. Behavior is unchanged.

### Changed
- Load the Kotlin update checker through `ServiceLoader`, so Java code never names a Kotlin class.

## [1.1.0] - 2026-10-24

The `v1.1.0` tag was pushed, but its release build failed, so no jar was published. Use 1.1.1.

### Added
- Add `glap.yml` with `gameplay.enderman-picks-up-blocks`, `gameplay.creeper-destroys-blocks`, and `gameplay.farmland-trampling`.
- Add `/glap version` to check for updates and `/glap reload` to reload `glap.yml`.
- Check the latest GLAP release on startup and in `/version` instead of Paper's update check.
- Build every push and publish a release for every `v*` tag with GitHub Actions.
- Open an issue every Monday when Paper has new commits.

### Changed
- Restructure the project as a Paper fork like Purpur: pin Paper with `paperCommit` and keep only GLAP patches in `glap-api` and `glap-server`.
- Keep Paper-facing GLAP classes in Java under `glap-server/src/main/java` and GLAP-only logic in Kotlin under `glap-server/src/main/kotlin`.
- Rewrite the README for GLAP1.

## [1.0.0] - 2026-10-24

### Added
- Start GLAP1 from Paper for Minecraft 26.3 (upstream commit `1b9bf5c8e`).

### Changed
- Show the server brand as GLAP1 in `/version`, the console, and the jar manifest.
