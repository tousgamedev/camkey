# CLAUDE.md — camkey

Context for Claude Code. Read this before making any change.

## What this project is

A lightweight Minecraft mod that adds a camera keyframe & playback system for
cinematic recording sessions. A producer captures named camera positions, then
plays them back as a smooth camera move.

- `/camkey add <sequence>` — capture current camera position + rotation as a keyframe appended to `<sequence>`
- `/camkey play <sequence> <seconds>` — play the sequence back, interpolated, over the given duration
- Sequences must persist across world reloads

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

## Decisions (pending — do not assume; ask)

- [ ] Client-side vs server-side command registration
- [ ] Storage: per-world `SavedData` (NBT) vs JSON file in the world folder
- [ ] Duration split: equal per segment vs weighted by distance
- [ ] End-of-playback behavior: stay at last keyframe vs return to start
- [ ] Player input during playback: blocked vs cancels playback

Known constraint: camera motion must be interpolated per **render frame**
(using partial tick), not per server tick. 20 TPS teleporting is visibly
jerky on a recording and counts as a hack around the engine.

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
