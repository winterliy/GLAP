# Changelog

## [1.1.0] - 2026-10-24

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
