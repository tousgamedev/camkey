# CLAUDE.md — camkey

Context for Claude Code. Read this before making any change.

## What this project is

A lightweight Minecraft mod that adds a camera keyframe & playback system for
cinematic recording sessions. A producer captures named camera positions, then
plays them back as a smooth camera move.

- `/camkey add <sequence>` — capture current camera position + rotation as a keyframe appended to `<sequence>` (creates it if new, resumes it if it exists; either way `<sequence>` becomes the active sequence)
- `/camkey use <sequence>` — switch the active sequence to an existing `<sequence>` without capturing a keyframe; fails gracefully if `<sequence>` doesn't exist (points at `/camkey add` instead, since `use` never creates)
- `/camkey play <sequence> <seconds>` — play the sequence back, interpolated, over the given duration
- `/camkey delete <sequence>` — remove the most recently added keyframe from `<sequence>` (repeatable to pop more than one, in order)
- `/camkey list` — read-only; shows existing sequences and their keyframe counts (needed since the active sequence is in-memory only and doesn't survive a world reload)
- Sequences must persist across world reloads
- Every successful action reports a clear chat confirmation, not just failures
  (e.g. "keyframe captured (3 total)", "now using intro", "playing intro (10s)",
  "cancelled", "deleted last keyframe (2 remaining)") — several triggers are
  keybinds with no other UI, so silent success is not acceptable.

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
3. Playback stays available both ways: `/camkey play <name> <seconds>` for
   full control (replaying an older sequence, a custom duration), and two
   playback keybinds as shortcuts that play the active/last sequence at a
   sensible default duration (config-driven) — **Toggle Playback (Preview)**
   shows the on-screen "Playing... (Press X to Cancel)" hint, **Toggle
   Playback (Record)** plays identically but suppresses that hint, so a
   take intended for actual recording never has it baked into the footage.
   Both **toggle**: press to start playback, press again to cancel it
   (whether mid-move or frozen on the final keyframe after it finished —
   see below); either keybind cancels an in-progress playback regardless of
   which one started it. The `/camkey play` command does not toggle, and
   always shows the hint; invoking it while playback is already running
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
| `playback`    | Playback state + per-frame camera control. Owns interpolation. |
| `interpolation` | Pure math: lerp, smoothstep/easing, angle wrapping. No Minecraft dependencies — unit-testable. |
| `command`     | Brigadier command tree. Thin: parses input, delegates, reports results. |

Rules:
- Commands contain no business logic — they call into capture/storage/playback.
- Interpolation must be swappable (easing is an interface/strategy, not an if-chain).
- Design for later extension: rotation easing, multiple simultaneous cameras. Don't build them, don't block them.

## Decisions

- [x] Client-side vs server-side command registration — **client-side**. Needed for
  the client render camera (incl. spectator free-fly); multiplayer sync is out of
  scope so the lack of server-side visibility/permissions doesn't matter.
- [x] Storage: per-world `SavedData` (NBT) vs JSON file in the world folder —
  **JSON file**. Backable up and hand-editable independently of the world save;
  see `readme.txt`. `storage` stays behind an interface regardless.
- [x] Keyframe capture UX: command-only vs keybind-only vs hybrid —
  **hybrid**. `/camkey add <name>` starts/names a sequence; a keybind appends
  to whichever sequence is active. See "User flow" above.
- [x] Rotation interpolation: in transit vs snap-after-arrival — **in
  transit**, same per-frame `t` as position.
- [x] Duration split: equal per segment vs weighted by distance — **weighted
  by distance** (arc-length parameterization). Required to satisfy the
  constant-velocity requirement: equal-per-segment makes speed vary with
  keyframe spacing. Per-keyframe manual timing (the "real" animation-tool
  approach) was considered but rejected as too much added command surface
  for the time box — the spec's `/camkey play <seq> <seconds>` takes one
  total duration, not per-keyframe times.

- [x] End-of-playback behavior: stay at last keyframe vs return to start —
  **stay**. The view freezes on the final keyframe until a playback keybind
  is pressed again to cancel. Implementation note (revised after reading
  decompiled `Camera.java`): `Camera.setPosition`/`setRotation` are
  `protected` and no NeoForge event exposes camera position at all, so a
  true camera-only override isn't possible through public API. Position is
  instead driven by moving the player entity itself each tick
  (`Entity.moveTo`), rotation is reinforced every render frame via
  `ViewportEvent.ComputeCameraAngles` for mouse-jitter immunity, and a
  temporary Spectator-gamemode switch (restored after) prevents collision/
  fall damage in place of the "entity never moves" protection a true
  camera-only override would have given for free — see
  `CamKeyPlaybackHandler`.
- [x] Player input during playback: blocked vs cancels playback —
  **blocked**. Movement input is ignored while the camera is overridden
  (the player can't see their real surroundings, so letting them move blind
  would be disorienting and pointless). The playback keybind is the one
  input that acts on playback state: it toggles start/cancel.

No pending decisions — see "User flow" above for how these fit together.

Known constraint (revised after reading decompiled `Camera.java`): position
and rotation only need updating once per **server tick**, not per render
frame — `Camera.setup()` already lerps between each tick's old/new entity
position and rotation using partial tick, the same mechanism vanilla uses
for every entity, so per-tick updates render smoothly for free. The one
exception is rotation specifically, reinforced every render frame via
`ComputeCameraAngles` — not for tick-rate smoothness, but because mouse
look writes to the entity's rotation continuously between ticks and would
otherwise visibly jitter if not overridden on every frame it's displayed.

## Production-readiness requirements

Every command must fail gracefully with a clear chat message, never a stack trace:
- Unknown sequence name
- Sequence with fewer than 2 keyframes
- Zero, negative, or absurd durations
- `play` while already playing
- Corrupt or missing save data on load (log a warning, don't crash the world)

User-facing text goes through translation keys in
`assets/camkey/lang/en_us.json`, not hardcoded strings.

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
