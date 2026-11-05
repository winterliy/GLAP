# Legacy 1.12.2 Command Compatibility

## Intent

Let players and operators use selected Minecraft 1.12.2 command forms on the current 26.3 server. Translation happens at the server command execution boundary, after Bukkit command preprocess events and before Brigadier executes the command.

## Goals

- Translate common 1.12.2 command syntax into the current vanilla command tree.
- Preserve the normal command source, permission checks, events, and modern validation.
- Parse command arguments without splitting SNBT or quoted text at embedded spaces.
- Add each command migration independently, with tests and a pushed commit.

## Non-goals

- Emulate a 1.12.2 network protocol or accept a 1.12.2 client connection.
- Recreate removed commands whose behavior cannot be represented faithfully by current commands without a dedicated implementation.
- Convert numeric legacy block, item, potion, or entity IDs, or rewrite historical NBT schemas in the first release.
- Change current command behavior or bypass permission checks.

## Approaches

1. **Translate at the command execution boundary (selected).** A small Kotlin translator recognizes known legacy roots, tokenizes balanced arguments, and rewrites supported syntax before the existing dispatcher parses it. This covers player, console, command block, and RCON command execution through the central `Commands.performCommand` path, and preserves vanilla permissions and validation.
2. **Intercept protocol packets.** This can alter network input earlier, but couples command compatibility to packet versions and signed-command handling. The goal is command syntax compatibility, so packet interception adds risk without value.
3. **Register aliases in the dispatcher.** This works for a renamed root with identical arguments, but cannot express syntax migrations such as `/blockdata` to `/data merge block` or old argument ordering.

The user selected the command-boundary translator. A translator only changes a command when a complete supported legacy form is recognized; otherwise input proceeds unchanged to Brigadier.

## First Release Scope

- `/blockdata <x> <y> <z> <snbt>` → `/data merge block <x> <y> <z> <snbt>`
- `/entitydata <target> <snbt>` → `/data merge entity <target> <snbt>`
- `/xp <amount>[L] [target]` → `/experience add <target-or-self> <amount> points|levels`
- `/effect <target> clear` and legacy effect-give forms → current `/effect` syntax
- `/replaceitem` entity/block forms when using textual item IDs and no legacy damage value or item NBT payload

Numeric legacy IDs, historical item damage values, and incompatible NBT schemas remain unsupported and are listed as such in the compatibility documentation.

## Constraints

- Translation runs after `PlayerCommandPreprocessEvent`; plugins see and can cancel the original command.
- Translated commands are parsed again with the original command source so vanilla permission and execution behavior remains authoritative.
- The translated source string must be used with its parse results so Brigadier argument ranges remain valid.
- Commands with signable chat arguments are not translated in this initial scope; add them only with explicit signed-chat tests.
- If `/xp` omits its target, `@s` is used for entity command sources; non-entity sources keep the normal missing-argument error.
- Unknown or malformed command forms are passed unchanged to the current dispatcher.

## Validation

- Unit tests cover nested SNBT, quoted whitespace, optional prefixes, malformed input, unknown roots, and each supported command form.
- Runtime smoke tests execute translated commands through the server console and verify their modern effects.
- Build and the focused GLAP test suite must pass before each feature commit is pushed.
