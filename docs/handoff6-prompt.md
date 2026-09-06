# Session prompt — paste this into the new chat

Everything below the rule is the prompt. It is also the record of what the
v2.5.0 pass was asked to do.

---

We're continuing **SafeShade** at `C:\Dev\SafeShade` — an Android app
(Kotlin + Jetpack Compose) paired with an ESP32 BLE wearable. The previous pass
shipped v2.4.0 and is fully committed and pushed.

## Read this first

1. **`docs/handoff6.md` — read all of it before doing anything.** It is
   deliberately self-contained: architecture as it actually is, the working
   style, the environment traps, the Shady inventory, the pitch-deck-versus-app
   matrix, the verified/not-verified ledger, known bugs and the firmware gaps.
2. **`DESIGN.md`** — the design system, current, and authoritative on visual
   decisions.
3. **`CLAUDE.md` is stale in places** (it describes a pre-ViewModel
   architecture) and **`PRODUCT.md` is stale on two points** (it says there is
   no ViewModel layer and that there is no cloud backend). **Trust the code.**
   Neither file is yours — do not edit either.

## Model allocation for this session

- **You, the orchestrator, are Fable 5.1.**
- **All existing refinements and all new UI/UX work → Fable 5.1** (subagents
  included).
- **Everything else → Opus 5**: backend, auth, database, protocol, data
  modelling, build, and analysis.
- Note: `subagent_type: "fork"` ignores the `model` override, so Opus work must
  go to a fresh general-purpose agent with `model: "opus"`, never a fork.

## Ground rules — settled, please don't re-litigate

- **Spawn parallel subagents freely.** Give each one a **hard file scope**, and
  tell it explicitly **not to run any writing `git` command** — you handle
  commits.
- **Commit with the `vX.Y.Z:` prefix, kept in sync with
  `app/build.gradle.kts`.** Long messages that explain *why*, including what was
  wrong before and **what wasn't verified**.
- **Confirm with me before any push.**
- **Do not commit or revert `docs/SafeShadev21/SafeShadev21.ino` or
  `CLAUDE.md`** — both are modified and neither is yours. `PRODUCT.md` is
  untracked and also not yours.
- **Verify on the device, not in a preview.** `adb` is on PATH and my Redmi Note
  10S is usually connected. Nearly every real fault in the last three passes was
  invisible in the code and in Compose previews.
- **Tell me plainly what you did not verify.**
- Update `DESIGN.md` in the same pass as anything it describes, and write
  `docs/handoff7.md` before the session closes.

## The work

### 0. My own list comes first

I'm compiling a further list of UI/UX enhancements, refinements and bugs. **When
it arrives, it jumps the queue.** Until then, start on the items below.

### 1. Shady — substantially more work

- Improve **all existing animations**. `docs/handoff6.md` §5 inventories them:
  9 moods, 12 subtle tap reactions, 14 bold ones, 16 idle stage behaviours.
- **The bottom Shady should be subtly 3D.** Right now it's completely 2D — flat
  Compose `Canvas` primitives. I mean shading and depth in the draw code, not a
  new engine.
- All-new animations, emotions, movements and expressions.
- **Shady should sometimes interact with other things in his world** —
  materials, pets, tech, appliances, nature. Today the only thing he interacts
  with is the ground he stands on.
- Keep the two standing rules: no speech or thought bubbles, and Shady stays
  absent from every emergency surface (`ShadyHost` enforces that by
  construction).

### 2. The adaptive-mode sub-page

The UI, arrangement and spacing are **still terrible**, and **the user should be
able to see much more information about each mode**. That page needs a lot of
work. `docs/handoff6.md` §6 says where it lives and points at pitch-deck slide
13, which already has a per-mode table of algorithm adaptation, UI changes and
priority features — that's the content to surface.

### 3. All-new backend work

- **Real log-in.**
- **Personal and "taking-care-of" profiles** — and please find **better wording**
  than "taking-care-of". Propose options early; the code already has
  `UserRole.GUARDIAN` / `UserRole.COMPANION` and a **Circle** tab to anchor on.
- **An actual database: Supabase**, already installed. **Make a new project in a
  new organisation.** (The Supabase MCP can `create_project` behind a cost
  confirmation but has no create-organisation tool, so I may need to make the
  org in the dashboard — tell me if so.)
- **Emails: Resend** (also installed, I believe — its MCP is not authorised yet,
  so it'll need me).
- **Heart rate, blood oxygen and body temperature.**
- **On-board microphone, on both the phone and the device** — a **30-second
  (customisable) voice recording of the surroundings as evidence collection**.
- **Loud-environment warnings.**
- **Actual emergency phone number caching** (and more) — this is the deck's
  "caches closest emergency services contacts and notifies independently".
- **Smart home integrations.**
- **Cloud tier.**
- **Heatmap.**
- **Family dashboard**, with Apple-style **Memoji** icons to choose from.
- **Mesh relay.**
- **Over-the-air updates.**
- **SafeShade Spark.**
- **A real brand-specific pairing screen** for **SafeShade S1**, **SafeShade 5G**
  and **SafeShade Spark**.

### 4. Recommend me more features

Both kinds: **features the pitch deck promises that the app doesn't have yet**
(`docs/handoff6.md` §7 is a grounded deck-versus-app matrix — start there, and
note the deck rows nobody has claimed yet, like the ERSS 112 JSON alert, 5G
positioning, C-V2X and the Smart City API), **and features that aren't in the
deck at all but would be genuinely useful, impressive additions.**

### 5. The standard all of it is held to

I know the hardware isn't ready. I want the Android app to **mimic the real
deployment production app** — as much as possible actually built now, and
**realistic stubs, placeholders, settings and controls** where real integration
genuinely isn't possible yet. I essentially want to be **completely done with
the app**, so that when the hardware is finished the only thing left is plugging
it in.

One thing to carry from last pass: **a stub must never claim an outcome it
hasn't got.** The one real defect of v2.4.0 was a confirmation tick that
appeared on a send which had failed, because every layer above the repository
discarded the result it was already being handed. A login, a database write, an
email send and an evidence upload can all fail quietly — the app's habit until
now has been to draw the happy path. The app already has `StubMark`
("representative data, not live"); use it rather than inventing a second
convention.

### 6. Still gated: the firmware

The consistency pass on `/firmware` stays gated on the app being finished.
**Don't start it.** `docs/handoff6.md` §10 lists what the app has learned that
the firmware will owe it when it does begin.

## Please raise early, not at the end

- The wording for "taking-care-of" profiles.
- Whether I need to create the Supabase organisation myself.
- Resend authorisation.
- Whether the cloud work should be **additive** — with fall alerts and SOS
  staying BLE/SMS-direct so a safety-critical path never depends on a server. I
  think it should be, but say so if you disagree.
