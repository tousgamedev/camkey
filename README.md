CamKey
======

A lightweight NeoForge mod for Minecraft 1.21.1 that adds a camera keyframe
& playback system — the kind of tool a Production Associate would use to
set up a cinematic camera move for a recording session.

Built for the CatFace / Aphmau Production "Lead Production Engineer"
technical assessment.

What This Is
==========
A producer names a camera move (a "sequence"), flies to a spot, presses a
key to capture it as a "keyframe" (a position + the direction you're
looking), flies to the next spot and captures another, and so on. Once a
sequence has at least two keyframes, it can be played back — the camera
smoothly glides between every captured spot, over however many seconds you
choose, instead of jump-cutting between them. Sequences are saved
automatically and are still there the next time the world is loaded.

**Demo:** [camkey demo.mp4](camkey%20demo.mp4) — capturing keyframes and
playing them back in-game.

How to Build & Run
==========
Requires **Java 21**.

```
.\gradlew.bat build        # compiles, runs the unit tests, and packages the mod jar
.\gradlew.bat test         # runs just the unit tests
.\gradlew.bat runClient    # launches a dev Minecraft client with the mod loaded
```
(use `./gradlew` instead of `.\gradlew.bat` on macOS/Linux)

The packaged mod jar is written to `build/libs/`. To use it in a regular
(non-dev) Minecraft install, drop that jar into the `mods/` folder of a
NeoForge **1.21.1 / 21.1.x** instance.

How to Use It
==========
The four keybinds below are **unbound by default** — bind them yourself
under **Options → Controls → Key Binds → CamKey** before you start, so they
can't collide with anything you already use.

| Keybind | What it does |
|---|---|
| Capture Keyframe | Captures your current position/look as a keyframe, appended to whichever sequence is active |
| Delete Last Keyframe | Removes the most recently captured keyframe (press again to remove further back) |
| Toggle Playback (Preview) | Plays/cancels the active sequence, with an on-screen "Press X to Cancel" hint |
| Toggle Playback (Record) | Identical playback, but with no on-screen hint and no start/cancel chat messages — for the take you're actually recording (errors are still shown) |

The playback keybinds, and `play`/`playactive` without a duration, use a
default of 10 seconds. To
change it, edit `defaultPlaybackSeconds` in `config/camkey-client.toml` in
your game directory (created on first launch).

| Command | What it does |
|---|---|
| `/camkey add <name>` | Starts (or resumes) a sequence by name, capturing a keyframe immediately, and makes it the active sequence |
| `/camkey add` | Captures a keyframe into the active sequence — the command version of the Capture Keyframe keybind |
| `/camkey use <name>` | Makes an existing sequence active *without* capturing — e.g. to resume one after a world reload |
| `/camkey play <name> [duration]` | Plays any saved sequence, regardless of what's currently active — over exactly `duration` if given, otherwise the default duration |
| `/camkey playactive [duration]` | Same, for the active sequence, so you don't have to retype its name |
| `/camkey delete <name>` | Removes the most recently captured keyframe from a named sequence |
| `/camkey delete` | Removes the most recently captured keyframe from the active sequence — the command version of the Delete Last Keyframe keybind |
| `/camkey list` | Lists every saved sequence and how many keyframes each has (flags any that failed to load, e.g. a corrupted file, instead of hiding or ignoring them) |

A duration is a number of seconds, optionally followed by a unit:
`/camkey play intro 10`, `/camkey play intro 10 seconds`, and
`/camkey play intro 1.5 minutes` all work (`second`/`seconds`/`minute`/
`minutes`). The longest allowed playback is 1 hour.

Typical session: `/camkey add intro` → fly to the next spot → **Capture
Keyframe** → repeat → **Toggle Playback (Preview)** to check it → **Toggle
Playback (Record)** once you're ready to actually capture footage.

