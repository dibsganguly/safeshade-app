# Icons wanted for `docs/Icons/`

One SVG per line, Hugeicons stroke-rounded style like the 102 already there.
Any filename works; `tools/gen_icons.py` reads the directory. Where a Hugeicons
name is known it is given in brackets. The note says where the glyph is used.

## 1. Adaptive modes and device shapes (highest value — unblocks dropping `material-icons-extended`)

Sixteen distinct glyphs. Modes and device shapes that share a concept today
share a Material glyph; they should not, because on the Device page the mode
row and the device-shape row sit a few rows apart.

- [ ] elderly person with a cane (`elderly`) — Elderly mode
- [ ] walking cane on its own (`walking-stick` / `cane`) — Cane device shape
- [ ] child (`child` / `kid`) — Kids mode
- [ ] bicycle (`bicycle-01`) — Bike mode
- [ ] cycling helmet (`helmet`) — Helmet mode
- [ ] hard hat / cap (`cap` / `hard-hat`) — Hat device shape
- [ ] paw print (`paw`) — Pet mode
- [ ] pet collar with tag (`collar` / `dog-collar`) — Collar device shape
- [ ] wrist / hand raised (`hand-wrist` / `hand-pointing-up`) — Wrist mode
- [ ] wristwatch (`watch-01`) — Watch device shape
- [ ] backpack (`backpack-01`) — Backpack mode and Backpack device shape (one glyph is fine here; both mean the bag)
- [ ] umbrella (`umbrella`) — Umbrella device shape
- [ ] pendant / gem on a chain (`necklace` / `diamond-01`) — Pendant device shape
- [ ] a circular-arrow reload (`arrow-reload-horizontal` / `refresh`) — Board "Sync weather and location" and every re-sync control; the most reused glyph in the app after the chevron

## 2. Replacing the remaining Material glyphs

- [ ] history / clock with arrow (`clock-04` / `history`) — Trip log
- [ ] timer / hourglass (`timer-01`) — Journey overdue event in the trip log
- [ ] share (`share-08`) — Share the emergency card, share a trip
- [ ] QR code (`qr-code`) — Emergency card, Medical ID
- [ ] single person (`user`) — Role "Me", role screens
- [ ] add person (`user-add-01`) — Add emergency contact, invite to Circle
- [ ] remove person (`user-remove-01`) — Remove a member / contact
- [ ] delete / bin (`delete-02`) — Delete a zone, clear history
- [ ] close / small x (`cancel-01`) — Remove a number from the SMS allowlist
- [ ] plus / add (`add-01` / `plus-sign`) — Add a zone (today it reuses the emergency cross, which is wrong)
- [ ] enter a place (`login-03`) — Zone: alert on entry
- [ ] leave a place (`logout-03`) — Zone: alert on exit
- [ ] padlock (`lock` / `square-lock-02`) — The SEALED chip on guardian-locked rows
- [ ] walking person (`walking` / `running-shoes`) — Journey, Walk with me
- [ ] bell (`notification-01`) — Notifications permission and reliability check
- [ ] bolt / flash (`flash`) — Battery optimisation row
- [ ] restart arrow (`restart` / `rotate-01`) — Autostart permission row
- [ ] full-screen corners (`full-screen`) — Full-screen alert permission row
- [ ] flask (`flask` / `test-tube`) — Developer scenarios
- [ ] code brackets (`source-code`) — Developer entry
- [ ] sun (`sun-03`) — Light theme
- [ ] moon (`moon-02`) — Dark theme
- [ ] half sun/moon or phone (`smart-phone-01`) — Follow system theme

## 3. Profile, account and cloud (Phase 1–2)

- [ ] user circle (`user-circle`) — Profile avatar placeholder and the Device page's top-right entry
- [ ] camera (`camera-01`) — Take a profile photo
- [ ] image / gallery (`image-01`) — Pick a profile photo
- [ ] pencil (`pencil-edit-02`) — Edit name, avatar, wearer
- [ ] mail (`mail-01`) — Email sign-in, invites
- [ ] key (`key-01`) — Password
- [ ] six-dot code / passcode (`password-validation` / `pin-code`) — One-time code entry
- [ ] sign in (`login-01`) — Sign in row
- [ ] sign out (`logout-01`) — Sign out row
- [ ] group of people (`user-group`) — People I look after, Circle members, family dashboard
- [ ] cloud with check (`cloud-saving-done-01` exists) — synced; **also wanted:** cloud with arrow up (`cloud-upload`) for pending, cloud with slash (`cloud-off`) for saved-on-this-phone-only, cloud with alert (`cloud-error`) for failed
- [ ] crown or star tiers (`crown`, `star` exists) — Plus / Pro plan
- [ ] credit card (`credit-card`) — Payment
- [ ] receipt / invoice (`invoice-01`) — Billing history
- [ ] shield with lock (`shield-key` / `security-lock`) — Privacy dashboard
- [ ] file export (`file-export`) — Export my data
- [ ] file with PDF (`pdf-01`) — Incident report
- [ ] eye off (`view-off`) — Hidden / never leaves the phone
- [ ] Google "G" and Apple mark — sign-in buttons (brand marks, monochrome outline versions)

