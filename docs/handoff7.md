# SafeShade — Session Handoff #7

**Rewritten at the end of Phase 2 (v2.6.0) of the v2.5→v2.7 pass, 2026-09-07.** Like
handoff6 it is **self-contained**: a fresh session with no memory should be
able to start Phase 2 from this file, `DESIGN.md` and the code. Handoff6 is
still the record of everything that led here; where this file and handoff6
differ, this file wins.

## Contents

- [0. What to trust](#0-what-to-trust)
- [1. The pass, and where it stands](#1-the-pass-and-where-it-stands)
- [2. Decisions made with the user this pass](#2-decisions-made-with-the-user-this-pass)
- [3. What Phase 1 shipped](#3-what-phase-1-shipped)
- [4. Architecture, as it is now](#4-architecture-as-it-is-now)
- [5. The cloud, as it is now](#5-the-cloud-as-it-is-now)
- [6. Phase 2 — the brief](#6-phase-2--the-brief)
- [7. Phase 3 and Phase 4 — the brief](#7-phase-3-and-phase-4--the-brief)
- [8. Verified on the device, and not](#8-verified-on-the-device-and-not)
- [9. Known bugs, debt and firmware owes](#9-known-bugs-debt-and-firmware-owes)
- [10. Traps, and the tools that route around them](#10-traps-and-the-tools-that-route-around-them)
- [11. What the user still has to do](#11-what-the-user-still-has-to-do)
- [12. Phase 2 close: what shipped, what was not verified, and whether to continue](#12-phase-2-close)

---

## 0. What to trust

| File | Status |
|---|---|
| `DESIGN.md` | **Current.** Updated in the same commits as the changes it describes. Read it before inventing any visual decision. |
| `docs/handoff7.md` | This file. Current. |
| `docs/handoff6.md` | History and the full pitch-deck-versus-app matrix, the Shady inventory before this pass, the firmware ledger. Still accurate except where §3 below changed things. |
| `docs/icons-wanted.md` | The list of ~95 custom icons the user is collecting. **Do not build Material-icon replacements; wait for the drop.** |
| `docs/wizards/google-signin.md` | Step-by-step for the user to mint the Google Web OAuth client ID. |
| `supabase/README.md` | Deploying edge functions, secrets, Auth SMTP → Resend, pasting email templates. |
| `CLAUDE.md`, `PRODUCT.md`, `docs/SafeShadev21/SafeShadev21.ino` | **Not ours.** Modified/untracked by another session. Never commit, revert or edit them. `CLAUDE.md`'s BLE notes are still correct and load-bearing. |
| The plan file | `C:\Users\KIIT0001\.claude\plans\we-re-continuing-safeshade-at-synthetic-wreath.md` was the approved plan. §6–7 here supersede it where they differ. |

**Trust the code over any document.**

---

## 1. The pass, and where it stands

The user's brief for this pass is to **finish the app**: mimic the production
deployment so that when the hardware lands the only thing left is plugging it
in. Three phases, each ending green and committed, each writing this file.

- **Phase 1 — done** (v2.5.0, `versionCode 8`). Ten commits, `0b963f1` to
  `a3e54cb`, local by the user's choice.
- **Phase 2 — done in scope, see §12 for what was not verified** (v2.6.0,
  `versionCode 9`). Twenty-one commits, `4131176` onward, this file's own
  two included, **not pushed**
  (the user authorises every push explicitly). `assembleDebug` green,
  **265 unit tests** pass (74 at the end of Phase 1).
- **Phase 3 — next.** Vitals, evidence, smart home, mesh, OTA, Spark and
  pairing, the picked recommendations. §7.
- **Phase 4 — the all-screen polish sweep.** The user's own UI/UX list and
  the custom icon drop land here; both jump the queue when they arrive.

### Model allocation (a standing instruction)

Fable 5.1 orchestrates and does all UI/UX, all Shady, the hardest tasks. Opus 5
for backend, schema, sync, protocol, data model, migrations; Sonnet 5 for
ordinary implementation; Haiku 4.5 for greps, inventories, sweeps. Every
sub-agent gets a hard file scope and "no writing git command". `fork` ignores
`model`. Opus hit the account's session limit twice this phase and each time
died mid-edit; the orchestrator recovered the tree both times (§10).

### File ownership

Sub-agents own `data/`, `repo/`, `di/`, `ui/vm/`, `cloud/`, `service/`,
`platform/`, tests, gradle, manifest. The orchestrator owns `ui/nav/`,
`ui/screens/`, `ui/board/`, `ui/shady/`, `ui/theme/`, `DESIGN.md`, handoffs.
Cross-cutting changes are sequenced: data layer and VM land with tests green,
then screens.

---

## 2. Decisions made with the user this pass

All settled. Do not re-litigate.

- **"People I look after"** is the section title; the person is a **wearer**
  in prose; the group is the **Circle**. Continues `RoleForkScreen`'s "Someone
  I look after" / "Me".
- **Nothing in the UI says "planned", "coming soon", "representative",
  "simulated" or "not in this version."** `StubMark` is deleted. The app reads
  exactly as it will at launch. The outcome rule survives unchanged: absent
  data is a dash, a failed action shows its reason, no tick before a result.
  What the firmware does not honour lives in
  `DeviceCapabilities.awaitingFirmware` (to be created in Phase 2; today the
  ledger is §9) and in this file.
- **Cloud is strictly additive.** Local DataStore stays the source of truth;
  fall/SOS/SMS never wait on a server; the app is fully usable signed out.
  `CloudContainer` is the last field of `AppContainer`, after
  `appStateRepository`, and that position is the enforcement.
- **Sign-in:** email OTP (six digits typed in-app) + password, **Google full**
  once the user supplies a Web client ID, **Apple front-end** until an Apple
  Services ID exists (the button reports that reason when tapped).
- **Resend:** the user has no domain, so every send uses
  `onboarding@resend.dev`, which delivers **only to the Resend account owner's
  address**; every other recipient reports Failed with Resend's reason, which
  is the honest behaviour. Emails must be **branded** (templates exist, §5).
- **Supabase:** org **SafeShade**, project **SafeShade App**
  (`qlgbxhlbzyykxagsvwzv`, ap-southeast-1). The user re-authorised the claude.ai
  connector to include the org; the MCP sees it. The DB password was shared in
  chat once; the MCP never needs it; it is in no file.
- **The polish sweep is Phase 4.**
- **Icons:** the user collects custom SVGs from `docs/icons-wanted.md` into
  `docs/Icons/`; the swap happens when they land.
- **The Device page's top-right control is the person's face**, and it opens
  the Profile page, which took over the `settings` route.
- **Walkie-talkie / emergency calling** is in scope (the device will have
  independent calling, a microphone and a speaker). Phase 2.
- **Resend's verified domain `eisfoundation.com` belongs to an unrelated
  project and is never used for SafeShade** (user, 2026-09-07). The sender
  stays `onboarding@resend.dev`. The account owner is dibsganguly@gmail.com.
- **The escalation ladder is off until a person switches it on**, and the
  emergency rung never places a call by itself: the dialer opens with the
  number ready. Decided by the orchestrator on the advisor's finding and
  recorded in `EscalationRunner` and `EscalationSettings`; the user has
  not been asked and may reverse the default.
- **Battery in telemetry is fabricated by the firmware** (starts 85, −1 every
  180 s). Every surface showing it is faithfully showing a synthetic number;
  no label says so in the UI (rule above); it is recorded here.

---

## 3. What Phase 1 shipped

### 3.1 Shady — `ui/shady/` (commit `5609448`)

- **Cel-shaded depth** inside the draw code: three flat tones (rim band along
  the lit upper-left edges, skin, core band along the shadowed edges, each the
  body's silhouette minus the same silhouette pushed diagonally via
  `Path.op DIFFERENCE`), a flat contact ellipse on the ground the body lifts
  off in a hop, highlights on hands/legs/nub, a lid shadow in the eye whites.
  No gradient anywhere — `DESIGN.md` records this as the one depth carve-out,
  character only.
- **Turns rotate** (`ShadyPose.turn`): 80 ms to edge-on, flip at the narrowest
  point, 80 ms out; the antenna base slides to the centreline, eyes pull
  together. **Antenna secondary motion** (`AntennaLag`) is derived from the
  pose's own frame-to-frame movement plus the stage's `worldX`.
- **New vocabulary:** eyes `WINK`, `HEART`; mouths `TONGUE`, `WHISTLE`,
  `GRIN`; flourishes `BLUSH`, `NOTES`, `SWEAT`, `HEART`, `LEAF`, `STEAM`; moods
  `PROUD`, `SLEEPY`, `CHILLY` (mapped in `shadyMoodFor` from a fresh success,
  quiet hours, outside temperature; the Board feeds temperature). Twelve moods
  now.
- **Reactions:** subtle 18 (+`wink blush tiptoe headBob leanIn stretchTall`),
  bold 22 (+`moonwalk cartwheel juggle dance faint leapSpin slide pounce`);
  `spin` gained an anticipation crouch.
- **Props — `ShadyProps.kt`.** One on stage at a time for a scene of 6–12
  beats: `PLANT` (watered, a new leaf unfurls, admired with a drifting leaf),
  `CAT` (walks on, gets petted with heart eyes, rubs against him with a blush,
  walks off), `PHONE` (buzzes on its pad, he startles, reads it, nods),
  `KETTLE` (steams, whistles, he jumps, then warms his hands), `BALL` (nudged
  off the edge, shrug), `BOX` (peeks in, climbs in — the front face draws over
  him — pops out dizzy), `UMBRELLA` (only when the Board's weather says rain ≥
  50 %; held, drawn in front). The director walks up to a prop and never
  through it (`bodyHalf` is set by the stage from real pixels). Props never
  report state.

### 3.2 Adaptive-mode page — `ui/screens/device/` (commit `36dc02c`)

- `ModePickerScreen` is a **list**: the current profile as a plate with its
  scene, then seven rows (64 dp scene thumbnail, name, one line on who it is
  for, the seal). Tapping opens **`ModeDetailScreen`**: scene band, status
  plate, fall detection in real units (what it trips at in g at the device's
  current sensitivity with the three sensitivities spelled out, rotation check
  in °/s, stillness), what is on the wearable (screen cycle, home screen,
  lights, poll rate, how a fall is cancelled), the deck's priority features as
  rows linked to screens, what a locked profile hides, an "In short" bank, and
  one COMMIT button — the only place a profile is chosen.
  **`ModeCompareScreen`** lists every profile's four differing figures.
- **`ModeFacts.kt`** is the content, sourced from the firmware with line
  citations in KDoc: 70000/50000/35000 LSB by sensitivity (≈4.3/3.1/2.1 g),
  Bike floor 55000 + gyro 20000, Helmet 70000 + gyro 25000, Pet off, Elderly
  stillness, Kids 2 s poll. Where the deck and the firmware disagree, the
  firmware wins.
- **Defect fixed:** `SafeShadeViewModel.setActiveMode` only wrote to the BLE
  link; nothing anywhere called `ProfileRepository.setActiveMode`, so a mode
  chosen with no device in range vanished and the old picker's "confirmed"
  effect could never fire. It now stores first, writes only on a usable link,
  and returns the ack as a `Deferred<Boolean>`; the detail page shows the real
  outcome ("Stored for the Device" offline).

### 3.3 The cloud foundation — `cloud/`, `supabase/` (commits `38d4be8`, `e6b3c32`)

See §5.

### 3.4 Onboarding and the avatar kit (commit `de25e01`)

- **`ui/board/Avatar.kt`**: one string id → an initial on an accent disc
  (`""`), a procedural face (`a:f…-s…-h…`, `AvatarSpec`: 4 face shapes, 6 skin
  tones, 9 hair styles incl. headscarf, turban, cap, 6 hair colours, brows,
  eyes, mouths, glasses, an accessory, a backdrop from the twelve accents), or
  an app-private photo (`photo:<file>` under `files/avatars/`). `Avatar()`,
  `AvatarPresetStrip` (24 presets, chosen one ringed in brass),
  `AvatarCustomiser` (every option drawn as the whole face with only that
  attribute changed). Apple's Memoji was the reference; this is ours.
- **Onboarding** (`ui/screens/onboarding/`): six steps of one shape — a
  scene at the top (`OnboardingScenes.kt`: welcome walks Shady on to light a
  board lamp; the fork stands one Shady beside a cane and one beside a phone
  and brings the chosen side forward; the wearer step peeks at each face
  chosen; permissions light three lamps and end proud; pairing searches with
  rings until the wearable's nub answers; the card is held up), a rail of bus
  ticks, a headline, at most a line of copy, the controls, actions pinned at
  the foot rising with the keyboard. Scenes take positions from the real width
  and run under the status bar. The wearer step has the name field, the
  preset strip and "Make your own face".
- **Data:** `DeviceSettings.wearerAvatarId`; `ProfileSnapshot.ownerName` /
  `ownerAvatarId` (the account holder — a Companion is their own wearer and
  onboarding sets both); DTOs nullable; `ProfileRepository.setOwner`; VM
  `setWearer(name, avatarId)` (**one write**; see §9), `setOwner`;
  `AppState.Ready.ownerName/ownerAvatarId`.
- **Developer screen → "Replay onboarding"** (`setOnboardingSeen(false)`), and
  a skipped step no longer writes a blank name over a stored one.

### 3.5 Label removal (commit `f70a4b6`)

`StubMark` deleted; `Gauge.stub` gone; Silent SOS no longer says the device
is not quiet; the About screen's "Not in this version" section, its four
blocked-feature cards and `BlockedFeature` are gone, and its footnote no
longer claims "no account and no server". `DESIGN.md`'s Honesty Rules restated
around outcomes.

### 3.6 Profile page (commit `210de5e`)

`ui/screens/profile/`: `ProfileScreen` (identity plate; "People I look after"
for a Guardian with the wearer as a row; "This phone": role, appearance as a
collapsed section of three, alert reliability; About + Developer) on the
`settings` route, reached from the Device page's face. `ProfileEditScreen`
(name beside face, preset strip, "Use a photo" via the system photo picker with
an app-private 512 px copy, "Make your own", one COMMIT save).
`SettingsScreen.kt` keeps only the shared helper properties. Two defects fixed
(§9). The Device page's mains plate now feeds its Protecting bay.

---

## 4. Architecture, as it is now

Unchanged from handoff6 §4 except:

- `AppContainer` ends with `val cloud: CloudContainer` (§5).
- `ProfileSnapshot` carries `ownerName`/`ownerAvatarId`; `DeviceSettings`
  carries `wearerAvatarId`; both in `ProfileDto`/`DeviceSettingsDto`, nullable.
- Routes added: `DEVICE_MODE_DETAIL/{mode}`, `DEVICE_MODE_COMPARE`,
  `SETTINGS_PROFILE_EDIT/{target}` (`ProfileTarget.OWNER|WEARER`). `SETTINGS`
  renders `ProfileScreen`. Motion and tab mapping untouched.
- `ui/board/` gained `Avatar.kt`; `KitGallery` shows the 24 presets and the
  12 moods (wrapping at four).
- `tools/adb/` — the device-driving helpers (§10).
- **Dependencies added:** supabase-kt **3.2.6** (not 3.8.0: it pulls
  kotlin-reflect 2.4.0, two metadata versions above our 2.2.10 compiler),
  Ktor 3.4.3 (`ktor-client-okhttp`; Realtime needs WebSockets), OkHttp pinned
  4.12.0, kotlinx-serialization 1.9.0 with the plugin on the project's Kotlin.
  Gson and kotlinx.serialization never meet: `data/local/Dtos.kt` is Gson,
  `cloud/dto/` is `@Serializable`; a class carrying both is a review-blocker.
- `BuildConfig.SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID` come
  from `local.properties` (git-ignored; the first two are filled in on this
  machine, the third is blank). Blank ⇒ `CloudSession.Disabled` and the fake
  client; the app runs exactly as before.
- Manifest: `safeshade://login-callback` BROWSABLE filter on `MainActivity`;
  `ACCESS_NETWORK_STATE` (without it `Connectivity.start()` returns false and
  reconnect-triggered sync never arms).

---

## 5. The cloud, as it is now

**Package `cloud/`:** `CloudResult { Ok, Failed(reason, retryable), Disabled }`,
`CloudErrors.toCloudFailure()` (supabase/Ktor/socket exceptions → plain
English, never a raw message), `CloudSession { Disabled, Loading, Guest,
SignedIn }`, `CloudClient` (small interface: OTP request/verify, password
sign-in/up, id-token sign-in for Google/Apple, sign-out, delete account,
upsert/select/invoke/uploadPrivate/signedUrl), `SupabaseCloudClient` (real;
`handleDeepLink(intent)` exists for MainActivity to call — **not yet called**),
`FakeCloudClient` (in `main`, serves the kit gallery), `CloudAuth`,
`CloudContainer` (client → auth → outbox → syncEngine; picks the disabled fake
when config is blank). `cloud/dto/` rows for every table. `cloud/sync/`:
`SyncState { LocalOnly, Syncing, Synced(at), Failed(reason) }`,
`OutboxPolicy` (dedupe by table+record, backoff 2^(4n) s capped 15 min,
terminal at 5, cap 500), `Outbox` (DataStore file `safeshade_cloud`, keys
`outbox_json_v1`, `pull_cursors_json_v1` — a second delegate on
`safeshade_prefs` would throw), `Connectivity`, `SyncEngine` (single-flight
drain; triggers: `kick()`, app foreground, network callback; `PayloadSource` /
`PullSource` interfaces with no-op defaults — **Phase 2 implements them from
the repositories**).

**`SupabaseCloudClient` has executed zero calls.** Every signature was
compile-checked against the 3.2.6 jars; nothing has signed in yet.

**Schema — live on SafeShade App.** `supabase/migrations/0001_init.sql`
(check_function_bodies off for its duration; the `is_circle_*` SQL helpers are
defined before `circle_members` exists), `0002_advisor_fixes.sql`,
`0003_function_grants.sql`. 19 tables, all RLS on: `profiles`, `circles`,
`circle_members` (owner|guardian|viewer; membership owner-write with one
bootstrap clause; `accept_invite(token)` is the only other way in),
`wearers`, `devices`, `medical_ids`, `emergency_contacts`, `alerts`,
`alert_deliveries` (client read-only; status `queued|sent|failed|unknown`),
`messages`, `vitals_samples`, `evidence`, `zones`, `zone_events`,
`smart_home_hooks`, `subscriptions` (client read-only), `invites`,
`firmware_releases` (public read), `device_sightings` (any signed-in user may
insert; read by the device's circle). `heatmap_cells` materialized view
(1.1 km cells, k ≥ 5), refreshed hourly by pg_cron (present), served through
`heatmap_in(bbox)` to authenticated only. `delete_account()`. Buckets
`evidence`, `voice` (private), `firmware`, `avatars` (public). The security
advisor's remaining lines are the six intentional "signed-in users can execute
a SECURITY DEFINER function" entries listed in `0003`.

**Edge functions — written, not deployed:** `send-alert-email` (takes
`{alert_id}` only; recipients resolved server-side; `Idempotency-Key:
<alert_id>:<recipient>`; writes `alert_deliveries`), `send-invite`. Branded
templates in `supabase/functions/_shared/email/` (layout with the emblem
inlined at 3 KB; otp, magic-link, invite, alert, digest; `render.ts`,
`resend.ts`, `emblem.ts`). No `supabase/config.toml` yet (an unrecognised key
breaks every CLI command; the README carries the snippet).

### Added in Phase 2

- Migrations `0004` (realtime publication: alerts, messages), `0005`
  (`bootstrap_circle_for`, `ensure_own_circle`), `0006` (messages `kind`,
  `audio_path`, `duration_ms`, `waveform`; the voice bucket's missing UPDATE
  policy). All applied through the MCP and saved under `supabase/migrations/`.
- `CloudContainer` wires `RepositoryPayloadSource` / `RepositoryPullSource`
  over the repositories plus `VoiceNoteRepository`; `CircleManager` holds the
  circle id, members, invites, tier, the dev override, the backfill marker and
  `share_alert_places`; `VoiceCloud` uploads and downloads recordings.
- `PayloadSource.resolve` answers Row, Skip or Failed(reason); a skip is
  counted (three abandon the entry with a plain reason); a failed upload is an
  ordinary failure carrying the real reason.
- `SyncEngine` conflates drains requested mid-drain, arms a timer for the
  soonest backed-off entry, exposes `draining`, pulls `circle_members` and
  `subscriptions` whole, and logs under `SafeShadeSync` (`Log.w`, no PII).

---

## 6. Phase 2 — the brief, as shipped

Every numbered item of the original brief landed; the item numbers are kept so
the plan file and this list line up. Commits are on `master`, `v2.6.0:` prefixed.

1. **Resend.** Authorised, API key created, Auth SMTP pointed at Resend by the
   user, branded OTP/magic-link templates pasted, both edge functions
   deployed. The invite email was received, branded, at the owner's address.
   (`cc48ea9`)
2. **Multi-wearer model.** `Wearer`, DataStore v2→v3, address-bound devices,
   `contactsFor`, `resolveWearerForDevice`, all tested. (`0cc7868`, `64fdb99`)
3. **`DeviceCapabilities.awaitingFirmware`** holds the ledger. (`0cc7868`)
4. **Sign-in.** OTP, password, **Google end to end** (verified: `auth.users`
   provider google), Apple front-end. One Tap's cool-down falls back to the
   Sign in with Google chooser. Account page with the sync plate and "What
   leaves this phone". (`1ae5e11`, `9a0c1b5`, `2394597`)
5. **Sync.** Push from the repositories, pull with merge rules, realtime on
   alerts and messages, the Circle and tier from the server, backfill on first
   sign-in, skips counted, backoff retried on a timer, Sync Now that clears
   backoff, a 401 that refreshes the session once, every push logged under
   `SafeShadeSync`. `SyncDot` on trip and message rows. (`e8d3c9a`, `b7b7a9f`,
   `2394597`)
6. **Circle as a family dashboard**, People I look after, the wearer editor,
   Guardians and invitations with each recipient's real status. (`14af211`,
   `64fdb99`, `42e7f77`)
7. **Plans** Free / Plus / Pro from `subscriptions`, Play Billing's real
   answer, the developer override. (`efc9b66`, `0ccf810`)
8. **Heat map** with the community layer from `heatmap_in`, Plus-gated.
   (`efc9b66`, `0ccf810`)
9. **Nearest emergency services** from Overpass, cached with age, under the
   offline directory. `EXT ERS` designed and tested. (`4eb37b8`)
10. **Talk**: push-to-talk voice notes, `messages` kind `voice`, private
    `voice` bucket, waveform, per-note state; **Call the wearable** on the
    Circle plate. The device `VOICE` chunk path is designed and tested,
    `awaitingFirmware`. (`6879b98`, `284f4fc`, `ecc8b27`)
11. **Escalation ladder** with its page and the live plate on the trip;
    **out-of-reach and low-battery notices** with their page; **Privacy** on
    the Profile with the heat-map place switch. (`6879b98`, `0fce91d`,
    `284f4fc`, `ecc8b27`)
12. `DESIGN.md` carries every new kit member and pattern: Person row, Sync
    dot, Waveform, the ladder plate, and the pages.

**Not done from the brief's verification line**, carried to §12: RLS proven
with a second account; a voice note between two phones; the call to the
wearable's SIM; sync observed going to Failed in airplane mode.

---

## 7. Phase 3 and Phase 4 — the brief

**Phase 3, version 2.7.0:** vitals (`LiveSensorData` gains nullable HR/SpO₂/
temp + source; telemetry fields 7–9 designed; `safety/VitalsScreen.kt`;
**Health Connect** as the real source), mic evidence (phone side real:
`RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE` on 34+, 30 s default via
`DialControl`, opt-in on SOS/fall, playback on the trip, opt-in upload) and
loud-environment warnings (`AudioRecord` RMS → dB, uncalibrated and said so),
smart home (webhook automations real; Matter/Google/Alexa plates whose Connect
reports the real outcome), community last-seen via `device_sightings` and lost-
pet mode, OTA (`firmware_releases`, download + SHA-256 verify real, install
over a designed chunk path reporting the device's actual response, `EXT VER`
query designed), Spark (`DeviceModel { S1, FIVE_G, SPARK }`, brand-specific
pairing with drawn product silhouettes, distress recording, NFC tag write
where the phone has NFC — the test phone does not — anti-removal toggle), and
the picked recommendations: positioning-source readout, navigation revived
(`setNavTarget` is a live orphan), brake light and Elderly path light surfaced
(firmware does both), ride log on `JourneyService`, virtual leash on RSSI,
stranger-danger quiet word, Quick Settings SOS tile + shortcuts, Glance widget,
incident PDF, Open-Meteo UV/heat/cold and air-quality nudges, walk-home mode.
Phase 3 may checkpoint at a green sub-phase if context runs short.

**Phase 4:** the all-screen polish sweep as a redesign pass. Brief: pages are
too text heavy. Screenshots of all routes in both themes and at 1.3×; fewer
paragraphs, more instruments, plates and rows; dark theme on the map picker
and the intro; `ChipRow` focus ring; back-chevron/title overlap; `WhyDisclosure`
count; `OptionWay` and the check-in constants into `ui/board/`; dead
`ui/navigation/` and the stranded `ONBOARDING_RELIABILITY` route; the icon swap
once the SVGs land and `material-icons-extended` dropped.

---

## 8. Verified on the device, and not

Test device: Redmi Note 10S, MIUI 14, Android 13 (API 33), 1.0× font scale.
**No wearable was connected at any point in Phase 1 or Phase 2.** Everything
below was driven over adb with `tools/adb/tapfind.py` (exact text first, then
contains) and read back from uiautomator dumps and screenshots.

### Verified in Phase 2

- **Google sign-in** end to end: `auth.users` row with provider google; the
  session survives a reinstall; the Account page reads the address.
- **Sync on the wire**: a wearer push (wearers + medical_ids rows), pulls, the
  realtime websocket (101), `heatmap_in` 200, `send-invite` 200 with the
  branded mail delivered to the owner's address. After the backfill and
  Sync Now fixes: profiles, wearers ×2, medical_ids, emergency_contacts ×2,
  devices, zones, alerts ×5 pushed in eight seconds, the outbox empty, the
  server row's `updated_at` moved, `circle_state` on disk holding the owner.
- **Sync dots**: five "On SafeShade Cloud" rows on the trip log.
- **The Safety hub** with "If nobody answers" (reads Off after the mapper fix)
  and "Out of reach"; both pages open and render; the ladder preview shows
  the contact then 112 with the right gaps.
- **Talk**: the permission sheet on the first hold; a two-second note
  recorded, its waveform drawn, uploaded (`storage.objects` has the path) and
  its `messages` row written (kind voice, 40 bars, 2763 ms) in one drain;
  Play marks it Heard.
- **Privacy**: the share switch flips Off and On, the line changes each way,
  `share_alert_places_v1` lands in the store.
- People, the wearer editor, the family dashboard, Guardians, Plans, the heat
  map's own layer, nearest services — each opened and photographed earlier in
  the phase (see the commit messages for what each one saw).

### Not verified — say this to the user, plainly, every time

- **Anything that needs a wearable**: the out-of-reach and low-battery notices
  firing, the ladder against a real alert, the wearer bound by address, the
  trip-detail ladder plate (needs a run to draw), Call the wearable.
- **RLS with a second account.** The user offered to create a throwaway user;
  nobody has signed in with one. Accept-invite has not been exercised.
- **A voice note arriving on a second phone**, the download path, the
  twenty-second cap, the voice bucket's UPDATE policy under a retry.
- **The 401 retry.** The stuck entry's last error was the I/O sentence, not
  the session one, so the observed 401 may not have surfaced as a
  `RestException`; the hook compiles and is untested live. See §9.
- **Sync going to Failed in airplane mode** was not driven this phase.
- **OTP mail branding**: the 02:53 OTP was not in Resend's sent list (likely
  Supabase's built-in SMTP at that moment); ask the user whether the later
  ones arrived branded.
- **The Google One Tap fallback** compiled and installed; the phone turned out
  to be signed in already, so the chooser was never opened.
- **Dark theme**, **1.3× text**, **TalkBack** on every page added this phase.
- Everything carried from Phase 1's list (frame timing, permission dialogs in
  onboarding, API 34+ paths, a second density, the map pinch).

---

## 9. Known bugs, debt and firmware owes

### Fixed in Phase 2 (recorded so nobody re-diagnoses them)

- **Twelve records at "Syncing" with no POST in the server log.** Drains
  requested during a drain were dropped; on a cold start every backfill request
  arrived during the foreground drain's pulls. Conflated now.
- **"Reading the Circle…" forever.** `circle_members` was pulled with a cursor;
  a fresh process got nothing. Whole-table pull now.
- **A medical ID stuck for an afternoon.** Overnight the token expired, three
  requests met 401 in seven seconds, the entry backed off, no timer re-tried
  it, and the Sync Now button was greyed on "anything queued". Timer, real
  in-flight flag, Sync Now clears backoff.
- **Sync dots never lit.** The lookup key was `table/id`; the state map is
  keyed by record id.
- **The ladder was armed by default and would have placed a direct 112 call**
  with "Call after a fall" on. Off by default, emergency rung dialer-only.
- **A voice note would have read "nothing left to send"** after three skips;
  the resolver now answers Failed with the upload's reason instead.
- **One Tap NoCredential on a phone with an account**: the cool-down; the
  helper falls back to the Sign in with Google chooser.

### Fixed in Phase 1

- **Mode selection never persisted** (§3.2).
- **Two consecutive `launchIo` setters lose the first write:** each reads
  `appState.value` before the other lands. `setWearer(name, avatarId)` is the
  pattern — one mutate. Grep for pairs of `setX(); setY()` in the nav graph
  before adding more.
- **A skipped onboarding step wrote a blank name** over a stored one.
- **The Device page's Protecting bay was never fed** (`protectedName`).
- **Replaying onboarding restores the step you left**, not welcome; that is
  nav state restoration and is fine, but a fresh install starts at welcome.

### Carried debt

- `PayloadResolver.message()` writes `author_id = userId` on every push, so
  re-pushing a pulled row (text or voice) reattributes it. Pre-existing for
  text; now reachable for voice via Sync Now. Opus flagged it; not changed.
- `FallAlertEvent` has no coordinates, so `alerts.lat/lon` are never sent and
  the heat map has only ever seen the community's other apps. The place switch
  is written for the day the model gains them.
- `runAuthedOrFail` keys on `RestException` 401; if supabase-kt surfaces the
  expired token as an I/O error instead, the hook never fires. Verify with a
  token allowed to expire on purpose.
- `WearableWatchMarks.lastConnectedAt` only starts counting from this build,
  so "has not connected yet" reads on a phone that paired months ago.
- `syncKey(table, id)` ignores its table argument by design; see its KDoc.
- **A ladder rung's "Asked the dialer to open" is all `ActionResult.Started`
  proves**: `startActivity` did not throw. From `AlertActionReceiver` with the
  app in the background, API 33's background-activity restriction can refuse
  the start silently; the full-screen notification path exists for the alert
  itself, not for a rung. Phase 3's first live alert test must watch for a
  rung that reads Dialled with no dialer on screen.
- **The twenty-second cap on a voice note**: `MediaRecorder` stops itself at
  `MAX_DURATION_MS`, and the release then calls `stop()` on a recorder that
  has already stopped, which throws and reports Failed while the file sits on
  disk. Not driven; the specific failure to look for.

- `ui/navigation/` is empty; `ONBOARDING_RELIABILITY` is a route constant
  nothing registers (a `ReliabilityScreen` exists — decide, then act). Phase 4.
- `OptionWay` and the check-in constants live in screens, not the kit.
  `ChipRow` has no focus ring. Back chevron overlaps the title's first 8 dp.
  `WhyDisclosure` takes no count. Quiet hours not persisted app-side. All
  from handoff6 §9, all Phase 4.
- The stored emergency contact with eleven digits — needs the owner.
- `PlateField` is `internal` in `ui.screens.safety` and imported by onboarding
  and profile; it belongs in `ui/board/`.
- The screen-off timeout on the test phone was set to 600000 ms for capture
  runs and **not restored** (the previous value was not read first).
- `tools/gen_icons.py` is unchanged; `material-icons-extended` still a
  dependency until the icon drop.

### Firmware owes the app (for the gated `/firmware` pass — do not start it)

Everything in handoff6 §10 still stands. Added by this pass's reading of the
wearable and gateway firmware:

1. **Every `EXT` write ACKs, including unknown tags**, and firmware version is
   not exposed over BLE. So nothing can be capability-probed; the app keeps a
   static `awaitingFirmware` ledger instead. An `EXT VER` query and a negative
   ACK for unknown tags would let the app stop guessing.
2. **Battery in telemetry is simulated** (starts 85, −1 / 180 s). No
   low-battery alert exists.
3. **The alert chain is optimistic downstream too:** the wearable POSTs an
   empty `/alert`; the gateway returns `{"queued":true}` before sending, to one
   hardcoded number, with demo coordinates on GNSS failure and no DEMO label
   in the shipped SMS; `/messages` truncates multi-line SMS to line 1; no auth
   on any gateway endpoint. **The gateway antenna issue is still open** (zero
   satellites, "SUSPECT").
4. No vitals, microphone, sound-level, OTA partition, mesh role or silent-SOS
   path exists on the wearable; `FREEFALL_THRESH` is dead code (the deck's
   "3-layer" is a promise).
5. Wire formats the app will design and unit-test ahead of firmware:
   `EXT SOSRELAY` (with ack), `EXT ERS`, `EXT VER`, a `VOICE` chunk path, an
   `OTA` chunk path, telemetry fields 7–9 for vitals, a mesh advertisement
   payload.

---

## 10. Traps, and the tools that route around them

All of handoff6 §3 still applies (nested KDoc comments, `git add` pathspecs,
CRLF, non-ASCII in docs, big heredocs, `uiautomator` bounds under the IME,
MSYS path mangling, blind taps, no two-finger gestures, `font_scale`,
`Scaffold` bottom bars, drawables with margins, dead flags, SOS sends real
SMS). Added this pass:

- **`tools/adb/drive.sh` + `tapfind.py`** — `source` it, then `launch`,
  `tab device`, `tapfind "profile"`, `field "their name"`, `hideime`, `shot`.
  `tapfind` matches exact text/description first, then contains; rows announce
  as "Name, State, detail". The README in that folder lists the traps it
  routes around: taps before `waitapp` land on whatever was there; `keyevent 4`
  with no keyboard pops the screen; MIUI ignores `cmd uimode`; the photo
  picker is multi-select and needs Done; **never `adb pull /sdcard/`
  wholesale** (it pulled the user's whole card once; only ever pull named
  files).
- **The `Bash` tool's heredoc parser fails on large Python scripts with
  quotes** — write the script with `Write`, then run it. Three patches were
  lost to this before the rule stuck.
- **A Python regex in a triple-quoted string with `\[`** silently changes
  meaning when edited through `sed`; prefer index-based cuts
  (`s.index(...)`) for deleting a block.
- **The Supabase MCP is authorised per organisation.** A new org needs the
  claude.ai connector re-authorised with it included.
- **`apply_migration` runs in one transaction**; a SQL-language function that
  reads a table defined later fails at creation (`check_function_bodies`).
- **Postgres grants EXECUTE to PUBLIC at creation**; `revoke ... from anon`
  alone does nothing — revoke from PUBLIC first, then grant back.
- **A gauge's `stub` flag, a "planned" lamp, a "representative" caption** —
  none may return. The rule is in `DESIGN.md`.

---

## 11. What the user still has to do

- **Push.** Ten v2.5.0 and twenty-one v2.6.0 commits are local. Ask before
  pushing.
- **A second account** (a throwaway user in the Supabase dashboard, or any
  second Google account on another phone) to prove RLS and the accept-invite
  path. The invite email can only be delivered to the owner's address until
  SafeShade has a sending domain, so the second account will need the link
  forwarded.
- **Say whether the OTP emails arrive branded**, and whether to enable
  Supabase's leaked-password protection (an advisor finding, one toggle).
- **Decide the ladder's default** (off now; §2).
- **Icons** from `docs/icons-wanted.md` into `docs/Icons/`. Thirty SVGs have
  appeared there; the swap waits for the user's word.
- **The eleven-digit emergency contact** on the test phone.
- **Their UI/UX list**, which jumps the queue.

---

## 12. Phase 2 close

**What shipped** is §6, item by item; **what was not verified** is §8. The
single decision taken without the user is the ladder's off-by-default (§2);
everything else in §2 was theirs.

**Context.** This session was compacted once during the phase. The working
state that matters is in the commits, this file, `DESIGN.md` and the memory
directory; nothing is held only in the conversation.

**Continue or hand over.** Hand over. Phase 3 is a different shape of work
(vitals, evidence, mesh, OTA, pairing) and starts from §7 with a fresh
session reading this file first; the sub-agent allocation in §1 still holds.
Before Phase 3 opens, the cheapest high-value hour is the §11 second-account
check, because RLS is the one claim in this phase that has been made and never
tested.