During playback you're automatically switched to Spectator mode (and
switched back afterward) so the camera move can't be interrupted by
collision or fall damage, and normal movement/attack input is suppressed
so it can't fight the scripted path. Either playback keybind cancels it at
any point, mid-move or after it's finished.

Playback is also always viewed in **first person**, even if you were in
third person (F5) — your previous view is restored when it ends. A keyframe
stores where the *camera* was, not where your player was: capture in third
person and the keyframe is the over-the-shoulder camera position, which
playback then views from directly. Watching it in third person would pull
the camera back behind that point a second time (and put your own player
model in the shot), so F5 is ignored until playback ends.

Assumptions
==========
The brief leaves a few things open. These are the calls I made, so it's
clear what the tool does in each case:

- **"Named keyframe" means a named *sequence*.** In `/camkey add intro`,
  `intro` is the name of the camera move being built. Each `add` (or
  Capture Keyframe press) adds one more stop to it, in the order captured.
  Individual keyframes don't have their own names. They're only ever used
  as steps in a sequence, so naming each one would be extra typing for no
  benefit.
- **Durations are in seconds unless you say otherwise.** The brief's own
  example is `/camkey play intro 10 seconds`, so a plain number means
  seconds, the word `seconds` is optional, and `minutes` is accepted too
  (`/camkey play intro 2 minutes`). If you leave the duration out, a
  default is used (10 seconds, changeable in the config file).
- **The duration is for the whole move, not each step.** `10 seconds`
  means the camera goes from the first keyframe to the last in 10 seconds.
  Longer stretches between keyframes get a bigger share of that time, so
  the camera keeps the same pace instead of rushing through long gaps.
- **Durations are capped at 1 hour.** Zero, negative, or longer than an
  hour is treated as a typo and rejected with a message, rather than
  starting a playback that ends instantly or effectively never.
- **"Smoothly" includes a gentle start and stop.** The move eases in at
  the start and out at the end, like a real camera operator would, rather
  than starting and stopping at full speed. (The brief lists easing as a
  bonus; linear motion is a one-line change.)
- **When playback ends, the camera holds on the last shot.** It stays there
  until you press a playback key to cancel, rather than snapping back to
  where you started. That avoids a jarring cut at the end of a take and
  gives the person recording time to stop the recording cleanly.
- **A sequence needs at least two keyframes to play.** One keyframe is a
  single position, and there's nothing to move between.
- **"Camera position" means what you see, not where your character is
  standing.** Keyframes capture the camera's viewpoint, and playback shows
  exactly that view.
- **You're in Creative mode.** This is a filming tool, and filming is done
  in Creative. When playback ends, your character is left where the camera
  stopped. In Creative that's harmless. In Survival you could fall or end up
  inside a block if the last keyframe was mid-air or inside a wall
  (Spectator mode is only applied while the camera is moving).
- **Single-player only.** Per the brief, multiplayer is out of scope. The
  automatic Spectator switch and per-world saving rely on the game running
  its own local world.

Architecture & Key Decisions
==========
The codebase is split into small packages, each with one job:

| Package | Responsibility |
|---|---|
| `model` | Plain data — `Keyframe`, `CameraSequence`. No Minecraft dependency, fully unit-testable. |
| `interpolation` | Pure math — lerp, easing, angle wrapping. No Minecraft dependency either. |
| `capture` | Reads the live camera into a `Keyframe`. |
| `storage` | Saves/loads sequences as JSON, behind an interface so the format could change later. |
| `playback` | The arc-length timeline and playback clock, plus the per-frame camera control that applies them to the game (`PlaybackCamera`, `SpectatorGuard`). |
| `session` | `CamKeySession` — which sequence is active, what's playing, and every user action's rules and failure messages. No Minecraft dependency; commands and keybinds both call into it. |
| `command` | The Brigadier `/camkey` command tree. Thin: parse, call the session, report. |
| `client` | NeoForge event glue — keybinds, the tick/render loop driving playback, the on-screen hint, and turning results into chat text. |

