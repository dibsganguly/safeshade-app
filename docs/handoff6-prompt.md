# Session prompt — paste this into the new chat

Everything below the rule is the prompt. It is also the record of what the
v2.5.0 pass was asked to do.

Two notes for the human, not part of the prompt:

- **Do not run `/init`.** It writes `CLAUDE.md`, which is stale on purpose,
  modified in the working tree by another session, and explicitly not ours to
  touch. `docs/handoff6.md` does the job `/init` would do, better and with the
  device/firmware context `/init` cannot know about.
- Paste this with **plan mode on**, as intended. Item 4 of the prompt depends on
  it.

---

We're continuing **SafeShade** at `C:\Dev\SafeShade` — an Android app
(Kotlin + Jetpack Compose) paired with an ESP32 BLE wearable. The previous pass
shipped v2.4.0 and is fully committed and pushed.

## Phase 0 — orient before you touch anything

Read, in this order:

1. **`docs/handoff6.md`, all of it.** It is deliberately self-contained:
   architecture as it actually is, the working style, the environment traps, the
   Shady inventory, the pitch-deck-versus-app matrix, the verified/not-verified
   ledger, known bugs and the firmware gaps. Everything else on this list adds
   history and detail around it.
2. **`DESIGN.md`** — the design system. Current, and authoritative on visual
   decisions.
3. **`docs/handoff1.md` through `handoff5.md`** — the earlier passes, for the
   history handoff6 summarises rather than repeats.
4. **The context files** in the repo root and in `docs/`: `PRODUCT.md`,
   `docs/PROJECT_CONTEXT.md`, `docs/EC200U_Project_Notes_and_Troubleshooting.md`
   (~1650 lines, gateway hardware caveats including an antenna issue that may or
   may not still be open), and `CLAUDE.md` — which is **stale in places** (it
   describes a pre-ViewModel architecture) though its BLE notes are still
   correct and still load-bearing. `PRODUCT.md` is stale on two points: it says
   there is no ViewModel layer, and it makes "no cloud backend" a product
   principle. **Trust the code over any document.** Neither `CLAUDE.md` nor
   `PRODUCT.md` nor the firmware is yours to edit.
5. **Both firmwares**, so you know exactly what is on the other end of the link:
   `docs/SafeShadev21/SafeShadev21.ino` (the wearable, ~6,500 lines — the
   protocol ground truth) and `docs/SafeShade_Gateway/SafeShade_Gateway.ino`
   (the EC200U cellular/GNSS gateway, ~635 lines).
6. **`docs/SafeShade 5G Hackathon-Final1.pdf`** — the pitch deck, 18 slides, the
   product promise of record. Slide 4 is the feature grid, 11 the architecture
   and cloud platform, 12 Spark, 13 the per-mode table, 16 the cloud tiers.

**Delegate the expensive reads.** The wearable firmware alone is 288KB; hand it
to an Opus 5 sub-agent and ask for a protocol-and-behaviour digest rather than
spending your own context on 6,500 lines of Arduino. Same for the EC200U notes.
You need the conclusions, not the text.

## Model allocation — please respect this, it is about my usage limits

**You are Fable 5.1, and you orchestrate.** Do these yourself:

- **All UI/UX work** — everything on my list below, **plus a proactive sweep of
  every page and sub-page** that is not yet perfect. Every screen should look
  modern, sleek, interactive, intuitive, and like it belongs in 2026. I have a
  very keen eye and taste for UI/UX, so hold yourself to that standard rather
  than to "no longer broken".
- **All Shady work.**
- **The hardest tasks**, whatever they turn out to be.

**Delegate everything else to sub-agents, choosing the model by complexity:**
**Opus 5** for hard non-UI work (auth, database schema, protocol, data
modelling, tricky async, architectural analysis), **Sonnet 5** for ordinary
implementation and research, **Haiku 4.5** for mechanical and high-volume work
(greps, inventories, file sweeps, renames, doc extraction).

You are **completely free to spawn as many concurrent sub-agents as required**.
Give each a **hard file scope**, and tell each explicitly **not to run any
writing `git` command** — you handle every commit.

One mechanical note: `subagent_type: "fork"` ignores the `model` override, so
anything that must run on a specific model needs a fresh agent, not a fork.

## Ground rules — settled, please don't re-litigate

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
- Update `DESIGN.md` in the same pass as anything it describes.

## Structure the whole session as 3 high-level phases

Plan the entire scope below as **three high-level phases**, each with as many
sub-phases as it needs. The reason is practical: if the context window runs
short after phase 1 or 2, I want to move to a fresh session cleanly rather than
mid-thought. So:

- Each phase must **end at a coherent, committed, green state** — `assembleDebug`
  passing, tests passing, nothing half-wired.
- **Write or update `docs/handoff7.md` at the end of every phase**, not only at
  the end of the session, and include the not-verified list each time.
- Say at the end of each phase how much context is left and whether you'd rather
  continue or hand over.

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

### 4. Recommend me more features — in the plan itself

Since I'm pasting this in **plan mode**, the plan you come back with should
contain **both**: all three phases with their tasks, **and the full set of
feature recommendations**, grouped so I can pick from them. I'll tell you which
sets I want added to the scope right after I approve the plan.

I want both kinds of recommendation:

- **Features the pitch deck promises that the app doesn't have yet.**
  `docs/handoff6.md` §7 is a grounded deck-versus-app matrix — start there, and
  note the rows nobody has claimed at all yet, like the ERSS 112 JSON alert, 5G
  positioning, C-V2X and the Smart City API.
- **Features that aren't in the deck at all** but would be genuinely useful,
  impressive additions.

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

Read both firmwares in phase 0 so you know what you're building against, but
**the consistency pass on `/firmware` stays gated on the app being finished —
don't start it, and don't edit either `.ino`.** `docs/handoff6.md` §10 lists
what the app has already learned that the firmware will owe it when that pass
does begin.

## Please raise early, not at the end

- The wording for "taking-care-of" profiles.
- Whether I need to create the Supabase organisation myself.
- Resend authorisation.
- Whether the cloud work should be **additive** — with fall alerts and SOS
  staying BLE/SMS-direct so a safety-critical path never depends on a server. I
  think it should be, but say so if you disagree.
