# Setting up Google sign-in

Follow this once. It takes about twenty minutes, most of which is waiting for
Google's consent screen form.

Nothing here has been done yet — there is no Google Cloud project, no OAuth
client, and `GOOGLE_WEB_CLIENT_ID` is not in `local.properties`. Until it is,
`BuildConfig.GOOGLE_WEB_CLIENT_ID` is blank and the app simply does not offer
the Google button.

**Do this after** `supabase/README.md` steps 1–3, because step 8 needs the
Supabase project to exist.

---

## The one thing that confuses everybody

You will create **two** OAuth client IDs, and you will paste the **Web** one
into both Supabase and the Android app.

That is not a typo, and it is the mistake this page exists to prevent.

- The **Android** client ID exists so Google will *issue* a token to your app,
  identified by its package name and signing certificate. You never type its
  value anywhere.
- The **Web** client ID is the *audience* of the ID token — the `aud` claim
  Supabase checks. So Supabase needs it, and Credential Manager on the phone
  needs to request a token with that audience, which means the app needs it too.

Pasting the Android client ID into either place produces
`Unacceptable audience in id_token`, which sounds like a signing problem and is
not.

---

## 1. Get the debug signing fingerprint first

You need the SHA-1 of the debug keystore for step 6. Get it now so you are not
hunting for it half-way through a form.