A few decisions worth calling out (fuller reasoning + the full decision log
live in `CLAUDE.md`, written as a running design log for this project):

- **JSON over NBT for storage.** Each sequence is a plain file at
  `<world folder>/camkey/<name>.json` that can be backed up, copied between
  worlds, or hand-edited in a text editor if a capture needs a manual tweak
  — none of which a binary NBT save would allow without extra tooling.
  Each file starts with a `formatVersion`, so a future layout change can
  upgrade older files instead of misreading them (a file from a *newer*
  version of the mod is refused rather than guessed at).
- **Command *and* keybind, not just one.** Starting/naming a sequence needs
  a command (a keybind has no way to type a name), but re-typing a name for
  every single keyframe while mid-flight framing a shot would be
  disruptive — so capturing/deleting/playing all also have a keybind that
  acts on whichever sequence is currently "active."
- **Camera speed is weighted by distance between keyframes, not split
  evenly.** Splitting a fixed duration evenly across keyframes makes speed
  vary with how far apart they are. Weighting each segment's share of the
  total time by its distance means keyframe spacing has no effect on speed.
  The move as a whole then eases in and out (smoothstep) for a cinematic
  start and stop; easing is a swappable strategy, so a strictly
  constant-speed (linear) move is a one-line change.
- **The camera move is actually driven by moving the player, not a
  "pure" camera override.** Minecraft's `Camera` class only exposes
  position/rotation setters as `protected`, and NeoForge has no event that
  exposes camera position at all — only rotation (`ComputeCameraAngles`).
  The sanctioned way to move the camera smoothly is to move the entity
  it's tracking (the player) and let Minecraft's own per-frame
  interpolation do the smoothing, same as it already does for every other
  entity. The player is switched to Spectator mode for the duration so
  this can't cause fall damage or collide with anything, and switched
  back the instant playback ends.
- **The rules are unit-tested without the game running.** Because the
  session, timeline, interpolation, and storage code don't depend on live
  game state, `src/test` covers them directly — e.g. distance-weighted
  timing, the shortest-way rotation across 180°, every "fail gracefully"
  case, and corrupt/old/newer save files — using JUnit 5 via ModDevGradle's
  unit-test support.
- **Playing a sequence never changes which one you're actively editing.**
  See "Design Notes" below.
- **A corrupted sequence file is reported honestly, not hidden or silently
  replaced.** `/camkey list` still shows it (flagged), and `add`/`use`/
  `play`/`delete` all distinguish "no sequence by that name" from "a file
  exists but couldn't be read" — critically, `add` on a corrupted sequence
  fails instead of silently creating a fresh empty one over it, which
  would otherwise destroy whatever was recoverable the next time it saved.

Known Limitations / What I'd Do With More Time
==========
These were discussed and deliberately scoped out to keep the foundation
small and focused on the minimum requirements, rather than things that
were missed. None are blocked by the current architecture — `storage`,
`interpolation`, and `playback` are built behind interfaces specifically
so these stay additive:

- **Dual storage (NBT + JSON)** — writing to both, with JSON treated as an
  importable hand-edit override on load. JSON-only was chosen instead to
  avoid the conflict-resolution logic a dual-write system would need.
- **Per-keyframe manual timing** — letting the producer set each
  keyframe's own duration, the way a real animation tool (Maya, After
  Effects, Blender) would, instead of one total duration auto-split by
  distance. Judged too much added command surface for the time box.
- **Arbitrary keyframe deletion/editing by index** — only the most recent
  keyframe can currently be removed. Editing the middle of a sequence
  would need a way to see indices too, which starts to look like a full
  sequence editor rather than a lightweight capture tool.
- **Jump to sequence start/end** — instantly previewing the first/last
  keyframe (e.g. to line up where one sequence should continue from
  another) without running full playback. Cheap to add later — it's just
  playback with one keyframe and zero duration.
- **Rotation easing independent of position easing** — they currently
  share the same per-frame curve.