## 4. Health, evidence and sound (Phase 3)

- [ ] heart with pulse (`heart-check` / `favourite` exists as a heart) — Heart rate
- [ ] blood drop / oxygen (`blood` / `droplet`) — SpO₂
- [ ] thermometer (`thermometer`) — Body temperature
- [ ] sleep / bed (`bed` / `sleeping`) — Sleep tracking
- [ ] footsteps (`footprint`) — Steps / activity
- [ ] microphone (`mic-01`) — Voice evidence recording, push-to-talk
- [ ] microphone off (`mic-off-01`) — Recording disabled
- [ ] waveform (`audio-wave-01`) — A recorded clip, playback row
- [ ] ear / loud (`ear` or `volume-high`) — Loud-environment warning
- [ ] telephone (`call`) — Call the wearable, call a contact
- [ ] walkie-talkie / radio handset (`walkie-talkie`) — Talk feature
- [ ] first-aid / hospital cross (`hospital-01` / `first-aid-kit`) — Nearest hospital
- [ ] police cap / badge (`police-badge`) — Nearest police station
- [ ] fire truck / flame (`fire-extinguisher` / `fire` exists as a light pattern) — Nearest fire station
- [ ] pharmacy / pill (`medicine-01`) — Nearest pharmacy

## 5. Smart home, updates, products and the picked features (Phase 3)

- [ ] house with signal (`smart-home` / `home-wifi`) — Smart home screen
- [ ] light bulb (`bulb`) — Night light rule
- [ ] plug / socket (`plug-socket`) — An appliance
- [ ] link / webhook (`link-04` / `webhook`) — Webhook automation
- [ ] chip / firmware (`cpu`) — Firmware version, updates
- [ ] download to device (`download-04`; `hard-drive-download` exists) — Get the update
- [ ] tag / version (`tag-01`) — Release version
- [ ] network nodes (`hierarchy` / `share-knowledge`) — Community relay, mesh
- [ ] radar / broadcast (`radar-01` / `signal`) — Community last-seen, positioning source
- [ ] ring (`ring` / a plain torus) — SafeShade Spark
- [ ] NFC symbol (`nfc`) — NFC Medical ID tag writing
- [ ] shield with slash / removal (`shield-off` / `unlink`) — Anti-removal alert
- [ ] route / turn arrow (`route-01` / `navigation-03`) — Navigation destination
- [ ] speedometer (`dashboard-speed-01`) — Ride stats
- [ ] leash / rope (`leash` or `rope`) — Virtual leash
- [ ] lost pet / paw with question (`paw` + `help-circle` exists) — Lost pet mode
- [ ] chat with lock (`message-lock-01`) — Quiet word (stranger danger)
- [ ] widget / grid tile (`dashboard-square-01`) — Home-screen widget
- [ ] tile / toggle (`toggle-on`) — Quick Settings tile
- [ ] sun with UV (`sun-cloud-01` / `uv-index`) — UV nudge
- [ ] hot / heat wave (`temperature-hot` / `heat`) — Heat nudge
- [ ] snowflake (`snow`) — Cold nudge
- [ ] wind / air (`wind` / `air-quality`) — Air quality
- [ ] sunset (`sunset`) — Walk-home mode
- [ ] alarm clock (`alarm-clock`) — Device offline too long
- [ ] battery with alert (`battery-warning` exists) — Low battery alert (already have)

## 6. Onboarding (Phase 1)

- [ ] sparkle / welcome (`sparkles`) — Welcome step
- [ ] two-way fork (`git-fork` / `route-02`) — "Who is this for?" fork
- [ ] bluetooth (`bluetooth`) — Permission row and pairing
- [ ] location pin with check (`location-check-01`; `pin-location` exists) — Location permission

Total wanted: about 95. The Material dependency can be dropped as soon as
section 1 and section 2 are in.
