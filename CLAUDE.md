# CLAUDE.md — camkey

Context for Claude Code. Read this before making any change.

## What this project is

A lightweight Minecraft mod that adds a camera keyframe & playback system for
cinematic recording sessions. A producer captures named camera positions, then
plays them back as a smooth camera move.

- `/camkey add <sequence>` — capture current camera position + rotation as a keyframe appended to `<sequence>` (creates it if new, resumes it if it exists; either way `<sequence>` becomes the active sequence)
- `/camkey use <sequence>` — switch the active sequence to an existing `<sequence>` without capturing a keyframe; fails gracefully if `<sequence>` doesn't exist (points at `/camkey add` instead, since `use` never creates)
- `/camkey play <sequence> [duration]` — play the sequence back, interpolated, over the given duration (config default if omitted)
- `/camkey playactive [duration]` — same, for the active sequence; fails gracefully if there is none
- `[duration]` is a number with an optional unit word: none or `second(s)` = seconds, `minute(s)` = ×60 (so the spec's own example `/camkey play intro 10 seconds` works). Unit conversion happens in `command` (`DurationUnit`); everything past it works in seconds.
- `/camkey delete <sequence>` — remove the most recently added keyframe from `<sequence>` (repeatable to pop more than one, in order)
- `/camkey list` — read-only; shows existing sequences and their keyframe counts (needed since the active sequence is in-memory only and doesn't survive a world reload)
- Sequences must persist across world reloads
- Every successful action reports a clear chat confirmation, not just failures
  (e.g. "keyframe captured (3 total)", "now using intro", "playing intro (10s)",
  "cancelled", "deleted last keyframe (2 remaining)") — several triggers are
  keybinds with no other UI, so silent success is not acceptable. One
  deliberate exception: the **Toggle Playback (Record)** keybind suppresses
  its own start/cancel confirmations, since anything in chat would end up in
  the recorded footage. Failures are always reported, even in Record mode.

See "User flow" below for how the command and the keybind work together.

This is a time-boxed (~3–4 hr) technical assessment. Favor a small, clean
foundation over features. Reviewers grade separation of concerns, correct
NeoForge usage, graceful failure, and README clarity.

## Stack (do not deviate)

- Minecraft **1.21.1**
- **NeoForge 21.1.x** — NOT Forge, NOT Fabric, NOT Quilt
- **ModDevGradle** build plugin
- **Java 21**
- **Mojang official mappings**
- Windows dev machine — use `.\gradlew.bat`, PowerShell syntax

Mod ID: `camkey` · Base package: `com.tous.camkey`

## API red flags — stop and correct if you produce any of these

These come from other loaders or older versions and will not compile or will
misbehave on NeoForge 1.21.1:

- Any `net.minecraftforge.*` import (old Forge) — NeoForge is `net.neoforged.*`
- `FMLJavaModLoadingContext` — inject `IEventBus` / `ModContainer` via the mod constructor instead
- `TickEvent.ClientTickEvent` with a `phase` check — use `ClientTickEvent.Pre` / `.Post`
- `@Mod.EventBusSubscriber` — use `@EventBusSubscriber`
- Anything from Fabric (`ClientTickEvents`, `CommandRegistrationCallback`, `fabric.mod.json`)
- APIs from 1.21.2+ (e.g. renamed render/camera methods, removed `bus` parameter on `@EventBusSubscriber`)

If you are not certain a class, method, or event exists in NeoForge 21.1.x,
**say so explicitly** rather than guessing. Name the exact event/class you are
relying on in your explanation so it can be verified.

## User flow (UX)

Hybrid command + keybind, chosen to match how Minecraft camera tools (e.g.
Replay Mod's path editor) already work — a producer is mid-flight framing a
shot, so re-typing a sequence name for every capture would be disruptive:

1. `/camkey add <name>` starts/names a sequence and captures the first
   keyframe. This is the one point where a name is required, and a command
   is the only clean way to supply it (a keybind has no argument slot).
   `/camkey use <name>` is the command-only counterpart that switches the
   active sequence to an existing one *without* capturing — for when you've
   found a sequence via `/camkey list` (e.g. after a world reload reset the
   in-memory active pointer) but aren't yet in position to add to it. `use`
   never creates; only `add` does.
2. A keybind captures another keyframe appended to whichever sequence is
   currently **active** (the one started in step 1). No GUI, no re-typing —
   just "capture here."
2a. A separate delete-last keybind pops the most recent keyframe off the
    active sequence — pressing it repeatedly pops further back, in order.
    `/camkey delete <name>` is the command equivalent for a named sequence,
    for correcting a sequence you're not currently active on. Fails
    gracefully if there's no active sequence, or it's already empty.
3. Playback stays available both ways: `/camkey play <name> [duration]` for
   full control (replaying an older sequence, a custom duration),
   `/camkey playactive [duration]` to play the active sequence without
   retyping its name (a separate literal, not `play [seconds]`, because a
   bare number would be ambiguous with a sequence name), and two
   playback keybinds as shortcuts that play the active/last sequence at a
   sensible default duration (config-driven) — **Toggle Playback (Preview)**
   shows the on-screen "Playing... (Press X to Cancel)" hint, **Toggle
   Playback (Record)** plays identically but suppresses that hint (and its
   start/cancel chat confirmations, never failures), so a take intended for
   actual recording never has them baked into the footage. The default
   duration is `defaultPlaybackSeconds` in `config/camkey-client.toml`
   (`CamKeyConfig`).
   Both **toggle**: press to start playback, press again to cancel it
   (whether mid-move or frozen on the final keyframe after it finished —
   see below); either keybind cancels an in-progress playback regardless of
   which one started it. The `play`/`playactive` commands do not toggle, and
   always show the hint; invoking either while playback is already running
   fails gracefully per the production-readiness requirements rather than
   canceling, since a typed command is a deliberate action, not a quick
   press meant to be hit twice.

"Active sequence" is the one piece of session state this implies — the
`command`/`capture` layer needs to track which sequence (if any) is open for
appending, and the keybind must fail gracefully (clear chat message, not a
silent no-op) if nothing is active.

Rotation interpolates **in transit**, concurrently with position, not after
arrival — see the `interpolation` angle-wrapping note below; a move-then-
snap-look split isn't "smooth" per the spec's requirement.

## Architecture

Separation of concerns is a graded requirement. No god classes.

| Package       | Responsibility                                              |
|---------------|-------------------------------------------------------------|
| `model`       | Plain data: `Keyframe`, `CameraSequence`. No Minecraft side effects. Prefer records. |
| `capture`     | Reads current camera/player state into a `Keyframe`.        |
| `storage`     | Saving/loading sequences. Behind an interface so the format can change. |
| `playback`    | Playback state + per-frame camera control (`PlaybackCamera`, `SpectatorGuard`). Owns interpolation. |
| `interpolation` | Pure math: lerp, smoothstep/easing, angle wrapping. No Minecraft dependencies — unit-testable. |
| `session`     | `CamKeySession`: the active-sequence/playback session state and every user action on it (validation, failure results). No Minecraft dependency — this is the "business logic" commands and keybinds share. |
| `command`     | Brigadier command tree. Thin: parses input, delegates, reports results. |
| `client`      | NeoForge client event glue: keybinds (`KeybindHandler`), the tick/render loop driving playback (`PlaybackDriver`), the playback hint HUD, the per-world session holder, `CommandResult` → chat text. |

Rules:
- Commands and keybinds contain no business logic — they call into `session` (which uses storage/playback) and `capture`.
- Interpolation must be swappable (easing is an interface/strategy, not an if-chain).
- Design for later extension: rotation easing, multiple simultaneous cameras. Don't build them, don't block them.

## Decisions

- [x] Client-side vs server-side command registration — **client-side**. Needed for
  the client render camera (incl. spectator free-fly); multiplayer sync is out of
  scope so the lack of server-side visibility/permissions doesn't matter.
- [x] Storage: per-world `SavedData` (NBT) vs JSON file in the world folder —
  **JSON file**. Backable up and hand-editable independently of the world save;
  files live at `<world>/camkey/<name>.json` (see README.md, "Architecture
  & Key Decisions"). Each file carries `formatVersion` (written/checked by
  `JsonSequenceStorage`, not part of the model); missing = 1, newer than
  supported = refused. `storage` stays behind an interface regardless.
- [x] Keyframe capture UX: command-only vs keybind-only vs hybrid —
  **hybrid**. `/camkey add <name>` starts/names a sequence; a keybind appends
  to whichever sequence is active. See "User flow" above.
- [x] Rotation interpolation: in transit vs snap-after-arrival — **in
  transit**, same per-frame `t` as position.
- [x] Duration split: equal per segment vs weighted by distance — **weighted
  by distance** (arc-length parameterization), so keyframe spacing doesn't
  change speed — equal-per-segment would make the camera rush through long
  segments and crawl through short ones. On top of that, one `Easing`
  (currently `SMOOTHSTEP`) is applied over the whole move for a cinematic
  ease-in/ease-out, so speed is *not* constant at the very start and end;
  `Easing.LINEAR` gives true constant velocity if ever wanted.
  Per-keyframe manual timing (the "real" animation-tool approach) was
  considered but rejected as too much added command surface
  for the time box — the spec's `/camkey play <seq> <seconds>` takes one
  total duration, not per-keyframe times.

- [x] End-of-playback behavior: stay at last keyframe vs return to start —
  **stay**. The view freezes on the final keyframe until a playback keybind
  is pressed again to cancel. Implementation note (revised after reading
  decompiled `Camera.java`): `Camera.setPosition`/`setRotation` are
  `protected` and no NeoForge event exposes camera position at all, so a
  true camera-only override isn't possible through public API. Position is
  instead driven by moving the player entity itself each tick (`setPos`
  plus the entity's previous position — *not* `Entity.moveTo`, see below),
  converting the stored camera/eye position to a feet position. Rotation
  is set every render frame via `ViewportEvent.ComputeCameraAngles`.
  Playback is forced to first person (previous view restored after), since
  keyframes store the camera's position, not the player's. A temporary
  Spectator-gamemode switch prevents collision/fall damage; the original
  game mode is kept in the server player's persistent data so it's restored
  on cancel, on logout, or on the next login after a crash — see
  `PlaybackCamera` and `SpectatorGuard`.
- [x] Player input during playback: blocked vs cancels playback —
  **blocked**. Movement input is ignored while the camera is overridden
  (the player can't see their real surroundings, so letting them move blind
  would be disorienting and pointless). The playback keybind is the one
  input that acts on playback state: it toggles start/cancel.

No pending decisions — see "User flow" above for how these fit together.

Known constraint (revised after reading decompiled `Camera.java` and
`Entity.java`): position only needs updating once per **tick** —
`Camera.setup()` lerps between the entity's previous position (`xo/yo/zo`)
and current position by partial tick every render frame. That only works
if the previous position is left at the start of the tick's stretch of
path: `Entity.moveTo` resets it to the new position, which turns the glide
into a 20 Hz step, so it must not be used. Rotation is different — the
player's view rotation is *not* lerped (`LocalPlayer.getViewYRot` returns
the raw value), so it's sampled from the path at each render frame's
partial tick and applied via `ComputeCameraAngles` (which also stops mouse
look fighting it).

## Production-readiness requirements

Every command must fail gracefully with a clear chat message, never a stack trace:
- Unknown sequence name
- Sequence with fewer than 2 keyframes
- Zero, negative, or absurd durations
- `play` while already playing
- Corrupt or missing save data on load (log a warning, don't crash the world)

User-facing text goes through translation keys in
`assets/camkey/lang/en_us.json`, not hardcoded strings.

## Tests

JUnit 5 (test-only, approved) via ModDevGradle's `neoForge.unitTest`, which
puts Minecraft's libraries (Gson, SLF4J) on the test classpath. Tests live in
`src/test/java` mirroring the main packages and cover the Minecraft-free code
(interpolation, playback timing, `CamKeySession` via an in-memory
`SequenceStorage`, `JsonSequenceStorage` via a temp dir). `.\gradlew.bat
build` runs them.

## Out of scope (do not build)

Particle effects, camera shake, visual polish, multiplayer sync.

## How to work with me

- I'm an experienced Unity developer; my Java is rusty and I'm new to modding.
  When introducing a Minecraft/NeoForge concept, relate it to the Unity
  equivalent if one exists (e.g. server tick ≈ `FixedUpdate`, render frame ≈ `LateUpdate`).
- Keep changes small and scoped to one concern per step. Explain what you changed and why.
- Do not add dependencies or mixins without asking first.
- Do not edit generated or template-filled files (`build/`, `run/`, `neoforge.mods.toml` placeholders).
- After code changes, run `.\gradlew.bat build` and report the result.
- Don't commit — I review and commit through GitHub Desktop.