- **Multiple simultaneous cameras** — only one active playback is
  implemented, though nothing in the architecture assumes there's only
  ever one.
- **Whole-sequence rename/delete** — only the last keyframe can be
  trimmed; scrapping an entire sequence currently means deleting its JSON
  file by hand.
- **Target-focus keyframes** — a keyframe that locks the camera onto a
  fixed point or entity while moving, instead of a captured rotation
  (e.g. circling a build while keeping it centered). Deferred since it
  touches both `model` and `interpolation`, not just a new command.
- **Bezier curve keyframes for pathing** — a keyframe that calculates
  a curved trajectory based on the previous and next node so the camera
  travels in a series of smooth curves instead of straight lines

Design Notes
==========
- **Playing a sequence doesn't change what you're editing.** If you watch a
  different sequence play back than the one you're currently building,
  your next captured keyframe still goes into the one you started — not
  the one you just watched. This keeps a quick preview from accidentally
  redirecting your work. To switch what you're editing, use the `use`
  command (or just start adding to a new one).

AI-Usage Notes
==========
This mod was built in an extended pair-programming session with **Claude
Code**, Anthropic's AI coding assistant. Roughly: product/design judgment
calls (what the tool should actually do, which tradeoffs mattered, when a
proposed approach was over-engineered for the time box) were mine; Claude
handled translating those decisions into code, researching the exact
NeoForge/Minecraft APIs involved, and drafting documentation. Several
NeoForge APIs (the client camera-override event, how mouse look is
computed, how gamemode is switched from client-side code) were verified
directly against NeoForge's decompiled source rather than assumed, since
getting engine internals wrong silently would be worse than not knowing.

A generalized summary of the corrections made during this project — per
the assessment's request for an example of the AI getting something wrong
and it being caught:

- **Exception misuse for an expected code path** — `CameraSequence
  .withoutLastKeyframe()` initially threw `IllegalStateException` when
  called on an already-empty sequence. Pressing "delete" with nothing to
  delete is a normal, expected user action, not a bug, so using an
  exception for it was flagged as overkill. Reworked to return
  `Optional<CameraSequence>` instead, letting the caller branch on an
  empty result without exception handling.
- **Hidden singleton dependency in `capture`** — `CameraCapture.capture()`
  initially reached into `Minecraft.getInstance().gameRenderer
  .getMainCamera()` internally instead of taking the `Camera` as a
  parameter, burying a dependency on live game state inside a method that
  didn't need to know how to find it. Changed to `capture(Camera camera)`,
  a pure function — the singleton lookup moves to the caller instead.
- **An assumed engine constraint turned out to be backwards** — early on,
  camera motion was assumed to need per-render-frame updates to avoid
  "20 TPS teleporting," and playback was assumed to need a true
  camera-only override with the player entity never moving. Reading
  `Camera.java`'s actual decompiled source showed both assumptions were
  wrong: `Camera.setPosition`/`setRotation` are `protected` with no public
  override event, so moving the camera at all means moving the player
  entity — and Minecraft already smooths every entity's position between
  ticks automatically, so per-tick updates are enough. The design was
  corrected to match reality instead of the original assumption once this
  was found. (A later review found the first implementation still stepped
  at 20 Hz: `Entity.moveTo` resets the "previous position" that smoothing
  relies on, and player rotation isn't smoothed at all. Fixed by setting
  the previous position explicitly and sampling rotation per render frame.)
- **A latent data-loss bug, found through manual testing, not code review**
  — `add()` originally treated "sequence file exists but failed to parse"
  identically to "sequence doesn't exist yet," silently creating a fresh
  empty sequence in its place. Harmless until the next keyframe capture
  saved over the corrupted file, permanently destroying whatever was
  recoverable in it — a real data-loss bug that static review of the code
  alone hadn't caught. Found by deliberately corrupting a saved JSON file
  and testing `list`/`use` against it, then fixed by having storage
  distinguish "missing" from "corrupt" everywhere a sequence is loaded.