On Windows, in **Git Bash** (this repo's shell), the keystore is at
`%USERPROFILE%\.android\debug.keystore`:

```bash
keytool -list -v \
  -keystore "$USERPROFILE/.android/debug.keystore" \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

Or in **PowerShell**:

```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Copy the line beginning `SHA1:` — the colon-separated hex, e.g.
`A1:B2:C3:...`.

**Notes**

- The path is `.android`, **not** `.keytool`. On this machine the file is
  confirmed present at `C:\Users\KIIT0001\.android\debug.keystore`.
- `android` is the standard password for the debug keystore for every Android
  developer on earth; it is not a secret and it is not yours.
- If `keytool` is not on your PATH, it ships with the JDK inside Android Studio:
  `"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"`.
- If the file does not exist, build the app once (`gradlew.bat assembleDebug`)
  — the debug keystore is generated on first use.
- **This fingerprint is for debug builds only.** A release APK is signed with a
  different key and needs its own Android OAuth client with that key's SHA-1, or
  Google sign-in works in development and fails for every real user.

## 2. Create a Google Cloud project

1. Go to <https://console.cloud.google.com/>.
2. Click the project dropdown in the top bar → **New Project**.
3. Name it `SafeShade`. Leave the organisation as-is.
4. **Create**, then make sure it is the selected project in the top bar before
   doing anything else. Configuring the wrong project is the second most common
   way this goes wrong.

## 3. Open the OAuth setup

In the left menu: **APIs & Services → OAuth consent screen**.

(Google has been migrating this into **Google Auth Platform**; if you land
there instead, the fields are the same and in the same order.)

## 4. Fill in the consent screen

1. **User Type**: **External**. (Internal only exists if you have a Google
   Workspace organisation, and it restricts sign-in to that organisation.)
2. **App name**: `SafeShade`
3. **User support email**: your own address.
4. **App logo**: optional. Uploading one triggers a verification review, so
   skip it while developing.
5. **Developer contact information**: your address again.
6. **Save and Continue**.
7. **Scopes**: change nothing. Sign-in needs only the default
   `openid`, `email`, `profile`. **Save and Continue**.
8. **Test users**: while the app is in *Testing*, only the addresses listed here
   can sign in. **Add your own Google address, and every address you will test
   with.** A "this app is blocked" or "access denied" error at sign-in is almost
   always a missing test user rather than a broken client id.
9. **Save and Continue** → **Back to Dashboard**.

You do **not** need to publish the app to test it. Leave it in *Testing*.

## 5. Create the **Web** OAuth client ID

1. **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
2. **Application type**: **Web application**.
3. **Name**: `SafeShade Web (Supabase audience)` — naming it so nobody later
   deletes it thinking it is unused.
4. **Authorised redirect URIs** → **Add URI**, and paste your Supabase callback:

   ```
   https://<your-project-ref>.supabase.co/auth/v1/callback
   ```

   Replace `<your-project-ref>` with the ref from your Supabase project URL. It
   must be exact — no trailing slash.
5. **Create**.
6. Copy the **Client ID** (it ends in `.apps.googleusercontent.com`) and the
   **Client secret**. Keep both; you need the ID twice and the secret once.

## 6. Create the **Android** OAuth client ID

1. **Create Credentials → OAuth client ID** again.
2. **Application type**: **Android**.
3. **Name**: `SafeShade Android (debug)`.
4. **Package name**: `com.safeshade`
   (this is `namespace` / `applicationId` in `app/build.gradle.kts`).
5. **SHA-1 certificate fingerprint**: paste the value from step 1.
6. **Create**.

You will never type this client's ID anywhere. Its whole job is to authorise
your package-and-certificate pair to obtain tokens. Repeat this step later with
the release keystore's SHA-1 before shipping.

## 7. Enable Google in Supabase

1. Supabase dashboard → **Authentication → Providers → Google**.
2. Turn **Enable Sign in with Google** on.
3. **Client ID (for OAuth)**: the **Web** client ID from step 5.
4. **Client Secret (for OAuth)**: the Web client secret from step 5.
5. There is also an **Authorized Client IDs** / *Client IDs* box for native
   sign-in. Put the **Web** client ID there as well — that is the audience
   Supabase will accept in an ID token minted on the phone.
6. **Save**.

## 8. Put the Web client ID in `local.properties`

In the repo root, in `local.properties` (git-ignored, alongside the Supabase
keys from `supabase/README.md`):

```properties
GOOGLE_WEB_CLIENT_ID=1234567890-abcdefghijklmnop.apps.googleusercontent.com
```

Then rebuild:

```bash
gradlew.bat assembleDebug
```

`app/build.gradle.kts` reads it through `prop()` into
`BuildConfig.GOOGLE_WEB_CLIENT_ID`. Blank means the app does not offer the
Google button at all, which is the correct behaviour on a machine that has not
been through this page.

Again: this is the **Web** client ID, in an Android app. See the box at the top.

---

## What still has to be built (Phase 2)

This page sets up the accounts and the configuration. The app side is not
wired yet:

- The Credential Manager call that actually asks Google for an ID token
  (`androidx.credentials` plus `googleid`) is **not a dependency of this build**
  and no screen requests one.
- `CloudClient.signInWithIdToken(IdProvider.GOOGLE, idToken, nonce)` exists and
  is implemented against supabase-kt, so once a token is obtained the exchange
  is one call.
- **The nonce matters.** Credential Manager takes a raw nonce; Google returns it
  hashed inside the token and Supabase compares the two. Pass the *same raw*
  nonce to `signInWithIdToken`. Omitting it when one was used fails the exchange
  with a message about the token being invalid, which sends people looking at
  their client IDs instead.

## When it does not work

| Symptom | Cause |
|---|---|
| `Unacceptable audience in id_token` | The Android client ID was used instead of the Web one, in Supabase or in the Credential Manager request. See the box at the top. |
| `Access blocked: SafeShade has not completed the Google verification process` | The signing-in address is not in the **Test users** list from step 4. |
| `redirect_uri_mismatch` | The redirect URI in step 5 does not exactly match the Supabase callback — usually a wrong project ref or a trailing slash. |
| No account picker appears, no error | Google Play Services has no signed-in account on the device, or the SHA-1 in step 6 does not match the key the installed APK was signed with. |
| Works on your phone, fails for testers | Release builds are signed with a different key. Add a second Android OAuth client with the release SHA-1. |
